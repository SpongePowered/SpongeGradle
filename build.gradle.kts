import net.kyori.indra.IndraExtension
import net.kyori.indra.gradle.IndraPluginPublishingExtension
import net.kyori.indra.util.grgit
import org.cadixdev.gradle.licenser.LicenseExtension

plugins {
    id("com.gradle.plugin-publish") apply false
    id("net.kyori.indra") apply false
    id("net.kyori.indra.license-header") apply false
    id("net.kyori.indra.publishing.gradle-plugin") apply false
}

group = "org.spongepowered"
version = "1.0.3-SNAPSHOT"

subprojects {
    plugins.apply {
        apply(JavaGradlePluginPlugin::class)
        apply("com.gradle.plugin-publish")
        apply("net.kyori.indra")
        apply("net.kyori.indra.license-header")
        apply("net.kyori.indra.publishing.gradle-plugin")
    }

    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }

    dependencies {
        "compileOnlyApi"("org.checkerframework:checker-qual:3.10.0")
    }

    tasks.withType(Jar::class).configureEach {
        manifest.attributes(
                "Git-Commit" to grgit(project)?.head()?.id,
                "Git-Branch" to grgit(project)?.branch?.current()?.name,
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
            ci = true
            publishing = true
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

    extensions.configure(com.gradle.publish.PluginBundleExtension::class) {
        website = "https://spongepowered.org/"
    }

    extensions.configure(LicenseExtension::class) {
        val name: String by project
        val organization: String by project
        val projectUrl: String by project

        (this as ExtensionAware).extra.apply {
            this["name"] = name
            this["organization"] = organization
            this["url"] = projectUrl
        }
        header = rootProject.file("HEADER.txt")
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
        pluginIdBase.set("$group.gradle")
    }
}
