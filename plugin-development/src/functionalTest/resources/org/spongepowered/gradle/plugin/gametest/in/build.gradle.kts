import org.spongepowered.gradle.plugin.config.PluginLoaders

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin")
}

sponge {
    apiVersion("8.0.0-SNAPSHOT")
    license("CHANGEME")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("example") {
        entrypoint("org.spongepowered.example.Example")
    }
    gameTests {
        version("1.0.0") // skips resolving a Sponge runtime for the version
    }
}

// The suite exists as soon as gametests are turned on, so a build script can add shared tests to it
sourceSets.named("gametest") {
    java.srcDir("shared/gametest/java")
}

afterEvaluate {
    val gametest = tasks.named<Test>("gametest").get()
    check(tasks.check.get().dependsOn.contains("gametest")) { "check does not run gametest" }
    check(!gametest.ignoreFailures) { "gametest is optional by default" }
    check(configurations.testRuntimeClasspath.get().allDependencies.none { it.name.endsWith("-gametest") || it is FileCollectionDependency }) { "the game leaked onto the test classpath" }
    check(configurations["gametestRuntimeOnly"].dependencies.any { it.name == "spongevanilla-gametest" }) { "no gametest module" }
    println("gametest suite configured")
}
