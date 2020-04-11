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

package org.spongepowered.gradle.meta

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.withConvention
import org.spongepowered.plugin.meta.McModInfo
import org.spongepowered.plugin.meta.PluginMetadata
import java.nio.file.Path

open class GenerateMetadata : DefaultTask() {

    @OutputFile
    val outputFile: RegularFileProperty = this.newOutputFile()

    @Input
    var mergeMetadata = true

    @get:InputFiles
    val metadataFiles: MutableList<Path> = mutableListOf()


    @TaskAction
    fun generateMetadata() {
        // Find extra metadata files
        var main: SourceSet
        project.withConvention(JavaPluginConvention::class) {
            main = sourceSets.getByName("main")
            metadataFiles.addAll(findExtraMetadataFiles(main))
        }

        // Read extra metadata files
        val metadata = mutableListOf<PluginMetadata>()
        if (mergeMetadata) {
            metadataFiles.forEach {
                McModInfo.DEFAULT.read(it).forEach { meta ->
                    val find: PluginMetadata? = metadata.find {
                        it.id == meta.id
                    }
                    find?.accept(meta) ?: metadata.add(meta)
                }

            }
        }
        if (!this.outputFile.isPresent) {
            this.outputFile.set(temporaryDir.toPath().resolve(McModInfo.STANDARD_FILENAME).toFile())
        }
        McModInfo.DEFAULT.write(outputFile.asFile.map { it.toPath() }.get(), metadata)
    }

    private fun findExtraMetadataFiles(sourceSet: SourceSet): List<Path> {
        return sourceSet.resources.matching { include(McModInfo.STANDARD_FILENAME) }.map { it.toPath().toAbsolutePath() }

    }
}
