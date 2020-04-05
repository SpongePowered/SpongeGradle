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
            newSet.compileClasspath += main.compileClasspath
            newSet.compileClasspath += main.output
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
            }
            dev.mixinSourceSets.all {
                dependencies.add(newSet.implementationConfigurationName, this.output)
                newSet.compileClasspath += this.compileClasspath
            }
            dev.accessorSourceSets.all {
                dependencies.add(newSet.implementationConfigurationName, this.output)
                newSet.compileClasspath += this.compileClasspath
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