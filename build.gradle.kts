plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("org.cadixdev.licenser") version "0.5.0"
}

group = "org.spongepowered"
version = "0.1"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.spongepowered:plugin-meta:0.6.2")
}

gradlePlugin {
    plugins {
        create("spongeplugingradle") {
            id = "org.spongepowered.gradle.plugin"
            implementationClass = "org.spongepowered.gradle.plugin.SpongePluginGradle"
        }
    }
}

license {
    val name: String by project
    val organization: String by project
    val projectUrl: String by project

    (this as ExtensionAware).extra.apply {
        this["name"] = name
        this["organization"] = organization
        this["url"] = projectUrl
    }
    header = project.file("HEADER.txt")

    include("**/*.java")
    newLine = false
}
