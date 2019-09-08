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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.spongepowered.gradle.meta.BundleMetaPlugin
import org.spongepowered.gradle.meta.GenerateMetadata
import org.spongepowered.gradle.meta.MetadataBaseExtension
import org.spongepowered.gradle.meta.MetadataExtension
import org.spongepowered.gradle.util.Constants
import java.util.*

class SpongeImpl {

    var common: Project? = null
    val extraDeps: MutableList<SourceSetOutput> = mutableListOf()
    var implementationId = "spongevanilla"
}

class ImplementationDevPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val impl = project.extensions.create(Constants.SPONGE_IMPL_EXTENSION, SpongeImpl::class.java, project, project.name.toLowerCase(Locale.ENGLISH))

        project.buildscript.dependencies.apply {
            add("classpath", "com.github.jengelman.gradle.plugins:shadow:4.0.4")
        }

        project.plugins.apply {
            apply("java-library")
            apply(BaseDevPlugin::class.java)
            apply(SpongeDevPlugin::class.java)
            apply(BundleMetaPlugin::class.java)
        }

        val api = project.extensions.findByType(SpongeDevExtension::class.java)!!
        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.apply {
            val mainImpl = getByName("main")
            create("java6") {
                compileClasspath = project.files(compileClasspath, mainImpl.compileClasspath, mainImpl.output)
            }
        }
        val register = impl.common!!.tasks.register("resolveApiRevision", ResolveApiVersionTask::class.java)

        val generateMetadata = project.tasks.getting(GenerateMetadata::class) {
            dependsOn(register)
        }

        project.dependencies.apply {
            add("implementation", impl.common!!)
            impl.extraDeps.forEach {
                add("implementation", it)
            }
            // Added for runtime decompiling with Mixins for debugging
            add("runtime", "net.minecraftforge:forgeflower:1.5.380.23")
        }
        project.configurations.forEach {
            it.resolutionStrategy.dependencySubstitution {
                substitute(module("org.spongepowered:spongeapi")).with(project(api.api!!.path))
                substitute(module("org.spongepowered:spongecommon")).with(project(impl.common!!.path))
            }
        }
        val compile = project.configurations.getting {
            exclude(mapOf("module" to "asm"))
            exclude(mapOf("module" to "asm-commons"))
            exclude(mapOf("module" to "asm-tree"))
        }

        // TODO - create nested dependency of metas.
        project.plugins.apply("com.github.johnrengelman.shadow")


        project.tasks.apply {
            val jar = getting(Jar::class) {
                classifier = "base"
            }

            val reobfJar = getByName("reobfJar")
            val commonreobfJar = impl.common!!.tasks.getByName("reobfJar")
            getting(ShadowJar::class) {
                dependsOn(reobfJar)
                dependsOn(commonreobfJar)
                classifier = ""
                mustRunAfter(reobfJar)
                from(zip(reobfJar.outputs.files.filter { it.isFile && it.endsWith(".jar") }))
                exclude("dummyThing")
            }

        }
        val shadowJar = project.tasks.getting(ShadowJar::class)

        project.artifacts {
            add("archives", shadowJar)
        }
    }
}