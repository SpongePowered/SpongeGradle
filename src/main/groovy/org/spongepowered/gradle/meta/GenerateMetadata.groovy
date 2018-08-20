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

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.specs.Specs
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.spongepowered.plugin.meta.McModInfo
import org.spongepowered.plugin.meta.PluginMetadata

import java.nio.file.Path
import java.util.function.Supplier

class GenerateMetadata extends DefaultTask {

    Supplier<List<PluginMetadata>> provider = {[]}
    Path target

    boolean mergeMetadata = true
    List<Path> metadataFiles = []

    GenerateMetadata() {
        // TODO: Instead of running all the time, this should consider
        // the given metadata as @Input together with all the extra metadata
        // files.
        outputs.upToDateWhen(Specs.satisfyNone())
    }

    List<PluginMetadata> getMetadata() {
        return this.provider.get()
    }

    @OutputFile
    File getOuputFile() {
        return getTarget().toFile()
    }

    Path getTarget() {
        return this.target ?: temporaryDir.toPath().resolve(McModInfo.STANDARD_FILENAME)
    }

    @TaskAction
    void generateMetadata() {
        // Find extra metadata files
        def java = project.convention.getPlugin(JavaPluginConvention)
        metadataFiles.addAll(findExtraMetadataFiles(java.sourceSets.main))

        def metadata = getMetadata()
        if (mergeMetadata) {
            // Read extra metadata files
            metadataFiles.each {
                McModInfo.DEFAULT.read(it).each { meta ->
                    def current = metadata.find { it.id == meta.id }
                    if (current) {
                        current.accept(meta)
                    } else {
                        metadata << meta
                    }
                }
            }
        }

        McModInfo.DEFAULT.write(getTarget(), metadata)
    }

    private static List<Path> findExtraMetadataFiles(SourceSet sourceSet) {
        return sourceSet.resources.matching { include McModInfo.STANDARD_FILENAME }.collect { it.toPath().toAbsolutePath() }
    }

}
