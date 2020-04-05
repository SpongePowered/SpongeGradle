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

import org.gradle.api.*
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Property
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
            val existing = it.findByType(CommonDevExtension::class)
            if (existing == null) {
                val api = project.findProject("SpongeAPI")


                val addedSourceSets = project.container(AddedSourceSet::class, AddedSourceFactory(project))
                it.create(Constants.SPONGE_DEV_EXTENSION, CommonDevExtension::class.java, addedSourceSets, project, api)
            } else {
                existing
            }
        }

        dev.licenseProject = "Sponge"
        val api = dev.api!!
        dev.common.version = dev.getImplementationVersion()
        super.apply(project)
        project.plugins.apply {
            apply("java-library")
            apply(BundleMetaPlugin::class.java)
            apply(SpongeSortingPlugin::class.java)
        }

        project.dependencies.apply {
            add("api", dev.api)
        }

        val projectSourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
        val subProj = (dev as? SpongeImpl)?.common
        dev.addedSourceSets.all(generateSourceSetAndconfigure(projectSourceSets, project, dev, subProj))


        subProj?.afterEvaluate {
            val subDev = extensions.findByType(CommonDevExtension::class)
            if (subDev != null) {
                applySourceSetDependencies(subDev, project)
            }
        }
        project.afterEvaluate {
            applySourceSetDependencies(dev, project)
        }

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
            val jar: Task? = findByName("jar")?.apply {
                (this as Jar).apply {
                    manifest {
                        attributes.putAll(mapOf(
                                "Implementation-Title" to dev.common.name,
                                "Implementation-Version" to dev.getImplementationVersion(),
                                "Implementation-Vendor" to dev.organization,
                                "Specification-Version" to dev.getApiReleasedVersion(),
                                "MCP-Mappings-Version" to dev.common.property("mcpMappings")
                        )
                        )
                    }
                }
            }
            val devJar = register("devJar", Jar::class.java) {
                dependsOn("resolveApiRevision")
                classifier = "dev"
                group = "build"
                setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
                (jar as? Jar)?.let {
                    this.manifest.from(it.manifest)
                }
                from(project.configurations.named("devOutput"))
            }

            // task configuration avoidance ftw. This allows us to avoid depending on
            // subproject evaluation.
            api.afterEvaluate {
                devJar.configure {
                    from(api.configurations.named("devOutput"))
                    from(api.tasks.named("genEventImpl"))
                }
            }
            if (dev is SpongeImpl) {
                dev.common.afterEvaluate {
                    devJar.configure {
                        from(dev.common.configurations.named("devOutput"))
                    }
                }
            }


            project.artifacts.apply {
                add("archives", devJar)
            }

        }

    }

    private fun applySourceSetDependencies(dev: CommonDevExtension, project: Project) {
        dev.apply {
            val thisDev = this
        }
    }

    private fun generateSourceSetAndconfigure(projectSourceSets: SourceSetContainer, project: Project, dev: CommonDevExtension, subProj: Project?): (AddedSourceSet).() -> Unit {
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

                if (dev is SpongeImpl) {
                    dev.extraDeps.add(this.output)
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
            if (subProj != null) subProj.afterEvaluate {
                val subSourceSets = subProj.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
                if (subSourceSets.names.contains(addedSourceDom.name)) {
                    val childSourceSet = subSourceSets.getByName(addedSourceDom.name)
                    val parentSet = projectSourceSets.findByName(addedSourceDom.name)
                    System.out.println("[${project.name}] Getting existing child SourceSet ${addedSourceDom.name} in ${subProj.path}")
                    childSourceSet.apply {
                        val child = this
                        newSet.configure {
                            project.dependencies.add(this.implementationConfigurationName, child.output)
                        }
                        output.dirs.forEach {
                            project.dependencies.add("devOutput", project.files(it.relativeTo(project.projectDir).path))
                        }
                        allSource.srcDirs.forEach {
                            project.dependencies.add("sourceOutput", project.files(it.relativeTo(project.projectDir).path))
                        }
                        parentSet!!.compileClasspath += compileClasspath
                    }
                } else {
                    val subSet = subSourceSets.register(addedSourceDom.name) {
                        System.out.println("[${project.name}] Creating new child SourceSet ${addedSourceDom.name} in ${subProj.path}")

                        this.output.dirs.forEach {
                            subProj.dependencies.add("devOutput", subProj.files(it.relativeTo(subProj.projectDir).path))
                        }
                        allSource.srcDirs.forEach {
                            subProj.dependencies.add("sourceOutput", subProj.files(it.relativeTo(subProj.projectDir).path))
                        }

                        if (dev is SpongeImpl) {
                            dev.extraDeps.add(this.output)
                        }
                        subProj.tasks.findByName("jar")?.apply {
                            (this as Jar).apply {
                                from(this@register)
                            }
                        }
                    }
                    val parentSet = projectSourceSets.findByName(addedSourceDom.name)
                    subSet.configure {
                        output.dirs.forEach {
                            project.dependencies.add("devOutput", project.files(it.relativeTo(project.projectDir).path))
                        }
                        allSource.srcDirs.forEach {
                            project.dependencies.add("sourceOutput", project.files(it.relativeTo(project.projectDir).path))
                        }
                        parentSet!!.compileClasspath += compileClasspath
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
        val source = factory.property(SourceType::class);
        if (!source.isPresent) {
            source.set(SourceType.Default)
        }
        return source
    }

}


open class CommonDevExtension(
        val addedSourceSets: NamedDomainObjectContainer<AddedSourceSet>,
        val common: Project,
        api: Project
) : SpongeDevExtension(api) {

    private var apiTrimmed: String = "-1"
    private var apiReleased: String = "-1"
    private var apiMinor: String = "-1"
    private var apiSplit: List<String> = emptyList()
    private var implVersion: String = ""
    val mixinSourceSets: NamedDomainObjectContainer<SourceSet> = common.container()
    val launchSourceSets: NamedDomainObjectContainer<SourceSet> = common.container()
    val accessorSourceSets: NamedDomainObjectContainer<SourceSet> = common.container()
    val invalidSourceSets: NamedDomainObjectContainer<SourceSet> = common.container()


    public fun isReleaseCandidate(): Boolean = (common.properties.get("recommendedVersion") as? String)?.endsWith("-SNAPSHOT")
            ?: false

    fun getSplitApiVersion(): List<String> {
        if (apiSplit.isEmpty()) {
            apiTrimmed = api?.version.toString().replace("-SNAPSHOT", "")
            apiSplit = apiTrimmed.split(".")
        }
        return apiSplit
    }

    fun fetchApiMinorVersion(): String {
        if (apiMinor != "-1") {
            return apiMinor
        }
        val split = getSplitApiVersion()
        val minor = if (split.size > 1) split[1] else (if (split.size > 0) split.last() else "-1")
        if (minor != "-1") {
            apiMinor = Math.max(Integer.parseInt(minor) - 1, 0).toString()
        } else {
            apiMinor = minor
        }
        return apiMinor
    }

    fun getApiReleasedVersion(): String {
        if (apiReleased != "-1") {
            return apiReleased
        }
        val split = getSplitApiVersion()

        val major = if (split.size > 2) split[0] + "." + fetchApiMinorVersion() else apiTrimmed
        apiReleased = major
        return apiReleased
    }

    fun getApiSuffix(): String {
        return if ((api?.version as? String)?.endsWith("-SNAPSHOT") == true) getSplitApiVersion()[0] + "." + fetchApiMinorVersion() else getApiReleasedVersion()
    }

    fun getImplementationVersion(): String {
        if (implVersion.isEmpty()) {
            implVersion = common.property("minecraftVersion")!! as String + "-" + getApiSuffix() + "." + common.property("recommendedVersion")!! as String
        }
        return implVersion
    }

}
