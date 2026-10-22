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
package org.spongepowered.gradle.plugin.config;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import javax.inject.Inject;

/**
 * Runs the plugin's gametests, the {@code @GameTest} methods under {@code src/test}, through the {@code test} task:
 * the platform's gametest module is added to the test classpath, the Sponge runtime boots in the test JVM before
 * JUnit discovers anything, and the plugin's own output is handed to it to load. Every value has a working default.
 */
public abstract class GameTestConfiguration {

    private final Property<Boolean> enabled;
    private final Property<String> version;
    private final Property<String> launchTarget;
    private final Property<String> pluginId;
    private final ConfigurableFileCollection pluginFiles;
    private final ConfigurableFileCollection libraries;
    private final SetProperty<String> excludes;
    private final DirectoryProperty workingDirectory;

    @Inject
    public GameTestConfiguration(final ObjectFactory factory) {
        this.enabled = factory.property(Boolean.class).convention(false);
        this.version = factory.property(String.class);
        this.launchTarget = factory.property(String.class).convention("sponge_server_gametest");
        this.pluginId = factory.property(String.class);
        this.pluginFiles = factory.fileCollection();
        this.libraries = factory.fileCollection();
        this.excludes = factory.setProperty(String.class);
        this.workingDirectory = factory.directoryProperty();
    }

    /** Whether the {@code test} task runs gametests; {@code sponge.gameTests { }} turns it on. */
    public Property<Boolean> getEnabled() {
        return this.enabled;
    }

    /**
     * The version of the platform's gametest module, {@code org.spongepowered:<platform>-gametest}. Defaults to the
     * version of the Sponge runtime {@code runServer} resolves, so the two always match.
     */
    public Property<String> getVersion() {
        return this.version;
    }

    public void version(final String version) {
        this.version.set(version);
    }

    /** The launch target the Sponge runtime boots with; the headless game test server by default. */
    public Property<String> getLaunchTarget() {
        return this.launchTarget;
    }

    public void launchTarget(final String launchTarget) {
        this.launchTarget.set(launchTarget);
    }

    /**
     * The plugin the tests belong to, which prefixes their names: {@code <plugin id>.<group>.<method_name>}. Defaults to
     * the plugin this project declares, when it declares exactly one; a module whose metadata another module writes names
     * it here.
     */
    public Property<String> getPluginId() {
        return this.pluginId;
    }

    public void pluginId(final String pluginId) {
        this.pluginId.set(pluginId);
    }

    /**
     * What Sponge loads as the plugin under test, as one plugin resource: by default the main and test outputs and the
     * jars of the projects this one depends on, a multi-module plugin's other parts. A built jar can stand in, e.g.
     * {@code plugin(tasks.named("shadowJar"))}. Jars are listed before directories, since Sponge reads the manifest
     * from the last path.
     */
    public ConfigurableFileCollection getPluginFiles() {
        return this.pluginFiles;
    }

    public void plugin(final Object... paths) {
        this.pluginFiles.setFrom(paths);
    }

    /**
     * What Sponge loads beside the plugin as game-layer libraries, each its own module. The modules on the test runtime
     * classpath are there by default, less JUnit and anything the game loads itself: Minecraft and the API with their
     * dependencies, and the libraries the Sponge runtime installs. {@code libraries(...)} adds to that: a file
     * dependency, a configuration, a task's jar.
     */
    public ConfigurableFileCollection getLibraries() {
        return this.libraries;
    }

    public void libraries(final Object... paths) {
        this.libraries.from(paths);
    }

    /**
     * Modules of the test runtime classpath kept out of the libraries, as {@code group:module} or a whole {@code group}:
     * what another platform's module brings along, or a test-only dependency the game should not see.
     */
    public SetProperty<String> getExcludes() {
        return this.excludes;
    }

    public void exclude(final String... modules) {
        this.excludes.addAll(modules);
    }

    /** Where the game test server runs; {@code build/gametest-run} by default, with the EULA accepted. */
    public DirectoryProperty getWorkingDirectory() {
        return this.workingDirectory;
    }
}
