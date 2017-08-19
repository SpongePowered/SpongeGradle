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
package org.spongepowered.gradle.cmap;

import static org.objectweb.asm.Opcodes.ASM5;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableTable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.TreeMap;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import javax.annotation.Nullable;

class VanillaCompactMappingProcessor {

    private static final String MINECRAFT_PACKAGE = "net/minecraft/";

    private final ImmutableBiMap<String, String> srgClasses;
    final ImmutableTable<String, String, String> srgFields;
    final ImmutableTable<String, MemberDescriptor, String> srgMethods;

    private final TreeMap<String, ClassMappings> mappings = new TreeMap<>(ClassNameComparator.INSTANCE);

    VanillaCompactMappingProcessor(SrgReader reader) {
        this.srgClasses = reader.getClasses();
        this.srgFields = reader.getFields();
        this.srgMethods = reader.getMethods();
    }

    Collection<ClassMappings> getMappings() {
        return this.mappings.values();
    }

    /**
     * Reads the classes from the specified JAR and collects the needed mappings.
     *
     * @param minecraftJar The Minecraft JAR
     * @throws IOException When an I/O error occurs while reading
     */
    void prepare(Path minecraftJar) throws IOException {
        try (JarInputStream jar = new JarInputStream(Files.newInputStream(minecraftJar))) {
            ZipEntry entry;
            while ((entry = jar.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String path = entry.getName();

                if (!path.endsWith(".class") || (path.indexOf('/') >= 0 && !path.startsWith(MINECRAFT_PACKAGE))) {
                    continue;
                }

                ClassReader reader = new ClassReader(jar);

                String className = reader.getClassName();
                ClassMappings classMappings = new ClassMappings(className, this.srgClasses.getOrDefault(className, className));

                if (this.mappings.put(className, classMappings) != null) {
                    throw new AssertionError("Mappings for " + className + " exist already");
                }

                reader.accept(new Visitor(classMappings), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
    }

    /**
     * Resolves super class mappings.
     */
    void process() {
        for (ClassMappings classMappings : this.mappings.values()) {
            processClass(classMappings);
        }
    }

    private void processClass(ClassMappings classMappings) {
        if (classMappings.superClass != null) {
            inheritMappings(classMappings, classMappings.superClass);
            classMappings.superClass = null;
        }

        if (classMappings.interfaces != null) {
            for (String iface : classMappings.interfaces) {
                inheritMappings(classMappings, iface);
            }

            classMappings.interfaces = null;
        }
    }

    private void inheritMappings(ClassMappings classMappings, String name) {
        ClassMappings parent = this.mappings.get(name);
        if (parent != null) {
            processClass(parent);

            // Add all fields and methods from the superclass
            classMappings.inheritableFields.putAll(parent.inheritableFields);
            classMappings.fields.putAll(classMappings.inheritableFields);
            classMappings.inheritableMethods.putAll(parent.inheritableMethods);
            classMappings.methods.putAll(classMappings.inheritableMethods);
        }
    }

    private class Visitor extends ClassVisitor {

        private final ClassMappings classMappings;

        Visitor(ClassMappings classMappings) {
            super(ASM5);
            this.classMappings = classMappings;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.classMappings.superClass = superName;
            this.classMappings.interfaces = interfaces;
        }

        @Override
        @Nullable
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            String mappedName = srgFields.get(this.classMappings.className, name);
            if (mappedName != null) {
                MemberDescriptor field = new MemberDescriptor(name, desc);
                this.classMappings.fields.put(field, mappedName);

                if ((access & Opcodes.ACC_PRIVATE) == 0) {
                    this.classMappings.inheritableFields.put(field, mappedName);
                }
            }
            return null;
        }

        @Override
        @Nullable
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MemberDescriptor method = new MemberDescriptor(name, desc);
            String mappedName = srgMethods.get(this.classMappings.className, method);
            if (mappedName != null) {
                this.classMappings.methods.put(method, mappedName);

                if ((access & Opcodes.ACC_PRIVATE) == 0) {
                    this.classMappings.inheritableMethods.put(method, mappedName);
                }
            }
            return null;
        }

    }

}
