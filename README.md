# SpongeGradle

**SpongeGradle** is a [Gradle](http://gradle.org/) plugin bundle which provides utility tasks for the [Sponge Project](/SpongePowered).
It focuses on developing projects related to SpongeAPI's development, SpongeAPI implementations, and projects targeting SpongeAPI.

This plugin is built with Gradle 4.10.3+ and is compatible with Gradle 5.x.
Please note that earlier versions of Gradle (including 4.9.x) are NOT compatible.

### How to use it

A multi-faceted question...

#### If you want to write Sponge Plugins

Apply the plugin like so:
```groovy
plugins {
  id("org.spongepowered.gradle.plugin")
}

dependencies {
  implementation("org.spongepowered:spongeapi:7.1.0")
}

```

That's about it! There's more options, but to get started, that's all that's needed.

What this will do for you:
- Configures the `JavaCompile` task to generate the plugin metadata file
- Adds any extra discovered plugin dependencies to the dependency model in the plugin-meta generated

#### If you want to write an API like SpongeAPI

A little more complicated, but you configure it with your needs

```kotlin
plugins {
  id("org.spongepowered.gradle.sponge.dev") // The sponge dev bits, explained further below
  id("org.spongepowered.gradle.sponge.deploy") // To set up deployment
  id("org.spongepowered.gradle.sort") // So we can sort fields
}

base {
  archivesBaseName = "spongeapi"
}

deploySponge {
  // These are property names you want to provide in your run configuration
  // to configure deploying, like the repo urls. By default, it's these values
  snapshotRepo = "spongeRepoSnapshot" 
  releaseRepo = "spongeRepoRelease"
}

dependencies {
 // ... declare your dependencies as normal
}

tasks {
  shadowJar {
    archiveClassifier = "shaded"
  }
  sortClassFields {
    add("main", "org.spongepowered.api.CatalogTypes") // Either do this line by line or some other option to have a list
  }
}
```

We can see that there's a deploy section and the tasks semi configured, but nothing really complicated.

#### If you want to write SpongeAPI implementations

So, this is a little more tricky, since SpongeAPI's official implementations are built using [ForgeGradle],
it's likely that there's *a bit* more involved, but SpongeGradle will take care of a majority of what's needed.

Here's what we've got for a *SpongeCommon* usage:
```kotlin
plugins {
  id("org.spongepowered.gradle.sponge.common") // This is explained further down in the plugins section
  id("net.minecraftforge.gradle")
}

spongeDev {
  api = project("SpongeAPI") // This can be rewritten/assigned to configure things to use a different project as an api
  common // this project, accessible from elsewhere
}

minecraft {
  // .. Some ForgeGradle configuration stuff.  can be ignored if you're building your own
}

// Nothing really to do here, just showing an example
dependencies {
  // minecraft dependency
  minecraft("net.minecraft:" + project.properties["minecraftDep"] + ":" + project.properties["minecraftVersion"])
  // API level dependency, like mixins, a tool that's needed
  api("org.spongepowered:mixin:0.8-SNAPSHOT")
  // Runtime dependencies, like sql libraries etc.
}

val api = spongeDev.api!!
// can do stuff with the api project reference
```

Then, we have to configure the parent container implementation, which doesn't really need much more than the
`CommonImplementationDevPlugin`, but it's explained anyways...

```kotlin
plugins {
    id("org.spongepowered.gradle.sponge.impl")
    id("net.minecraftforge.gradle")
}

dependencies {
    minecraft("net.minecraft:server:1.14.4")
}

minecraft {
    mappings("snapshot", spongeDev.common.properties["mcpMappings"]!! as String)
}


spongeDev {
}

```

### Plugins

There are a few plugins that are provided by **SpongeGradle**, they're named appropriate to their function, and not
always appropriate for it's relation to Sponge.

They are listed as: `#. `**`Plugin Name`**` org.gradle.plugin.id - Description`
1. [**`PluginDevPlugin`**](src/main/kotlin/org/spongepowered/gradle/dev/PluginDevPlugin.kt) `org.spongepowered.gradle.plugin` -
Applies the plugin metadata generation to create the `mcmod.info` file based on the `@Plugin` annotation, based on the SpongeAPI version.
Also automatically applies the properties defined by the `BaseDevPlugin` (found below).
If nested plugins are contained within the project, it is possible to configure them, see [configuring MetaPlugin](#configuringMetaPlugins).
    - Applied Plugins:
        - `BaseDevPlugin` To associate the sponge repositories and dependencies
        - `BundleMetaPlugin`
            - Automatically applies a `PluginMetadata` creation aspect to generate an `mcmod.info` for a plugin, as well supports creating more
             plugin metas. 
    - Applied Task Configurations:
        - `JavaCompile` will apply the SpongeAPI plugin-meta Annotation Processor and attach the generated meta files
        - `processResources` will exclude the generated meta files
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
          buildscript {
             repositories {
                 maven {
                     name = "sponge"
                     url = "https://repo.spongepowered.org/maven"                
                 }
                 maven {
                     name = "forge"
                     url = "https://files.minecraftforge.net/maven"
                 }
             }
          }
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
    - Applies Plugins:
        - `java-library`
    - Adds Tasks: 
        - [**`GenerateMetadata`**](src/main/kotlin/org/spongepowered/gradle/meta/GenerateMetadata.kt) `generateMetadata` - Generates a metadata
        file according to the [Sponge Plugin-Meta Spec]
    - Adds Extensions:
        - [**`MetadataBaseExtension`**](src/main/kotlin/org/spongepowered/gradle/meta/MetadataBaseExtension.kt) `sponge`
        Can be configured by using, or by default, the project will be metadata'ed based on the project's
        name, version, description, and if the project has the `url` property assigned.
        ```groovy
         sponge {
            plugins {
               spongeapi {
                   meta {
                       name = "spongeapi"
                       description = "A Plugin API for Minecraft"
                       url = "https://spongepowered.org/"
                   }
               }     
            }
         }
        ```

1. [**`BundleMetaPlugin`**](src/main/kotlin/org/spongepowered/gradle/meta/MetadataPlugin.kt) `org.spongepowered.gradle.meta.bundle` -
Provides bundling capabilities for nested `PluginMeta`s to exist within a project. Useful if a project is providing not just an
API but also an implementation (like a Libs plugin, or in the case of Sponge, SpongeCommon/SpongeForge/SpongeVanilla providing SpongeAPI).
This overrides the `MetadataPlugin` and provides an extended configuration. Also automatically generated by the project application
1. [**`SpongeDevPlugin`**](src/main/kotlin/org/spongepowered/gradle/dev/SpongeDevPlugin.kt) `org.spongepowered.gradle.sponge.dev` - 
Applies various Sponge Team development settings and plugins and configures them. Used for developing SpongeAPI and its implementations.
    - Extension: You can use this in your build.gradle
        ```groovy
        spongeDev {
            organization = "SpongePowered" // defaulted, can be changed, used for license headers
            url = "https://www.spongepowered.org" // defaulted, used for license headers
            licenseProject = "SpongeAPI" // defaulted, can be changed for license headers
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
            - Requires that the `spongeDev.organization`,`url`, and `licenseProject` are defined
            - Configures the `deploy` extension to use the generated values as a git:
               ```groovy
              deploySponge {
                url = "https://github.com/${spongeDev.organization}/${project.name}"
                git = "${url}.git"
                scm = "scm:git:${git}"
                dev = "scm:git:git@github.com:${spongeDev.organization}.${project.name}.git"
                description = project.description
              }
               ```
    - Configures:
        - Appends `Git-Commit` and `Git-Branch` to jar manifests if information is provided by gradle properties (jenkins)
        - Modifies the `javaCompile` task to have the following options:
            - `compilerArgs += ["-Xlint:all", "-Xlint:-path", "-parameters"]`
            - `deprecation = false`
            - `encoding = "UTF-8"`
        - Adds `javadoc` task to link to other api docs, and prevents fail on error:
            ```groovy
              javadoc {
                options {
                  encoding = "UTF-8"
                  failOnError = false
                  links += [
                     "http://www.slf4j.org/apidocs/",
                     "https://google.github.io/guava/releases/21.0/api/docs/",
                     "https://google.github.io/guice/api-docs/4.1/javadoc/",
                     "https://zml2008.github.io/configurate/configurate-core/apidocs/",
                     "https://zml2008.github.io/configurate/configurate-hocon/apidocs/",
                     "https://flow.github.io/math/",
                     "https://flow.github.io/noise/",
                     "http://asm.ow2.org/asm50/javadoc/user/",
                     "https://docs.oracle.com/javase/8/docs/api/"
                  ]
                  options += ["-Xdoclint:none", "-quiet"]
                }
              }
            ```
        - Populates the `jar` Manifest:
            ```groovy
            jar {
              manifest {
                attributes(
                   "Specification-Title": api.name,
                   "Specification-Version": api.version,
                   "Specification-Vendor": api.organization,
                   "Created-By": "${System.properties["java-version"]} (${Sysstem.properties["java.vendor"]}"
                )
              }
              if (project.properties["commit"]) {
                manifest.attributes("Git-Commit": it)
              }
              if (project.properties["branch"]) {
                manifest.attributes("Git-Branch": it)
              }
            }
            ```
        - Adds the `sourceOutput` Configuration and adds all sources to the configuration as output
    - Tasks:
        - Adds `javadocJar` task creation to create a `javadoc` output jar
           - Configured to be part of the Maven Publication output
        - Adds `sourceJar` task creation to create a sources output jar
           - Configured to be part of the Maven publication output
           - Pulls all nested projects sources output into the main jar
1. [**`DeployPlugin`**](src/main/kotlin/org/spongepowered/gradle/deploy/DeployImplementationPlugin.kt) `org.spongepowered.sponge.deploy` - Adds and configures
a variety of aspects for building a library set of jars for deploying to a Maven repository
    - Extension:
        ```groovy
        deploySponge {
           description = "Some description, what is going to be emitted in a pom"
           url = "Defaulted url pulled by the project, can be customized otherwise"
           git = "Auto defined git:git@github.com:group/example.git"
           snapshotRepo = "spongeRepoSnapshot" // Defaults property key, use -PspongeRepoRelease=https://repo.somewhere.com/maven at runtime
           releaseRepo = "spongeRepoRelease" // Defaults property key, use -PspongeRepoRelease=https://repo.somewhere.com/maven at runtime
           username = "spongeUsername" // Defaulted property key, use -PspongeUsername=someMavenUsername at runtime
           pass = "somePassword" // Defaulted property key, use -PsomePassword=AVeryStrong-Password-With-Lots-O-Numbers-And-Words-12345 at runtime
           license = "MIT License" // Defaulted, change if your project uses a different license
           licenseUrl = "http://opensource.org/licenses/MIT" // Defaulted, change if you use a different license
        }
        ```
    - Applied Plugins:
        - [`maven-publish`](https://docs.gradle.org/5.6.2/userguide/publishing_maven.html#publishing_maven)
    - Configurations:
        - Configures a `MavenPublication` named `mavenJava`
            - Utilizes the configured username and password's value as property keys to get the credentials at runtime
            - Configures the repository baed on the url and whether the project version has `-SNAPSHOT` included
            - Utilizes the archives base name as the artifact
            - Associates issue management, scm, license to the maven POM
        - Adds the artifact repository for publishing
1. [**`MixinDevPlugin`**](src/main/kotlin/org/spongepowered/gradle/dev/MixinDevPlugin.kt) `org.spongepowered.gradle.mixin` - Adds [ForgeFlower] to runtime dependencies only to enable mixin 
decompilation output Not actually used by any specific plugins 
1. [**`CommonImplementationDevPlugin`**](src/main/kotlin/org/spongepowered/gradle/dev/CommonImplementationDevPlugin.kt)  `org.spongepowered.gradle.sponge.common` -
Adds several "Implementation of the API" project configurations and continues to bundle and resolve the various 
needed bits to make parts of the implementation build. This extends `SpongeDevPlugin` and applies the configurations above
along with the following changes:
   - **Properties REQUIRED**:
      - `minecraftVersion`: The minecraft version being depended on, such as `1.14.4` 
   - Extension: 
       ```groovy
       spongeDev { // Also inherits the extension values from the SpongeDevPlugin
         // By default, this is a sponge environment, but as an API implementing project,
         // this can be replaced with any other project
         api = project("SpongeAPI")
       }
       ```
   - Applied Plugins:
     - `java-library`
     - `BundleMetaPlugin`
     - `SpongeSortingPlugin`
   - Configures:
     - Adds `devOutput` as a dependency configuration
     - Adds a `java6` SourceSet to the project
       - Adds its output to `devOutput`
       - Adds its sources to `sourceOutput`
     - Changes `javaCompile` to add `-Xlint:processing` as a compiler option
     - Requests `generateMetadata` to depend on `resolveApiRevision`
     - `jar` Manifest will use compiled API revision and implementation version information
         - Adds MCP mappings if it's a property available as `mcpMappings`
   - Tasks:
     - Adds `resolveApiRevision` to gather the git branch information to generate an implementation dependent version string
     to be recognized as a "here's the commit hash this was built on"
     - Adds `devJar` that consumes the output in the `devOutput` configuration
        - Adds the output jar as an archive to `archives` configuration
1. [**`ImplementationDevPlugin`**](src/main/kotlin/org/spongepowered/gradle/dev/ImplementationDevPlugin.kt) `org.spongepowered.gradle.sponge.impl` -
Applies a "Parent" implementation plugin aspect. Useful if a target platform is needed to separate from a "common"
implementation of the API. Used for SpongeForge and SpongeVanilla.
   - Extension:
      ```groovy
      spongeDev { // Again, extends the super extension from CommonImplementationDevPlugin etc.
          // any additional dependencies can be declared under
          extraDeps += otherProject.sourceSets.foo.output
          parent // This parent project reference
          common // The common implementation project reference
          api // The API project reference
      }
      ```
   - Configuration:
       - Adds the `main` output and compile classpath to the `java6` compile classpath
       - Defines the common project as an `implementation` dependency
       - Adds `https://files.minecraftforge.net/maven` as a Maven repository
       - Applies the dependency changes from the parent after the common project is defined
   - Applied Buildscript Dependencies:
   - Applied Plugins:
       - `BaseDevPlugin`
       - `SpongeDevPlugin`
       - `BundleMetaPlugin`
       - `com.github.johnrengelman.shadow` version 4.0.4
        

### Building SpongeGradle
**SpongeGradle** can of course be built using [Gradle](http://gradle.org/). To perform a build simply execute:

    gradle

To add the compiled jar to your local maven repository, run:

    gradle build install

