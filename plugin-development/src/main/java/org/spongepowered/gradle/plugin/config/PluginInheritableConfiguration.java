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
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

public abstract class PluginInheritableConfiguration {

    private final Property<String> version;

    private final PluginLinksConfiguration links;

    private final PluginBrandingConfiguration branding;

    private final NamedDomainObjectContainer<PluginContributorConfiguration> contributors;

    private final NamedDomainObjectContainer<PluginDependencyConfiguration> dependencies;


    @Inject
    public PluginInheritableConfiguration(final ObjectFactory objects) {
        this.version = objects.property(String.class).convention("0.1");
        this.links = objects.newInstance(PluginLinksConfiguration.class);
        this.branding = objects.newInstance(PluginBrandingConfiguration.class);

        this.contributors = objects.domainObjectContainer(PluginContributorConfiguration.class);
        this.dependencies = objects.domainObjectContainer(PluginDependencyConfiguration.class);
    }

    @Input
    @Optional
    public Property<String> getVersion() {
        return this.version;
    }

    public void version(final String version) {
        this.version.set(version);
    }

    @Nested
    public PluginLinksConfiguration getLinks() {
        return this.links;
    }

    public void links(final Action<? super PluginLinksConfiguration> action) {
        action.execute(this.links);
    }

    @Nested
    public PluginBrandingConfiguration getBranding() {
        return this.branding;
    }

    public void branding(final Action<? super PluginBrandingConfiguration> action) {
        action.execute(this.branding);
    }

    @Nested
    public NamedDomainObjectContainer<PluginContributorConfiguration> getContributors() {
        return this.contributors;
    }

    public void contributors(final Action<? super NamedDomainObjectContainer<PluginContributorConfiguration>> action) {
        action.execute(this.contributors);
    }

    public void contributor(final String name, final Action<? super PluginContributorConfiguration> action) {
        this.contributors.register(name, action);
    }

    @Nested
    public NamedDomainObjectContainer<PluginDependencyConfiguration> getDependencies() {
        return this.dependencies;
    }

    public void dependencies(final Action<? super NamedDomainObjectContainer<PluginDependencyConfiguration>> action) {
        action.execute(this.dependencies);
    }

    public void dependency(final String name, final Action<? super PluginDependencyConfiguration> action) {
        this.dependencies.register(name, action);
    }
}
