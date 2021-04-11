import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.PluginDependency

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin")
}

group = "org.spongepowered.test"
version = "1.0-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
}

sponge {
    apiVersion("8.0.0")
    plugin("example") {
        loader(PluginLoaders.JAVA_PLAIN)
        displayName("Example")
        mainClass("org.spongepowered.example.Example")
        description("Just testing things...")
        links {
            homepage("https://spongepowered.org")
            source("https://spongepowered.org/source")
            issues("https://spongepowered.org/issues")
        }
        contributor("Spongie") {
            description("Lead Developer")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}
