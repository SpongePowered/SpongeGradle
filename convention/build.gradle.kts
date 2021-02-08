plugins {
    groovy
}

tasks.withType(GroovyCompile::class).configureEach {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.release.set(indra.javaVersions.target)
}

dependencies {
    api(project(":spongegradle-plugin-development"))
    implementation(localGroovy())
    api("net.kyori:indra-common:1.3.1")
    api("gradle.plugin.org.cadixdev.gradle:licenser:0.5.0")
    api("com.google.code.gson:gson:2.8.6")
}

indraPluginPublishing {
    plugin(
        id = "convention",
        mainClass = "org.spongepowered.gradle.convention.SpongeConventionPlugin",
        displayName = "SpongePowered Convention",
        description = "Gradle conventions for Sponge organization projects",
        tags = listOf("sponge", "convention")
    )
    plugin(
        id = "implementation",
        mainClass = "org.spongepowered.gradle.convention.SpongeImplementationConventionPlugin",
        displayName = "SpongePowered Implementation Convention",
        description = "Conventions for implementations of Sponge projects",
        tags = listOf("sponge", "minecraft", "convention")
    )
}