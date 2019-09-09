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
package org.spongepowered.gradle.meta

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getting
import org.gradle.language.jvm.tasks.ProcessResources
import org.spongepowered.gradle.util.Constants
import org.spongepowered.plugin.meta.PluginMetadata
import java.util.*

/**
 *
 * Applied by "org.spongepowered.meta.base"
 *
 */
open class MetadataPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val metaExtension: MetadataBaseExtension = project.extensions.let {
            val existing = it.findByType(MetadataBaseExtension::class.java)
            if (existing == null) {
                val projectName = project.name.toLowerCase(Locale.ENGLISH)
                it.create(Constants.METADATA_EXTENSION, MetadataBaseExtension::class.java, project, projectName)
            } else {
                existing
            }
        }
        project.plugins.apply("java-library")

        val genMeta = project.tasks.register("generateMetadata", GenerateMetadata::class.java) {
            doFirst {
                metaExtension.plugins.map {
                    val meta = PluginMetadata(it.id!!)
                    it.meta.accept(meta)
                }
            }
        }

        project.tasks.getting(ProcessResources::class) {
            from(genMeta)
        }
    }

}

/**
 * Used with id: `org.spongepowered.meta` that is to default creating the
 * defined to generate the `mcmod.info` file, along with providing the Sponge
 * [plugin meta](https://github.com/spongepowered/plugin-meta) AP the metadata
 * based on the project.
 */
open class BundleMetaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val metaExt = project.extensions.create(Constants.METADATA_EXTENSION, MetadataExtension::class.java, project, project.name.toLowerCase(Locale.ENGLISH))

        project.plugins.apply(MetadataPlugin::class.java)

        metaExt.plugin.meta.inherit(project)
        project.afterEvaluate {
            metaExt.plugins.maybeCreate(metaExt.plugin.id!!)
        }

    }
}