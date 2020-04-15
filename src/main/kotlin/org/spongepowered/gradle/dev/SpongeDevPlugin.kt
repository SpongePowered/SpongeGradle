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

import net.minecrell.gradle.licenser.LicenseExtension
import net.minecrell.gradle.licenser.Licenser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.spongepowered.gradle.deploy.DeployImplementationExtension
import org.spongepowered.gradle.deploy.DeployImplementationPlugin
import org.spongepowered.gradle.sort.SpongeSortingPlugin
import org.spongepowered.gradle.util.Constants

open class SpongeDevExtension(val project: Project) {
    val organization: Property<String> = defaultOrganization()
    val url: Property<String> = defaultUrl()
    val licenseProject: Property<String> = defaultLicense()
    val api: Property<Project> = project.objects.property(Project::class.java)

    private fun defaultOrganization(): Property<String> {
        val org:  Property<String> = project.objects.property()
        org.set("SpongePowered")
        return org
    }

    private fun defaultUrl(): Property<String> {
        val url: Property<String> = project.objects.property()
        url.set("https://www.spongepowered.org")
        return url
    }

    private fun defaultLicense(): Property<String> {
        val license: Property<String> =project.objects.property()
        license.set("Sponge")
        return license
    }

    public fun organization(org: String) {
        organization.set(org)
    }

    public fun url(url: String) {
        this.url.set(url)
    }

    public fun license(projectName: String) {
        this.licenseProject.set(projectName)
    }

    public open fun api(apiProject: Project) {
        this.api.set(apiProject)
    }

    public open fun api(apiProjectProvider: Provider<Project>) {
        this.api.set(apiProjectProvider)
    }
}

open class SpongeDevPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val devExtension = project.extensions.let {
            it.findByType(SpongeDevExtension::class)
                    ?: it.create(Constants.SPONGE_DEV_EXTENSION, SpongeDevExtension::class.java, project)
        }


        // Apply the BaseDevPlugin for sponge repo and Java configuration
        project.plugins.apply(BaseDevPlugin::class.java)
        // Apply Test dependencies
        project.dependencies.apply {
            add("testImplementation", Constants.Dependencies.jUnit)
            add("testImplementation", "org.hamcrest:hamcrest-library:1.3")
            add("testImplementation", "org.mockito:mockito-core:2.8.47")
        }
        project.plugins.apply(JavaLibraryPlugin::class)

        // Configure Java compile
        //  - Add javadocJar
        //  - Add Specification info for jar manifests
        //  - Add LICENSE.txt to the processResources for jar inclusion
        configureJavaCompile(project)
        val javadocTask = configureJavadocTask(project)
        val javadocJar: TaskProvider<Jar> = configureJavadocJarTask(project, javadocTask)

        configureJarTask(project, devExtension)

        project.tasks.withType(ProcessResources::class).named("processResources").configure {
            from("LICENSE.txt")
        }

        configureGitCommitBranchManifests(project)

        configureLicenser(project, devExtension)
        configureCheckstyle(project, devExtension)

        // Add sorting
        project.plugins.apply(SpongeSortingPlugin::class.java)

