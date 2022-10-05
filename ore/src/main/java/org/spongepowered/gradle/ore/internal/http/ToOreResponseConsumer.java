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

import org.apache.hc.core5.concurrent.CallbackContribution;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.nio.AsyncEntityConsumer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.gradle.ore.internal.OreResponse;
import org.spongepowered.gradle.ore.internal.OreSession;
import org.spongepowered.gradle.ore.internal.model.ErrorResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Parse the output of a HTTP request into an ore response.
 *
 * @param <V> the value type
 */
final class ToOreResponseConsumer<V> implements AsyncResponseConsumer<OreResponse<V>> {

    private final Supplier<AsyncEntityConsumer<V>> entityConsumerSupplier;
    private final AtomicReference<AsyncEntityConsumer<?>> entityConsumerRef = new AtomicReference<>();

    public ToOreResponseConsumer(final Supplier<AsyncEntityConsumer<V>> entityConsumerSupplier) {
        this.entityConsumerSupplier = entityConsumerSupplier;
    }

    public ToOreResponseConsumer(final AsyncEntityConsumer<V> consumer) {
        this.entityConsumerSupplier = () -> consumer;
    }

    @Override
    public void consumeResponse(
        final HttpResponse response,
        final @Nullable EntityDetails entityDetails,
        final HttpContext context,
        final @Nullable FutureCallback<OreResponse<V>> resultCallback
    ) throws HttpException, IOException {
        final int code = response.getCode();
        if (code >= 200 && code < 300) { // ok
            final AsyncEntityConsumer<V> responseConsumer = Objects.requireNonNull(this.entityConsumerSupplier.get(), "entity consumer");
            this.entityConsumerRef.set(responseConsumer);

            responseConsumer.streamStart(entityDetails, new CallbackContribution<V>(resultCallback) {
                @Override
                public void completed(final V result) {
                    if (resultCallback != null) {
                        resultCallback.completed(OreResponse.success(result));
                    }
                }
            });

        } else if (code == HttpStatus.SC_UNAUTHORIZED) {
            if (resultCallback != null) {
                resultCallback.completed(OreResponse.reauthenticate());
            }
        } else {
            // error response consumer
            if (entityDetails != null) {
                final AsyncEntityConsumer<ErrorResponse> responseConsumer = new JsonEntityConsumer<>(OreSession.GSON, ErrorResponse.class);
                this.entityConsumerRef.set(responseConsumer);
                responseConsumer.streamStart(entityDetails, new CallbackContribution<ErrorResponse>(resultCallback) {
                    @Override
                    public void completed(final ErrorResponse result) {
                        if (resultCallback != null) {
                            resultCallback.completed(OreResponse.failure(code, result == null ? null : result.error()));
                        }
                    }
                });
            }
        }
    }

    @Override
    public void informationResponse(final HttpResponse response, final HttpContext context) {
    }

    @Override
    public void failed(final Exception cause) {
        final AsyncEntityConsumer<?> consumer = this.entityConsumerRef.get();
        if (consumer != null) {
            consumer.failed(cause);
        }
        this.releaseResources();
    }

    @Override
    public void updateCapacity(final CapacityChannel capacityChannel) throws IOException {
        final AsyncEntityConsumer<?> consumer = this.entityConsumerRef.get();
        if (consumer != null) {
            consumer.updateCapacity(capacityChannel);
        }
    }

    @Override
    public void consume(final ByteBuffer src) throws IOException {
        final AsyncEntityConsumer<?> consumer = this.entityConsumerRef.get();
        if (consumer != null) {
            consumer.consume(src);
        }
    }

    @Override
    public void streamEnd(final List<? extends Header> trailers) throws HttpException, IOException {
        final AsyncEntityConsumer<?> consumer = this.entityConsumerRef.get();
        if (consumer != null) {
            consumer.streamEnd(trailers);
        }
    }

    @Override
    public void releaseResources() {
        final AsyncEntityConsumer<?> consumer = this.entityConsumerRef.getAndSet(null);
        if (consumer != null) {
            consumer.releaseResources();
        }
    }
}
