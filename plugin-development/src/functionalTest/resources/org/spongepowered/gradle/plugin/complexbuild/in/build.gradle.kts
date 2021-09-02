import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

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
    license("CHANGEME")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("example") {
        displayName("Example")
        entrypoint("org.spongepowered.example.Example")
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
