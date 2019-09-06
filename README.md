# SpongeGradle

**SpongeGradle** is a [Gradle](http://gradle.org/) plugin which provides utility tasks for the [Sponge Project](/SpongePowered).
It is useful for not just developing against SpongeAPI, but also developing Sponge implementations, as well as some general utilities
for minecraft related projects using sponge. 

###Plugins

There are a few plugins that are provided by **SpongeGradle**, they're named appropriate to their function, and not
always appropriate for it's relation to Sponge.

They are listed as: `#. `**`Plugin Name`**` org.gradle.plugin.id - Description`

1. [**`PluginDevPlugin`** `org.spongepowered.gradle.plugindev`](src/main/kotlin/org/spongepowered/gradle/plugindev/PluginDevPlugin.kt) -
Provides Sponge's [`PluginMeta` generation]() tasks and applies the generation of the various
plugin meta generations into an `mcmod.info` for API 7> and whatever proposed new format for
ModLauncher moving forward with SpongeAPI 8.
1. [**`BaseDevPlugin`** `org.spongepowered.gradle.base`](src/main/kotlin/org/spongepowered/gradle/plugindev/BaseDevPlugin.kt) -
1. [**`ImplementationDevPlugin`** `org.spongepowered.gradle.implementation`](src/main/kotlin/)


### Tasks

The following tasks are currently available:

```groovy
// Sort AccessTransformer (AT) configuration files
sortAccessTransformers {
    add <sourceSet>
}
```

`sourceSet` specifies the sourceset to search for AT configurations

```groovy
// Sort member fields in specified files
sortClassFields {
    add (sourceSet), <fullyQualifiedClassName>
}
```

`sourceSet` can be a `SourceSet` instance or the name of the source set to process, if omitted the task assumes the `main` SourceSet.

### Building SpongeGradle
**SpongeGradle** can of course be built using [Gradle](http://gradle.org/). To perform a build simply execute:

    gradle

To add the compiled jar to your local maven repository, run:

    gradle build install

