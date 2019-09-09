package org.spongepowered.gradle.ore

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.LinkedHashMap

open class OreDeployPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.findByName("signArchives")?.let {
            project.tasks.register("oreDeploy", OreDeployTask::class.java) {
                dependsOn(it)
            }
        }
    }

}
