import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin")
}

group = "org.spongepowered.test"
version = "1234"
description = "An example of properties coming from build configuration"

// This is the 'minimal' build configuration
sponge {
    apiVersion("8.0.0")

    license("CHANGEME")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("example") {
        displayName("Example")
        entrypoint("org.spongepowered.example.Example")
    }
}
