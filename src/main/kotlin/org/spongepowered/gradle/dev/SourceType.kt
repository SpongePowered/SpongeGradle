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

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

enum class SourceType {
    Default {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            // do nothing
        }
    },
    Launch {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            dev.launchSourceSets.add(newSet)
            val launchImpl = project.configurations.named(newSet.implementationConfigurationName)
            val launchCompile = project.configurations.named(newSet.compileConfigurationName)
            dev.accessorSourceSets.all {
                System.out.println("[${project.name}] Adding SourceSet(${newSet.name}: Launch) to compile classpath: ${dev.project.name}:${this.name}")
                this.compileClasspath += newSet.output
                val accessorImplName = this.implementationConfigurationName
                val accessorCompileName = this.compileConfigurationName
                launchImpl.configure {
                    dependencies.add(accessorImplName, this)
                }
                launchCompile.configure {
                    dependencies.add(accessorCompileName, this)
                }
            }

            dev.mixinSourceSets.all {
                System.out.println("[${project.name}] Adding SourceSet(${newSet.name}: Launch) to compile classpath: ${dev.project.name}:${this.name}")
                this.compileClasspath += newSet.output
                val mixinImplName = this.implementationConfigurationName
                val mixinCompileName = this.compileConfigurationName
                launchImpl.configure {
                    dependencies.add(mixinImplName, this)
                }
                launchCompile.configure {
                    dependencies.add(mixinCompileName, this)
                }
            }
        }
    },
    Accessor {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            dev.accessorSourceSets.add(newSet)
            val accessorImplName = newSet.implementationConfigurationName
            val accessorImpl = project.configurations.named(accessorImplName)
            val accessorCompileName = newSet.compileConfigurationName
            val accessorCompile = project.configurations.named(accessorCompileName)

            dev.mixinSourceSets.all {
                System.out.println("[${project.name}] Adding SourceSet(${newSet.name}: Accessor) to compile classpath: ${dev.project.name}:${this.name}")
                this.compileClasspath += newSet.output
                val mixinImplName = this.implementationConfigurationName
                accessorImpl.configure {
                    dependencies.add(mixinImplName, this)
                }
                val mixinCompileName = this.compileConfigurationName
                accessorCompile.configure {
                    dependencies.add(mixinCompileName, this)
                }
            }
        }
    },
    Mixin {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            dev.mixinSourceSets.add(newSet)
            project.configurations.maybeCreate(newSet.implementationConfigurationName)
            project.configurations.maybeCreate(newSet.compileConfigurationName)
        }
    },
    Invalid {
        override fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            newSet.java.srcDir(project.projectDir.path + "invalid" + File.pathSeparator + "src" + File.pathSeparator + "main" + File.pathSeparator + "java")
            dev.invalidSourceSets.add(newSet)
            dev.launchSourceSets.all {
                System.out.println("[${project.name}] Adding ${this.name}(implementation: ${dev.project.name}:${newSet.name})")
                val launchImpl = project.configurations.named(this.implementationConfigurationName)
                val launchCompile = project.configurations.named(this.compileConfigurationName)
                launchImpl.configure {
                    dependencies.add(newSet.implementationConfigurationName, this)
                }
                launchCompile.configure {
                    dependencies.add(newSet.compileConfigurationName, this)
                }
                newSet.compileClasspath += this.output
            }
            dev.mixinSourceSets.all {
                val mixinImpl = project.configurations.named(this.implementationConfigurationName)
                val mixinCompile = project.configurations.named(this.compileConfigurationName)
                mixinImpl.configure {
                    dependencies.add(newSet.implementationConfigurationName, this)
                }
                mixinCompile.configure {
                    dependencies.add(newSet.compileConfigurationName, this)
                }
                newSet.compileClasspath += this.output
            }
            dev.accessorSourceSets.all {
                val accessorImpl = project.configurations.named(this.implementationConfigurationName)
                val accessorCompile = project.configurations.named(this.compileConfigurationName)
                accessorImpl.configure {
                    dependencies.add(newSet.implementationConfigurationName, this)
                }
                accessorCompile.configure {
                    dependencies.add(newSet.compileConfigurationName, this)
                }
                newSet.compileClasspath += this.output
            }
            project.tasks.named("compileJava").configure {
                (this as JavaCompile).apply {
                    val map = newSet.java.srcDirTrees.map { it.dir.path }
                    System.out.println("Excluding ${map} ")
                    exclude(map)
                }
            }
        }
    };

    abstract fun onSourceSetCreated(newSet: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project)
}