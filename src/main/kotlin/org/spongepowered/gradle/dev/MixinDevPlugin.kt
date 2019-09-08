package org.spongepowered.gradle.dev

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.spongepowered.asm.gradle.plugins.MixinGradlePlugin

class MixinDevPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // First things first, apply the Java setup
        target.plugins.apply(BaseDevPlugin::class.java)
        target.buildscript.dependencies.apply {
            add("classpath", "org.spongepowered:mixingradle:0.6-SNAPSHOT")
            add("classpath", "gradle.plugin.net.minecrell:vanillagradle:2.2-5")
        }
        target.plugins.apply(MixinGradlePlugin::class.java)
    }
}