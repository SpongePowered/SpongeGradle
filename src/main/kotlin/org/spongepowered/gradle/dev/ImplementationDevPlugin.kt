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
package org.spongepowered.gradle.dev

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getting
import org.spongepowered.gradle.meta.BundleMetaPlugin
import org.spongepowered.gradle.meta.GenerateMetadata
import org.spongepowered.gradle.util.Constants

open class SpongeImpl() : SpongeDevExtension() {

    var common: Project? = null
    val extraDeps: MutableList<SourceSetOutput> = mutableListOf()
}

open class ImplementationDevPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val impl = project.extensions.create(Constants.SPONGE_DEV_EXTENSION, SpongeImpl::class.java)

        project.plugins.apply {
            apply("java-library")
            apply(BaseDevPlugin::class.java)
            apply(SpongeDevPlugin::class.java)
            apply(BundleMetaPlugin::class.java)
        }

        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.apply {
            val mainImpl = getByName("main")
            create("java6") {
                compileClasspath = project.files(compileClasspath, mainImpl.compileClasspath, mainImpl.output)
            }
        }
        project.afterEvaluate {
            impl.common?.let {
                val register = tasks.register("resolveApiRevision", ResolveApiVersionTask::class.java)
                this.tasks.getting(GenerateMetadata::class) {
                    dependsOn(register)
                }
                afterEvaluate {
                    this.dependencies.apply {
                        add("implementation", it)
                        impl.extraDeps.forEach {
                            add("implementation", it)
                        }
                    }
                }
            }
        }

        project.configurations.getByName("compile") {
            exclude(mapOf("module" to "asm"))
            exclude(mapOf("module" to "asm-commons"))
            exclude(mapOf("module" to "asm-tree"))
        }

        // TODO - create nested dependency of metas.
        project.plugins.apply("com.github.johnrengelman.shadow")


        project.tasks.apply {
            getting(Jar::class) {
                classifier = "base"
            }
            // TODO - figure out ForgeGradle 3 reobf stuff

        }
        project.tasks.apply {
            getting(ShadowJar::class) {
                project.artifacts.add("archives", this)
            }
        }

    }
}