# SpongeGradle

**SpongeGradle** is a [Gradle](http://gradle.org/) plugin which provides utility tasks for the [Sponge Project](/SpongePowered).
It focuses on developing projects related to SpongeAPI's development, SpongeAPI implementations, and projects targeting SpongeAPI.

### Plugins

There are a few plugins that are provided by **SpongeGradle**, they're named appropriate to their function, and not
always appropriate for it's relation to Sponge.

They are listed as: `#. `**`Plugin Name`**` org.gradle.plugin.id - Description`

1. [**`BaseDevPlugin`**](src/main/kotlin/org/spongepowered/gradle/dev/BaseDevPlugin.kt) `org.spongepowered.gradle.base` -
Provides a base of applying the `java-library` plugin, Java compatibility for Java 8, and adds the 
[Sponge Maven Repo](https://repo.spongepowered.org/maven) as a repository for dependency lookups. Does not apply other **SpongeGradle** plugins,
 but is applied by other SpongeGradle plugins.
    - Applied Plugins:
        - `eclipse`
        - `idea`
            - Enables inheriting output directories
        - `java-library`
    - Applied Configurations:
        - `java.sourceCompatibility = JavaVersion.VERSION_1_8`
        - `java.targetCompatibility = JavaVersion.VERSION_1_8`
        - ```
          repositories {
              maven {
                  name = "sponge"
                  url = "https://repo.spongepowered.org/maven"                
              }
          }
          ```
    - Note: Applied by various plugins as a base, due to inter-plugin dependency of what each plugin uses
1. [**`MetadataPlugin`**](src/main/kotlin/org/spongepowered/gradle/meta/MetadataPlugin.kt) `org.spongepowered.gradle.meta` -
Provides [PluginMeta]() generation and configuration for exposing into a `mcmod.info` file.
1. [**`BundleMetaPlugin`**](src/main/kotlin/org/spongepowered/gradle/meta/MetadataPlugin.kt) `org.spongepowered.gradle.meta.bundle` -
Provides bundling capabilities for nested `PluginMeta`s to exist within a project. Useful if 
1. [**`PluginDevPlugin`**](src/main/kotlin/org/spongepowered/gradle/dev/PluginDevPlugin.kt) `org.spongepowered.gradle.plugin` -
Applies the plugin metadata generation to create the `mcmod.info` file based on the `@Plugin` annotation, based on the SpongeAPI version.
If nested plugins are contained within the project, it is possible to configure them, see [configuring MetaPlugin](#configuringMetaPlugins).
    - Applied Plugins:
        - `BaseDevPlugin` To associate the sponge repositories and dependencies
        - `BundleMetaPlugin`
            - Automatically applies a `PluginMetadata` creation aspect to generate an `mcmod.info` for a plugin, as well supports creating more
             plugin metas. 
    - Applied Task Configurations:
        - `JavaCompile` will apply the SpongeAPI plugin-meta Annotation Processor and attach the generated meta files
        - `processResources` will exclude the generated meta files
1. [**`SpongeDevPlugin`**](src/main/kotlin/org/spongepowered/gradle/dev/SpongeDevPlugin.kt) `org.spongepowered.gradle.sponge.dev` - 
Applies various Sponge Team development settings and plugins and configures them. Used for developing SpongeAPI, and it's implementations.
    - Extension: You can use this in your build.gradle
        ```groovy
        spongeDev {
            api = project(":SpongeAPI") // Basically just need a project reference to the API
            organization = "SpongePowered" // defaulted, can be changed, used for license headers
            url = "https://www.spongepowered.org" // defaulted, used for license headers
        }   
        ```
    - Applied Plugins:
        - [`BaseDevPlugin`](src/main/kotlin/org/spongepowered/gradle/dev/BaseDevPlugin.kt)
        - [`net.minecrell.licenser`](https://github.com/minecrell/Licenser/) Default licensing plugin
            - Configured to use the project  name, dev organization, and dev url for licenses
            - Includes the project's API `HEADER.txt`
            - Includes all `**/*.java` files
            - `newLine = false`
        - [`checkstyle`](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)
            - Defines the `checkstyle` task to run **only if `checkstyle` task is explicitly called**
            - Defines the base directory for the project directory
            - Uses `checkstyle-suppressions.xml` from the project directory
            - Sets the `severity` to `warning`
        - [`SortingPlugin`](src/main/kotlin/org/spongepowered/gradle/sort/SpongeSortingPlugin.kt) Adds sorting tasks
            - Adds the `sortClassFields` and `sortAccessTransformers` tasks
        - [`DeploySpongePlugin`](src/main/kotlin/org/spongepowered/gradle/deploy/DeployImplementationPlugin.kt) Configures deploying Sponge projects
            - Defines the urls based on the `spongeDev` extension and sets up a Maven deployer
            - Requires 
    - Configures:
        - Appends `Git-Commit` and `Git-Branch` to jar manifests if information is provided by gradle properties (jenkins)
        - 
    - Tasks:
        - Adds `javadocJar` task creation to create a `javadoc` jar
1. [**`MixinDevPlugin`** `org.spongepowered.gradle.mixin`] 
1. [**`ImplementationDevPlugin`**](src/main/kotlin/) `org.spongepowered.gradle.sponge.impl` -
Applies a "Sponge" Implementation aspect to the project. 
    - Extension:
        ```groovy
        spongeImpl {
            common = project(":SpongeCommon")
            extraDeps += project(":SpongeCommon:SpongeAPI").sourceSets.java6
            implementationId = "spongeforge"
        }
        ```
    - Applied Buildscript Dependencies:
        - `com.github.jengelman:shadow:4.0.4` Shadow plugin
    - Applied Plugins:
        - [`BaseDevPlugin`]
        - [`SpongeDevPlugin`]
        - `BundleMetaPlugin`
        



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

