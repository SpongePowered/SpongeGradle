package org.spongepowered.gradle.meta

import org.spongepowered.plugin.meta.PluginMetadata

import java.util.regex.Pattern

abstract class PluginElement(id: String, var name: String? = id) {

    var id: String?
        get() = this.name
        set(id) {
            if (this.name == id) {
                return
            }

            assert(!this.registered) { "Cannot change plugin ID after element was registered" }
            assert(Pattern.matches(PluginMetadata.ID_PATTERN.pattern(), id)) { "Plugin ID must match pattern " + PluginMetadata.ID_PATTERN.pattern() }
            this.name = id
        }

    private var registered: Boolean = false


    fun register() {
        assert(Pattern.matches(PluginMetadata.ID_PATTERN.pattern(), name)) { "Plugin ID must match pattern " + PluginMetadata.ID_PATTERN.pattern() }
        this.registered = true
    }
}
