plugins {
    groovy
}

tasks.withType(GroovyCompile::class).configureEach {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.release.set(indra.javaVersions().target())
    options.compilerArgs.add("-Xlint:all")
}

dependencies {
    api(project(":spongegradle-plugin-development"))
    implementation(localGroovy())
    api("net.kyori:indra-common:2.0.5")
    api("gradle.plugin.org.cadixdev.gradle:licenser:0.6.0")
    api("com.google.code.gson:gson:2.8.6")
}

indraPluginPublishing {
    plugin(
        "sponge.dev",
        "org.spongepowered.gradle.convention.SpongeConventionPlugin",
        "SpongePowered Convention",
        "Gradle conventions for Sponge organization projects",
        listOf("sponge", "convention")
    )
    /*plugin(
        id = "sponge.impl",
        mainClass = "org.spongepowered.gradle.convention.SpongeImplementationConventionPlugin",
        displayName = "SpongePowered Implementation Convention",
        description = "Conventions for implementations of Sponge projects",
        tags = listOf("sponge", "minecraft", "convention")
    )*/
}