# SpongeGradle

A suite of plugins for Sponge plugin developers, and Sponge's own projects.

## `org.spongepowered.gradle.plugin`

The gradle plugin to assist with plugin development.

- Generates plugin metadata files for the `main` source set
- Adds a task to run a SpongeVanilla server

Example:

```kotlin
plugins {
    // [...any plugins...] 
    id("org.spongepowered.gradle.plugin") version "1.0-SNAPSHOT"
}

sponge {
    apiVersion("8.0.0")
    plugin("example") {
        loader(PluginLoaders.JAVA_PLAIN)
        displayName("Example")
        version("0.1")
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

// [...]

```