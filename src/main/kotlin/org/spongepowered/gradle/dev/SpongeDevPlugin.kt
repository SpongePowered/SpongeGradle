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
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
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

open class SpongeDevExtension() {

    var api: Project? = null
    var organization = "SpongePowered"
    var url: String = "https://www.spongepowered.org"

}

open class SpongeDevPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val devExtension =  project.extensions.let {
            val existing = it.findByType(SpongeDevExtension::class.java)
            if (existing == null) {
                it.create(Constants.SPONGE_DEV_EXTENSION, SpongeDevExtension::class.java)
            } else {
                existing
            }
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
            val javadocJar = creating(Jar::class) {
                dependsOn(javadoc)
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
                this["name"] = project.name
                this["organization"] = devExtension.organization
                this["url"] = devExtension.url
            }
            devExtension.api?.let {
                header = it.file("HEADER.txt")
            }
            include("**/*.java")
            newLine = false
        }
        project.afterEvaluate {

        }

        // Configure Checkstyle but make the task only run explicitly
        project.plugins.apply(CheckstylePlugin::class.java)
        project.extensions.configure(CheckstyleExtension::class.java) {
            toolVersion = "8.16.8"
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

    }
}