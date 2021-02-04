dependencies {
    implementation("org.spongepowered:plugin-meta:0.6.2")
}

indraPluginPublishing {
    plugin(
            id = "plugin",
            mainClass = "org.spongepowered.gradle.plugin.SpongePluginGradle",
            displayName = "Sponge Plugin",
            description = "Set up a project for building Sponge plugins",
            tags = listOf("minecraft", "sponge", "plugin-development")
    )
}