plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    groovy
    eclipse
    idea
    `maven-publish`
    `java-library`
    id(Deps.Script.gradlePublish) version Versions.gradlePublish
    id(Deps.Script.licenser) version Versions.licenser
}

defaultTasks("clean", "licenseFormat", "build")

group = SpongeGradle.group
version = SpongeGradle.version

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    jcenter()
}

dependencies {
    compile(Deps.httpmime)
    compile(Deps.pluginMeta)
    compile(Deps.asm)
    implementation(Deps.groovy)
    implementation(Deps.jsr)
}

tasks {
    withType(GroovyCompile::class.java) {
        groovyOptions.optimizationOptions?.set("indy", true)
    }
}

gradlePlugin {
    plugins {
        create("metaBasePlugin") {
            id = GradlePlugins.MetaBase.id
            implementationClass = GradlePlugins.MetaBase.clazz
        }
        create("metaPlugin"){
            id = GradlePlugins.Meta.id
            implementationClass = GradlePlugins.Meta.clazz
        }
        create("spongePluginBasePlugin") {
            id = GradlePlugins.PluginBase.id
            implementationClass = GradlePlugins.PluginBase.clazz
        }
        create("spongePluginPlugin") {
            id = GradlePlugins.PluginPlugin.id
            implementationClass = GradlePlugins.PluginPlugin.clazz
        }
        create("spongeDistributionPlugin") {
            id = GradlePlugins.SpongeDistribution.id
            implementationClass = GradlePlugins.SpongeDistribution.clazz
        }
        create("spongeImplementationPlugin") {
            id = GradlePlugins.ImplementationPlugin.id
            implementationClass = GradlePlugins.ImplementationPlugin.clazz
        }
    }
}
pluginBundle {
    website = "https://github.com/${SpongeGradle.organization}/${SpongeGradle.name}"
    vcsUrl = website
    description = project.description
    tags = listOf(Tags.minecraft, Tags.sponge)

    plugins {
        create("metaPlugin") {

            id = GradlePlugins.Meta.id
            displayName = GradlePlugins.Meta.name
            description = GradlePlugins.PluginPlugin.desc
        }
        create("spongePluginPlugin") {
            id = GradlePlugins.PluginPlugin.id
            displayName = GradlePlugins.PluginPlugin.name
            description = GradlePlugins.PluginPlugin.desc
        }
        create("oreDeployPlugin") {
            id = GradlePlugins.OreDeploy.id
            displayName = GradlePlugins.OreDeploy.name
            description = GradlePlugins.OreDeploy.desc
        }
        create("spongeImplementationPlugin") {
            id = GradlePlugins.ImplementationPlugin.id
            displayName = GradlePlugins.ImplementationPlugin.name
            description = GradlePlugins.ImplementationPlugin.desc
        }
        create("spongeDeploy") {
            id = GradlePlugins.SpongeDistribution.id
            displayName = GradlePlugins.SpongeDistribution.name
            description = GradlePlugins.SpongeDistribution.desc
        }

    }

    mavenCoordinates {
        artifactId = SpongeGradle.name
    }
}


license {
    header = file("HEADER.txt")
    newLine = false
    ext {
        this["name"] = project.name
        this["organization"] = SpongeGradle.organization
        this["url"] = SpongeGradle.url
    }

    include("**/*.java", "**/*.groovy", "**/*.kt")
}

tasks {
    val jar by getting(Jar::class) {
        manifest {
            attributes(mapOf(
                    "Created-By" to "${System.getProperty("java.vesrion")} (${System.getProperty("java.vm.vendor")}",
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to SpongeGradle.organization

            ))
        }
    }

}

val sourceJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(java.sourceSets["main"]!!.allSource)
}

val groovydoc by tasks.getting(Groovydoc::class)

val groovydocJar by tasks.creating(Jar::class) {
    dependsOn(groovydoc)
    classifier = "groovydoc"
    from(groovydoc.destinationDir)
}

artifacts {
    add("archives", sourceJar)
    add("archives", groovydocJar)
}


publishing {
    publications {
        val pluginMaven = (get("pluginMaven") as MavenPublication)
        pluginMaven.apply {
            artifactId = SpongeGradle.name
            artifact(sourceJar)
            artifact(groovydocJar)

        }
    }

    // Set by the build server
    project.properties["spongeRepo"]?.let { repo ->
        repositories {
            maven {
                setUrl(repo)
                val spongeUsername: String? by project.properties
                val spongePassword: String? by project.properties
                spongeUsername?.let {
                    spongePassword?.let {
                        credentials {
                            username = spongeUsername
                            password = spongePassword
                        }
                    }
                }
            }
        }

    }

}