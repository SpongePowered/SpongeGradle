import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.PluginDependency

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
    plugin("example") {
        loader(PluginLoaders.JAVA_PLAIN)
        displayName("Example")
        mainClass("org.spongepowered.example.Example")
    }
}
