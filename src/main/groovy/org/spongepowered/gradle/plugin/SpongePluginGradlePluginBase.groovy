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
package org.spongepowered.gradle.plugin

import static org.spongepowered.gradle.plugin.SpongeExtension.EXTENSION_NAME

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.spongepowered.plugin.meta.McModInfo
import org.spongepowered.plugin.meta.PluginMetadata

import java.nio.file.Files
import java.nio.file.Path

class SpongePluginGradlePluginBase implements Plugin<Project> {

    private static final String FILE_PREFIX = 'mcmod'
    private static final String FILE_EXTENSION = '.info'

    private static final String PLUGIN_ANNOTATION_PROCESSOR = 'org.spongepowered.plugin.processor.PluginProcessor'
    private static final String DISABLE_ANNOTATION_PROCESSING = '-proc:none'

    @Override
    void apply(Project project) {
        project.with {
            SpongeExtension extension = extensions.findByType(SpongeExtension) ?: extensions.create(EXTENSION_NAME, SpongeExtension, project)
            tasks.compileJava.doFirst { JavaCompile compile ->
                // Handle annotation processing compiler arguments
                def args = compile.options.compilerArgs
                if (args.contains(DISABLE_ANNOTATION_PROCESSING)) {
                    logger.error('Cannot run plugin annotation processor; annotation processing is disabled. Plugin metadata will NOT be generated.')
                    return
                }

                int pos = args.indexOf('-processor')
                if (pos >= 0) {
                    // Add plugin annotation processor if annotation processors are configured manually
                    args[pos + 1] += ',' + PLUGIN_ANNOTATION_PROCESSOR
                }

                // Set up Gradle contributed metadata
                def tmpDir = temporaryDir.toPath()

                def path = Files.createTempFile(tmpDir, FILE_PREFIX, FILE_EXTENSION)
                def meta = extension.plugins.collect {
                    PluginMetadata meta = new PluginMetadata(it.id)
                    it.meta.accept(meta)
                    return meta
                }
                McModInfo.DEFAULT.write(path, meta)

                def java = project.convention.getPlugin(JavaPluginConvention)
                def extra = [path.toAbsolutePath(), *findExtraMetadataFiles(java.sourceSets.main)]
                args << '-AextraMetadataFiles=' + extra.join(';')

                // Set up final generated metadata file
                def output = tmpDir.resolve(McModInfo.STANDARD_FILENAME)
                args << '-AmetadataOutputFile=' + output.toAbsolutePath()

                // Include generated metadata file in process resources task
                ((CopySpec) tasks.processResources).from output.toFile()
            }
        }
    }

    private static List<Path> findExtraMetadataFiles(SourceSet sourceSet) {
        return sourceSet.resources.matching { include McModInfo.STANDARD_FILENAME }.collect { it.toPath().toAbsolutePath() }
    }

}
