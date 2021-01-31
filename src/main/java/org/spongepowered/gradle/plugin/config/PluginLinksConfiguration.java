package org.spongepowered.gradle.plugin.config;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

public class PluginLinksConfiguration {

    private final Property<URI> homepage;
    private final Property<URI> source;
    private final Property<URI> issues;

    @Inject
    public PluginLinksConfiguration(final ObjectFactory factory) {
        this.homepage = factory.property(URI.class);
        this.source = factory.property(URI.class);
        this.issues = factory.property(URI.class);
    }

    public Property<URI> homepage() {
        return this.homepage;
    }

    public void homepage(final String homepage) throws URISyntaxException {
        this.homepage.set(new URI(homepage));
    }

    public Property<URI> source() {
        return this.source;
    }

    public void source(final String source) throws URISyntaxException {
        this.source.set(new URI(source));
    }

    public Property<URI> issues() {
        return this.issues;
    }

    public void issues(final String issues) throws URISyntaxException {
        this.issues.set(new URI(issues));
    }
}
