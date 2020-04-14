object SpongeGradle {

    const val group = Groups.sponge
    const val version = "0.11.2-SNAPSHOT"
    const val organization = "SpongePowered"
    const val url = "https://www.spongepowered.org"
    const val name = "SpongeGradle"
    const val extension = "sponge"

}

object Deps {
    const val httpmime = "${Groups.httpcomp}:${Libs.httpmime}:${Versions.httpmime}"
    const val pluginMeta = "${Groups.sponge}:${Libs.pluginMeta}:${Versions.pluginMeta}"
    const val asm = "${Groups.asm}:${Libs.asm}:${Versions.asm}"
    const val jsr = "${Groups.findbugs}:${Libs.jsr}:${Versions.jsr305}"
    const val licenser = "net.minecrell.licenser:net.minecrell.licenser.gradle.plugin:0.4.1"
    const val mixingradle = "org.spongepowered:mixingradle:0.7-SNAPSHOT"
    const val shadow = "com.github.jengelman.gradle.plugins:shadow:4.0.4"

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
    object BaseDevPlugin {
        const val id = "org.spongepowered.gradle.base"
        const val clazz = "org.spongepowered.gradle.dev.BaseDevPlugin"
        const val name = "Base Development Plugin"
        const val desc = "A base plugin providing basic gradle plugins and sponge repository access"
    }
    object Meta {
        const val id = "org.spongepowered.gradle.meta"
        const val clazz = "org.spongepowered.gradle.meta.MetadataPlugin"
        const val name = "Sponge Plugin metadata generator"
        const val desc = "Gradle plugin for automatically generating a mcmod.info file with the project properties"
    }
    object BundleMeta {
        const val id = "org.spongepowered.gradle.meta.bundle"
        const val clazz = "org.spongepowered.gradle.meta.BundleMetaPlugin"
        const val name = "Bundled Plugin metadata generator"
        const val desc = "Gradle plugin that provides the project with nesting capabilities for PluginMeta representation"
    }
    object PluginDevPlugin {
        const val id = "org.spongepowered.gradle.plugin"
        const val clazz = "org.spongepowered.gradle.dev.PluginDevPlugin"
        const val name = "Sponge Plugin Developer Gradle integrations"
        const val desc = "Gradle plugin providing integration for plugins made for the Sponge platform"
    }
    object SpongeDev {
        const val id = "org.spongepowered.gradle.sponge.dev"
        const val clazz = "org.spongepowered.gradle.dev.SpongeDevPlugin"
        const val name = "Sponge Development Plugin"
        const val desc = "Gradle plugin to set up developing Sponge and it's implementations"
    }
    object SpongeDeploy {
        const val id = "org.spongepowered.gradle.sponge.deploy"
        const val clazz = "org.spongepowered.gradle.deploy.DeployImplementationPlugin"
        const val name = "Sponge Deployment Plugin"
        const val desc = "Gradle plugin to set up the deployment of Sponge"
    }
    object SpongeSort {
        const val id = "org.spongepowered.gradle.sort"
        const val clazz = "org.spongepowered.gradle.sort.SpongeSortingPlugin"
        const val name = "Sponge Sorting"
        const val desc = "Enables tasks for sorting specific types of things for Sponge related development"
    }
    object OreDeploy {
        const val id = "org.spongepowered.gradle.ore"
        const val name = "Ore Deploy Plugin"
        const val clazz = "org.spongepowered.gradle.ore.OreDeployPlugin"
        const val desc = "Gradle plugin for providing direct deployment to Ore"
    }
    object ImplementationPlugin {
        const val id = "org.spongepowered.gradle.sponge.impl"
        const val clazz = "org.spongepowered.gradle.dev.ImplementationDevPlugin"
        const val name = "Sponge Implementation Support Plugin"
        const val desc = "Gradle plugin for simplified build setup for implementing Sponge"
    }
    object CommonImplementationPlugin {
        const val id = "org.spongepowered.gradle.sponge.common"
        const val clazz = "org.spongepowered.gradle.dev.CommonImplementationDevPlugin"
        const val name = "Sponge Common implementation support plugin"
        const val desc = "Gradle plugin for setting up SpongeCommon's implementation setup"
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
    const val jsr305 = "3.0.2"
    const val gradlePublish = "0.11.0"
    const val licenser = "0.4"
}

object Groups {
    const val sponge = "org.spongepowered"
    const val httpcomp = "org.apache.httpcomponents"
    const val asm = "org.ow2.asm"
    const val groovy = "org.codehaus.groovy"
    const val findbugs = "com.google.code.findbugs"
}
