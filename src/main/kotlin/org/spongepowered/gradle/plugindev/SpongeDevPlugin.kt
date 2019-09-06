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
package org.spongepowered.gradle.plugindev

import net.minecrell.gradle.licenser.LicenseExtension
import net.minecrell.gradle.licenser.Licenser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getting
import org.gradle.language.jvm.tasks.ProcessResources
import org.spongepowered.gradle.deploy.DeployImplementationExtension
import org.spongepowered.gradle.deploy.DeployImplementationPlugin
import org.spongepowered.gradle.sort.SpongeSortingPlugin
import org.spongepowered.gradle.util.Constants
import java.util.*

class SpongeDevExtension {

    var api: Project? = null
    var organization = "SpongePowered"
    var url: String = "https://www.spongepowered.org"

}

class SpongeDevPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val devExtension = project.extensions.create(Constants.SPONGE_DEV_EXTENSION, SpongeDevExtension::class.java, project, project.name.toLowerCase
        (Locale.ENGLISH))

        project.plugins.apply(BaseDevPlugin::class.java)
        project.dependencies.apply {
            add("testCompile", Constants.Dependencies.jUnit)
            add("testCompile", "org.hamcrest:hamcrest-library:1.13")
            add("testCompile", "org.mockito:mockito-core:2.8.47")
        }
        project.tasks.apply {
            val javaCompile = getting(JavaCompile::class) {
                options.apply {
                    compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-path", "-parameters"))
                    isDeprecation = false
                    encoding = "UTF-8"
                }
            }
            val javadoc by getting(Javadoc::class)
            javadoc.options {
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
            val javadocJar = creating(Jar::class) {
                dependsOn(javadoc)
                classifier = "javadoc"
                from(javadoc.destinationDir)
            }
            val jar = getting(Jar::class) {
                manifest {
                    attributes["Specification-Title"] = devExtension.api!!.name
                    attributes["Specification-Version"] = devExtension.api!!.version
                    attributes["Specification-Vendor"] = devExtension.organization
                    attributes["Created-By"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
                }

            }

            val processResources = getting(ProcessResources::class) {
                from("LICENSE.txt")
            }

        }
        project.plugins.apply(CheckstylePlugin::class.java)
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
        project.afterEvaluate {
            val archive = tasks.getting(AbstractArchiveTask::class) {
                if (duplicatesStrategy == DuplicatesStrategy.INCLUDE) {
                    duplicatesStrategy = DuplicatesStrategy.FAIL
                }
            }
        }
        project.plugins.apply(Licenser::class.java)

        project.extensions.configure(LicenseExtension::class.java) {
            (this as ExtraPropertiesExtension).apply {
                this["name"] = project.name
                this["organization"] = devExtension.organization
                this["url"] = devExtension.url
            }
            header = devExtension.api!!.file("HEADER.txt")
            include("**/*.java")
            newLine = false
        }

        project.plugins.apply(CheckstylePlugin::class.java)
        project.extensions.configure(CheckstyleExtension::class.java) {
            toolVersion = "8.7"
            configFile = devExtension.api!!.file("checkstyle.xml")
            configProperties.apply {
                put("basedir", project.projectDir)
                put("suppressions", project.file("checkstyle-suppressions.xml"))
                put("severity", "warning")
            }
        }

        // Disables checkstyle tasks by default, running the checkstyle task explicitly enables
        // all default provided checksstyle tasks
        val checkstyle = project.tasks.getting(Checkstyle::class) {
            dependsOn("checkstyle")
        }
        project.gradle.taskGraph.whenReady {
            if (!allTasks.contains(checkstyle as Task)) {
                allTasks.filter { it.name.startsWith("checkstyle") }.forEach { it.enabled = false }
            }
        }

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

    }
}