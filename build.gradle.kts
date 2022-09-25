import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import net.kyori.indra.IndraExtension
import net.kyori.indra.crossdoc.CrossdocExtension
import net.kyori.indra.gradle.IndraPluginPublishingExtension
import net.kyori.indra.licenser.spotless.IndraSpotlessLicenserExtension

plugins {
    id("com.gradle.plugin-publish") apply false
    id("net.kyori.indra") apply false
    id("com.diffplug.spotless") apply false
    id("net.kyori.indra.crossdoc") apply false
    id("net.kyori.indra.licenser.spotless") apply false
    id("net.kyori.indra.publishing.gradle-plugin") apply false
}

group = "org.spongepowered"
version = "2.0.2"

subprojects {
    plugins.apply {
        apply(JavaGradlePluginPlugin::class)
        apply("com.gradle.plugin-publish")
        apply("net.kyori.indra")
        apply("net.kyori.indra.crossdoc")
        apply("net.kyori.indra.licenser.spotless")
        apply("net.kyori.indra.publishing.gradle-plugin")
        apply("net.kyori.indra.git")
    }

    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }

    dependencies {
        "compileOnlyApi"("org.checkerframework:checker-qual:3.24.0")
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

    tasks.named("publishPlugins") {
        onlyIf { net.kyori.indra.util.Versioning.isRelease(project) }
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
            standardOptions()
            formatAnnotations()
            importOrderFile(rootProject.file(".spotless/sponge.importorder"))
        }

        project.plugins.withId("groovy") {
            groovy {
                standardOptions()
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

    extensions.configure(SigningExtension::class) {
        val spongeSigningKey = project.findProperty("spongeSigningKey") as String?
        val spongeSigningPassword = project.findProperty("spongeSigningPassword") as String?
        if (spongeSigningKey != null && spongeSigningPassword != null) {
            val keyFile = file(spongeSigningKey)
            if (keyFile.exists()) {
                useInMemoryPgpKeys(file(spongeSigningKey).readText(Charsets.UTF_8), spongeSigningPassword)
            } else {
                useInMemoryPgpKeys(spongeSigningKey, spongeSigningPassword)
            }
        } else {
            signatories = PgpSignatoryProvider() // don't use gpg agent
        }
    }

    extensions.findByType(IndraPluginPublishingExtension::class)?.apply {
        pluginIdBase("$group.gradle")
        website("https://spongepowered.org/")
    }

    extensions.configure(CrossdocExtension::class) {
        baseUrl(providers.gradleProperty("javadocLinkRoot"))
        nameBasedDocumentationUrlProvider {
            projectNamePrefix.set("spongegradle-")
            lowercaseProjectName.set(true)
        }
    }
}
