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
        suite("test") // 2.4.0's behaviour: gametests in src/test, run by test
        version("1.0.0") // skips resolving a Sponge runtime for the version
    }
}

afterEvaluate {
    check(sourceSets.findByName("gametest") == null) { "a gametest suite was created" }
    check(!tasks.test.get().ignoreFailures) { "test is optional" }
    check(configurations.testRuntimeOnly.get().dependencies.any { it.name == "spongevanilla-gametest" }) { "no gametest module on test" }
    println("test suite configured")
}
