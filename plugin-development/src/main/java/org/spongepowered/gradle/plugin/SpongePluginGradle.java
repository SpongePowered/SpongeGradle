/*
 * This file is part of spongegradle-plugin-development, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.gradle.plugin;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.spongepowered.gradle.common.Constants;
import org.spongepowered.gradle.plugin.task.WritePluginMetadataTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpongePluginGradle implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        project.getLogger().lifecycle("SpongePowered Plugin 'GRADLE' Toolset Version '{}'", Constants.VERSION);
        project.getRepositories().maven(r -> r.setUrl(Constants.Repositories.SPONGE));
        project.getPlugins().apply(JavaLibraryPlugin.class);

        final SpongePluginExtension sponge = project.getExtensions().create("sponge", SpongePluginExtension.class, project);

        final Provider<Directory> generatedResourcesDirectory = project.getLayout().getBuildDirectory().dir("generated/sponge/plugin");

        final TaskProvider<WritePluginMetadataTask> writePluginMetadata = project.getTasks().register("writePluginMetadata", WritePluginMetadataTask.class, task -> {
            task.getConfigurations().addAll(sponge.plugins());
            task.getOutputDirectory().set(generatedResourcesDirectory);
        });

        final NamedDomainObjectProvider<Configuration> spongeApi = project.getConfigurations().register("spongeApi", config -> config.defaultDependencies(deps -> {
            if (sponge.version().isPresent()) {
                deps.add(project.getDependencies().create(Constants.Dependencies.SPONGE_GROUP + ":spongeapi:" + sponge.version().get() + "-SNAPSHOT"));
            }
        }));

        project.getDependencies().getComponents().withModule("org.spongepowered:spongevanilla", SpongeVersioningMetadataRule.class);

        final NamedDomainObjectProvider<Configuration> spongeRuntime = project.getConfigurations().register("spongeRuntime", conf -> {
            conf.defaultDependencies(a -> {
                final Dependency dep = project.getDependencies().create(
                    Constants.Dependencies.SPONGE_GROUP
                        + ":" + sponge.platform().get().artifactId()
                        + ":+:universal");

                /*if (dep instanceof ModuleDependency) {
                    ((ModuleDependency) dep).getAttributes().attribute(SpongeVersioningMetadataRule.API_TARGET, sponge.version().get());
                }*/

                a.add(dep);
            });
        });

        project.afterEvaluate(a -> {
            if (sponge.version().isPresent()) {
                project.getLogger().lifecycle("SpongeAPI '{}' has been set within the 'spongeApi' configuration. 'runClient' and 'runServer'"
                    + " tasks will be available. You may use these to test your plugin.", sponge.version().get());

                spongeRuntime.configure(config -> {
                    config.getAttributes().attribute(SpongeVersioningMetadataRule.API_TARGET, sponge.version().get());
                });
            } else {
                project.getLogger().lifecycle("SpongeAPI version has not been set within the 'sponge' configuration via the 'version' task. No "
                    + "tasks will be available to run a client or server session for debugging.");
            }
        });

        final TaskProvider<JavaExec> runServer = project.getTasks().register("runServer", JavaExec.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.setDescription("Run a Sponge server to test this plugin");
            task.setStandardInput(System.in);

            final Provider<FileCollection> spongeRuntimeFiles = spongeRuntime.map(c -> c.getIncoming().getFiles());
            task.getInputs().files(spongeRuntimeFiles);
            task.classpath(spongeRuntimeFiles);
            task.getMainClass().set(sponge.platform().get().mainClass());
            final Directory workingDirectory = project.getLayout().getProjectDirectory().dir("run");
            task.setWorkingDir(workingDirectory);

            task.doFirst(a -> {
                final Path path = workingDirectory.getAsFile().toPath();
                try {
                    Files.createDirectories(path);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        project.getPlugins().withType(JavaPlugin.class, v -> {
            project.getExtensions().getByType(SourceSetContainer.class).named(SourceSet.MAIN_SOURCE_SET_NAME, s -> {
                    s.getResources().srcDir(generatedResourcesDirectory);

                    project.getTasks().named(s.getProcessResourcesTaskName()).configure(processResources -> processResources.dependsOn(writePluginMetadata));
                });

            runServer.configure(it -> it.classpath(project.getTasks().named(JavaPlugin.JAR_TASK_NAME))); // TODO: is there a sensible way to run without the jar?
        });

        project.getPlugins().withType(JavaLibraryPlugin.class, v -> project.getConfigurations().named(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME)
            .configure(config -> config.extendsFrom(spongeApi.get())));
    }
}
