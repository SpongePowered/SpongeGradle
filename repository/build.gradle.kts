indraPluginPublishing {
    plugin(
        "repository",
        "org.spongepowered.gradle.repository.RepositoryPlugin",
        "SpongePowered Repository",
        "Configure the Sponge maven repository automatically on projects",
        listOf("maven", "repository", "project", "settings")
    )
}
