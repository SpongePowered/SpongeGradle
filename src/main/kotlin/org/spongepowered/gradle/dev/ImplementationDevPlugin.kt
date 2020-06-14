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
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType
import org.spongepowered.gradle.util.Constants

open class ImplementationDevPlugin : CommonImplementationDevPlugin() {
    override fun apply(project: Project) {
        val impl = project.extensions.create(Constants.SPONGE_DEV_EXTENSION, SpongeImpl::class.java, project)
        // This is basically to ensure that common can be configured with the appropriate
        // conventions before we continue adding more.
        super.apply(project)

        project.tasks.apply {
            withType(Jar::class).named("jar").configure {
                debug(project.logger, "[${project.name}] Configuring devOutput Jar")

                from(impl.common.map { it.configurations.named("devOutput") })
            }
        }

        project.afterEvaluate {
            if (impl.addForgeFlower.get()) {
                project.repositories {
                    maven("https://files.minecraftforge.net/maven")
                }
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

    public override fun common(commonProjectProvider: Provider<Project>) {
        val implExtension = this
        this.common.set(commonProjectProvider)

        this.project.afterEvaluate {
            val commonProject = implExtension.common.get()
            if (this.childProjects.contains(commonProject.name)) {
                commonProject.afterEvaluate(applyCommonDependenciesForMatchingSets(implExtension))
            } else {
                project.afterEvaluate {
                    applyCommonDependenciesForMatchingSets(implExtension)(commonProject)
                }
            }
        }
    }

    private fun applyCommonDependenciesForMatchingSets(implExtension: SpongeImpl): (Project).() -> Unit {
        return {
            this.configure<CommonDevExtension> {
                val commonProject = this.project
                val commonSourceSets = commonProject.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
                val implProject = implExtension.project
                val implSourceSets = implProject.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
                debug(implProject.logger, "[${implProject.name}] Configuring SubProject(${commonProject.path}) dependencies to ${implProject.path}")
                this.addedSourceSets.all {
                    val commonSet = this
                    val commonSource = commonSourceSets.named(this.name).get()
                    implExtension.addedSourceSets.findByName(commonSet.name)?.apply {
                        val implSource = implSourceSets.named(this.name).get()
                        applyNamedDependencyOnOutput(commonSource, implProject, implSource, implSource.implementationConfigurationName, commonProject)
                        commonSet.configurations.forEach {
                            val config = commonProject.configurations.named(it)
                            applyNamedDependencyOnConfiguration(config, commonProject, implProject, implSource, implSource.implementationConfigurationName)
                        }
                    }
                }
                this.mixinSourceSets.all {
                    val commonMixin = this
                    implExtension.mixinSourceSets.all {
                        applyNamedDependencyOnOutput(commonMixin, implProject, this, implementationConfigurationName, commonProject)
                    }
                    implExtension.invalidSourceSets.all {
                        this.compileClasspath += commonMixin.output
                        applyNamedDependencyOnOutput(commonMixin, implProject, this, implementationConfigurationName, commonProject)
                    }
                }
                this.accessorSourceSets.all {
                    val commonAccessor = this
                    implExtension.mixinSourceSets.all {
                        applyNamedDependencyOnOutput(commonAccessor, implProject, this, implementationConfigurationName, commonProject)
                    }
                    implExtension.accessorSourceSets.all {
                        applyNamedDependencyOnOutput(commonAccessor, implProject, this, implementationConfigurationName, commonProject)
                    }
                    implExtension.invalidSourceSets.all {
                        applyNamedDependencyOnOutput(commonAccessor, implProject, this, implementationConfigurationName, commonProject)
                    }
                    val sourceSets = implProject.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
                    val implMain = sourceSets["main"]
                    applyNamedDependencyOnOutput(commonAccessor, implProject, implMain, implMain.implementationConfigurationName, commonProject)
                }
                this.launchSourceSets.all {
                    val commonLaunch = this
                    implExtension.mixinSourceSets.all {
                        applyNamedDependencyOnOutput(commonLaunch, implProject, this, implementationConfigurationName, commonProject)
                    }
                    implExtension.launchSourceSets.all {
                        applyNamedDependencyOnOutput(commonLaunch, implProject, this, implementationConfigurationName, commonProject)
                    }
                    implExtension.accessorSourceSets.all {
                        applyNamedDependencyOnOutput(commonLaunch, implProject, this, implementationConfigurationName, commonProject)
                    }
                    implExtension.invalidSourceSets.all {
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
                        if (this.name.equals(commonDefault.name)) {
                            applyNamedDependencyOnOutput(commonDefault, implProject, this, implementationConfigurationName, commonProject)
                        }
                    }
                }
                implExtension.licenseProject.set(this.licenseProject)
                implExtension.organization.set(this.organization)
                implExtension.url.set(this.url)
            }
        }
    }

    private fun applyNamedDependencyOnOutput(commonSet: SourceSet, parentProject: Project, targetSet: SourceSet, configurationName: String, childProject: Project) {
        debug(parentProject.logger, "[${parentProject.name}] Adding Child ${childProject.path}(${commonSet.name}).output to Parent ${parentProject.path}(${targetSet.name}).$configurationName ")
        targetSet.compileClasspath += commonSet.output
//        if (!targetSet.name.equals("main")) {
//            targetSet.compileClasspath += commonSet.compileClasspath
//        }
        parentProject.dependencies.add(configurationName, commonSet.output)
    }

    private fun applyNamedDependencyOnConfiguration(configProvider: NamedDomainObjectProvider<Configuration>, childProject: Project, implProject: Project, targetSet: SourceSet, dependencyConfigName: String) {
        configProvider.configure {
            debug(implProject.logger, "[${implProject.name}] Extending Parent ${implProject.path}(${targetSet.name}).$dependencyConfigName from Child ${childProject.path}.${this.name}")
            val configToExtend = this
            if (configToExtend.isCanBeResolved) {
                targetSet.compileClasspath += configToExtend
            }
            implProject.configurations.named(dependencyConfigName).configure {
                this.extendsFrom(configToExtend)
            }
        }
    }

    fun defaultForgeFlowerProperty(): Property<Boolean> {
        val useForgeFlower = project.objects.property(Boolean::class.java)
        if (!useForgeFlower.isPresent) {
            useForgeFlower.set(false)
        }
        return useForgeFlower
    }
}
