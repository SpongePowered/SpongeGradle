dependencies {
    implementation(libs.pluginMeta) {
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
    // implementation("org.spongepowered:vanillagradle:0.2-SNAPSHOT")
    // implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.10")
}

testing.suites.named("functionalTest", JvmTestSuite::class) {
    dependencies {
        implementation(libs.gson)
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
