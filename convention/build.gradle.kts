plugins {
    groovy
}

tasks.withType(GroovyCompile::class).configureEach {
    options.compilerArgs.add("-Xlint:all")
}

dependencies {
    api(project(":spongegradle-plugin-development"))
    implementation(localGroovy())
    api("net.kyori:indra-common:3.0.0")
    api("net.kyori:indra-licenser-spotless:3.0.0")
    api("com.google.code.gson:gson:2.9.1")
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
