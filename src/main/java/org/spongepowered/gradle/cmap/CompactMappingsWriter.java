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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

final class CompactMappingsWriter {

    private static final char CLASS_COUNT_IDENTIFIER = 't';
    private static final char CLASS_IDENTIFIER = 'c';
    private static final char FIELD_IDENTIFIER = 'f';
    private static final char METHOD_IDENTIFIER = 'm';

    private static final char NEW_LINE = '\n';

    static void write(Path path, Collection<ClassMappings> mappings) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            write(writer, mappings);
        }
    }

    static void write(BufferedWriter writer, Collection<ClassMappings> mappings) throws IOException {
        writeLine(writer, CLASS_COUNT_IDENTIFIER, Integer.toString(mappings.size()));

        for (ClassMappings classMappings : mappings) {
            writeLine(writer, CLASS_IDENTIFIER, classMappings.className, classMappings.mappedName,
                    Integer.toString(classMappings.fields.size()), Integer.toString(classMappings.methods.size()));

            writeMembers(writer, FIELD_IDENTIFIER, classMappings.fields);
            writeMembers(writer, METHOD_IDENTIFIER, classMappings.methods);
        }
    }

    private static void writeMembers(BufferedWriter writer, char identifier, Map<MemberDescriptor, String> members)
            throws IOException {
        for (Map.Entry<MemberDescriptor, String> entry : members.entrySet()) {
            MemberDescriptor member = entry.getKey();
            writeLine(writer, identifier, member.name, member.desc, entry.getValue());
        }
    }

    private static void writeLine(BufferedWriter writer, char identifier, String... args) throws IOException {
        writer.write(identifier);
        for (String arg : args) {
            writer.write(' ');
            writer.write(arg);
        }
        writer.write(NEW_LINE);
    }

}
