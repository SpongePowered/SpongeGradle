dependencies {
    implementation("org.spongepowered:plugin-meta:0.8.0")
    // implementation("org.spongepowered:vanillagradle:0.2-SNAPSHOT")
    // implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.10")
}

testing.suites.named("functionalTest", JvmTestSuite::class) {
    dependencies {
        implementation("com.google.code.gson:gson:2.9.1")
    }
}

indraPluginPublishing {
    plugin(
        "plugin",
        "org.spongepowered.gradle.plugin.SpongePluginGradle",
        "Sponge Plugin",
        "Set up a project for building Sponge plugins",
        listOf("minecraft", "sponge", "plugin-development")
    )
}
