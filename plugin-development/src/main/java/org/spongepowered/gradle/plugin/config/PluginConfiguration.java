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
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class PluginConfiguration implements Named {

    private final String name;
    private final ObjectFactory factory;

    private final Property<String> loader;

    private final Property<String> displayName;

    private final Property<String> version;

    private final Property<String> mainClass;

    private final Property<String> description;

    private final PluginLinksConfiguration links;

    private final NamedDomainObjectContainer<PluginContributorConfiguration> contributors;

    private final NamedDomainObjectContainer<PluginDependencyConfiguration> dependencies;

    @Inject
    public PluginConfiguration(final String name, final ObjectFactory factory) {
        this.name = name;
        this.factory = factory;

        this.loader = factory.property(String.class);
        this.displayName = factory.property(String.class).convention(this.name);
        this.version = factory.property(String.class).convention("0.1");
        this.mainClass = factory.property(String.class);
        this.description = factory.property(String.class);
        this.links = factory.newInstance(PluginLinksConfiguration.class);

        this.contributors = factory.domainObjectContainer(PluginContributorConfiguration.class);
        this.dependencies = factory.domainObjectContainer(PluginDependencyConfiguration.class);
    }

    @Override
    @Input
    public @Nonnull String getName() {
        return this.name;
    }

    @Input
    public Property<String> getLoader() {
        return this.loader;
    }

    public void loader(final String loader) {
        this.loader.set(loader);
    }

    @Input
    public Property<String> getDisplayName() {
        return this.displayName;
    }

    public void displayName(final String displayName) {
        this.displayName.set(displayName);
    }

    @Input
    public Property<String> getVersion() {
        return this.version;
    }

    public void version(final String version) {
        this.version.set(version);
    }

    @Input
    public Property<String> getMainClass() {
        return this.mainClass;
    }

    public void mainClass(final String mainClass) {
        this.mainClass.set(mainClass);
    }

    @Input
    public Property<String> getDescription() {
        return this.description;
    }

    public void description(final String description) {
        this.description.set(description);
    }

    @Nested
    public PluginLinksConfiguration getLinks() {
        return this.links;
    }

    public void links(final Action<? super PluginLinksConfiguration> action) {
        action.execute(this.links);
    }

    @Nested
    public NamedDomainObjectContainer<PluginContributorConfiguration> getContributors() {
        return this.contributors;
    }

    public void contributors(final Action<? super NamedDomainObjectContainer<PluginContributorConfiguration>> action) {
        action.execute(this.contributors);
    }

    public void contributor(final String name, final Action<? super PluginContributorConfiguration> action) {
        final PluginContributorConfiguration configuration = this.factory.newInstance(PluginContributorConfiguration.class, name);
        action.execute(configuration);
        this.contributors.add(configuration);
    }

    @Nested
    public NamedDomainObjectContainer<PluginDependencyConfiguration> getDependencies() {
        return this.dependencies;
    }

    public void dependencies(final Action<? super NamedDomainObjectContainer<PluginDependencyConfiguration>> action) {
        action.execute(this.dependencies);
    }

    public void dependency(final String name, final Action<? super PluginDependencyConfiguration> action) {
        final PluginDependencyConfiguration configuration = this.factory.newInstance(PluginDependencyConfiguration.class, name);
        action.execute(configuration);
        this.dependencies.add(configuration);
    }
}