//        configureDeploy(project, devExtension)
        val sourceJar: TaskProvider<Jar> = configureSourceJar(project, devExtension)

        configureSourceAndDevOutput(project, sourceJar, javadocJar, devExtension)

    }

    private fun configureJavadocJarTask(project: Project, javadocTask: TaskProvider<Javadoc>): TaskProvider<Jar> {
        val javadocJar: TaskProvider<Jar> = project.tasks.register("javadocJar", Jar::class) {
            group = "build"
            classifier = "javadoc"
            from(javadocTask)
        }
        return javadocJar
    }

    private fun configureSourceAndDevOutput(project: Project, sourceJar: TaskProvider<Jar>, javadocJar: TaskProvider<Jar>, devExtension: SpongeDevExtension) {
        project.extensions.findByType(PublishingExtension::class)?.apply {
            publications {
                val mavenJava = findByName("mavenJava") as? MavenPublication
                mavenJava?.let {
                    it.artifact(sourceJar)
                    it.artifact(javadocJar)
                }
            }
        }


        addSourceJarAndJavadocJarToArtifacts(project, sourceJar, javadocJar)


        project.configurations.register("devOutput")
        project.dependencies.apply {

            project.sourceSet("main")?.output?.let {
                add("devOutput", project.fileTree(it))
            }
            project.sourceSet("ap")?.let {
                this.add("devOutput", it.output)
            }
        }
        configureSourceOutputForProject(project)
    }

    private fun addSourceJarAndJavadocJarToArtifacts(project: Project, sourceJar: TaskProvider<Jar>, javadocJar: TaskProvider<Jar>) {
        project.afterEvaluate {
            project.extensions.findByType(PublishingExtension::class)?.publications {

                (findByName("spongeGradle") as? MavenPublication)?.apply {
                    sourceJar.configure {
                        artifact(this)
                    }
                    javadocJar.configure {
                        artifact(this)
                    }
                }
            }
        }
        project.artifacts {
            add("archives", sourceJar)
            add("archives", javadocJar)
        }
    }

    private fun configureSourceOutputForProject(project: Project) {
        project.afterEvaluate {
            project.dependencies.apply {
                project.sourceSet("main")?.allSource?.srcDirs?.forEach {
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

    private fun configureSourceJar(project: Project, devExtension: SpongeDevExtension): TaskProvider<Jar> {
        val sourceOutputConf = project.configurations.register("sourceOutput")
        val sourceJar: TaskProvider<Jar> = project.tasks.register("sourceJar", Jar::class.java) {
            classifier = "sources"
            group = "build"
            from(sourceOutputConf)
            if (devExtension is CommonDevExtension) {
                devExtension.api.map {
                    this@register.from(it.configurations.named("sourceOutput"))

                }
            }
            if (devExtension is SpongeImpl) {
                devExtension.common.map {
                    this@register.from(it.configurations.named("sourceOutput"))
                }
            }
        }
        return sourceJar
    }

    private fun configureDeploy(project: Project, devExtension: SpongeDevExtension) {
        // Set up the deploy aspect - after we've created the configurations for sources and dev jars.
        project.plugins.apply(DeployImplementationPlugin::class.java)
        project.extensions.configure(DeployImplementationExtension::class.java) {
            url = "https://github.com/${devExtension.organization}/${project.name}"
            git = "{$url}.git"
            scm = "scm:git:{$git}"
            dev = "scm:git:git@github.com:${devExtension.organization}.${project.name}.git"
            description = project.description
        }
    }

    private fun configureCheckstyle(project: Project, devExtension: SpongeDevExtension) {
        // Configure Checkstyle but make the task only run explicitly
        project.plugins.apply(CheckstylePlugin::class.java)
        project.extensions.configure(CheckstyleExtension::class.java) {
            toolVersion = "8.24"
            devExtension.api.map {
                configFile = it.file("checkstyle.xml")
            }
            configProperties.apply {
                put("basedir", project.projectDir)
                put("suppressions", project.file("checkstyle-suppressions.xml"))
                put("severity", "warning")
            }
        }
    }

    private fun configureLicenser(project: Project, devExtension: SpongeDevExtension) {
        // Apply Licenser
        project.plugins.apply(Licenser::class.java)

        project.extensions.configure(LicenseExtension::class.java) {
            newLine = false
        }

        project.afterEvaluate {
            project.extensions.configure(LicenseExtension::class.java) {
                (this as ExtensionAware).extra.apply {
                    this["name"] = devExtension.licenseProject.get()
                    this["organization"] = devExtension.organization.get()
                    this["url"] = devExtension.url.get()
                }
                val apiProject = devExtension.api.get()
                header = apiProject.file("HEADER.txt")

                include("**/*.java")
                newLine = false
            }

        }
    }

    private fun configureGitCommitBranchManifests(project: Project) {
        // Add commit and branch information to jar manifest
        val commit: String? = project.properties["commit"] as String?
        val branch: String? = project.properties["branch"] as String?
        if (commit != null) {
            project.tasks.withType(Jar::class).named("jar").configure {
                manifest {
                    attributes["Git-Commit"] = commit
                    attributes["Git-Branch"] = branch
                }
            }
        }
    }

    private fun configureJarTask(project: Project, devExtension: SpongeDevExtension) {
        project.tasks.withType(Jar::class).named("jar").configure {
            manifest {
                devExtension.api.map {
                    attributes["Specification-Title"] = it.name
                    attributes["Specification-Version"] = it.version
                }
                attributes["Specification-Vendor"] = devExtension.organization
                attributes["Created-By"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})"
            }

        }
    }

    private fun configureJavaCompile(project: Project) {
        project.tasks.withType(JavaCompile::class).named("compileJava").configure {
            options.apply {
                compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-path", "-parameters"))
                isDeprecation = false
                encoding = "UTF-8"
            }
        }
    }

    private fun configureJavadocTask(project: Project): TaskProvider<Javadoc> {
        val javadoc = project.tasks.withType(Javadoc::class).named("javadoc")
        javadoc.configure {
            options {
                encoding = "UTF-8"
                charset("UTF-8")
                isFailOnError = false
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
        return javadoc
    }
}


fun Project.sourceSet(name: String): SourceSet? = convention.findPlugin(JavaPluginConvention::class.java)?.sourceSets?.findByName(name)