import java.util.Locale

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    eclipse
    idea
    maven
    `maven-publish`
    `java-library`
    id("com.gradle.plugin-publish") version "0.11.0"
    id("net.minecrell.licenser") version "0.4"
}

defaultTasks("clean", "licenseFormat", "build")

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
    compile("org.apache.httpcomponents:httpmime:4.5.3")
    compile("org.spongepowered:plugin-meta:0.4.1")
    compile("org.ow2.asm:asm:5.2")
    implementation("net.minecrell.licenser:net.minecrell.licenser.gradle.plugin:0.4.1")
//    implementation(Deps.mixingradle)
    // Yay, we can depend on the shadow classes to configure them statically!
    implementation("com.github.jengelman.gradle.plugins:shadow:4.0.4") {
        exclude(group = "org.codehaus.groovy")
    }
    implementation("com.google.code.findbugs:jsr305:3.0.2")
}

tasks.withType(JavaCompile::class.java) {
    // This is needed because shadow shades Log4j annotation processor
    // and it breaks java compilation because some option is not set or class is not provided.
    options.compilerArgs.add("-proc:none")
}

gradlePlugin {
    plugins {
        create("BaseDevPlugin") {
            id = "org.spongepowered.gradle.base"
            implementationClass = "org.spongepowered.gradle.dev.BaseDevPlugin"
        }
        create("MetadataPlugin") {
            id = "org.spongepowered.gradle.meta"
            implementationClass = "org.spongepowered.gradle.meta.MetadataPlugin"
        }
        create("BundleMetaPlugin") {
            id = "org.spongepowered.gradle.meta.bundle"
            implementationClass = "org.spongepowered.gradle.meta.BundleMetaPlugin"
        }
        create("PluginDevPlugin") {
            id = "org.spongepowered.gradle.plugin"
            implementationClass = "org.spongepowered.gradle.dev.PluginDevPlugin"
        }
        create("SpongeDevPlugin") {
            id = "org.spongepowered.gradle.sponge.dev"
            implementationClass = "org.spongepowered.gradle.dev.SpongeDevPlugin"
        }
        create("DeploySpongePlugin") {
            id = "org.spongepowered.gradle.sponge.deploy"
            implementationClass = "org.spongepowered.gradle.deploy.DeployImplementationPlugin"
        }
        create("SpongeSortingPlugin") {
            id = "org.spongepowered.gradle.sort"
            implementationClass = "org.spongepowered.gradle.sort.SpongeSortingPlugin"
        }
        create("CommonImplementationDevPlugin") {
            id = "org.spongepowered.gradle.sponge.common"
            implementationClass = "org.spongepowered.gradle.dev.CommonImplementationDevPlugin"
        }
        create("ImplementationDevPlugin") {
            id = "org.spongepowered.gradle.sponge.impl"
            implementationClass = "org.spongepowered.gradle.dev.ImplementationDevPlugin"
        }

    }
}
pluginBundle {
    website = "https://github.com/SpongePowered/SpongeGradle"
    vcsUrl = website
    description = project.description
    tags = listOf("minecraft", "sponge", "minecraftforge", "spongegradle")

    (plugins) {
        "BaseDevPlugin" {
            displayName = "Base Development Plugin"
            description = "A base plugin providing basic gradle plugins and sponge repository access"
        }
        "MetadataPlugin" {
            displayName = "Sponge Plugin metadata generator"
            description = "Gradle plugin for automatically generating a mcmod.info file with the project properties"
        }
        "BundleMetaPlugin" {
            displayName = "Bundled Plugin metadata generator"
            description = "Gradle plugin that provides the project with nesting capabilities for PluginMeta representation"
        }
        "PluginDevPlugin" {
            displayName = "Sponge Plugin Developer Gradle integrations"
            description = "Gradle plugin providing integration for plugins made for the Sponge platform"
        }
        "SpongeDevPlugin" {
            displayName = "Sponge Development Plugin"
            description = "Gradle plugin to set up developing Sponge and it's implementations"
        }
        "DeploySpongePlugin" {
            displayName = "Sponge Deployment Plugin"
            description = "Gradle plugin to set up the deployment of Sponge"
        }
        "SpongeSortingPlugin" {
            displayName = "Sponge Sorting"
            description = "Enables tasks for sorting specific types of things for Sponge related development"
        }
        "ImplementationDevPlugin" {
            displayName = "Sponge Implementation Support Plugin"
            description = "Gradle plugin for simplified build setup for implementing Sponge"
        }
        "CommonImplementationDevPlugin" {
            displayName = "Sponge Common implementation support plugin"
            description = "Gradle plugin for setting up SpongeCommon's implementation setup"
        }
    }
}


license {
    header = file("HEADER.txt")
    newLine = false
    ext {
        this["name"] = project.name
        this["organization"] = "SpongePowered"
        this["url"] = "https://www.spongepowered.org"
    }

    include("**/*.java", "**/*.groovy", "**/*.kt")
}

tasks {
    val organization: String by project
    val jar by getting(Jar::class) {
        manifest {
            attributes(mapOf(
                    "Created-By" to "${System.getProperty("java.version")} (${System.getProperty("java.vm.vendor")}",
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to organization

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

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/spongepowered/SpongeGradle")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
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