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
        override fun onSourceSetCreated(newSet: SourceSet, main: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            // do nothing
        }
    },
    Launch {
        override fun onSourceSetCreated(newSet: SourceSet, main: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            dev.launchSourceSets.add(newSet)
            dependencies.add(main.apiConfigurationName, newSet.output)
            dev.accessorSourceSets.all {
                dependencies.add(this.implementationConfigurationName, newSet.output)
                this.compileClasspath += newSet.compileClasspath
            }
            dev.invalidSourceSets.all {
                dependencies.add(this.implementationConfigurationName, newSet.output)
                this.compileClasspath += newSet.compileClasspath
            }
            dev.mixinSourceSets.all {
                this.compileClasspath += newSet.compileClasspath
            }
        }
    },
    Accessor {
        override fun onSourceSetCreated(newSet: SourceSet, main: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            main.compileClasspath += newSet.compileClasspath
            dev.launchSourceSets.all {
                dependencies.add(newSet.implementationConfigurationName, this.output)
                newSet.compileClasspath += this.compileClasspath
            }
            dev.accessorSourceSets.add(newSet)
            dependencies.add(main.apiConfigurationName, newSet.output)
            dev.invalidSourceSets.all {
                dependencies.add(this.implementationConfigurationName, newSet.output)
                this.compileClasspath += newSet.compileClasspath
            }
            dev.mixinSourceSets.all {
                dependencies.add(this.implementationConfigurationName, newSet.output)
                this.compileClasspath += newSet.compileClasspath
            }
        }
    },
    Mixin {
        override fun onSourceSetCreated(newSet: SourceSet, main: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            dev.mixinSourceSets.add(newSet)
            System.out.println("[${project.name}] Adding SourceSet(${newSet.name}: Mixin) to compile classpath: ${main.output}")
            newSet.compileClasspath += main.output
            System.out.println("[${project.name}] SourceSet(${newSet.name}: Mixin) compile classpath is now: ${newSet.compileClasspath}")

        }
    },
    Invalid {
        override fun onSourceSetCreated(newSet: SourceSet, main: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project) {
            newSet.compileClasspath += main.compileClasspath
            newSet.java.srcDir(project.projectDir.path + "invalid" + File.pathSeparator + "src" + File.pathSeparator + "main" + File.pathSeparator + "java")

            dev.invalidSourceSets.add(newSet)
            dependencies.add(newSet.implementationConfigurationName, main.output)
            dev.launchSourceSets.all {
                dependencies.add(newSet.implementationConfigurationName, this.output)
                newSet.compileClasspath += this.compileClasspath
                newSet.compileClasspath += this.allSource
            }
            dev.mixinSourceSets.all {
                dependencies.add(newSet.implementationConfigurationName, this.output)
                newSet.compileClasspath += this.compileClasspath
                newSet.compileClasspath += this.allSource
            }
            dev.accessorSourceSets.all {
                dependencies.add(newSet.implementationConfigurationName, this.output)
                newSet.compileClasspath += this.compileClasspath
                newSet.compileClasspath += this.allSource
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

    abstract fun onSourceSetCreated(newSet: SourceSet, main: SourceSet, dev: CommonDevExtension, dependencies: DependencyHandler, project: Project)
}