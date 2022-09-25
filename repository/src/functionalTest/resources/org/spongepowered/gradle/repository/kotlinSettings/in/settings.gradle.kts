plugins {
    id("org.spongepowered.gradle.repository")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        sponge.releases()
        sponge.snapshots()
    }
}
