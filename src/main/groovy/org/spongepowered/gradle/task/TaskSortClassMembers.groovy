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
package org.spongepowered.gradle.task

import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction

import java.nio.charset.Charset

/**
 * Gradle tasks to sort fields in a class naively, sorts fields in between the
 * semaphore comments <tt>// SORTFIELDS:ON</tt> and <tt>// SORTFIELDS:OFF</tt>
 */
class TaskSortClassMembers extends DefaultTask {
    
    /**
     * Files to process 
     */
    Set<File> files = []
    
    /**
     * Platform-specific newline
     */
    def newline = sprintf("%n")
    
    /**
     * Regex for matching modifiers, used to identify actual field declarations 
     */
    def modifiers = ~/^\s*((public|protected|private|static|abstract|final|synchronized|transient|native|volatile)\s+)+/
    
    /**
     * Regex for matching identifiers, used to find the field name in the
     * declaration
     */
    def identifier = ~/^(.*?\s)(\p{javaJavaIdentifierStart}\p{javaJavaIdentifierPart}*\s*)$/
    
    /**
     * Regex for matching the processing semaphores
     */
    def semaphores = ~/\/\/\s*SORTFIELDS\s*:\s*(ON|OFF)/
    
    /**
     * Field wrapper used to keep all of the component parts of a field
     * declaration together and allow them to be sorted based on name. Also
     * stores the field ordinal to preserve ordering in case natural ordering
     * fails. 
     */
    class Field implements Comparable<Field> {
        
        /**
         * Next field ordinal 
         */
        static int nextIndex
     
        /**
         * Comment lines, accumulated here until the field declaration is
         * located   
         */
        def comment = []
        
        /**
         * Field modifiers, eg. public static final
         */
        def modifiers = ""
        
        /**
         * Field type, basically whatever is between the modifiers and the field
         * name
         */
        def type = ""
        
        /**
         * Field name
         */
        def name = ""
        
        /**
         * Field initialiser, basically whatever is between the field name and
         * the end of the line
         */
        def initialiser = ""
        
        /**
         * Field ordinal
         */
        def index = Field.nextIndex++
        
        boolean isValid() {
            modifiers != "" && type != "" && name != "" && initialiser != ""
        }
        
        boolean isHasContent() {
            comment.size() > 0
        }
        
        /**
         * Returns accumulated field comments as a String. In actual fact we
         * accumulate everything we don't recognise as a field in the "comments"
         * for the field, and this simply returns accumulated content 
         * 
         * @return
         */
        String flush() {
            String commentBlock = ""
            for (commentLine in comment) {
                commentBlock <<= commentLine << newline
            }
            commentBlock
        }
        
        @Override
        String toString() {
            this.flush() << modifiers << type << name << initialiser
        }
        
        @Override
        int compareTo(Field other) {
            int diff = this.name.compareTo(other.name)
            return (diff == 0) ? this.index - other.index : diff 
        }
    }
    
    /**
     * Add a resource for processing, the resource name should be a
     * fully-qualified class name.
     * 
     * @param resourceName Resource to add
     */
    void add(String resourceName) {
        this.add("main", resourceName)
    }
    
    /**
     * Add a resource for processing, the resource name should be a
     * fully-qualified class name.
     * 
     * @param sourceSetName Sourceset to use
     * @param resourceName Resource to add
     */
    void add(String sourceSetName, String resourceName) {
        SourceSet sourceSet = project.sourceSets.findByName(sourceSetName)
        if (!sourceSet) {
            throw new InvalidUserDataException("Could not find specified sourceSet '${sourceSetName} for task")
        }
        this.add(sourceSet, resourceName)
    }
    
    /**
     * Add a resource for processing, the resource name should be a
     * fully-qualified class name.
     * 
     * @param sourceSet Sourceset to use
     * @param resourceName Resource to add
     */
    void add(SourceSet sourceSet, String resourceName) {
        if (!sourceSet instanceof SourceSet) {
            throw new InvalidUserDataException("${sourceSet} is not a SourceSet")
        }
        
        if (!resourceName) {
            throw new InvalidUserDataException("${resourceName} is not a valid resource name")
        }

        def foundResource = false        
        def resourceFileName = sprintf("%s.java", resourceName.replace(".", File.separator))
        
        sourceSet.allJava.srcDirs.each { srcDir ->
            def sourceFile = new File(srcDir, resourceFileName)
            if (sourceFile.exists()) {
                foundResource = true
                files += sourceFile 
            } 
        }
        
        if (!foundResource) {
            throw new InvalidUserDataException("${resourceName} could not be found")
        }
    }
    
    /**
     * Main task action, sort added files 
     */
    @TaskAction
    void sortFiles() {
        for (file in files) {
            this.sortFile(file)
        }
    }
    
    /**
     * Sort a class file
     * 
     * @param file File to sort
     */
    private void sortFile(File file) {
        if (!file.exists()) {
            return
        }
        
        project.logger.lifecycle "Sorting fields in: {}", file

        // Flag switched by the processing semaphore        
        def active = false
        
        // File content for output
        def output = ""
        
        // Sorted field set
        def fields = new TreeSet<Field>()
        
        // Current field being accumulated
        def current = new Field()
        
        for (String line : Files.readLines(file, Charset.defaultCharset())) {
            if (current.initialiser) {
                if (current.initialiser.contains(';')) {
                    if (current.type) {
                        // Found field name and type, append the field
                        fields += current
                    } else {
                        // Can't identify the field, just flush it
                        output << current.flush()
                    }

                    current = new Field()
                } else {
                    // Append all lines until we find the end of the statement
                    current.initialiser <<= newline << line;
                    continue
                }
            }

            def semaphore = line =~ semaphores
            if (semaphore) {    // If semaphore found, switch processing state
                if ("OFF" == semaphore.group(1) && active) {
                    for (field in fields) {
                        output <<= newline << field << newline
                    }
                    if (fields.size()) {
                        output <<= newline
                    }
                    fields.clear()
                }
                active = "ON" == semaphore.group(1)
                output <<= line << newline
                continue
            }
            if (!active) {      // If not currently active, just passthrough
                output <<= line << newline
                continue
            } else if (!line) {
                continue
            }
            
            def match = line =~ modifiers
            if (match) {        // Found a field declaration
                current.modifiers = match.group()
                def assignmentPos = line.indexOf("=")
                def typeAndName = line.substring(current.modifiers.length(), assignmentPos)
                current.initialiser = line.substring(assignmentPos)
                def idMatch = typeAndName =~ identifier
                if (idMatch) {
                    current.type = idMatch.group(1)
                    current.name = idMatch.group(2)
                }
            } else {
                current.comment += line
            }
        }
        
        // Flush any remaining accumulated content
        if (current.hasContent) {
            output << current.flush()
        }
        
        Files.write(output, file, Charset.defaultCharset())
    }

}
