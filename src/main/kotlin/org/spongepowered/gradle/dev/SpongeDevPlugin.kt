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

import net.minecrell.gradle.licenser.LicenseExtension
import net.minecrell.gradle.licenser.Licenser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getting
import org.gradle.language.jvm.tasks.ProcessResources
import org.spongepowered.gradle.deploy.DeployImplementationExtension
import org.spongepowered.gradle.deploy.DeployImplementationPlugin
import org.spongepowered.gradle.sort.SpongeSortingPlugin
import org.spongepowered.gradle.util.Constants

open class SpongeDevExtension(val api: Project? = null) {
    var organization = "SpongePowered"
    var url: String = "https://www.spongepowered.org"
    var licenseProject: String = "SpongeAPI"
}

open class SpongeDevPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val devExtension =  project.extensions.let {
            it.findByType(SpongeDevExtension::class) ?: it.create(Constants.SPONGE_DEV_EXTENSION, SpongeDevExtension::class.java, project)
        }

        // Apply the BaseDevPlugin for sponge repo and Java configuration
        project.plugins.apply(BaseDevPlugin::class.java)
        // Apply Test dependencies
        project.dependencies.apply {
            add("testCompile", Constants.Dependencies.jUnit)
            add("testCompile", "org.hamcrest:hamcrest-library:1.3")
            add("testCompile", "org.mockito:mockito-core:2.8.47")
        }

        // Configure Java compile
        //  - Add javadocJar
        //  - Add Specification info for jar manifests
        //  - Add LICENSE.txt to the processResources for jar inclusion
        project.tasks.apply {
            val javaCompile = getting(JavaCompile::class) {
                options.apply {
                    compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-path", "-parameters"))
                    isDeprecation = false
                    encoding = "UTF-8"
                }
            }
            val javadoc = getting(Javadoc::class) {
                options {
                    encoding = "UTF-8"
                    charset("UTF-8")
                    (this as StandardJavadocDocletOptions).apply {
                        links?.addAll(mutableListOf(
                                "http://www.slf4j.org/apidocs/",
                                "https://google.github.io/guava/releases/21.0/api/docs/",
                                "https://google.github.io/guice/api-docs/4.1/javadoc/",
                                "https://zml2008.github.io/configurate/configurate-core/apidocs/",
                                "https://zml2008.github.io/configurate/configurate-hocon/apidocs/",
                                "https://flow.github.io/math/",
                                "https://flow.github.io/noise/",
                                "http://asm.ow2.org/asm50/javadoc/user/",
                                "https://docs.oracle.com/javase/8/docs/api/"
                        ))
                        addStringOption("-Xdoclint:none", "-quiet")
                    }
                }
            }
            register("javadocJar", Jar::class.java) {
                dependsOn(javadoc)
                group = "build"
                classifier = "javadoc"
                from(javadoc)
            }

            val jar = getting(Jar::class) {
                manifest {
                    devExtension.api?.let {
                        attributes["Specification-Title"] = it.name
                        attributes["Specification-Version"] = it.version
                    }
                    attributes["Specification-Vendor"] = devExtension.organization
                    attributes["Created-By"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
                }

            }

            val processResources = getting(ProcessResources::class) {
                from("LICENSE.txt")
            }
        }

        // Add commit and branch information to jar manifest
        val commit: String? = project.properties["commit"] as String?
        val branch: String? = project.properties["branch"] as String?
        if (commit != null) {
            project.afterEvaluate {
                val jar = tasks.getting(Jar::class) {
                    manifest {
                        attributes["Git-Commit"] = commit
                        attributes["Git-Branch"] = branch
                    }
                }
            }
        }
        // Archives task configuration
        // todo

        // Apply Licenser
        project.plugins.apply(Licenser::class.java)

        project.extensions.configure(LicenseExtension::class.java) {
            (this as ExtensionAware).extra.apply {
                this["name"] = devExtension.licenseProject
                this["organization"] = devExtension.organization
                this["url"] = devExtension.url
            }
            devExtension.api?.let {
                header = it.file("HEADER.txt")
            }
            include("**/*.java")
            newLine = false
        }

        // Configure Checkstyle but make the task only run explicitly
        project.plugins.apply(CheckstylePlugin::class.java)
        project.extensions.configure(CheckstyleExtension::class.java) {
            toolVersion = "8.24"
            devExtension.api?.let {
                configFile = it.file("checkstyle.xml")

            }
            configProperties.apply {
                put("basedir", project.projectDir)
                put("suppressions", project.file("checkstyle-suppressions.xml"))
                put("severity", "warning")
            }
        }

        // Add sorting
        project.plugins.apply(SpongeSortingPlugin::class.java)

        // Set up the deploy aspect
        project.plugins.apply(DeployImplementationPlugin::class.java)
        project.extensions.configure(DeployImplementationExtension::class.java) {
            url = "https://github.com/${devExtension.organization}/${project.name}"
            git = "{$url}.git"
            scm = "scm:git:{$git}"
            dev = "scm:git:git@github.com:${devExtension.organization}.${project.name}.git"
            description = project.description
        }
        val sourceJar = project.tasks.register("sourceJar", Jar::class.java) {
            classifier = "sources"
            group = "build"
            from(project.configurations.named("sourceOutput"))
        }
        if (devExtension is CommonDevExtension) {
            devExtension.api?.afterEvaluate {
                sourceJar.configure {
                    from(devExtension.api.configurations.named("sourceOutput"))
                }
            }
        }
        if (devExtension is SpongeImpl) {
            devExtension.common.afterEvaluate {
                sourceJar.configure {
                    from(devExtension.common.configurations.named("sourceOutput"))
                }
            }
        }

        project.artifacts {
            add("archives", sourceJar)
        }


        project.configurations.register("devOutput")
        project.dependencies.apply {
            add("devOutput", project.fileTree(project.sourceSets("main").output))
            project.sourceSet("ap")?.let {
                this.add("devOutput", it.output)
            }
        }
        project.configurations.register("sourceOutput")
        project.dependencies.apply {
            project.sourceSets("main").allSource.srcDirs.forEach {
                add("sourceOutput", project.files(it.relativeTo(project.projectDir).path))
            }
            project.sourceSet("ap")?.let {
                it.java.sourceDirectories.forEach {
                    add("sourceOutput", project.files(it.relativeTo(project.projectDir).path))
                }
            }
        }

    }
}


fun Project.sourceSets(name: String): SourceSet = convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName(name)
fun Project.sourceSet(name: String): SourceSet? = convention.getPlugin(JavaPluginConvention::class.java).sourceSets.findByName(name)