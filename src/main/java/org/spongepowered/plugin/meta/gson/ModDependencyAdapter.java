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

package org.spongepowered.plugin.meta.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.spongepowered.plugin.meta.PluginMetadata;

import java.io.IOException;

public final class ModDependencyAdapter extends TypeAdapter<PluginMetadata.Dependency> {

    public static final ModDependencyAdapter INSTANCE = new ModDependencyAdapter();

    private static final char VERSION_SEPARATOR = '@';

    private ModDependencyAdapter() {
    }

    @Override
    public PluginMetadata.Dependency read(JsonReader in) throws IOException {
        final String version = in.nextString();
        int pos = version.indexOf(VERSION_SEPARATOR);
        if (pos < 0) {
            return new PluginMetadata.Dependency(version, null);
        } else {
            return new PluginMetadata.Dependency(version.substring(0, pos), version.substring(pos + 1));
        }
    }

    @Override
    public void write(JsonWriter out, PluginMetadata.Dependency value) throws IOException {
        if (value.getVersion() == null) {
            out.value(value.getId());
        } else {
            out.value(value.getId() + VERSION_SEPARATOR + value.getVersion());
        }
    }

}
