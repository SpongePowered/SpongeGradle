import java.util.Locale

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    eclipse
    idea
    maven
    `maven-publish`
    `java-library`
    id(Deps.Script.gradlePublish) version Versions.gradlePublish
    id(Deps.Script.licenser) version Versions.licenser
}

defaultTasks("clean", "licenseFormat", "build")

group = SpongeGradle.group
version = SpongeGradle.version

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
repositories {
    jcenter()
    mavenCentral()
    gradlePluginPortal()
    maven {
        name = "sponge"
        setUrl("https://repo.spongepowered.org/maven")
    }
    maven("https://files.minecraftforge.net/maven")
}

dependencies {
    compile(Deps.httpmime)
    compile(Deps.pluginMeta)
    compile(Deps.asm)
    implementation(Deps.licenser)
//    implementation(Deps.mixingradle)
    // Yay, we can depend on the shadow classes to configure them statically!
    implementation(Deps.shadow) {
        exclude(group = "org.codehaus.groovy")
    }
    implementation(Deps.jsr)
}

tasks.withType(JavaCompile::class.java) {
    // This is needed because shadow shades Log4j annotation processor
    // and it breaks java compilation because some option is not set or class is not provided.
    options.compilerArgs.add("-proc:none")
}

gradlePlugin {
    plugins {
        create("BaseDevPlugin") {
            id = GradlePlugins.BaseDevPlugin.id
            implementationClass = GradlePlugins.BaseDevPlugin.clazz
        }
        create("MetadataPlugin") {
            id = GradlePlugins.Meta.id
            implementationClass = GradlePlugins.Meta.clazz
        }
        create("BundleMetaPlugin") {
            id = GradlePlugins.BundleMeta.id
            implementationClass = GradlePlugins.BundleMeta.clazz
        }
        create("PluginDevPlugin") {
            id = GradlePlugins.PluginDevPlugin.id
            implementationClass = GradlePlugins.PluginDevPlugin.clazz
        }
        create("SpongeDevPlugin") {
            id = GradlePlugins.SpongeDev.id
            implementationClass = GradlePlugins.SpongeDev.clazz
        }
        create("DeploySpongePlugin") {
            id = GradlePlugins.SpongeDeploy.id
            implementationClass = GradlePlugins.SpongeDeploy.clazz
        }
        create("SpongeSortingPlugin") {
            id = GradlePlugins.SpongeSort.id
            implementationClass = GradlePlugins.SpongeSort.clazz
        }
        create("CommonImplementationDevPlugin") {
            id = GradlePlugins.CommonImplementationPlugin.id
            implementationClass = GradlePlugins.CommonImplementationPlugin.clazz
        }
        create("ImplementationDevPlugin") {
            id = GradlePlugins.ImplementationPlugin.id
            implementationClass = GradlePlugins.ImplementationPlugin.clazz
        }

    }
}
pluginBundle {
    website = "https://github.com/${SpongeGradle.organization}/${SpongeGradle.name}"
    vcsUrl = website
    description = project.description
    tags = listOf(Tags.minecraft, Tags.sponge)

    plugins {
        create("BaseDevPlugin") {
            id = GradlePlugins.BaseDevPlugin.id
            displayName = GradlePlugins.BaseDevPlugin.name
            description = GradlePlugins.BaseDevPlugin.desc
        }
        create("MetadataPlugin") {
            id = GradlePlugins.Meta.id
            displayName = GradlePlugins.Meta.name
            description = GradlePlugins.Meta.desc
        }
        create("BundleMetaPlugin") {
            id = GradlePlugins.BundleMeta.id
            displayName = GradlePlugins.BundleMeta.name
            description = GradlePlugins.BundleMeta.desc
        }
        create("PluginDevPlugin") {
            id = GradlePlugins.PluginDevPlugin.id
            displayName = GradlePlugins.PluginDevPlugin.name
            description = GradlePlugins.PluginDevPlugin.desc
        }
        create("SpongeDevPlugin") {
            id = GradlePlugins.SpongeDev.id
            displayName = GradlePlugins.SpongeDev.name
            description = GradlePlugins.SpongeDev.desc
        }
        create("DeploySpongePlugin") {
            id = GradlePlugins.SpongeDeploy.id
            displayName = GradlePlugins.SpongeDeploy.name
            description = GradlePlugins.SpongeDeploy.desc
        }
        create("SpongeSortingPlugin") {
            id = GradlePlugins.SpongeSort.id
            displayName = GradlePlugins.SpongeSort.name
            description = GradlePlugins.SpongeSort.desc
        }
        create("ImplementationDevPlugin") {
            id = GradlePlugins.ImplementationPlugin.id
            displayName = GradlePlugins.ImplementationPlugin.name
            description = GradlePlugins.ImplementationPlugin.desc
        }
        create("CommonImplementationDevPlugin") {
            id = GradlePlugins.CommonImplementationPlugin.id
            displayName = GradlePlugins.CommonImplementationPlugin.name
            description = GradlePlugins.CommonImplementationPlugin.desc
        }
    }

    mavenCoordinates {
        artifactId = SpongeGradle.name
    }
}


license {
    header = file("HEADER.txt")
    newLine = false
    ext {
        this["name"] = project.name
        this["organization"] = SpongeGradle.organization
        this["url"] = SpongeGradle.url
    }

    include("**/*.java", "**/*.groovy", "**/*.kt")
}

tasks {
    val jar by getting(Jar::class) {
        manifest {
            attributes(mapOf(
                    "Created-By" to "${System.getProperty("java.vesrion")} (${System.getProperty("java.vm.vendor")}",
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to SpongeGradle.organization

            ))
        }
    }

}

val sourceJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"]!!.allSource)
}
artifacts {
    add("archives", sourceJar)
}


publishing {
    publications {
        val pluginMaven = (get("pluginMaven") as MavenPublication)
        pluginMaven.apply {
            artifactId = SpongeGradle.name.toLowerCase(Locale.ENGLISH)
            artifact(sourceJar)

        }
    }

    // Set by the build server
    project.properties["spongeRepo"]?.let { repo ->
        repositories {
            maven {
                setUrl(repo)
                val spongeUsername: String? = project.property("spongeUsername") as String?
                val spongePassword: String? = project.property("spongePassword") as String?
                spongeUsername?.let {
                    spongePassword?.let {
                        credentials {
                            username = spongeUsername
                            password = spongePassword
                        }
                    }
                }
            }
        }

    }

}