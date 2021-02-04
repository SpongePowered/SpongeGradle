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

import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.spongepowered.plugin.metadata.PluginDependency;

import javax.inject.Inject;

public class PluginDependencyConfiguration implements Named {

    private final String name;

    @Input
    private final Property<String> version;

    @Input
    private final Property<PluginDependency.LoadOrder> loadOrder;

    @Input
    private final Property<Boolean> optional;

    @Inject
    public PluginDependencyConfiguration(final String name, final ObjectFactory factory) {
        this.name = name;
        this.version = factory.property(String.class);
        this.loadOrder = factory.property(PluginDependency.LoadOrder.class).convention(PluginDependency.LoadOrder.UNDEFINED);
        this.optional = factory.property(Boolean.class).convention(false);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Property<String> version() {
        return this.version;
    }

    public void version(final String version) {
        this.version.set(version);
    }

    public Property<PluginDependency.LoadOrder> loadOrder() {
        return this.loadOrder;
    }

    public void loadOrder(final PluginDependency.LoadOrder loadOrder) {
        this.loadOrder.set(loadOrder);
    }

    public Property<Boolean> optional() {
        return this.optional;
    }

    public void optional(final boolean optional) {
        this.optional.set(optional);
    }
}
