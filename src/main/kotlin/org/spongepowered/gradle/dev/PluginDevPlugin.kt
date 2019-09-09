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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getting
import org.spongepowered.gradle.meta.BundleMetaPlugin
import org.spongepowered.gradle.meta.GenerateMetadata
import org.spongepowered.plugin.meta.McModInfo

object AP {
    const val processor = "org.spongepwoered.plugin.processor.PluginProcessor"
    const val processing = "-proc:none"
}

open class PluginDevPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.apply(BaseDevPlugin::class.java)

        // Now to configure the rest of Sponge Meta such
        target.plugins.apply(BundleMetaPlugin::class.java)
        target.tasks.apply {
            val genMeta = creating(GenerateMetadata::class) {
                mergeMetadata = false
                ouputFile
            }
            getting(JavaCompile::class) {
                inputs.files(genMeta)
                dependsOn(genMeta)
                doFirst {
                    val compilerArgs = options.compilerArgs
                    if (compilerArgs.contains(AP.processing)) {
                        logger.error("Cannot run plugin annotation processor; annotation processing is disabled. Plugin metadata will NOT be merged" +
                                " with the @Plugin annotation")
                        return@doFirst
                    }
                    var pos = compilerArgs.indexOf("-processor")
                    if (pos >= 0) {
                        compilerArgs[pos + 1] += "," + AP.processor
                    }
                    val generateMetadata = genMeta.container["generateMetadata"] as GenerateMetadata
                    val out = generateMetadata.ouputFile
                    val extra = mutableListOf(out?.path)
                    extra.addAll(generateMetadata.metadataFiles.map { it.toAbsolutePath().toString() })
                    compilerArgs.add("-AextraMetadataFiles=" + extra.joinToString(separator = ";"))
                }
            }
            val proc = getByName("processResources", CopySpec::class)
            proc.exclude(McModInfo.STANDARD_FILENAME)
        }
    }
}
