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

import com.google.common.io.ByteStreams
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class TaskDecorateAnonInnerClasses extends AbstractTask {
    
    static class DecoratingVisitor extends ClassVisitor {
        
        def fieldName

        DecoratingVisitor(ClassVisitor cv, String name) {
            super(Opcodes.ASM5, cv)
            this.fieldName = name
        }
        
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, fieldName, "Ljava/lang/String;", null, name).visitEnd()
            super.visit(version, access, name, signature, superName, interfaces)
        }
        
    }

    def anonInnerClass = ~/\$[0-9]+\.class$/
    
    def fieldName = '$OBFNAME' 
    
    @InputFile
    def inJar

    @OutputFile
    def outJar
    
    def exclusions = []
    def inclusions = []
    
    def exclude(Object exclusion) {
        exclusions.add(exclusion)
    }

    def include(Object inclusion) {
        inclusions.add(inclusion)
    }
    
    @TaskAction
    public void process() throws IOException
    {
        File output = project.file(outJar)
        File input = project.file(inJar)

        if (output.exists()) {
            output.delete()
        }

        output.parentFile.mkdirs()

        ZipInputStream zin = new ZipInputStream(new FileInputStream(input))
        ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))

        for (ZipEntry entry = null; (entry = zin.nextEntry) != null;)
        {
            if (entry.isDirectory()) {
                zout.putNextEntry(entry)
                continue
            }

            ZipEntry newEntry = new ZipEntry(entry.name)
            newEntry.setTime(entry.time)
            zout.putNextEntry(newEntry)

            byte[] classData = ByteStreams.toByteArray(zin)
            
            def include = inclusions.size == 0
            for (String inclusion : inclusions) {
                if (entry.name.contains(inclusion)) {
                    include = true
                    break 
                }
            }

            if (include && entry.name.endsWith(".class") && entry.name =~ anonInnerClass) {
                def exclude = false
                for (String exclusion : exclusions) {
                    exclude |= entry.name.contains(exclusion)
                }
                
                if (!exclude) {
                    project.logger.info("Decorating inner class {}", entry.name)
                    
                    ClassWriter cw = new ClassWriter(0)
                    new ClassReader(classData).accept(new DecoratingVisitor(cw, fieldName), 0)
                    classData = cw.toByteArray()
                }
            }
            
            zout.write(classData)
        }

        zout.flush()
        zout.close()
        zin.close()
    }

}
