# SpongeGradle

**SpongeGradle** is a [Gradle](http://gradle.org/) plugin which provides utility tasks for the [Sponge Project](/SpongePowered).

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
    add {sourceSet}, <fullyQualifiedClassName>
}
```

`sourceSet` can be a `SourceSet` instance or the name of the source set to process, if omitted the task assumes the `main` SourceSet.

### Building SpongeGradle
**SpongeGradle** can of course be built using [Gradle](http://gradle.org/). To perform a build simply execute:

    gradle

To add the compiled jar to your local maven repository, run:

    gradle build install

