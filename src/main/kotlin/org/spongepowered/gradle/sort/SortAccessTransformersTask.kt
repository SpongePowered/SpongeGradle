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
package org.spongepowered.gradle.sort

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.spongepowered.gradle.util.TextConstants
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset
import java.util.*
import javax.inject.Inject

open class SortAccessTransformersTask @Inject constructor() : DefaultTask() {

    @Input // Gradle 6 probably removes this in favor of ObjectFactory.fileCollection(), but that's only in Gradle 5.3+
    val accessorTransformers = project.layout.configurableFiles()

    /**
     * Main task action, sort added files
     */
    @TaskAction
    fun sortFiles() {
        for (file in accessorTransformers) {
            this.sortFile(file)
        }
    }

    /**
     * Add a resource for processing, the resource name should be a
     * fully-qualified class name.
     *
     * @param sourceSetName Sourceset to use
     * @param resourceName Resource to add
     */
    fun add(sourceSetName : String, resourceName : String) {
        val java = project.convention.getPlugin(JavaPluginConvention::class.java)
        val sourceSet : SourceSet? = java.sourceSets.findByName(sourceSetName)
        when (sourceSet) {
            is SourceSet -> {
                if (resourceName.isEmpty()) {
                        throw InvalidUserDataException("$resourceName is not a valid resource name")
                    }
                var foundResource = false
                val resourceFileName = String.format("%s.java", resourceName.replace(".", File.separator))
                sourceSet.allJava.srcDirs.forEach {
                        srcDir ->
                        this.logger.log(LogLevel.INFO, "Attempting to locate: %s", resourceFileName)
                        val sourceFile = srcDir.resolve(resourceFileName)
                        if (sourceFile.exists()) {
                            foundResource = true
                            this.accessorTransformers.plus(sourceFile)
                        }
                    }
                if (!foundResource) {
                        throw InvalidUserDataException("$resourceName could not be found")
                    }
            }
            else -> throw InvalidUserDataException("Could not find specified sourceSet '$sourceSetName for task")
        }
    }

    /**
     * Sort a class file
     *
     * @param file File to sort
     */
    private fun sortFile(file : File) {
        if (!file.exists()) {
            return
        }

        project.logger.lifecycle("Sorting fields in: {}", file)

        // Flag switched by the processing semaphore
        var active = false

        // File content for output
        var output = ""

        // Sorted field set
        val fields = TreeSet<Field>()

        // Current field being accumulated
        var current = Field()
        file.readLines(Charset.defaultCharset()).forEach { line ->
            if (!current.initializer.isEmpty()) {
                if (current.initializer.contains(";")) {
                    if (!current.type.isEmpty()) {
                        fields.add(current)
                    } else {
                        output += current.flush()
                    }
                    current = Field()
                } else {
                    current.initializer += TextConstants.newLine + line
                }
            }
            val semaphore = TextConstants.semaphores.find(line)
            when (semaphore) {
                is MatchResult ->  {
                    if ("OFF" == semaphore.groups[1]!!.value) {
                        fields.forEach { field ->
                            output += TextConstants.newLine + field + TextConstants.newLine
                        }
                        if (fields.isNotEmpty()) {
                            output += TextConstants.newLine
                        }
                        fields.clear()
                    }
                    active = "ON" == semaphore.groups[1]!!.value
                    output += line + TextConstants.newLine
                    return@forEach
                }
            }
            if (!active) {
                output += line + TextConstants.newLine
                return@forEach
            } else if (line.isEmpty()) {
                return@forEach
            }

            val matched = TextConstants.modifiers.find(line)
            when (matched) {
                is MatchResult -> { // found a field declaration
                    current.modifiers = matched.groups[0]!!.value
                    val assignedPos = line.indexOf("=")
                    val typeAndName = line.substring(current.modifiers.length, assignedPos)
                    current.initializer = line.substring(assignedPos)
                    val idMatch = TextConstants.identifier.find(typeAndName)
                    when (idMatch) {
                        is MatchResult -> {
                            current.type = idMatch.groups[1]!!.value
                            current.name = idMatch.groups[2]!!.value
                        }
                    }
                }
                else -> {
                    current.comment += line
                }
            }



        }
        // Flush any remaining accumulated content
        if (current.isHasContent()) {
            output += current.flush()
        }
        val newPath = file.toPath()
        file.delete()
        val newFile = newPath.toFile()
        val writer = FileWriter(newFile)
        writer.write(output)
        writer.close()
    }
}

