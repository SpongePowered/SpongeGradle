/*
 * This file is part of SpongeGradle, licensed under the MIT License (MIT).
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

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.spongepowered.gradle.common.Constants;
import org.spongepowered.gradle.plugin.task.worker.WritePluginMetadataTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpongePluginGradle implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        project.getRepositories().maven(r -> r.setUrl(Constants.Repositories.SPONGE));
        final SpongePluginExtension sponge = project.getExtensions().create("sponge", SpongePluginExtension.class, project);
        final Provider<Directory> generatedResourcesDirectory = project.getLayout().getBuildDirectory().dir("generated/sponge/plugin");

        project.getPlugins().withType(JavaPlugin.class, v -> project.getExtensions().getByType(SourceSetContainer.class)
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME).getResources().srcDir(generatedResourcesDirectory));

        project.getTasks().register("writePluginMetadata", WritePluginMetadataTask.class, task -> {
            task.getConfigurations().addAll(sponge.plugins());
            task.getOutputDirectory().set(generatedResourcesDirectory);
        });

        final Configuration spongeRuntime = project.getConfigurations().create("spongeRuntime").defaultDependencies(a -> {
            a.add(project.getDependencies().create(Constants.Dependencies.SPONGE_GROUP + ":" + sponge.platform().get().artifactId() + ":" +
                "1.16.5-" + sponge.version().get() + "-RC+:universal"));
        });

        spongeRuntime.attributes(a -> {
            a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
            a.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
            a.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
        });

        project.getTasks().register("runServer", JavaExec.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.setDescription("Run a Sponge server to test this plugin");
            task.setStandardInput(System.in);

            final FileTree spongeRuntimeFiles = spongeRuntime.getIncoming().getFiles().getAsFileTree();
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
    }
}
