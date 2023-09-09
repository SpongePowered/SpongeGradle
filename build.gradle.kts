import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import net.kyori.indra.IndraExtension
import net.kyori.indra.crossdoc.CrossdocExtension
import net.kyori.indra.gradle.IndraPluginPublishingExtension
import net.kyori.indra.licenser.spotless.IndraSpotlessLicenserExtension

plugins {
    alias(libs.plugins.gradlePluginPublish) apply false
    alias(libs.plugins.indra) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.indra.crossdoc) apply false
    alias(libs.plugins.indra.licenserSpotless) apply false
    alias(libs.plugins.indra.gradlePlugin) apply false
}

group = "org.spongepowered"
version = "2.2.0-SNAPSHOT"

subprojects {
    apply(plugin = "net.kyori.indra")
    apply(plugin = "net.kyori.indra.licenser.spotless")
    apply(plugin = "net.kyori.indra.git")

    if (project.name != "spongegradle-testlib") {
        plugins.apply(JavaGradlePluginPlugin::class)
        apply(plugin = "com.gradle.plugin-publish")
        apply(plugin = "net.kyori.indra.publishing.gradle-plugin")
        apply(plugin = "net.kyori.indra.crossdoc")

        tasks.named("publishPlugins") {
            onlyIf { net.kyori.indra.util.Versioning.isRelease(project) }
        }

        extensions.configure(TestingExtension::class) {
            suites.withType(JvmTestSuite::class).configureEach {
                useJUnitJupiter(rootProject.libs.versions.junit.get())
            }

            val functionalTest = suites.register("functionalTest", JvmTestSuite::class) {
                dependencies {
                    implementation(project())
                    implementation(project(":spongegradle-testlib"))
                }
                testType.set(TestSuiteType.FUNCTIONAL_TEST)
            }

            tasks.named("check") {
                dependsOn(functionalTest)
            }

            extensions.getByType(GradlePluginDevelopmentExtension::class).testSourceSets(functionalTest.get().sources)
        }
    }


    dependencies {
        "compileOnlyApi"(rootProject.libs.jetbrainsAnnotations)
    }

    val indraGit = extensions.getByType(net.kyori.indra.git.IndraGitExtension::class)
    tasks.withType(Jar::class).configureEach {
        indraGit.applyVcsInformationToManifest(manifest)
        manifest.attributes(
            "Specification-Title" to project.name,
            "Specification-Vendor" to "SpongePowered",
            "Specification-Version" to project.version,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "SpongePowered"
        )
    }

    extensions.configure(IndraExtension::class) {
        github("SpongePowered", "SpongeGradle") {
            ci(true)
            publishing(true)
        }
        mitLicense()

        configurePublications {
            pom {
                developers {
                    developer {
                        name.set("SpongePowered Team")
                        email.set("staff@spongepowered.org")
                    }
                }
            }
        }

        signWithKeyFromPrefixedProperties("sponge")
        val spongeSnapshotRepo = project.findProperty("spongeSnapshotRepo") as String?
        val spongeReleaseRepo = project.findProperty("spongeReleaseRepo") as String?
        if (spongeReleaseRepo != null && spongeSnapshotRepo != null) {
            publishSnapshotsTo("sponge", spongeSnapshotRepo)
            publishReleasesTo("sponge", spongeReleaseRepo)
        }
    }

    extensions.configure(SpotlessExtension::class) {
        fun FormatExtension.standardOptions() {
            endWithNewline()
            indentWithSpaces(4)
            trimTrailingWhitespace()
            this.toggleOffOn("@formatter:off", "@formatter:on")
        }

        java {
            target("src/*/java/**/*.java", "src/*/groovy/**/*.java")
            standardOptions()
            formatAnnotations()
            importOrderFile(rootProject.file(".spotless/sponge.importorder"))
            removeUnusedImports()
        }

        project.plugins.withId("groovy") {
            groovy {
                standardOptions()
                excludeJava()
                importOrderFile(rootProject.file(".spotless/sponge.importorder"))
            }
        }

        kotlinGradle {
            standardOptions()
        }
    }

    extensions.configure(IndraSpotlessLicenserExtension::class) {
        val name: String by project
        val organization: String by project
        val projectUrl: String by project

        licenseHeaderFile(rootProject.file("HEADER.txt"))
        property("name", name)
        property("organization", organization)
        property("url", projectUrl)
    }

    extensions.findByType(IndraPluginPublishingExtension::class)?.apply {
        pluginIdBase("$group.gradle")
        website("https://spongepowered.org/")
    }

    extensions.findByType(CrossdocExtension::class)?.apply {
        baseUrl(providers.gradleProperty("javadocLinkRoot"))
        nameBasedDocumentationUrlProvider {
            projectNamePrefix.set("spongegradle-")
            lowercaseProjectName.set(true)
        }
    }
}
