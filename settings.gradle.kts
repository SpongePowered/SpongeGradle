pluginManagement {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }

    plugins {
        val indraVersion = "1.3.1"
        id("com.gradle.plugin-publish") version "0.12.0"
        id("net.kyori.indra") version indraVersion
        id("net.kyori.indra.license-header") version indraVersion
        id("net.kyori.indra.publishing.gradle-plugin") version indraVersion
    }
}

rootProject.name = "SpongeGradle"

sequenceOf("convention", "plugin-development").forEach {
    include(it)
    findProject(":$it")?.name = "${rootProject.name.toLowerCase(java.util.Locale.ROOT)}-$it"
}
