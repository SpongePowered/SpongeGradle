/*
 * This file is part of plugin-meta, licensed under the MIT License (MIT).
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

package org.spongepowered.plugin.meta;

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.spongepowered.plugin.meta.gson.ModMetadataAdapter;
import org.spongepowered.plugin.meta.gson.ModMetadataCollectionAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class McModInfo {

    public static final String STANDARD_FILENAME = "mcmod.info";

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final String INDENT = "    ";

    public static final McModInfo DEFAULT = new McModInfo(ModMetadataCollectionAdapter.DEFAULT);

    private final ModMetadataCollectionAdapter adapter;

    private McModInfo(ModMetadataCollectionAdapter adapter) {
        this.adapter = adapter;
    }

    public List<PluginMetadata> fromJson(String json) {
        try {
            return this.adapter.fromJson(json);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    public List<PluginMetadata> read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, CHARSET)) {
            return read(reader);
        }
    }

    public List<PluginMetadata> read(InputStream in) throws IOException {
        return this.adapter.fromJson(new BufferedReader(new InputStreamReader(in, CHARSET)));
    }

    public List<PluginMetadata> read(Reader reader) throws IOException {
        return this.adapter.fromJson(reader);
    }

    public List<PluginMetadata> read(JsonReader reader) throws IOException {
        return this.adapter.read(reader);
    }

    public String toJson(PluginMetadata... meta) {
        return toJson(asList(meta));
    }

    public String toJson(List<PluginMetadata> meta) {
        StringWriter writer = new StringWriter();
        try {
            write(writer, meta);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
        return writer.toString();
    }

    public void write(Path path, PluginMetadata... meta) throws IOException {
        write(path, asList(meta));
    }

    public void write(Path path, List<PluginMetadata> meta) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, CHARSET)) {
            write(writer, meta);
        }
    }

    public void write(Writer writer, PluginMetadata... meta) throws IOException {
        write(writer, asList(meta));
    }

    public void write(Writer writer, List<PluginMetadata> meta) throws IOException {
        try (JsonWriter json = new JsonWriter(writer)) {
            json.setIndent(INDENT);
            write(json, meta);
            writer.write('\n'); // Add new line at the end of the file
        }
    }

    public void write(JsonWriter writer, PluginMetadata... meta) throws IOException {
        write(writer, asList(meta));
    }

    public void write(JsonWriter writer, List<PluginMetadata> meta) throws IOException {
        this.adapter.write(writer, meta);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final GsonBuilder gson = new GsonBuilder();
        private final ImmutableMap.Builder<String, Class<?>> extensions = ImmutableMap.builder();

        private Builder() {
        }

        public GsonBuilder gson() {
            return this.gson;
        }

        public Builder registerExtension(String key, Class<?> extensionClass) {
            this.extensions.put(key, extensionClass);
            return this;
        }

        public Builder registerExtension(String key, Class<?> extensionClass, Object typeAdapter) {
            registerExtension(key, extensionClass);
            this.gson.registerTypeAdapter(extensionClass, typeAdapter);
            return this;
        }

        public McModInfo build() {
            return new McModInfo(new ModMetadataCollectionAdapter(new ModMetadataAdapter(this.gson.create(), this.extensions.build())));
        }

    }

}
