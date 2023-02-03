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

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.nio.AsyncEntityConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.gradle.util.GradleVersion;
import org.spongepowered.gradle.ore.internal.OreResponse;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class HttpWrapper implements AutoCloseable {

    private final CloseableHttpAsyncClient client;

    public HttpWrapper(final Consumer<HttpAsyncClientBuilder> builderConfigurer) {
        // Configure the HTTP client
        // This won't actually launch a thread pool until the first request is performed.
        final IOReactorConfig config = IOReactorConfig.custom()
            .setSoTimeout(Timeout.ofSeconds(5))
            .build();

        final HttpAsyncClientBuilder clientBuilder = HttpAsyncClientBuilder.create()
            .setIOReactorConfig(config)
            .setUserAgent(
                "SpongeGradle-Ore/" + this.getClass().getPackage().getImplementationVersion() + " Gradle/" + GradleVersion.current() + " Java/"
                    + System.getProperty("java.version"))
            .setRetryStrategy(new DefaultHttpRequestRetryStrategy(5, TimeValue.ofMilliseconds(500)));
        builderConfigurer.accept(clientBuilder);
        this.client = clientBuilder.build();
    }

    public CloseableHttpAsyncClient client() {
        this.client.start();
        return this.client;
    }

    // TODO: Pass request URLs to be exposed via responses?

    public <T> CompletableFuture<OreResponse<T>> request(final SimpleHttpRequest request, final AsyncEntityConsumer<T> responseConsumer) {
        return this.request(SimpleRequestProducer.create(request), responseConsumer);
    }

    public <T> CompletableFuture<OreResponse<T>> request(final AsyncRequestProducer request, final AsyncEntityConsumer<T> responseConsumer) {
        final FutureToCompletable<OreResponse<T>> ret = new FutureToCompletable<>();
        this.client().execute(
            request,
            new ToOreResponseConsumer<>(responseConsumer),
            ret
        );
        return ret.future();
    }

    public <T> CompletableFuture<OreResponse<T>> get(final URI destination, final AsyncEntityConsumer<T> responseConsumer) {
        return this.request(SimpleHttpRequest.create(Method.GET, destination), responseConsumer);
    }

    @Override
    public void close() throws IOException {
        this.client.close();
    }
}
