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

import org.gradle.api.*
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.spongepowered.gradle.meta.BundleMetaPlugin
import org.spongepowered.gradle.meta.GenerateMetadata
import org.spongepowered.gradle.sort.SpongeSortingPlugin
import org.spongepowered.gradle.util.Constants

open class CommonImplementationDevPlugin : SpongeDevPlugin() {

    class AddedSourceFactory(val project: Project) : NamedDomainObjectFactory<AddedSourceSet> {
        override fun create(name: String): AddedSourceSet {
            return AddedSourceSet(name, project.objects)
        }
    }

    override fun apply(project: Project) {
        // This is designed to basically create the extension if
        // we're a common implementation project. The common implementation
        // project itself is not fully runnable, but is able to at least
        // get a majority of it's own set up working.
        // This will ignore creating the extension if we're a parent
        // implementation project that has already created the extension
        val dev = project.extensions.let {
            it.findByType(CommonDevExtension::class)
                    ?: it.create(Constants.SPONGE_DEV_EXTENSION, CommonDevExtension::class.java, project)
        }

        dev.licenseProject.set("Sponge")
        super.apply(project)
        project.plugins.apply {
            apply("java-library")
            apply(BundleMetaPlugin::class.java)
            apply(SpongeSortingPlugin::class.java)
        }

        project.afterEvaluate {
            project.dependencies.apply {
                add("api", dev.api.map {
                    project(it.path)
                }.get())
            }
        }

        val projectSourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
        dev.addedSourceSets.all(generateSourceSetAndconfigure(projectSourceSets, project, dev))

        project.tasks.apply {
            getting(JavaCompile::class) {
                options.compilerArgs.add("-Xlint:-processing")
            }
            val register = register("resolveApiRevision", ResolveApiVersionTask::class.java) {
                group = "sponge"
            }

            getting(GenerateMetadata::class) {
                dependsOn(register)
            }
            val jarTask = withType(Jar::class).named("jar")
            jarTask.configure {
                manifest {
                    attributes.putAll(
                            mapOf(
                                    "Implementation-Vendor" to dev.organization,
                                    "Specification-Version" to dev.getApiReleasedVersion()
                                    )
                    )
                }
            }
            val devJar = register("devJar", Jar::class.java) {
                dependsOn("resolveApiRevision")
                classifier = "dev"
                group = "build"
                setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
                this.manifest {
                    from(jarTask.map { it.manifest })
                }
                from(project.configurations.named("devOutput"))
                from(dev.api.map { it.configurations.named("devOutput") })
                from(dev.api.map { it.tasks.named("genEventImpl") })
            }

            project.artifacts.apply {
                add("archives", devJar)
            }

        }

    }

    private fun generateSourceSetAndconfigure(projectSourceSets: SourceSetContainer, project: Project, dev: CommonDevExtension): (AddedSourceSet).() -> Unit {
        return {
            val addedSourceDom = this
            val main by projectSourceSets.getting
            val newSet = projectSourceSets.register(this.name) {
                val newSourceSet = this

                System.out.println("[${project.name}] Adding SourceSet(${newSourceSet.name}: ${addedSourceDom.sourceType.get()})")
                this.output.dirs.forEach {
                    project.dependencies.add("devOutput", project.files(it.relativeTo(project.projectDir).path))
                }
                allSource.srcDirs.forEach {
                    project.dependencies.add("sourceOutput", project.files(it.relativeTo(project.projectDir).path))
                }

                project.tasks.findByName("jar")?.apply {
                    (this as Jar).apply {
                        from(newSourceSet)
                    }
                }


                project.afterEvaluate {
                    System.out.println("[${project.name}] Realizing SourceSet(${newSourceSet.name}: ${addedSourceDom.sourceType.get()})")
                    addedSourceDom.sourceType.get().onSourceSetCreated(newSourceSet, main, dev, project.dependencies, project)
                    addedSourceDom.dependsOn.forEach {
                        projectSourceSets.named(it).configure {
                            newSourceSet.compileClasspath += this.compileClasspath
                            project.dependencies.add(newSourceSet.implementationConfigurationName, this.output)

                        }
                    }
                }
            }
            project.afterEvaluate {
                newSet.configure {
                    if (addedSourceDom.isJava6) {
                        project.tasks {
                            findByName("compile${addedSourceDom.name}Java")?.apply {
                                (this as? JavaCompile)?.apply {
                                    sourceCompatibility = "1.6"
                                    targetCompatibility = "1.6"
                                }
                            }
                        }
                    }

                }

            }


        }
    }
}

open class AddedSourceSet(
        val name: String,
        val factory: ObjectFactory
) {
    var isJava6: Boolean = false
    val dependsOn: MutableList<String> = mutableListOf()
    val sourceType: Property<SourceType> = defaultPropertyType()

    fun defaultPropertyType(): Property<SourceType> {
        val source = factory.property(SourceType::class)
        if (!source.isPresent) {
            source.set(SourceType.Default)
        }
        return source
    }

}


