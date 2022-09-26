plugins {
    id("org.spongepowered.gradle.repository")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val sponge = (this as ExtensionAware).extensions.getByType(org.spongepowered.gradle.repository.SpongeRepositoryExtension::class)
        sponge.releases()
        sponge.snapshots()
    }
}
