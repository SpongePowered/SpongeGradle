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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.spongepowered.gradle.meta.GenerateMetadata
import org.spongepowered.gradle.meta.MetadataBasePlugin

import java.nio.file.Path

class SpongePluginBasePlugin implements Plugin<Project> {

    private static final String PLUGIN_ANNOTATION_PROCESSOR = 'org.spongepowered.plugin.processor.PluginProcessor'
    private static final String DISABLE_ANNOTATION_PROCESSING = '-proc:none'

    @Override
    void apply(Project project) {
        project.with {
            plugins.apply(MetadataBasePlugin)

            tasks.generateMetadata.mergeMetadata = false

            tasks.compileJava.dependsOn tasks.generateMetadata
            tasks.compileJava.doFirst { JavaCompile compile ->
                // Handle annotation processing compiler arguments
                def args = compile.options.compilerArgs
                if (args.contains(DISABLE_ANNOTATION_PROCESSING)) {
                    logger.error('Cannot run plugin annotation processor; annotation processing is disabled. ' +
                            'Plugin metadata will NOT be merged with the @Plugin annotation!')
                    return
                }

                int pos = args.indexOf('-processor')
                if (pos >= 0) {
                    // Add plugin annotation processor if annotation processors are configured manually
                    args[pos + 1] += ',' + PLUGIN_ANNOTATION_PROCESSOR
                }

                GenerateMetadata generateMetadata = tasks.generateMetadata
                Path generatedPath = generateMetadata.target

                def extra = [generatedPath, *generateMetadata.metadataFiles]
                args << '-AextraMetadataFiles=' + extra.join(';')

                // Set up final generated metadata file
                args << '-AmetadataOutputFile=' + generatedPath
            }
        }
    }

}
