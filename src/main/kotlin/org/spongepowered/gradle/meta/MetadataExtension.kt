package org.spongepowered.gradle.meta

import org.gradle.api.Action
import org.gradle.api.Project

open class MetadataExtension(project: Project, id: String) : MetadataBaseExtension(project) {

    val plugin: Plugin

    init {
        this.plugin = Plugin(project, id)
    }

    override fun createPlugin(name: String): Plugin {
        if (name == plugin.name) {
            plugin.register()
            return plugin
        }

        return super.createPlugin(name)
    }

    fun plugin(action: Action<Plugin>) {
        action.execute(plugin)
    }
}
