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

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import java.util.Objects;

/**
 * A configuration that can specify a metadata container
 */
public interface MetadataContainerConfiguration {

    /**
     * License of the output artifact.
     *
     * @return the artifact license
     */
    @Input Property<String> getLicense();

    /**
     * Set the license.
     *
     * @param license the new license
     */
    default void license(final String license) {
        this.getLicense().set(license);
    }

    /**
     * Get the mappings notation, as described in {@link DependencyHandler}.
     *
     * <p>This indicates the Minecraft mappings the mod will be compiled into
     * for distribution. Handling of this attribute is runtime-dependent.</p>
     *
     * @return the mappings used
     */
    @Optional
    @Input
    Property<String> getMappings();

    /**
     * Set the mappings.
     *
     * @param mappings the mappings dependency notation
     * @see #getMappings()
     */
    default void mappings(final String mappings) {
        this.getMappings().set(mappings);
    }

    /**
     * Get information about the plugin loader.
     *
     * @return the loader
     */
    @Nested PluginLoaderConfiguration getLoader();

    default void loader(final Action<? super PluginLoaderConfiguration> action) {
        Objects.requireNonNull(action, "action").execute(this.getLoader());
    }

    /**
     * Get the global configuration that plugins may inherit from.
     *
     * @return the global configuration
     */
    @Nested PluginInheritableConfiguration getGlobal();

    default void global(final Action<? super PluginInheritableConfiguration> action) {
        Objects.requireNonNull(action, "action").execute(this.getGlobal());
    }

    /**
     * Get a container to which individual plugins can be registered.
     *
     * @return the plugin container
     */
    @Nested NamedDomainObjectContainer<PluginConfiguration> getPlugins();

    default void plugins(final Action<? super NamedDomainObjectContainer<PluginConfiguration>> action) {
        action.execute(this.getPlugins());
    }

    default void plugin(final String name, final Action<? super PluginConfiguration> action) {
        this.getPlugins().register(name, action);
    }

}
