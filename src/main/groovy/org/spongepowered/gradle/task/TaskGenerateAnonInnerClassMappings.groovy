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

import static org.objectweb.asm.Opcodes.ASM5

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor

import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class TaskGenerateAnonInnerClassMappings extends DefaultTask {

    private static final ANON_INNER_CLASS = ~/\$[0-9]+$/

    @InputFile
    def deobfJar

    @InputFile
    def recompJar

    @OutputFile
    def outFile

    @TaskAction
    void process() throws IOException {
        def deobf = project.file(deobfJar).toPath()
        def recomp = project.file(recompJar).toPath()
        def output = project.file(outFile).toPath()

        // Use a tree map so we can later iterate over the sorted map
        // We need to process the outer classes before the inner ones
        // For example, if we need to remap an anonymous class of an anonymous
        // class the outer class will be already renamed so we need to compensate
        // that.
        Map<String, List<String>> deobfInnerClassMap = new TreeMap()

        new ZipInputStream(deobf.newInputStream()).withStream { zin ->
            for (ZipEntry entry = null; (entry = zin.nextEntry) != null;) {
                if (entry.directory || !entry.name.endsWith('.class')) {
                    continue
                }

                // Read the inner classes from the class file
                def reader = new ClassReader(zin)
                InnerClassReader innerClassReader = new InnerClassReader()
                reader.accept(innerClassReader, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)

                if (innerClassReader.innerClasses) {
                    logger.debug('Found potential class in deobfuscated jar: {}', entry.name)
                    deobfInnerClassMap[entry.name - '.class'] = innerClassReader.innerClasses
                }
            }
        }

        Map<String, List<String>> recompInnerClassMap = new HashMap()

        new ZipInputStream(recomp.newInputStream()).withStream { zin ->
            for (ZipEntry entry = null; (entry = zin.nextEntry) != null;) {
                if (entry.directory || !entry.name.endsWith('.class') || !deobfInnerClassMap.keySet().any { entry.name.startsWith(it) }) {
                    continue
                }

                // Read the inner classes from the class file
                def reader = new ClassReader(zin)
                InnerClassReader innerClassReader = new InnerClassReader()
                reader.accept(innerClassReader, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)

                if (innerClassReader.innerClasses) {
                    logger.debug('Found potential class counter part in recompiled jar: {}', entry.name)
                    recompInnerClassMap[entry.name - '.class'] = innerClassReader.innerClasses
                }
            }
        }

        Map<String, String> result = new LinkedHashMap()
        deobfInnerClassMap.each { name, innerClasses ->
            if (result.containsKey(name)) {
                // Remap our name if necessary
                name = result[name]
            }

            def recompInnerClasses = recompInnerClassMap[name]

            if (innerClasses.size() != recompInnerClasses.size()) {
                logger.info('Inner class count changed after recompile for {}', name)
                logger.info('Deobfuscated: {}', innerClasses)
                logger.info('Recompiled: {}', recompInnerClasses)
                return
            }

            for (int i = 0; i < innerClasses.size(); i++) {
                def deobfName = innerClasses[i]
                if (deobfName =~ ANON_INNER_CLASS) {
                    def newName = recompInnerClasses[i]
                    assert newName =~ ANON_INNER_CLASS, "Anonymous class was changed to named class during recompile ($deobfName -> $newName)"
                    if (deobfName != newName) {
                        result[deobfName] = newName
                        logger.lifecycle('Mapping class {} -> {}', deobfName, newName)
                    }
                }
            }
        }

        Files.newBufferedWriter(output, StandardOpenOption.CREATE).withWriter { writer ->
            result.each { source, mapped ->
                writer << "CL: $mapped $source\n"
            }
        }
    }

    private static class InnerClassReader extends ClassVisitor {

        private final List<String> innerClasses = []
        private String outerName
        private boolean anonInnerClass

        InnerClassReader() {
            super(ASM5)
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.outerName = name
        }

        @Override
        void visitInnerClass(String name, String classOuterName, String classInnerName, int access) {
            // Some anonymous inner classes have itself as inner class... (WTF, Java?)
            if (name != this.outerName) {
                // The class is only interesting for us if it is anonymous and it is actually part of our outer class
                if (name =~ ANON_INNER_CLASS && name[0..name.lastIndexOf('$')-1] == this.outerName) {
                    anonInnerClass = true
                }

                innerClasses.add(name)
            }
        }

        @Override
        void visitEnd() {
            if (anonInnerClass) {
                // Remove classes we're not interested in (named classes after our anonymous classes)
                def itr = innerClasses.listIterator()
                while (itr.hasPrevious()) {
                    if (itr.previous() =~ ANON_INNER_CLASS) {
                        break;
                    }

                    itr.remove()
                }
            } else {
                // The class has no inner classes we're interested in
                innerClasses.clear()
            }
        }

    }

}
