package org.spongepowered.gradle.dev

import org.gradle.api.Plugin
import org.gradle.api.Project

open class MixinDevPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // First things first, apply the Java setup
        project.plugins.apply(BaseDevPlugin::class.java)


        project.repositories.apply {
            maven {
                name = "forge"
                setUrl("https://files.minecraftforge.net/repo")
            }
        }
        project.dependencies.apply {
            // Added for runtime decompiling with Mixins for debugging
            add("runtime", "net.minecraftforge:forgeflower:1.5.380.23")
        }
        // TODO - Apply Mixin Gradle and configure it
    }
}