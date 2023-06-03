pluginManagement {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { pluginManagement.repositories.forEach { add(it) } }
}

rootProject.name = "SpongeGradle"

sequenceOf(
    "convention",
    "plugin-development",
    "ore",
    "repository",
    "testlib"
).forEach {
    include(it)
    findProject(":$it")?.name = "${rootProject.name.lowercase()}-$it"
}
