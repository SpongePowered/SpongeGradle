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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

final class SrgReader {

    private static final char SEPARATOR = ' ';
    private static final Splitter SEPARATOR_SPLITTER = Splitter.on(SEPARATOR);

    private static final String CLASS_MAPPING_PREFIX = "CL:";
    private static final String FIELD_MAPPING_PREFIX = "FD:";
    private static final String METHOD_MAPPING_PREFIX = "MD:";

    private static final char MEMBER_SEPARATOR = '/';

    private final ImmutableBiMap.Builder<String, String> classes = ImmutableBiMap.builder();
    private final ImmutableTable.Builder<String, String, String> fields = ImmutableTable.builder();
    private final ImmutableTable.Builder<String, MemberDescriptor, String> methods = ImmutableTable.builder();

    void read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            read(reader);
        }
    }

    void read(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if ((line = line.trim()).isEmpty()) {
                continue; // TODO: Warning?
            }

            Iterator<String> parts = SEPARATOR_SPLITTER.split(line).iterator();
            String prefix = parts.next();

            switch (prefix) {
                case CLASS_MAPPING_PREFIX:
                    addClass(parts.next(), parts.next());
                    break;
                case FIELD_MAPPING_PREFIX:
                    addField(parseMember(parts.next()), parseMember(parts.next()));
                    break;
                case METHOD_MAPPING_PREFIX:
                    addMethod(parseMember(parts.next()), parts.next(), parseMember(parts.next()));
                    break;
            }
        }
    }

    private void addClass(String source, String mapped) {
        if (source.equals(mapped)) {
            return;
        }

        this.classes.put(source, mapped);
    }

    private void addField(String[] source, String[] mapped) {
        if (source[1].equals(mapped[1])) {
            return;
        }

        this.fields.put(source[0], source[1], mapped[1]);
    }

    private void addMethod(String[] source, String sourceDesc, String[] mapped) {
        if (source[1].equals(mapped[1])) {
            return;
        }

        this.methods.put(source[0], new MemberDescriptor(source[1], sourceDesc), mapped[1]);
    }

    private static String[] parseMember(String member) {
        int pos = member.lastIndexOf(MEMBER_SEPARATOR);
        return new String[]{member.substring(0, pos), member.substring(pos + 1)};
    }

    ImmutableBiMap<String, String> getClasses() {
        return this.classes.build();
    }

    ImmutableTable<String, String, String> getFields() {
        return this.fields.build();
    }

    ImmutableTable<String, MemberDescriptor, String> getMethods() {
        return this.methods.build();
    }

}
