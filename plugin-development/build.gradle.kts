plugins {
    `jvm-test-suite`
}

dependencies {
    implementation("org.spongepowered:plugin-meta:0.8.0")
    // implementation("org.spongepowered:vanillagradle:0.2-SNAPSHOT")
    // implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.10")
}

testing.suites.withType(JvmTestSuite::class).configureEach {
    useJUnitJupiter("5.9.0")
}

val functionalTest = testing.suites.register("functionalTest", JvmTestSuite::class) {
    dependencies {
        implementation(project)
        implementation("net.kyori:mammoth-test:1.2.0")
        implementation("com.google.code.gson:gson:2.9.1")
    }
}

tasks.check {
    dependsOn(functionalTest)
}

gradlePlugin.testSourceSets(functionalTest.get().sources)

indraPluginPublishing {
    plugin(
        "plugin",
        "org.spongepowered.gradle.plugin.SpongePluginGradle",
        "Sponge Plugin",
        "Set up a project for building Sponge plugins",
        listOf("minecraft", "sponge", "plugin-development")
    )
}
