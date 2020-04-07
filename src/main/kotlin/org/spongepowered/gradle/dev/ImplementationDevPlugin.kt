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
@file:Suppress("UnstableApiUsage")

package org.spongepowered.gradle.dev

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.spongepowered.gradle.util.Constants


open class ImplementationDevPlugin : CommonImplementationDevPlugin() {
    override fun apply(project: Project) {
        val impl = project.extensions.create(Constants.SPONGE_DEV_EXTENSION, SpongeImpl::class.java, project)
        // This is basically to ensure that common can be configured with the appropriate
        // conventions before we continue adding more.
        super.apply(project)

        project.tasks.apply {
            withType(Jar::class).named("jar").configure {
                System.out.println("[${project.name}] Configuring devOutput Jar")

                from(impl.common.map { it.configurations.named("devOutput") })
            }
        }


//        project.dependencies.apply {
//            add("implementation", common)
//        }
        if (impl.addForgeFlower.get()) {
            project.repositories {
                maven("https://files.minecraftforge.net/maven")
            }


//            common.afterEvaluate {
//                impl.parent.dependencies.apply {
//                    add("implementation", common)
//                    add("runtime", "net.minecraftforge:forgeflower:1.5.380.23")
//                    impl.extraDeps.forEach {
//                        add("implementation", it)
//                    }
//                }
//            }
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

open class SpongeImpl(val parent: Project) : CommonDevExtension(project = parent) {

    val extraDeps: MutableList<SourceSetOutput> = mutableListOf()
    val addForgeFlower: Property<Boolean> = defaultForgeFlowerProperty()

    public override fun common(commonProject: Project) {
        this.common.set(commonProject)
        commonProject.plugins.apply {
            if (this.findPlugin(CommonImplementationDevPlugin::class.java) == null) {
                apply(CommonImplementationDevPlugin::class.java)
            }
        }

    }

    public override fun common(commonProjectProvider: Provider<Project>) {
        this.common.set(commonProjectProvider.map {
            val commonProject = it
            if (!commonProject.plugins.hasPlugin(CommonImplementationDevPlugin::class.java)) {
                commonProject.plugins.apply(CommonImplementationDevPlugin::class.java)
            }
            val implExtension = this
            commonProject.extensions.findByType(CommonDevExtension::class.java)?.apply {
                this.addedSourceSets.all {

                }
                this.mixinSourceSets.all {
                    val commonMixin = this
                    implExtension.mixinSourceSets.all {
                        this.compileClasspath += commonMixin.compileClasspath
                        project.dependencies.add(this.compileOnlyConfigurationName, commonMixin.output)
                    }
                    implExtension.invalidSourceSets.all {
                        this.compileClasspath += commonMixin.output
                    }
                }
                this.accessorSourceSets.all {
                    val commonAccessor = this
                    implExtension.mixinSourceSets.all {
                        this.compileClasspath += commonAccessor.output
                        project.dependencies.add(this.compileOnlyConfigurationName, commonAccessor.output)
                    }
                    implExtension.launchSourceSets.all {
                        this.compileClasspath += commonAccessor.compileClasspath
                        project.dependencies.add(this.compileOnlyConfigurationName, commonAccessor.output)
                    }
                    implExtension.invalidSourceSets.all {
                        this.compileClasspath += commonAccessor.output
                    }
                }
                this.launchSourceSets.all {
                    val commonLaunch = this
                    implExtension.mixinSourceSets.all {
                        this.compileClasspath += commonLaunch.compileClasspath
                        project.dependencies.add(this.compileOnlyConfigurationName, commonLaunch.output)
                    }
                    implExtension.launchSourceSets.all {
                        this.compileClasspath += commonLaunch.compileClasspath
                        project.dependencies.add(this.compileOnlyConfigurationName, commonLaunch.output)
                    }
                    implExtension.accessorSourceSets.all {
                        this.compileClasspath += commonLaunch.output
                        project.dependencies.add(this.compileOnlyConfigurationName, commonLaunch.output)
                    }
                    implExtension.invalidSourceSets.all {
                        this.compileClasspath += commonLaunch.output
                    }
                }
                this.invalidSourceSets.all {
                    val commonInvalid = this
                    implExtension.mixinSourceSets.all {
                        this.compileClasspath += commonInvalid.compileClasspath
                        project.dependencies.add(this.compileOnlyConfigurationName, commonInvalid.output)
                    }
                    implExtension.launchSourceSets.all {
                        this.compileClasspath += commonInvalid.compileClasspath
                        project.dependencies.add(this.compileOnlyConfigurationName, commonInvalid.output)
                    }
                    implExtension.accessorSourceSets.all {
                        this.compileClasspath += commonInvalid.output
                        project.dependencies.add(this.compileOnlyConfigurationName, commonInvalid.output)
                    }
                    implExtension.invalidSourceSets.all {
                        this.compileClasspath += commonInvalid.output
                    }
                }
            }
            commonProject
        })
    }


    fun defaultForgeFlowerProperty(): Property<Boolean> {
        val useForgeFlower = parent.objects.property(Boolean::class.java);
        if (!useForgeFlower.isPresent) {
            useForgeFlower.set(false);
        }
        return useForgeFlower;
    }
}
