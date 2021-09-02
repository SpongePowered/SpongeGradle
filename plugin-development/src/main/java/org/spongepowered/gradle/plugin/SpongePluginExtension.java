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

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.spongepowered.gradle.common.SpongePlatform;
import org.spongepowered.gradle.plugin.config.MetadataContainerConfiguration;
import org.spongepowered.gradle.plugin.config.PluginConfiguration;
import org.spongepowered.gradle.plugin.config.PluginInheritableConfiguration;
import org.spongepowered.gradle.plugin.config.PluginLoaderConfiguration;

import javax.inject.Inject;

public class SpongePluginExtension implements MetadataContainerConfiguration {

    // Plugin metadata
    private final Property<String> license;
    private final Property<String> mappings;
    private final PluginLoaderConfiguration loader;
    private final PluginInheritableConfiguration global;
    private final NamedDomainObjectContainer<PluginConfiguration> plugins;

    // Dependency management
    private final Property<SpongePlatform> platform;
    private final Property<String> apiVersion;
    private final Property<Boolean> injectRepositories;

    @Inject
    public SpongePluginExtension(final ObjectFactory factory) {
        this.license = factory.property(String.class);
        this.mappings = factory.property(String.class);
        this.loader = factory.newInstance(PluginLoaderConfiguration.class);
        this.global = factory.newInstance(PluginInheritableConfiguration.class);
        this.plugins = factory.domainObjectContainer(PluginConfiguration.class);

        this.platform = factory.property(SpongePlatform.class).convention(SpongePlatform.VANILLA);
        this.apiVersion = factory.property(String.class);
        this.injectRepositories = factory.property(Boolean.class).convention(true);
    }

    @Override
    public Property<String> getLicense() {
        return this.license;
    }

    @Override
    public Property<String> getMappings() {
        return this.mappings;
    }

    @Override
    public PluginLoaderConfiguration getLoader() {
        return this.loader;
    }

    @Override
    public PluginInheritableConfiguration getGlobal() {
        return this.global;
    }

    @Override
    public NamedDomainObjectContainer<PluginConfiguration> getPlugins() {
        return this.plugins;
    }

    protected Property<SpongePlatform> platform() {
        return this.platform;
    }

    public void platform(final SpongePlatform platform) {
        this.platform.set(platform);
    }

    protected Property<String> apiVersion() {
        return this.apiVersion;
    }

    public void apiVersion(final String version) {
        this.apiVersion.set(version);
    }

    public Property<Boolean> injectRepositories() {
        return this.injectRepositories;
    }

    public void injectRepositories(final boolean value) {
        this.injectRepositories.set(value);
    }
}
