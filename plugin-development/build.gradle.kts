dependencies {
    implementation("org.spongepowered:plugin-meta:0.6.2")
    // implementation("org.spongepowered:vanillagradle:0.2-SNAPSHOT")
    // implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.10")
}

val functionalTest by sourceSets.creating

configurations.named(functionalTest.compileClasspathConfigurationName) { extendsFrom(configurations.testCompileClasspath.get()) }
configurations.named(functionalTest.runtimeClasspathConfigurationName) { extendsFrom(configurations.testRuntimeClasspath.get()) }

dependencies {
    functionalTest.implementationConfigurationName("com.google.code.gson:gson:2.8.6")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}

val functionalTestTask = tasks.register("functionalTest", Test::class) {
    description = "Run functional tests"
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    mustRunAfter(tasks.test)
}

tasks.check {
    dependsOn(functionalTestTask)
}

gradlePlugin.testSourceSets(functionalTest)

indraPluginPublishing {
    plugin(
            "plugin",
            "org.spongepowered.gradle.plugin.SpongePluginGradle",
            "Sponge Plugin",
            "Set up a project for building Sponge plugins",
            listOf("minecraft", "sponge", "plugin-development")
    )
}