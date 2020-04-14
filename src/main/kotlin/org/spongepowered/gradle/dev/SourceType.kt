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

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

enum class SourceType {
    Default {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            project.getLogger().lifecycle("[${project.name}] Adding SourceSet ${dev.project.path}:${newSet.name} to default sets")
            dev.defaultSourceSets.add(newSet)
        }
    },
    Launch {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            project.getLogger().lifecycle("[${project.name}] Adding SourceSet ${dev.project.path}:${newSet.name} to launch sets")
            dev.launchSourceSets.add(newSet)
            dev.accessorSourceSets.all {
                val accessorImplName = this.implementationConfigurationName
                val accessorSet = this
                this.compileClasspath += newSet.output
                applyNamedDependencyOnOutput(newSet, accessorSet, project, accessorImplName)
            }

            dev.mixinSourceSets.all {
                val mixinImplName = this.implementationConfigurationName
                val mixinSet = this
                this.compileClasspath += newSet.output
                applyNamedDependencyOnOutput(newSet, mixinSet, project, mixinImplName)
            }
        }
    },
    Accessor {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            project.getLogger().lifecycle("[${project.name}] Adding SourceSet ${dev.project.path}:${newSet.name} to accessor sets")
            dev.accessorSourceSets.add(newSet)

            dev.mixinSourceSets.all {
                this.compileClasspath += newSet.output
                val mixinSet = this
                val mixinImplName = this.implementationConfigurationName
                applyNamedDependencyOnOutput(newSet, mixinSet, project, mixinImplName)
            }
        }
    },
    Mixin {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            project.getLogger().lifecycle("[${project.name}] Adding SourceSet ${dev.project.path}:${newSet.name} to mixin sets")
            dev.mixinSourceSets.add(newSet)
        }
    },
    Invalid {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            project.getLogger().lifecycle("[${project.name}] Adding SourceSet ${dev.project.path}:${newSet.name} to invalid sets")
            dev.invalidSourceSets.add(newSet)
            val newCompileName = newSet.compileConfigurationName
            val newImplName = newSet.implementationConfigurationName
            dev.launchSourceSets.all {
                applyNamedDependencyOnOutput(this, newSet, project, newImplName)
            }
            dev.mixinSourceSets.all {
                applyNamedDependencyOnOutput(this, newSet, project, newImplName)
            }
            dev.accessorSourceSets.all {
                applyNamedDependencyOnOutput(this, newSet, project, newImplName)
            }
            newSet.java {
                project.logger.lifecycle("[${project.name}] Changing Invalid SourceSet(${newSet.name}) source directory...")
                srcDir("invalid" + File.separator + "main" + File.separator + "java")
            }
            project.tasks.named("compileJava").configure {
                (this as JavaCompile).apply {
                    val map = newSet.java.srcDirs.map { it.path }
                    project.getLogger().lifecycle("Excluding ${map} ")
                    exclude(map)
                }
            }
        }

    };

    abstract fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project)

    fun applyConfigurationDependency(configuration: NamedDomainObjectProvider<Configuration>, project: Project, dev: CommonDevExtension, newSet: SourceSet, newConfigName: String, dependencies: DependencyHandler) {
        val configExtending = project.configurations.named(newConfigName)
        configuration.configure {
            val configToExtend = this
            project.getLogger().lifecycle("[${project.name}] Adding ${project.path}.${this.name} to ${dev.project.path}(${newSet.name}).$newConfigName")
            if (this.isCanBeResolved) {
                newSet.compileClasspath += this
            }
//            configExtending.get().extendsFrom(configToExtend)

        }
    }

    fun applyNamedDependencyOnOutput(sourceAdding: SourceSet, targetSource: SourceSet, implProject: Project, dependencyConfigName: String) {
        implProject.getLogger().lifecycle("[${implProject.name}] Adding ${implProject.path}(${sourceAdding.name}) to ${implProject.path}(${targetSource.name}).$dependencyConfigName")
        implProject.dependencies.add(dependencyConfigName, sourceAdding.output)
    }
}