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

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
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
import java.util.Collections;

public final class SpongePluginGradle implements Plugin<Project> {

    private @MonotonicNonNull Project project;

    @Override
    public void apply(final Project project) {
        this.project = project;

        project.getLogger().lifecycle("SpongePowered Plugin 'GRADLE' Toolset Version '{}'", Constants.VERSION);
        project.getPlugins().apply(JavaLibraryPlugin.class);
        // project.getPlugins().apply(ProvideMinecraftPlugin.class);
        // project.getPlugins().apply(IdeaExtPlugin.class);
        // project.getPlugins().apply(EclipsePlugin.class);

        // final MinecraftExtension minecraft = project.getExtensions().getByType(MinecraftExtension.class);
        final SpongePluginExtension sponge = project.getExtensions().create("sponge", SpongePluginExtension.class, project);

        this.configurePluginMetaGeneration(sponge);

        this.addApiDependency(sponge);
        final NamedDomainObjectProvider<Configuration> spongeRuntime = this.addRuntimeDependency(sponge);
        final TaskProvider<JavaExec> runServer = this.createRunTask(spongeRuntime, sponge);

        project.afterEvaluate(a -> {
            if (sponge.apiVersion().isPresent()) {
                // TODO: client run task
                /*project.getLogger().lifecycle("SpongeAPI '{}' has been set within the 'spongeApi' configuration. 'runClient' and 'runServer'"
                    + " tasks will be available. You may use these to test your plugin.", sponge.version().get());*/

                spongeRuntime.configure(config -> {
                    config.getAttributes().attribute(SpongeVersioningMetadataRule.API_TARGET, sponge.apiVersion().get());
                });
            } else {
                project.getLogger().info("SpongeAPI version has not been set within the 'sponge' configuration via the 'version' task. No "
                    + "tasks will be available to run a client or server session for debugging.");
                runServer.configure(t -> t.setEnabled(false));
            }
            if (sponge.injectRepositories().get()) {
                project.getRepositories().maven(r -> {
                    r.setUrl(Constants.Repositories.SPONGE);
                    r.setName("sponge");
                });
            }
        });
    }

    private void addApiDependency(final SpongePluginExtension sponge) {
        // SpongeAPI dependency
        final NamedDomainObjectProvider<Configuration> spongeApi = this.project.getConfigurations()
            .register("spongeApi", config -> config.defaultDependencies(deps -> {
            if (sponge.apiVersion().isPresent()) {
                deps.add(
                    this.project.getDependencies().create(
                        Constants.Dependencies.SPONGE_GROUP
                            + ":" + Constants.Dependencies.SPONGE_API
                            + ":" + sponge.apiVersion().get() + "-SNAPSHOT"
                    )
                );
            }
        }));

        // Add SpongeAPI as a dependency
        this.project.getPlugins().withType(JavaLibraryPlugin.class, v ->
            this.project.getConfigurations().named(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME)
                .configure(config -> config.extendsFrom(spongeApi.get())));
    }

    private NamedDomainObjectProvider<Configuration> addRuntimeDependency(final SpongePluginExtension sponge) {
        this.project.getDependencies().getComponents().withModule("org.spongepowered:spongevanilla", SpongeVersioningMetadataRule.class);
        return this.project.getConfigurations().register("spongeRuntime", conf -> {
            conf.defaultDependencies(a -> {
                final Dependency dep = this.project.getDependencies().create(
                    Constants.Dependencies.SPONGE_GROUP
                        + ":" + sponge.platform().get().artifactId()
                        + ":+:universal");

                a.add(dep);
            });
        });
    }

