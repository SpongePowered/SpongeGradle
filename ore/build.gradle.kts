plugins {
    groovy
}

tasks.withType(GroovyCompile::class).configureEach {
    options.compilerArgs.add("-Xlint:all")
}

dependencies {
    api(libs.mammoth)
    implementation(localGroovy())
    implementation(libs.gson)
    implementation(libs.apacheHttp.client)
}

sourceSets.main {
    multirelease {
        alternateVersions(9)
    }
}

indraPluginPublishing {
    plugin(
        "ore",
        "org.spongepowered.gradle.ore.OreDeploymentPlugin",
        "Ore Deployment",
        "Deploy Sponge plugins to the Ore plugin repository",
        listOf("ore", "publishing", "sponge", "minecraft", "plugin")
    )
}
