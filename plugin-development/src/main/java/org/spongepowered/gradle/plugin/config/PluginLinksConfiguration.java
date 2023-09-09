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

import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.inject.Inject;

public class PluginLinksConfiguration {

    private final Property<URI> homepage;
    private final Property<URI> source;
    private final Property<URI> issues;

    private final Property<URL> legacyHomepage;
    private final Property<URL> legacySource;
    private final Property<URL> legacyIssues;

    @Inject
    public PluginLinksConfiguration(final ObjectFactory factory) {
        this.legacyHomepage = factory.property(URL.class);
        this.legacySource = factory.property(URL.class);
        this.legacyIssues = factory.property(URL.class);

        this.homepage = factory.property(URI.class).convention(urlToUri(this.legacyHomepage));
        this.source = factory.property(URI.class).convention(urlToUri(this.legacySource));
        this.issues = factory.property(URI.class).convention(urlToUri(this.legacyIssues));
    }

    private Provider<URI> urlToUri(final Property<URL> url) {
        return url.map(old -> {
            try {
                return old.toURI();
            } catch (final URISyntaxException ex) {
                throw new GradleException("Failed to convert the provided URL to a URI, please set the URI directly", ex);
            }
        });
    }

    @Input
    @Optional
    public Property<URI> getHomepageLink() {
        return this.homepage;
    }

    @Deprecated
    @Internal
    public Property<URL> getHomepage() {
        return this.legacyHomepage;
    }

    public void homepage(final String homepage) throws URISyntaxException {
        this.homepage.set(new URI(homepage));
    }

    @Input
    @Optional
    public Property<URI> getSourceLink() {
        return this.source;
    }

    @Internal
    @Deprecated
    public Property<URL> getSource() {
        return this.legacySource;
    }

    public void source(final String source) throws URISyntaxException {
        this.source.set(new URI(source));
    }

    @Input
    @Optional
    public Property<URI> getIssuesLink() {
        return this.issues;
    }

    @Deprecated
    @Internal
    public Property<URL> getIssues() {
        return this.legacyIssues;
    }

    public void issues(final String issues) throws URISyntaxException {
        this.issues.set(new URI(issues));
    }
}
