pluginManagement {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }

    plugins {
        val indraVersion = "3.0.0"
        id("com.gradle.plugin-publish") version "1.0.0"
        id("com.diffplug.spotless") version "6.11.0"
        id("net.kyori.indra") version indraVersion
        id("net.kyori.indra.crossdoc") version indraVersion
        id("net.kyori.indra.publishing.gradle-plugin") version indraVersion
        id("net.kyori.indra.licenser.spotless") version indraVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        pluginManagement.repositories.forEach { add(it) }
    }
}

rootProject.name = "SpongeGradle"

sequenceOf("convention", "plugin-development", "repository", "testlib").forEach {
    include(it)
    findProject(":$it")?.name = "${rootProject.name.toLowerCase(java.util.Locale.ROOT)}-$it"
}