open class CommonDevExtension(project: Project) : SpongeDevExtension(project) {

    private val apiTrimmed: Property<String> = project.objects.property()
    private val apiReleased: Property<String> = project.objects.property()
    private val apiMinor: Property<String> = project.objects.property()
    private val apiSplit: Property<List<String>> = project.objects.property()
    private val implVersion: Property<String> = project.objects.property()
    val addedSourceSets: NamedDomainObjectContainer<AddedSourceSet> = project.container(AddedSourceSet::class, CommonImplementationDevPlugin.AddedSourceFactory(project))
    val common: Property<Project> = project.objects.property(Project::class.java)
    val mixinSourceSets: NamedDomainObjectContainer<SourceSet> = project.container()
    val launchSourceSets: NamedDomainObjectContainer<SourceSet> = project.container()
    val accessorSourceSets: NamedDomainObjectContainer<SourceSet> = project.container()
    val invalidSourceSets: NamedDomainObjectContainer<SourceSet> = project.container()

    open fun common(commonProject: Project) {
        common(commonProjectProvider = project.provider { commonProject })
    }

    open fun common(commonProjectProvider: Provider<Project>) {
        this.common.set(commonProjectProvider)
        project.tasks.withType(Jar::class).named("jar").configure {
            manifest {
                attributes(
                        mapOf(
                                "Implementation-Title" to commonProjectProvider.map { it.name }.get(),
                                "Implementation-Version" to getImplementationVersion(),
                                "MCP-Mappings-Version" to commonProjectProvider.map { it.property("mcpMappings")!! }.get()
                        )
                )
            }
        }
        commonProjectProvider.map { it.version = getImplementationVersion() }
    }

    override fun api(apiProject: Project) {
        super.api(apiProject)
        apiProject.plugins.apply {
            if (this.findPlugin(SpongeDevPlugin::class.java) == null) {
                apply(SpongeDevPlugin::class.java)
            }
        }
    }

    override fun api(apiProjectProvider: Provider<Project>) {
        super.api(apiProjectProvider.map {
            val apiProject = it
            if (!apiProject.plugins.hasPlugin(SpongeDevPlugin::class.java)) {
                apiProject.plugins.apply(SpongeDevPlugin::class.java)
            }
            apiProject
        })
    }

    fun isReleaseCandidate(): Provider<Boolean> {
        return common.map {
            (it.properties.get("recommendedVersion") as? String)?.endsWith("-SNAPSHOT") ?: false
        }
    }

    fun getSplitApiVersion(): Provider<List<String>> {
        if (apiSplit.isPresent) {
            return apiSplit
        }
        return api.map {
            val split = it.version.toString().replace("-SNAPSHOT", "").split(".")
            apiSplit.set(split)
            split
        }
    }

    fun fetchApiMinorVersion(): Provider<String> {
        if (apiMinor.isPresent) {
            return apiMinor
        }
        val split = getSplitApiVersion()
        return split.map { splitVersion ->
            val minor = if (splitVersion.size > 1) splitVersion[1] else (if (splitVersion.size > 0) splitVersion.last() else "-1")
            if (minor != "-1") {
                apiMinor.set(Math.max(Integer.parseInt(minor) - 1, 0).toString())
            } else {
                apiMinor.set(minor)
            }
            apiMinor.get()
        }
    }

    fun getApiReleasedVersion(): Provider<String> {
        if (apiReleased.isPresent) {
            return apiReleased
        }
        return api.map {
            val split = getSplitApiVersion()
            val splitVersion = split.get()
            if (splitVersion.size > 2) {
                val major = fetchApiMinorVersion().map {
                    "${splitVersion[0]}.${it}"
                }.getOrElse(apiTrimmed.get())
                apiReleased.set(major)
                major
            } else {
                apiReleased.set(apiTrimmed)
                apiTrimmed.get()
            }
        }

    }

    fun getApiSuffix(): Provider<String> {
        return api.map { proj ->
            if ((proj.version as? String)?.endsWith("-SNAPSHOT") == true) {
                getSplitApiVersion().map { versionedStringList ->
                    fetchApiMinorVersion().map {
                        versionedStringList[0] + "." + it
                    }.getOrElse("0.0")
                }.getOrElse("0.0.0-SNAPSHOT")
            } else {
                getApiReleasedVersion().get()
            }
        }
    }

    fun getImplementationVersion(): String {
        if (implVersion.isPresent) {
            return implVersion.get()
        }
        implVersion.set(common.map {
            it.property("minecraftVersion")!! as String + "-" + getApiSuffix().get() + it.property("recommendedVersion")!! as String
        })
        return implVersion.get()
    }

}
