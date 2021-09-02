import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin")
}

sponge {
    apiVersion("8.0.0")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    license("CHANGEME")
    plugin("example") {
        displayName("Example")
        version("0.1")
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
