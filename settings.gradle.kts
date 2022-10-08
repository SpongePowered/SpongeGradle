pluginManagement {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { pluginManagement.repositories.forEach { add(it) } }
}

rootProject.name = "SpongeGradle"

sequenceOf("convention", "plugin-development", "repository", "testlib").forEach {
    include(it)
    findProject(":$it")?.name = "${rootProject.name.toLowerCase(java.util.Locale.ROOT)}-$it"
}
