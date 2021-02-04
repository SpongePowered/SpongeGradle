dependencies {
    api(project(":spongegradle-plugin-development"))
    api("net.kyori:indra-common:1.3.1")
    api("gradle.plugin.org.cadixdev.gradle:licenser:0.5.0")
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