object SpongeGradle {

    const val group = Groups.sponge
    const val version = "0.11.0-SNAPSHOT"
    const val organization = "SpongePowered"
    const val url = "https://www.spongepowered.org"
    const val name = "SpongeGradle"

}

object Deps {
    const val httpmime = "${Groups.httpcomp}:${Libs.httpmime}:${Versions.httpmime}"
    const val pluginMeta = "${Groups.sponge}:${Libs.pluginMeta}:${Versions.pluginMeta}"
    const val asm = "${Groups.asm}:${Libs.asm}:${Versions.asm}"
    const val groovy = "${Groups.groovy}:${Libs.groovy}:${Versions.groovy}"
    const val jsr = "${Groups.findbugs}:${Libs.jsr}:${Versions.jsr305}"

    object Script {
        const val gradlePublish = "com.gradle.plugin-publish"
        const val licenser = "net.minecrell.licenser"
    }
}

object Tags {
    const val minecraft = "minecraft"
    const val sponge = "sponge"
}

object GradlePlugins {
    object Main {
        const val id = "org.spongepowered.gradle"
        const val clazz = "org.spongepowered.gradle.SpongeGradlePlugin"
    }
    object MetaBase {
        const val id = "org.spongepowered.meta.base"
        const val clazz = "org.spongepowered.gradle.meta.MetadataBasePlugin"
    }
    object Meta {
        const val id = "org.spongepowered.meta"
        const val clazz = "org.spongepowered.gradle.meta.MetadataPlugin"
        const val name = "Sponge Plugin metadata generator"
        const val desc = "Gradle plugin for automatically generating a mcmod.info file with the project properties"
    }
    object PluginBase {
        const val id = "org.spongepowered.plugin.base"
        const val clazz = "org.spongepowered.gradle.plugin.SpongePluginBasePlugin"
    }
    object PluginPlugin {
        const val id = "org.spongepowered.plugin"
        const val clazz = "org.spongepowered.gradle.plugin.SpongePluginPlugin"
        const val name = "Sponge Plugin Gradle integration"
        const val desc = "Gradle plugin providing integration for plugins made for the Sponge platform"
    }
    object OreDeploy {
        const val id = "org.spongepowered.ore"
        const val name = "Ore Deploy Plugin"
        const val desc = "Gradle plugin for providing direct deployment to Ore"
    }
    object SpongeDistribution {
        const val id = "org.spongepowered.distribution"
        const val clazz = "org.spongepowered.gradle.sponge.deploy.DeployImplementationPlugin"
        const val name = "Sponge Distribution Plugin"
        const val desc = "Simplified distribution setup for deploying Sponge to maven repositories"
    }
    object ImplementationPlugin {
        const val id = "org.spongepowered.implementation"
        const val clazz = "org.spongepowered.gradle.sponge.impl.SpongeImplementationPlugin"
        const val name = "Sponge Implementation Support Plugin"
        const val desc = "Gradle plugin for simplified build setup for implementing Sponge"
    }

}
object Libs {
    const val httpmime = "httpmime"
    const val pluginMeta = "plugin-meta"
    const val asm = "asm"
    const val groovy = "groovy-all"
    const val jsr = "jsr305"
}

object Versions {
    const val httpmime = "4.5.3"
    const val pluginMeta = "0.4.1"
    const val asm = "5.2"
    const val groovy = "2.4.12:indy"
    const val jsr305 = "3.0.2"
    const val gradlePublish = "0.9.10"
    const val licenser = "0.4"
}

object Groups {
    const val sponge = "org.spongepowered"
    const val httpcomp = "org.apache.httpcomponents"
    const val asm = "org.ow2.asm"
    const val groovy = "org.codehaus.groovy"
    const val findbugs = "com.google.code.findbugs"
}
