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

import com.google.common.base.Charsets
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class TaskGenerateClassRemap extends AbstractTask {

    def anonInnerClass = ~/\$[0-9]+\.class$/
    def fieldName = '$OBFNAME' 
    
    @InputFile
    def inJar

    @OutputFile
    def outFile
    
    @TaskAction
    public void process() throws IOException
    {
        File output = project.file(outFile)
        File input = project.file(inJar)

        if (output.exists()) {
            output.delete()
        }

        output.parentFile.mkdirs()
        output.createNewFile()

        PrintWriter out = new PrintWriter(Files.newWriter(output, Charsets.UTF_8));
        ZipInputStream zin = new ZipInputStream(new FileInputStream(input))

        for (ZipEntry entry = null; (entry = zin.nextEntry) != null;)
        {
            if (entry.isDirectory()) {
                continue
            }

            byte[] classData = ByteStreams.toByteArray(zin)
            
            if (entry.name.endsWith(".class") && entry.name =~ anonInnerClass) {
                project.logger.info("Parsing inner class {}", entry.name)
                
                new ClassReader(classData).accept(new ClassVisitor(Opcodes.ASM5) {
                    
                    def className
                    
                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        className = name
                        super.visit(version, access, name, signature, superName, interfaces)
                    }
                    
                    @Override
                    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                        if (fieldName.equals(name)) {
                            out.printf("CL: %s %s\n", value, className)
                        }
                        super.visitField(access, name, desc, signature, value);
                    }
                }, 0)
            }
        }

        out.flush()
        out.close()
        zin.close()
    }

}
