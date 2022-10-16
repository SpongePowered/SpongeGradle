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
    id("org.spongepowered.gradle.plugin") version "2.0.2"
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

## [`org.spongepowered.gradle.ore`](https://plugins.gradle.org/plugin/org.spongepowered.gradle.ore)

A plugin to deploy artifacts to the [Ore](https://ore.spongepowered.org) plugin repository.

When using SpongeGradle, this plugin is auto-configured based on the plugin configuration used.

Multiple publications are supported for situations where a project produces multiple artifacts.

Full DSL:

```kotlin
oreDeployment {
    oreEndpoint("https://ore.spongepowered.org/") // default
    apiKey().set(
        providers.gradleProperty("org.spongepowered.ore.apiToken")
            .orElse(providers.environmentVariable("ORE_TOKEN"))
    ) // default value

    publications {
        register("default") {
            // Ore project ID, taken from the first plugin created by SpongeGradle when present
            projectId.set("id")
            createForumPost.set(true) // default
            // Contents (aka release notes) for the version
            versionBody.set("") // default (empty)
            // Channel
            channel.set("Release") // default
            // Artifact -- must be a single file
            publishArtifacts.from(tasks.jar.map { it.outputs }) // default when SpongeGradle is present
        }
    }
    
    // alternatively:
    defaultPublication {
        // same as above
    }
}
```

## [`org.spongepowered.gradle.repository`](https://plugins.gradle.org/plugin/org.spongepowered.gradle.repository)

Provides a simple way to register Sponge's maven repository in a buildscript:

```groovy
plugins {
    id 'org.spongepowered.gradle.repository' version '<version>'
}

repositories {
    sponge.releases()
    sponge.snapshots()
}
```

This extension is applied to both the `settings` `dependencyResolutionManagement` repository section, as well as the buildscript `repositories` section.

> **Note**
> The Kotlin stub generation for `settings.gradle.kts` files does not generate stubs for the `dependencyResolutionManagement.repositories {}` block. The extension will have to be read manually, using something like:
> ```kotlin
> val sponge = (this as ExtensionAware).extensions.getByType(org.spongepowered.gradle.repository.SpongeRepositoryExtension::class)
> ```
