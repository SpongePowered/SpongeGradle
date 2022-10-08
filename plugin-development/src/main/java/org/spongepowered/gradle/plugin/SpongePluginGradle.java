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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.process.CommandLineArgumentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.spongepowered.gradle.common.Constants;
import org.spongepowered.gradle.common.SpongePlatform;
import org.spongepowered.gradle.plugin.config.PluginConfiguration;
import org.spongepowered.gradle.plugin.task.WritePluginMetadataTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public final class SpongePluginGradle implements Plugin<Object> {

    private @UnknownNullability Project project;

    @Override
    public void apply(final @NotNull Object target) {
        if (target instanceof Project) {
            this.applyToProject((Project) target);
        } else if (target instanceof Settings) {
            this.applyToSettings((Settings) target);
        } else if (target instanceof Gradle) {
             // no-op
        } else {
            throw new GradleException(
                    "Sponge gradle plugin target '" + target
                            + "' is of unexpected type " + target.getClass()
                            + ", expecting a Project or Settings instance"
            );
        }
    }

    public void applyToProject(final Project project) {
        this.project = project;

        project.getLogger().lifecycle("SpongePowered Plugin 'GRADLE' Toolset Version '{}'", Constants.VERSION);
        project.getPlugins().apply(JavaLibraryPlugin.class);
        // project.getPlugins().apply(ProvideMinecraftPlugin.class);
        // project.getPlugins().apply(IdeaExtPlugin.class);
        // project.getPlugins().apply(EclipsePlugin.class);

        // final MinecraftExtension minecraft = project.getExtensions().getByType(MinecraftExtension.class);
        final SpongePluginExtension sponge = project.getExtensions().create("sponge", SpongePluginExtension.class);

        // Only inject repositories if the project does not have sponge repos from other sources
        sponge.injectRepositories().convention(
            !project.getGradle().getPlugins().hasPlugin(SpongePluginGradle.class)
                && !project.getGradle().getPlugins().hasPlugin("org.spongepowered.gradle.repository")
        );

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
                    final String apiVersion = sponge.apiVersion().get();

                    config.getAttributes().attribute(
                        SpongeVersioningMetadataRule.API_TARGET,
                        generateApiReleasedVersion(apiVersion)
                    );
                });
            } else {
                project.getLogger().info("SpongeAPI version has not been set within the 'sponge' configuration via the 'version' task. No "
                    + "tasks will be available to run a client or server session for debugging.");
                runServer.configure(t -> t.setEnabled(false));
            }
            if (sponge.injectRepositories().get()) {
                SpongePluginGradle.addRepository(project.getRepositories());
            } else {
                project.getLogger().info("SpongeGradle: Injecting repositories was disabled, not adding the Sponge repository at the project level");
            }
        });
    }

    private void applyToSettings(final Settings settings) {
       SpongePluginGradle.addRepository(settings.getDependencyResolutionManagement().getRepositories());
       settings.getGradle().getPlugins().apply(SpongePluginGradle.class);
    }

    private static void addRepository(final RepositoryHandler handler) {
        handler.maven(r -> {
            r.setUrl(Constants.Repositories.SPONGE);
            r.setName("spongeAll");
        });
    }

    private static String generateApiReleasedVersion(final String apiVersion) {
        final String[] apiSplit = apiVersion.replace("-SNAPSHOT", "").split("\\.");
        final boolean isSnapshot = apiVersion.contains("-SNAPSHOT");

        // This is to determine if the split api version has at the least a minimum version.
        final String apiMajor = apiSplit[0];
        final String minorVersion;
        if (apiSplit.length > 1) {
            minorVersion = apiSplit[1];
        } else {
            minorVersion = "0";
        }
        final int latestReleasedVersion = Math.max(Integer.parseInt(minorVersion) - 1, 0);
        // And then here, we determine if the api version still has a patch version, to just ignore it.
        final String latestReleasedApiMinor = isSnapshot ? String.valueOf(latestReleasedVersion) : minorVersion;
        return apiMajor + "." + latestReleasedApiMinor + ".0";
    }

    private void addApiDependency(final SpongePluginExtension sponge) {
        // SpongeAPI dependency
        final NamedDomainObjectProvider<Configuration> spongeApi = this.project.getConfigurations()
            .register("spongeApi", config -> config
                .setVisible(false)
                .defaultDependencies(deps -> {
                    if (sponge.apiVersion().isPresent()) {
                        final String apiVersion = sponge.apiVersion().get();
                        deps.add(
                            this.project.getDependencies().create(
                                Constants.Dependencies.SPONGE_GROUP
                                    + ":" + Constants.Dependencies.SPONGE_API
                                    + ":" + apiVersion
                            )
                        );
                    }
                }));

        this.project.getPlugins().withType(JavaLibraryPlugin.class, v -> {
            // Add SpongeAPI as a dependency
            this.project.getConfigurations().named(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME)
                .configure(config -> config.extendsFrom(spongeApi.get()));
            // and as an AP
            this.project.getConfigurations().named(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                .configure(config -> config.extendsFrom(spongeApi.get()));
        });
    }

    private NamedDomainObjectProvider<Configuration> addRuntimeDependency(final SpongePluginExtension sponge) {
        this.project.getDependencies().getComponents().withModule("org.spongepowered:spongevanilla", SpongeVersioningMetadataRule.class);
        this.project.getDependencies().getAttributesSchema().attribute(SpongeVersioningMetadataRule.API_TARGET).getCompatibilityRules().add(ApiVersionCompatibilityRule.class);
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
        final Directory projectDir = this.project.getLayout().getProjectDirectory();
        final Property<SpongePlatform> spongePlatform = sponge.platform();
        final TaskProvider<JavaExec> runServer = this.project.getTasks().register("runServer", JavaExec.class, task -> {
            task.setGroup(Constants.TASK_GROUP);
            task.setDescription("Run a Sponge server to test this plugin");
            task.setStandardInput(System.in);

            final Provider<FileCollection> spongeRuntimeFiles = spongeRuntime.map(c -> c.getIncoming().getFiles());
            task.getInputs().files(spongeRuntimeFiles);
            task.classpath(spongeRuntimeFiles);
            task.getMainClass().set(sponge.platform().get().mainClass());
            final Directory workingDirectory = projectDir.dir("run");
            task.setWorkingDir(workingDirectory);

            // Register the javaagent
            task.getJvmArgumentProviders().add(new CommandLineArgumentProvider() {
                @Override
                public Iterable<String> asArguments() {
                    for (final ResolvedArtifactResult dep : spongeRuntime.get().getIncoming().artifactView(view -> view.setLenient(true))
                        .getArtifacts()) {
                        final ComponentIdentifier id = dep.getVariant().getOwner();
                        task.getLogger().debug("Inspecting artifact {}", id);
                        if (id instanceof ModuleComponentIdentifier) {
                            final ModuleComponentIdentifier moduleId = (ModuleComponentIdentifier) id;
                            if (moduleId.getGroup().equals(Constants.Dependencies.SPONGE_GROUP)
                                && moduleId.getModule().equals(spongePlatform.get().artifactId())) {
                                task.getLogger().info("Using file {} as Sponge agent", dep.getFile());
                                return Collections.singletonList("-javaagent:" + dep.getFile());
                            }
                        }
                    }
                    task.getLogger().error("Failed to find a java agent!");
                    return Collections.emptyList();
                }
            });

            task.doFirst(new Action<Task>() {
                @Override
                public void execute(final Task a) {
                    final Path path = workingDirectory.getAsFile().toPath();
                    try {
                        Files.createDirectories(path);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });

        this.project.afterEvaluate(p -> {
            final TaskProvider<AbstractArchiveTask> archiveTask;
            if (p.getPlugins().hasPlugin(Constants.Plugins.SHADOW_PLUGIN_ID)) {
                archiveTask = p.getTasks().named(Constants.Plugins.SHADOW_JAR_TASK_NAME, AbstractArchiveTask.class);
            } else {
                archiveTask = p.getTasks().named(JavaPlugin.JAR_TASK_NAME, AbstractArchiveTask.class);
            }
            runServer.configure(new Action<JavaExec>() {
                @Override
                public void execute(final JavaExec it) {
                    it.dependsOn(archiveTask);
                    it.classpath(archiveTask.map(AbstractArchiveTask::getArchiveFile));
                }
            }); // TODO: is there a sensible way to run without the jar?
        });

        return runServer;
    }

    private void configurePluginMetaGeneration(final SpongePluginExtension sponge) {
        // Configure some useful default values
        sponge.getPlugins().configureEach(new ConfigurePluginAction(this.project, sponge));

        // Then configure the generated sources
        final Provider<Directory> generatedResourcesDirectory = this.project.getLayout().getBuildDirectory().dir("generated/sponge/plugin");

        final TaskProvider<WritePluginMetadataTask> writePluginMetadata = this.project.getTasks().register("writePluginMetadata", WritePluginMetadataTask.class,
            new Action<WritePluginMetadataTask>() {
                @Override
                public void execute(final WritePluginMetadataTask task) {
                    task.getSourceContainer().set(sponge);
                    task.getOutputDirectory().set(generatedResourcesDirectory);
                }
            }
        );

        this.project.getPlugins().withType(JavaPlugin.class, v -> {
            this.project.getExtensions().getByType(SourceSetContainer.class).named(SourceSet.MAIN_SOURCE_SET_NAME, new Action<SourceSet>() {
                @Override
                public void execute(final SourceSet s) {
                    s.getResources().srcDir(writePluginMetadata.map(Task::getOutputs));
                }
            });
        });
    }

    private static class ConfigurePluginAction implements Action<PluginConfiguration> {
        private final Provider<String> displayName;
        private final Provider<String> version;
        private final Provider<String> description;
        private final Provider<String> spongeApiVersion;

        ConfigurePluginAction(final Project project, final SpongePluginExtension sponge) {
            this.displayName = project.provider(project::getName);
            this.version = project.provider(() -> project.getVersion() == null ? null : String.valueOf(project.getVersion()));
            this.description = project.provider(project::getDescription);
            this.spongeApiVersion = sponge.apiVersion();
        }

        @Override
        public void execute(final PluginConfiguration plugin) {
            plugin.getDisplayName().convention(this.displayName);
            plugin.getVersion().convention(this.version);
            plugin.getDescription().convention(this.description);
            plugin.getDependencies().matching(dep -> dep.getName().equals(Constants.Dependencies.SPONGE_API))
                .configureEach(dep -> dep.getVersion().convention(this.spongeApiVersion));
        }
    }

}
