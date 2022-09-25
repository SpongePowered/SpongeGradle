indraPluginPublishing {
    plugin(
        "repository",
        "org.spongepowered.gradle.repository.RepositoryPlugin",
        "SpongePowered Repository",
        "Configure the Sponge maven repository automatically on projects",
        listOf("maven", "repository", "project", "settings")
    )
}

testing.suites.withType(JvmTestSuite::class).configureEach {
    useJUnitJupiter("5.9.0")
}

val functionalTest = testing.suites.register("functionalTest", JvmTestSuite::class) {
    dependencies {
        implementation(project)
        implementation("net.kyori:mammoth-test:1.2.0")
    }
}

tasks.check {
    dependsOn(functionalTest)
}

gradlePlugin.testSourceSets(functionalTest.get().sources)
