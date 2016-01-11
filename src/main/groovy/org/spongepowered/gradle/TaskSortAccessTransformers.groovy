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
package org.spongepowered.gradle

import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction;

import java.io.File
import java.io.ObjectInputStream.ValidationList
import java.nio.charset.Charset
import java.util.Map
import java.util.Map.Entry
import java.util.Set;
 
/**
 * Gradle task to sort configuration entries in an AccessTransformer config
 * file. Entries are sorted by package, then by access modifier, then by name.
 * If line comments are encountered in the file they are treated as categories
 * and the lines following the comment are added to that "category".
 */
class TaskSortAccessTransformers extends DefaultTask {
    
    /**
     * Platform-specific newline 
     */
    def newline = sprintf("%n")
    
    /**
     * Files to process, discovered when a sourceset is added using {@link #add}
     */
    Set<File> files = []
    
    /**
     * ctor, add some sugar to TreeMap 
     */
    TaskSortAccessTransformers() {
        TreeMap.metaClass {
            tree = { key -> 
                def value = delegate.get(key)
                if (!value) {
                    value = new TreeMap()
                    delegate.put(key, value)
                }
                value
            }
            set = { key -> 
                def value = delegate.get(key)
                if (!value) {
                    value = new TreeSet()
                    delegate.put(key, value)
                }
                value
            }
        }
    }
    
    /**
     * Add a sourceset for processing
     * 
     * @param sourceSet sourceset to process
     */
    void add(SourceSet sourceSet) {
        if (!sourceSet instanceof SourceSet) {
            throw new InvalidUserDataException("${sourceSet} is not a SourceSet")
        }
        sourceSet.resources.findAll { file ->
            file.file && file.name.toLowerCase().endsWith("_at.cfg")
        }.each { atFile ->
            this.files.add(atFile)
        }
    }
    
    /**
     * Add a specific file for processing
     * 
     * @param file AT config file to process
     */
    void add(File file) {
        this.files.add(file)
    }
    
    /**
     * Main task action, sort the files which were added using {@link #add} 
     */
    @TaskAction
    void sortFiles() {
        for (file in files) {
            this.sortAtFile(file)
        }
    }
    
    /**
     * Sort an AT config file
     * 
     * @param file file to sort
     */
    private void sortAtFile(File file) {
        if (!file.exists()) {
            return
        }
        
        project.logger.lifecycle "Sorting AccessTransformer config: {}", file
        
        // Section -> Access -> Package -> Class -> Entry
        Map<String, Map<String, Map<String, Map<String, String>>>> tree = new TreeMap()
        
        // Current category
        Map<String, Map<String, Map<String, String>>> section = tree.tree("")

        def output = "# @ ${file.name} sorted on ${new Date().dateTimeString}" << newline
        
        for (String line : Files.readLines(file, Charset.defaultCharset())) {
            if (line?.isEmpty()) {          // Skip empty lines
                continue
            }
            if (line.startsWith("#")) {     // Line comment
                if (line.length() > 2 && line.startsWith("# @")) {
                    continue
                }
                String sectionName = line.substring(1).trim()
                if (sectionName.length() < 1) {
                    continue
                }
                section = tree.tree(sectionName)
            } else {                        // Valid AT line?
                String[] parts = line.split("\\s+", 3)
                if (parts.length < 2) {     // Invalid, just emit the line
                    output <<= line << newline
                    continue
                }
                if (parts.length < 3) {     // No field name, class modifier
                    parts += ""
                }
                def (modifier, className, tail) = parts
                def packageName = ""
                def pos = className.lastIndexOf('.')
                if (pos > -1) {
                    packageName = className.substring(0, ++pos)
                    className = className.substring(pos)
                }
                
                section.tree(packageName).tree(modifier).set(className).add(tail)
            }
        }
        
        // emit the sorted lines
        for (category in tree.entrySet()) {
            if (category.value.size() > 0) {
                if (category.key.length() > 0) {
                    output <<= newline << "# ${category.key}"
                }
                
                for (pkg in category.value.entrySet()) {
                    output <<= newline
                    for (acc in pkg.value.entrySet()) {
                        for (cls in acc.value.entrySet()) {
                            for (entry in cls.value) {
                                output <<= "${acc.key} ${pkg.key}${cls.key} ${entry}" << newline
                            }
                        }
                    }
                }
            }
        }
        
        Files.write(output, file, Charset.defaultCharset())
    }
}
