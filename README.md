# SpongeGradle

A suite of plugins for Sponge plugin developers, and Sponge's own projects.

## [`org.spongepowered.gradle.plugin`](https://plugins.gradle.org/plugin/org.spongepowered.gradle.plugin)

The gradle plugin to assist with plugin development.

- Generates plugin metadata files for the `main` source set
- Adds a task to run a SpongeVanilla server

Quick start: (see [sponge-plugin-template](https://github.com/SpongePowered/sponge-plugin-template) for a full example)

```kotlin
plugins {
    // [...any plugins...] 
    id("org.spongepowered.gradle.plugin") version "2.0.1"
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

// [...]

```