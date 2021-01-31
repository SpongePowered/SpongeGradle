package org.spongepowered.gradle.plugin.config;

import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public class PluginDependencyConfiguration implements Named {

    private final String name;
    private final Property<String> version;

    @Inject
    public PluginDependencyConfiguration(final String name, final ObjectFactory factory) {
        this.name = name;
        this.version = factory.property(String.class);
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
}
