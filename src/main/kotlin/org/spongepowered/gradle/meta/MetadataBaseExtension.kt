/*
 * This file is part of SpongeGradle, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.gradle.meta

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.codehaus.groovy.runtime.MethodClosure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.spongepowered.plugin.meta.PluginDependency
import org.spongepowered.plugin.meta.PluginMetadata
import java.util.ArrayList
import java.util.function.Consumer

open class MetadataBaseExtension(protected val project: Project) {
    val plugins: NamedDomainObjectContainer<Plugin>

    init {
        this.plugins = project.container(Plugin::class.java, MethodClosure(this, "createPlugin"))
    }

    protected open fun createPlugin(name: String): Plugin {
        val plugin = Plugin(project, name)
        plugin.register()
        return plugin
    }

    fun plugins(closure: Closure<*>) {
        plugins.configure(closure)
    }

    class Plugin(protected val project: Project, id: String) : PluginElement(id) {
        val meta: Meta

        init {
            this.meta = Meta(project)
        }

        fun meta(action: Action<Meta>) {
            action.execute(meta)
        }

        fun meta(@DelegatesTo(Plugin.Meta::class) closure: Closure<*>) {
            project.configure(meta, closure)
        }

        class Meta(project: Project) : Consumer<PluginMetadata> {

            private var name: Any? = null
            private var version: Any? = null
            private var description: Any? = null
            private var url: Any? = null
            var dependencies: NamedDomainObjectContainer<Dependency>? = null
            var authors: List<String> = ArrayList()

            init {
                this.dependencies = project.container(Dependency::class.java)
            }

            fun getName(): String {
                return resolve(this.name)!!
            }

            fun setName(name: String) {
                this.name = name
            }

            fun setName(name: Any) {
                this.name = name
            }

            fun getVersion(): String {
                return resolve(this.version)!!
            }

            fun setVersion(version: String) {
                this.version = version
            }

            fun setVersion(version: Any) {
                this.version = version
            }

            fun getDescription(): String {
                return resolve(this.description)!!
            }

            fun setDescription(description: String) {
                this.description = description
            }

            fun setDescription(description: Any) {
                this.description = description
            }

            fun getUrl(): String {
                return resolve(this.url)!!
            }

            fun setUrl(url: String) {
                this.url = url
            }

            fun setUrl(url: Any) {
                this.url = url
            }

            fun inherit(project: Project) {
                this.name = project.name
                this.version = project.version
                this.description = project.description
                this.url = project.findProperty("url")
            }

            fun dependencies(closure: Closure<*>) {
                dependencies!!.configure(closure)
            }

            override fun accept(meta: PluginMetadata) {
                meta.name = getName()
                meta.version = getVersion()
                meta.description = getDescription()
                meta.url = getUrl()

                dependencies?.forEach { meta.addDependency(it.build()) }

                meta.authors.addAll(this.authors)
            }

            class Dependency(id: String) : PluginElement(id) {

                private var version: Any? = null
                var optional = false

                fun getVersion(): String {
                    return resolve(version)!!
                }

                fun setVersion(version: String) {
                    this.version = version
                }

                fun setVersion(version: Any) {
                    this.version = version
                }

                fun forceVersion(version: String) {
                    this.version = "[$version]"
                }

                fun forceVersion(version: Any) {
                    this.version = "[" + resolve(version) + "]"
                }

                fun build(): PluginDependency {
                    return PluginDependency(PluginDependency.LoadOrder.BEFORE, id!!, getVersion(), optional)
                }

                fun isOptional(): Boolean {
                    return optional
                }
            }
        }
    }
}

fun resolve(o: Any?): String? {
    if (o == null) {
        return null
    }

    if (o is String) {
        return o
    }

    if (o is Closure<*>) {
        return resolve(o.call())
    }

    return o as String
}
