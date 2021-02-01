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

    @Input
    private final Property<String> loader;

    @Input
    private final Property<String> displayName;

    @Input
    private final Property<String> version;

    @Input
    private final Property<String> mainClass;

    @Input
    private final Property<String> description;

    @Input
    private final PluginLinksConfiguration links;

    @Nested
    private final NamedDomainObjectContainer<PluginContributorConfiguration> contributors;

    @Nested
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
    public @Nonnull String getName() {
        return this.name;
    }

    public Property<String> loader() {
        return this.loader;
    }

    public void loader(final String loader) {
        this.loader.set(loader);
    }

    public Property<String> displayName() {
        return this.displayName;
    }

    public void displayName(final String displayName) {
        this.displayName.set(displayName);
    }

    public Property<String> version() {
        return this.version;
    }

    public void version(final String version) {
        this.version.set(version);
    }

    public Property<String> mainClass() {
        return this.mainClass;
    }

    public void mainClass(final String mainClass) {
        this.mainClass.set(mainClass);
    }

    public Property<String> description() {
        return this.description;
    }

    public void description(final String description) {
        this.description.set(description);
    }

    public PluginLinksConfiguration links() {
        return this.links;
    }

    public void links(final Action<? super PluginLinksConfiguration> action) {
        action.execute(this.links);
    }

    public NamedDomainObjectContainer<PluginContributorConfiguration> contributors() {
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

    public NamedDomainObjectContainer<PluginDependencyConfiguration> dependencies() {
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
