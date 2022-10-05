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
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.nio.StreamChannel;
import org.apache.hc.core5.http.nio.entity.AbstractCharAsyncEntityProducer;

import java.io.IOException;
import java.nio.CharBuffer;

public final class JsonEntityProducer extends AbstractCharAsyncEntityProducer {
    private final Gson gson;
    private final Object value;
    private volatile CharBuffer created;

    public JsonEntityProducer(final Gson gson, final Object value) {
        super(4096, -1, ContentType.APPLICATION_JSON);
        this.gson = gson;
        this.value = value;
    }

    @Override
    protected int availableData() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected void produceData(final StreamChannel<CharBuffer> channel) throws IOException {
        if (this.created == null) {
            synchronized (this) {
                if (this.created == null) {
                    this.created = CharBuffer.wrap(this.gson.toJson(this.value));
                }
            }
        }

        channel.write(this.created);
        if (!this.created.hasRemaining()) {
            channel.endStream();
        }
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void failed(final Exception cause) {
    }

    @Override
    public void releaseResources() {
        this.created = null;
        super.releaseResources();
    }
}
