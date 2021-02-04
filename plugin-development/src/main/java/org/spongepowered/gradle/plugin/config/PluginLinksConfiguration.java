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

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

public class PluginLinksConfiguration {

    private final Property<URL> homepage;
    private final Property<URL> source;
    private final Property<URL> issues;

    @Inject
    public PluginLinksConfiguration(final ObjectFactory factory) {
        this.homepage = factory.property(URL.class);
        this.source = factory.property(URL.class);
        this.issues = factory.property(URL.class);
    }

    public Property<URL> homepage() {
        return this.homepage;
    }

    public void homepage(final String homepage) throws MalformedURLException {
        this.homepage.set(new URL(homepage));
    }

    public Property<URL> source() {
        return this.source;
    }

    public void source(final String source) throws MalformedURLException {
        this.source.set(new URL(source));
    }

    public Property<URL> issues() {
        return this.issues;
    }

    public void issues(final String issues) throws MalformedURLException {
        this.issues.set(new URL(issues));
    }
}
