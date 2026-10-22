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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.mammoth.ProjectOrSettingsPlugin;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.testing.Test;
import org.gradle.process.CommandLineArgumentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.spongepowered.gradle.common.Constants;
import org.spongepowered.gradle.common.SpongePlatform;
import org.spongepowered.gradle.plugin.config.GameTestConfiguration;
import org.spongepowered.gradle.plugin.config.PluginConfiguration;
import org.spongepowered.gradle.plugin.task.WritePluginMetadataTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SpongePluginGradle implements ProjectOrSettingsPlugin {

    private @UnknownNullability Project project;

    @Override
    public void applyToProject(
        final @NotNull Project target,
        final @NotNull PluginContainer plugins,
        final @NotNull ExtensionContainer extensions,
        final @NotNull TaskContainer tasks
    ) {
        this.project = target;

        project.getLogger().lifecycle("SpongePowered Plugin 'GRADLE' Toolset Version '{}'", Constants.VERSION);
        plugins.apply(JavaLibraryPlugin.class);
        // project.getPlugins().apply(ProvideMinecraftPlugin.class);
        // project.getPlugins().apply(IdeaExtPlugin.class);
        // project.getPlugins().apply(EclipsePlugin.class);

        // final MinecraftExtension minecraft = project.getExtensions().getByType(MinecraftExtension.class);
        final SpongePluginExtension sponge = extensions.create("sponge", SpongePluginExtension.class);

        // Only inject repositories if the project does not have sponge repos from other sources
        sponge.injectRepositories().convention(
            !this.isAppliedToSettingsOf(target)
                && !project.getGradle().getPlugins().hasPlugin("org.spongepowered.gradle.repository")
        );

        this.configurePluginMetaGeneration(sponge);

        this.addApiDependency(sponge);
        final NamedDomainObjectProvider<Configuration> spongeRuntime = this.addRuntimeDependency(sponge);
        final TaskProvider<JavaExec> runServer = this.createRunTask(spongeRuntime, sponge);
        this.configureGameTests(spongeRuntime, sponge);

        project.afterEvaluate(a -> {
            if (sponge.minecraftVersion().isPresent() && sponge.apiVersion().isPresent()) {
                // TODO: client run task
                /*project.getLogger().lifecycle("SpongeAPI '{}' has been set within the 'spongeApi' configuration. 'runClient' and 'runServer'"
                    + " tasks will be available. You may use these to test your plugin.", sponge.version().get());*/

                spongeRuntime.configure(config -> {
                    final String minecraftVersion = sponge.minecraftVersion().get();
                    final String apiVersion = sponge.apiVersion().get();

                    config.getAttributes().attribute(
                        SpongeVersioningMetadataRule.MINECRAFT_TARGET,
                        minecraftVersion
                    );

                    config.getAttributes().attribute(
                        SpongeVersioningMetadataRule.API_TARGET,
                        generateApiReleasedVersion(apiVersion)
                    );
                });
            } else {
                if (!sponge.minecraftVersion().isPresent()) {
                    project.getLogger().info("Minecraft version has not been set within the 'sponge' configuration via the 'minecraftVersion' task. No "
                        + "tasks will be available to run a client or server session for debugging.");
                }

                if (!sponge.apiVersion().isPresent()) {
                    project.getLogger().info("SpongeAPI version has not been set within the 'sponge' configuration via the 'version' task. No "
                        + "tasks will be available to run a client or server session for debugging.");
                }
                runServer.configure(t -> t.setEnabled(false));
            }
            if (sponge.injectRepositories().get()) {
                SpongePluginGradle.addRepository(project.getRepositories());
            } else {
                project.getLogger().info("SpongeGradle: Injecting repositories was disabled, not adding the Sponge repository at the project level");
            }
        });
    }

    @Override
    public void applyToSettings(@NotNull Settings target, @NotNull PluginContainer plugins, @NotNull ExtensionContainer extensions) {
       SpongePluginGradle.addRepository(target.getDependencyResolutionManagement().getRepositories());
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
        return apiMajor + "." + latestReleasedApiMinor;
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
        this.project.getDependencies().getAttributesSchema().attribute(SpongeVersioningMetadataRule.MINECRAFT_TARGET).getCompatibilityRules().add(ApiVersionCompatibilityRule.class);
        this.project.getDependencies().getAttributesSchema().attribute(SpongeVersioningMetadataRule.API_TARGET).getCompatibilityRules().add(ApiVersionCompatibilityRule.class);
        return this.project.getConfigurations().register("spongeRuntime", conf -> {
            conf.defaultDependencies(a -> {
                final Dependency dep = this.project.getDependencies().create(
                    Constants.Dependencies.SPONGE_GROUP
                        + ":" + sponge.platform().get().artifactId()
                        + ":" + sponge.minecraftVersion().getOrElse("") + "+:universal");

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

            boolean hasShadow = false;
            for (String id : Constants.Plugins.SHADOW_PLUGIN_IDS) {
                if (p.getPlugins().hasPlugin(id)) {
                    hasShadow = true;
                    break;
                }
            }
            if (hasShadow) {
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

    /**
     * Gametests through the {@code test} task, when {@code sponge.gameTests} is configured: the platform's gametest module
     * on the test classpath, at the version of the Sponge runtime unless one is given; the Sponge runtime on the test
     * runtime classpath, whose JUnit launcher interceptor boots the game before discovery; the plugin's own files as one
     * plugin resource through Sponge's {@code SPONGE_PLUGINS} environment locator; and the rest of the test runtime
     * classpath as game-layer libraries through {@code sponge.resources}.
     */
    private void configureGameTests(final NamedDomainObjectProvider<Configuration> spongeRuntime, final SpongePluginExtension sponge) {
        final GameTestConfiguration gameTests = sponge.getGameTests();
        final Provider<String> version = gameTests.getVersion().orElse(this.project.provider(() -> {
            for (final ResolvedArtifactResult artifact : spongeRuntime.get().getIncoming().artifactView(view -> view.setLenient(true)).getArtifacts()) {
                final ComponentIdentifier id = artifact.getVariant().getOwner();
                if (id instanceof ModuleComponentIdentifier
                    && ((ModuleComponentIdentifier) id).getModule().equals(sponge.platform().get().artifactId())) {
                    return ((ModuleComponentIdentifier) id).getVersion();
                }
            }
            throw new GradleException("No Sponge runtime to take the gametest module version from; set sponge.gameTests.version");
        }));
        final Provider<String> module = version.map(v -> Constants.Dependencies.SPONGE_GROUP + ":" + sponge.platform().get().artifactId() + "-gametest:" + v);
        this.project.getPlugins().withType(JavaPlugin.class, v -> this.project.afterEvaluate(p -> {
            if (!gameTests.getEnabled().get()) {
                return;
            }
            final SourceSetContainer sourceSets = p.getExtensions().getByType(SourceSetContainer.class);
            final SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
            final SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            if (gameTests.getPluginFiles().isEmpty()) { // the default; ConfigurableFileCollection.convention needs Gradle 8.8
                // This project's outputs, and the projects it depends on: a multi-module plugin's other parts
                final Configuration runtime = p.getConfigurations().getByName(test.getRuntimeClasspathConfigurationName());
                gameTests.getPluginFiles().from(main.getOutput(), test.getOutput(),
                    runtime.getIncoming().artifactView(view -> view.componentFilter(id -> id instanceof ProjectComponentIdentifier)).getFiles());
            }
            gameTests.getWorkingDirectory().convention(p.getLayout().getBuildDirectory().dir("gametest-run"));
            gameTests.getPluginId().convention(p.provider(() -> sponge.getPlugins().size() == 1 ? sponge.getPlugins().iterator().next().getName() : null));

            final DependencyHandler dependencies = p.getDependencies();
            // The test declaration and engine to compile against, and, being a plugin itself, found on the classpath at runtime
            dependencies.addProvider(test.getCompileOnlyConfigurationName(), module);
            dependencies.addProvider(test.getRuntimeOnlyConfigurationName(), module);
            dependencies.add(test.getRuntimeOnlyConfigurationName(), p.files(spongeRuntime));
            // A plugin built against Sponge's dev jar must not load it as a second copy of Sponge; the runtime provides it
            final Map<String, String> spongeDevJar = new HashMap<>();
            spongeDevJar.put("group", Constants.Dependencies.SPONGE_GROUP);
            spongeDevJar.put("module", "sponge");
            p.getConfigurations().named(test.getRuntimeClasspathConfigurationName(), c -> c.exclude(spongeDevJar));

            p.getTasks().named(JavaPlugin.TEST_TASK_NAME, Test.class, task -> {
                task.useJUnitPlatform();
                task.getInputs().files(gameTests.getPluginFiles());
                // Where the engine looks for the tests: the compiled test classes, under the plugin they belong to
                final Provider<String> roots = test.getOutput().getClassesDirs().getElements()
                    .map(dirs -> dirs.stream().map(dir -> dir.getAsFile().getAbsolutePath()).collect(Collectors.joining("&")))
                    .zip(gameTests.getPluginId().orElse(""), (dirs, id) -> id.isEmpty() ? dirs : id + "=" + dirs);
                task.getJvmArgumentProviders().add(new GameTestArguments(gameTests.getLaunchTarget(), roots));
                // The plugin: Sponge unions these paths with the last one winning, manifest included, so any jar goes before
                // the plugin's own directories, whose META-INF/MANIFEST.MF carries its mixin and access widener attributes.
                final Provider<String> plugin = gameTests.getPluginFiles().getElements()
                    .map(files -> files.stream()
                        .map(FileSystemLocation::getAsFile)
                        .sorted(Comparator.comparing(File::isDirectory))
                        .map(File::getAbsolutePath)
                        .collect(Collectors.joining("&")));
                // Its libraries: whatever was added, then the rest of the test runtime classpath, each its own game-layer
                // module as on a dev run's classpath. Anything the game loads itself stays out, so the plugin runs against
                // the game's copy as it does in production, and JUnit stays out since the launcher must keep its own.
                final Provider<Set<String>> excludes = gameTests.getExcludes();
                final Configuration runtime = p.getConfigurations().getByName(test.getRuntimeClasspathConfigurationName());
                final GameModules game = new GameModules(runtime, spongeRuntime.get());
                gameTests.getLibraries().from(runtime.getIncoming().artifactView(view -> view.componentFilter(id -> SpongePluginGradle.isLibrary(id)
                    && !game.contains(id) && !SpongePluginGradle.isExcluded(id, excludes.get()))).getFiles());
                task.getInputs().files(gameTests.getLibraries());
                // Passed through sponge.resources, which loads them into the game layer whether or not they are plugins and
                // keeps the app classpath's copies out of it; the installer puts the game's own libraries ahead of them
                final Provider<String> libraries = gameTests.getLibraries().getElements()
                    .map(libs -> libs.stream().map(l -> l.getAsFile().getAbsolutePath()).collect(Collectors.joining(File.pathSeparator)));
                task.doFirst(new Action<Task>() {
                    @Override
                    public void execute(final Task t) {
                        final Test run = (Test) t;
                        run.environment("SPONGE_PLUGINS", plugin.get());
                        run.systemProperty("sponge.resources", libraries.get());
                        final Path directory = gameTests.getWorkingDirectory().get().getAsFile().toPath();
                        run.setWorkingDir(directory.toFile());
                        try {
                            Files.createDirectories(directory);
                            Files.write(directory.resolve("eula.txt"), Collections.singletonList("eula=true"));
                        } catch (final IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
            });
        }));
    }

    /**
     * The modules the game loads itself, as {@code group:module}: Minecraft and the API with everything they depend on, and
     * the libraries the Sponge runtime installs, which its jar lists in {@code sponge-libraries.json}. Versions are left
     * out, since the game's copy is the one a server loads whatever the plugin was built against.
     */
    private static final class GameModules {

        private static final String LIBRARIES = "sponge-libraries.json";

        private final Configuration runtime;
        private final Configuration spongeRuntime;
        private @Nullable Set<String> modules;

        GameModules(final Configuration runtime, final Configuration spongeRuntime) {
            this.runtime = runtime;
            this.spongeRuntime = spongeRuntime;
        }

        boolean contains(final ComponentIdentifier id) {
            if (!(id instanceof ModuleComponentIdentifier)) {
                return false;
            }
            final ModuleComponentIdentifier module = (ModuleComponentIdentifier) id;
            return this.modules().contains(module.getGroup() + ":" + module.getModule());
        }

        // Resolved on first use, which is when the test task resolves its libraries
        private Set<String> modules() {
            if (this.modules == null) {
                final Set<String> modules = new HashSet<>();
                final Deque<ResolvedComponentResult> pending = new ArrayDeque<>();
                for (final ResolvedComponentResult component : this.runtime.getIncoming().getResolutionResult().getAllComponents()) {
                    final @Nullable ModuleVersionIdentifier id = component.getModuleVersion();
                    if (id != null && (id.getGroup().equals("net.minecraft")
                        || id.getGroup().equals(Constants.Dependencies.SPONGE_GROUP) && id.getName().equals(Constants.Dependencies.SPONGE_API))) {
                        pending.add(component);
                    }
                }
                while (!pending.isEmpty()) {
                    final ResolvedComponentResult component = pending.poll();
                    final @Nullable ModuleVersionIdentifier id = component.getModuleVersion();
                    if (id != null && !modules.add(id.getGroup() + ":" + id.getName())) {
                        continue;
                    }
                    for (final DependencyResult dependency : component.getDependencies()) {
                        if (dependency instanceof ResolvedDependencyResult) {
                            pending.add(((ResolvedDependencyResult) dependency).getSelected());
                        }
                    }
                }
                for (final File file : this.spongeRuntime.getFiles()) {
                    GameModules.installed(file, modules);
                }
                this.modules = modules;
            }
            return this.modules;
        }

        private static void installed(final File jar, final Set<String> modules) {
            if (!jar.getName().endsWith(".jar")) {
                return;
            }
            try (final ZipFile zip = new ZipFile(jar)) {
                final @Nullable ZipEntry entry = zip.getEntry(GameModules.LIBRARIES);
                if (entry == null) {
                    return;
                }
                try (final Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                    // {"dependencies": {"<layer>": [{"group": ..., "module": ..., "version": ...}, ...], ...}}
                    for (final Map.Entry<String, JsonElement> layer : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("dependencies").entrySet()) {
                        for (final JsonElement library : layer.getValue().getAsJsonArray()) {
                            final JsonObject coordinates = library.getAsJsonObject();
                            modules.add(coordinates.get("group").getAsString() + ":" + coordinates.get("module").getAsString());
                        }
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException("Reading " + GameModules.LIBRARIES + " from " + jar, e);
            }
        }
    }

    private static boolean isExcluded(final ComponentIdentifier id, final Set<String> excludes) {
        if (!(id instanceof ModuleComponentIdentifier)) {
            return false;
        }
        final ModuleComponentIdentifier module = (ModuleComponentIdentifier) id;
        return excludes.contains(module.getGroup()) || excludes.contains(module.getGroup() + ":" + module.getModule());
    }

    private static boolean isLibrary(final ComponentIdentifier id) {
        if (!(id instanceof ModuleComponentIdentifier)) {
            return false; // a project is a part of the plugin (see the plugin files default), a file the Sponge runtime jar
        }
        final ModuleComponentIdentifier module = (ModuleComponentIdentifier) id;
        final String group = module.getGroup();
        return !group.equals("net.minecraft")
            && !group.startsWith("org.junit")
            && !group.equals("org.opentest4j")
            && !group.equals("org.apiguardian")
            && !(group.equals(Constants.Dependencies.SPONGE_GROUP)
                && (module.getModule().equals(Constants.Dependencies.SPONGE_API) || module.getModule().endsWith("-gametest")));
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
                    // A module of a plugin whose metadata another module writes declares no plugin of its own
                    task.onlyIf(t -> !sponge.getPlugins().isEmpty());
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

    /**
     * The JVM arguments that make the test JVM boot the game, Sponge's launcher interceptor and what it launches, and
     * tell the gametest engine where the tests are
     */
    public static final class GameTestArguments implements CommandLineArgumentProvider {
        private final Provider<String> launchTarget;
        private final Provider<String> roots;

        GameTestArguments(final Provider<String> launchTarget, final Provider<String> roots) {
            this.launchTarget = launchTarget;
            this.roots = roots;
        }

        @Input
        public Provider<String> getLaunchTarget() {
            return this.launchTarget;
        }

        @Input
        public Provider<String> getRoots() {
            return this.roots;
        }

        @Override
        public Iterable<String> asArguments() {
            return Arrays.asList(
                "-Djunit.platform.launcher.interceptors.enabled=true",
                "-Dsponge.test.args=--launchTarget " + this.launchTarget.get(),
                "-Dsponge.gametest.roots=" + this.roots.get()
            );
        }
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