    private TaskProvider<JavaExec> createRunTask(final NamedDomainObjectProvider<Configuration> spongeRuntime, final SpongePluginExtension sponge) {
        // Dev server run configurations

        /*minecraft.version().set(spongeRuntime.map(conf -> {
            final ArtifactCollection collection = conf.getIncoming().artifactView(viewSpec -> viewSpec.componentFilter(id -> id instanceof ModuleComponentIdentifier && ((ModuleComponentIdentifier) id).getModule().equals(sponge.platform().get().artifactId()))).getArtifacts();
            for (final ResolvedArtifactResult result : collection) {
                final String version = result.getVariant().getAttributes().getAttribute(SpongeVersioningMetadataRule.MINECRAFT_TARGET);
                if (version != null) {
                    return version;
                }
            }
            throw new GradleException("Could not determine selected Minecraft version");
        }));
        minecraft.version().finalizeValueOnRead();*/

        /* TODO: fixing SV metadata so these work
        // Because we have VanillaGradle, we can use dev environment-like run configurations
        // These are defined the same way they are in SpongeVanilla
        minecraft.getRuns().server(server -> {
            server.mainClass("org.spongepowered.vanilla.applaunch.Main");
            server.args("--nogui", "--launchTarget", "sponge_server_dev");
            server.classpath().from(spongeRuntime, spongeApi);
        });

        minecraft.getRuns().client(client -> {
            client.mainClass("org.spongepowered.vanilla.applaunch.Main");
            client.args("--launchTarget", "sponge_client_dev");
            client.classpath().from(spongeRuntime, spongeApi);
        });

         */
        final TaskProvider<JavaExec> runServer = this.project.getTasks().register("runServer", JavaExec.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.setDescription("Run a Sponge server to test this plugin");
            task.setStandardInput(System.in);

            final Provider<FileCollection> spongeRuntimeFiles = spongeRuntime.map(c -> c.getIncoming().getFiles());
            task.getInputs().files(spongeRuntimeFiles);
            task.classpath(spongeRuntimeFiles);
            task.getMainClass().set(sponge.platform().get().mainClass());
            final Directory workingDirectory = this.project.getLayout().getProjectDirectory().dir("run");
            task.setWorkingDir(workingDirectory);

            // Register the javaagent
            task.getJvmArgumentProviders().add(() -> {
                for (final ResolvedArtifactResult dep : spongeRuntime.get().getIncoming().artifactView(view -> view.setLenient(true)).getArtifacts()) {
                    final ComponentIdentifier id = dep.getVariant().getOwner();
                    task.getLogger().debug("Inspecting artifact {}", id);
                    if (id instanceof ModuleComponentIdentifier) {
                        final ModuleComponentIdentifier moduleId = (ModuleComponentIdentifier) id;
                        if (moduleId.getGroup().equals(Constants.Dependencies.SPONGE_GROUP)
                            && moduleId.getModule().equals(sponge.platform().get().artifactId())) {
                            task.getLogger().info("Using file {} as Sponge agent", dep.getFile());
                            return Collections.singletonList("-javaagent:" + dep.getFile());
                        }
                    }
                }
                task.getLogger().error("Failed to find a java agent!");
                return Collections.emptyList();
            });

            task.doFirst(a -> {
                final Path path = workingDirectory.getAsFile().toPath();
                try {
                    Files.createDirectories(path);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        this.project.getPlugins().withType(JavaPlugin.class, v -> {
            // TODO: Use shadow jar here if that plugin is applied
            runServer.configure(it -> it.classpath(this.project.getTasks().named(JavaPlugin.JAR_TASK_NAME))); // TODO: is there a sensible way to run without the jar?
        });

        return runServer;
    }

    private void configurePluginMetaGeneration(final SpongePluginExtension sponge) {
        // Configure some useful default values
        sponge.plugins().configureEach(plugin -> {
            plugin.getDisplayName().convention(this.project.provider(this.project::getName));
            plugin.getVersion().convention(this.project.provider(() -> String.valueOf(this.project.getVersion())));
            plugin.getDependencies().matching(dep -> dep.getName().equals(Constants.Dependencies.SPONGE_API))
                .configureEach(dep -> dep.getVersion().convention(sponge.apiVersion()));
        });

        // Then configure the generated sources
        final Provider<Directory> generatedResourcesDirectory = this.project.getLayout().getBuildDirectory().dir("generated/sponge/plugin");

        final TaskProvider<WritePluginMetadataTask> writePluginMetadata = this.project.getTasks().register("writePluginMetadata", WritePluginMetadataTask.class, task -> {
            task.getConfigurations().addAll(sponge.plugins());
            task.getOutputDirectory().set(generatedResourcesDirectory);
        });

        this.project.getPlugins().withType(JavaPlugin.class, v -> {
            this.project.getExtensions().getByType(SourceSetContainer.class).named(SourceSet.MAIN_SOURCE_SET_NAME, s -> {
                s.getResources().srcDir(generatedResourcesDirectory);

                this.project.getTasks().named(s.getProcessResourcesTaskName()).configure(processResources -> processResources.dependsOn(writePluginMetadata));
            });
        });
    }

}
