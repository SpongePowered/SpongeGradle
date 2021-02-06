dependencies {
    implementation("org.spongepowered:plugin-meta:0.6.2")
    implementation("org.spongepowered:vanillagradle:0.2-SNAPSHOT")
    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.10")
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