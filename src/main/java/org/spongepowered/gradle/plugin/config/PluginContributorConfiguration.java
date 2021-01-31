package org.spongepowered.gradle.plugin.config;

import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public class PluginContributorConfiguration implements Named {

    private final String name;
    private final Property<String> description;

    @Inject
    public PluginContributorConfiguration(final String name, final ObjectFactory factory) {
        this.name = name;
        this.description = factory.property(String.class);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Property<String> description() {
        return this.description;
    }

    public void description(final String description) {
        this.description.set(description);
    }
}
