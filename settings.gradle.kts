pluginManagement {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }

    plugins {
        val indraVersion = "2.2.0"
        id("com.gradle.plugin-publish") version "0.21.0"
        id("com.diffplug.spotless") version "6.11.0"
        id("net.kyori.indra") version indraVersion
        id("net.kyori.indra.publishing.gradle-plugin") version indraVersion
    }
}

rootProject.name = "SpongeGradle"

sequenceOf("convention", "plugin-development").forEach {
    include(it)
    findProject(":$it")?.name = "${rootProject.name.toLowerCase(java.util.Locale.ROOT)}-$it"
}
