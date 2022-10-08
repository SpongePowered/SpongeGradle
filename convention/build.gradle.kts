plugins {
    groovy
}

tasks.withType(GroovyCompile::class).configureEach {
    options.compilerArgs.add("-Xlint:all")
}

dependencies {
    api(project(":spongegradle-plugin-development"))
    implementation(localGroovy())
    api(libs.indra.common)
    api(libs.indra.licenserSpotless)
    api(libs.gson)
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
