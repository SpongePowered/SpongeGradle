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

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.spongepowered.gradle.common.SpongePlatform;
import org.spongepowered.gradle.plugin.config.PluginConfiguration;

import javax.inject.Inject;

public class SpongePluginExtension {

    private final ObjectFactory factory;
    private final NamedDomainObjectContainer<PluginConfiguration> plugins;

    private final Property<SpongePlatform> platform;
    private final Property<String> version;

    @Inject
    public SpongePluginExtension(final Project project, final ObjectFactory factory) {
        this.factory = factory;
        this.plugins = project.container(PluginConfiguration.class);
        this.platform = factory.property(SpongePlatform.class);
        this.version = factory.property(String.class);
    }

    protected NamedDomainObjectContainer<PluginConfiguration> plugins() {
        return this.plugins;
    }

    public void plugins(final Action<? super NamedDomainObjectContainer<PluginConfiguration>> action) {
        action.execute(this.plugins);
    }

    public void plugin(final String name, final Action<? super PluginConfiguration> action) {
        final PluginConfiguration configuration = this.factory.newInstance(PluginConfiguration.class, name);
        action.execute(configuration);
        this.plugins.add(configuration);
    }

    protected Property<SpongePlatform> platform() {
        return this.platform;
    }

    public void platform(final SpongePlatform platform) {
        this.platform.set(platform);
    }

    protected Property<String> version() {
        return this.version;
    }

    public void version(final String version) {
        this.version.set(version);
    }
}
