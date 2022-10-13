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

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.nio.support.classic.AbstractClassicEntityProducer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executor;

public final class AsyncLegacyEntityProducer extends AbstractClassicEntityProducer {
    private volatile HttpEntity entity;

    public AsyncLegacyEntityProducer(final HttpEntity entity, final Executor executor) {
        super(8192, ContentType.parse(entity.getContentType()), executor);
        this.entity = entity;
    }

    @Override
    protected void produceData(final ContentType contentType, final OutputStream outputStream) throws IOException {
        this.entity.writeTo(outputStream);
    }

    @Override
    public String getContentEncoding() {
        return this.entity.getContentEncoding();
    }

    @Override
    public void releaseResources() {
        final HttpEntity entity = this.entity;
        this.entity = null;
        if (entity != null) {
            try {
                entity.close();
            } catch (final IOException ex) {
                this.failed(ex);
            }
        }
        super.releaseResources();
    }
}
