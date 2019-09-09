package org.spongepowered.gradle.dev

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.existing
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.provideDelegate
import org.spongepowered.gradle.meta.BundleMetaPlugin
import org.spongepowered.gradle.meta.GenerateMetadata
import org.spongepowered.gradle.sort.SpongeSortingPlugin
import org.spongepowered.gradle.util.Constants

open class CommonImplementationDevPlugin : SpongeDevPlugin() {
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
                val api = project.project("SpongeAPI")
                val mcVersion = project.property("minecraftVersion")!! as String
                it.create(Constants.SPONGE_DEV_EXTENSION, CommonDevExtension::class.java, project, api, mcVersion)
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
        val java6 = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.register("java6") {
            this.output.dirs.forEach {
                project.dependencies.add("devOutput", project.files(it.relativeTo(project.projectDir).path))
            }
            allSource.srcDirs.forEach {
                project.dependencies.add("sourceOutput", project.files(it.relativeTo(project.projectDir).path))
            }

            if (dev is SpongeImpl) {
                dev.extraDeps.add(this.output)
            }
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
            findByName("compileJava6Java")?.apply {
                (this as? JavaCompile)?.apply {
                    sourceCompatibility = "1.6"
                    targetCompatibility = "1.6"
                }
            }
            val jar: Task? = findByName("jar")?.apply {
                (this as Jar).apply {
                    from(java6.map { it.output })
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

            // TODO - AccessTransformers are no longer a thing I think....
//            findByName("sortAccessTransformers")?.apply {
//                (this as SortAccessTransformersTask).apply {
//                    add("main", "common_at.cfg")
//                }
//            }
        }

    }
}

open class CommonDevExtension(val common: Project, api: Project, val minecraftVersion: String) : SpongeDevExtension(api) {

    private var apiTrimmed: String = "-1"
    private var apiReleased: String = "-1"
    private var apiMinor: String = "-1"
    private var apiSplit: List<String> = emptyList()
    private var implVersion: String = ""

    public fun isReleaseCandidate(): Boolean = (common.properties.get("recommendedVersion") as? String)?.endsWith("-SNAPSHOT") ?: false

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
