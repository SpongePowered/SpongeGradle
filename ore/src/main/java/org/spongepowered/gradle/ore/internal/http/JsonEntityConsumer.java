/*
 * This file is part of spongegradle-ore, licensed under the MIT License (MIT).
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
package org.spongepowered.gradle.ore.internal.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.nio.entity.AbstractCharAsyncEntityConsumer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.CharBuffer;

public final class JsonEntityConsumer<T> extends AbstractCharAsyncEntityConsumer<T> {
    private final Gson gson;
    private final Type type;

    private volatile StringBuilder builder = new StringBuilder();

    public JsonEntityConsumer(final Gson gson, final Class<T> type) {
        this.gson = gson;
        this.type = type;
    }

    public JsonEntityConsumer(final Gson gson, final TypeToken<T> token) {
        this.gson = gson;
        this.type = token.getType();
    }

    @Override
    protected void streamStart(final ContentType contentType) throws HttpException {
        if (!ContentType.APPLICATION_JSON.equals(contentType)) {
            throw new HttpException("Incorrect content type received for a json object, expected " + ContentType.APPLICATION_JSON + " but got " + contentType);
        }
    }

    @Override
    protected T generateContent() throws IOException {
        try {
            return this.gson.fromJson(this.builder.toString(), this.type);
        } catch (final JsonParseException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    protected int capacityIncrement() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected void data(final CharBuffer src, final boolean endOfStream) {
        this.builder.append(src);
    }

    @Override
    public void releaseResources() {
        this.builder = null;
    }
}
