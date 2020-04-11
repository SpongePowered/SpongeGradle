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
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
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
                project.getLogger().lifecycle("[${project.name}] Configuring devOutput Jar")

                from(impl.common.map { it.configurations.named("devOutput") })
            }
        }


//        project.dependencies.apply {
//            add("implementation", common)
//        }
        project.afterEvaluate {
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

open class SpongeImpl(project: Project) : CommonDevExtension(project = project) {

    val addForgeFlower: Property<Boolean> = defaultForgeFlowerProperty()

    override public fun common(commonProjectProvider: Provider<Project>) {
        val implExtension = this
        this.common.set(commonProjectProvider.map {
            val commonProject = it
            commonProject.plugins.withType(CommonImplementationDevPlugin::class.java).whenPluginAdded {
                commonProject.afterEvaluate {
                    commonProject.extensions.configure(CommonDevExtension::class.java) {
                        val implProject = implExtension.project
                        implProject.getLogger().lifecycle("[${implProject.name}] Configuring SubProject(${commonProject.path}) dependencies to ${implProject.name}")
                        this.mixinSourceSets.all {
                            val commonMixin = this
                            val commonMixinImpl = commonProject.configurations.named(this.implementationConfigurationName)
                            val commonMixinCompile = commonProject.configurations.named(this.compileConfigurationName)
                            implExtension.mixinSourceSets.all {
                                applyNamedDependencyOnOutput(commonMixin, implProject, this, implementationConfigurationName, commonProject)
                                applyNamedDependencyOnConfiguration(commonMixinImpl, commonProject, implProject, this, implementationConfigurationName)
                                applyNamedDependencyOnConfiguration(commonMixinCompile, commonProject, implProject, this, compileConfigurationName)
                            }
                            implExtension.invalidSourceSets.all {
                                this.compileClasspath += commonMixin.output
                                applyNamedDependencyOnOutput(commonMixin, implProject, this, implementationConfigurationName, commonProject)
                            }
                        }
                        this.accessorSourceSets.all {
                            val commonAccessor = this
                            val commonAccessorCompile = commonProject.configurations.named(this.compileConfigurationName)
                            val commonAccessorImpl = commonProject.configurations.named(this.implementationConfigurationName)
                            implExtension.mixinSourceSets.all {
                                applyNamedDependencyOnOutput(commonAccessor, implProject, this, implementationConfigurationName, commonProject)
                            }
                            implExtension.accessorSourceSets.all {
                                applyNamedDependencyOnOutput(commonAccessor, implProject, this, implementationConfigurationName, commonProject)
                                applyNamedDependencyOnConfiguration(commonAccessorCompile, commonProject, implProject, this, compileConfigurationName)
                                applyNamedDependencyOnConfiguration(commonAccessorImpl, commonProject, implProject, this, implementationConfigurationName)
                            }
                            implExtension.invalidSourceSets.all {
                                applyNamedDependencyOnConfiguration(commonAccessorImpl, commonProject, implProject, this, implementationConfigurationName)
                                applyNamedDependencyOnConfiguration(commonAccessorCompile, commonProject, implProject, this, compileConfigurationName)
                                applyNamedDependencyOnOutput(commonAccessor, implProject, this, implementationConfigurationName, commonProject)
                            }
                            val sourceSets = implProject.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
                            val implMain = sourceSets["main"]
                            applyNamedDependencyOnOutput(commonAccessor, implProject, implMain, implMain.implementationConfigurationName, commonProject)
                        }
                        this.launchSourceSets.all {
                            val commonLaunch = this
                            val commonLaunchImpl = commonProject.configurations.named(commonLaunch.implementationConfigurationName)
                            val commonLaunchCompile = commonProject.configurations.named(commonLaunch.compileConfigurationName)
                            implExtension.mixinSourceSets.all {
                                applyNamedDependencyOnConfiguration(commonLaunchImpl, commonProject, implProject, this, implementationConfigurationName)
                                applyNamedDependencyOnConfiguration(commonLaunchCompile, commonProject, implProject, this, compileConfigurationName)
                                applyNamedDependencyOnOutput(commonLaunch, implProject, this, implementationConfigurationName, commonProject)
                            }
                            implExtension.launchSourceSets.all {
                                applyNamedDependencyOnConfiguration(commonLaunchImpl, commonProject, implProject, this, implementationConfigurationName)
                                applyNamedDependencyOnConfiguration(commonLaunchCompile, commonProject, implProject, this, compileConfigurationName)
                                applyNamedDependencyOnOutput(commonLaunch, implProject, this, implementationConfigurationName, commonProject)
                            }
                            implExtension.accessorSourceSets.all {
                                applyNamedDependencyOnConfiguration(commonLaunchImpl, commonProject, implProject, this, implementationConfigurationName)
                                applyNamedDependencyOnConfiguration(commonLaunchCompile, commonProject, implProject, this, compileConfigurationName)
                                applyNamedDependencyOnOutput(commonLaunch, implProject, this, implementationConfigurationName, commonProject)
                            }
                            implExtension.invalidSourceSets.all {
                                applyNamedDependencyOnConfiguration(commonLaunchImpl, commonProject, implProject, this, implementationConfigurationName)
                                applyNamedDependencyOnConfiguration(commonLaunchCompile, commonProject, implProject, this, compileConfigurationName)
                                applyNamedDependencyOnOutput(commonLaunch, implProject, this, implementationConfigurationName, commonProject)
                            }
                        }
                        this.invalidSourceSets.all {
                            val commonInvalid = this
                            implExtension.invalidSourceSets.all {
                                applyNamedDependencyOnOutput(commonInvalid, implProject, this, implementationConfigurationName, commonProject)
                            }
                        }
                        this.defaultSourceSets.all {
                            val commonDefault = this
                            implExtension.defaultSourceSets.all {
                                val defaultCompile = commonProject.configurations.named(commonDefault.compileConfigurationName)
                                val defaultImpl = commonProject.configurations.named(commonDefault.implementationConfigurationName)
                                if (this.name.equals(commonDefault.name)) {
                                    applyNamedDependencyOnOutput(commonDefault, implProject, this, implementationConfigurationName, commonProject)
                                    applyNamedDependencyOnConfiguration(defaultCompile, commonProject, implProject,this, compileConfigurationName)
                                    applyNamedDependencyOnConfiguration(defaultImpl, commonProject, implProject,this, implementationConfigurationName)
                                }
                            }
                        }
                    }

                }
            }
            commonProject
        })
    }

    private fun applyNamedDependencyOnOutput(commonSet: SourceSet, parentProject: Project, targetSet: SourceSet, configurationName: String, childProject: Project) {
        parentProject.getLogger().lifecycle("[${parentProject.name}] Adding Child ${childProject.path}(${commonSet.name}).output to Parent ${parentProject.path}(${targetSet.name}).$configurationName ")
        targetSet.compileClasspath += commonSet.output
        if (!targetSet.name.equals("main")) {
            targetSet.compileClasspath += commonSet.compileClasspath
        }
        parentProject.dependencies.add(configurationName, commonSet.output)
    }

    private fun applyNamedDependencyOnConfiguration(configProvider: NamedDomainObjectProvider<Configuration>, childProject: Project, implProject: Project, targetSet: SourceSet, dependencyConfigName: String) {
        configProvider.configure {
            implProject.getLogger().lifecycle("[${implProject.name}] Extending Parent ${implProject.path}(${targetSet.name}).$dependencyConfigName from Child ${childProject.path}.${this.name}")
            val configToExtend = this
            implProject.configurations.named(dependencyConfigName).configure {
                this.extendsFrom(configToExtend)
            }
        }
    }

    fun defaultForgeFlowerProperty(): Property<Boolean> {
        val useForgeFlower = project.objects.property(Boolean::class.java);
        if (!useForgeFlower.isPresent) {
            useForgeFlower.set(false);
        }
        return useForgeFlower;
    }
}
