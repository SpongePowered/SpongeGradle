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
package org.spongepowered.gradle.deploy

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getPlugin

class DeployImplementationPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val base = target.convention.getPlugin(BasePluginConvention::class)
        val config = target.extensions.create("deploySponge", DeployImplementationExtension::class.java)


        val description = config.description
        target.extensions.configure(PublishingExtension::class.java) {
            publications {
                create<MavenPublication>("mavenJava") {

                    config.repo?.let {
                        target.findProperty(it)?.let { repo -> repo as String
                            repositories {
                                (create(repo) as MavenArtifactRepository).apply {
                                    credentials {
                                        config.username?.let {
                                            username = target.findProperty(it) as String?
                                        }
                                        config.pass?.let {
                                            password = target.findProperty(it) as String?
                                        }
                                    }
                                }
                                artifactId = base.archivesBaseName
                                pom {
                                    name.set(base.archivesBaseName)
                                    description!!
                                    packaging = "jar"
                                    url.set(config.url)
                                    scm {
                                        url.set(config.git)
                                        connection.set(config.scm)
                                        developerConnection.set(config.dev)
                                    }
                                    issueManagement {
                                        system.set("GitHub Issues")
                                        url.set("https://github.com/${config.organization}/${target.name}/issues")
                                    }
                                    licenses {
                                        license {
                                            name.set(config.license)
                                            url.set(config.licenseUrl)
                                            distribution.set(config.licenseDistribution)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }



    }
}
open class DeployImplementationExtension {
    var description: String? = null
    var url: String? = null
    var git: String? = null
    var scm: String? = null
    var dev: String? = null
    var organization: String? = "SpongePowered"
    var repo: String? = "spongeRepo"
    var username: String? = "spongeUsername"
    var pass: String? = "spongePassword"
    var license: String? = "MIT License"
    var licenseUrl: String? = "http://opensource.org/licenses/MIT"
    var licenseDistribution: String? = "repo"

}
