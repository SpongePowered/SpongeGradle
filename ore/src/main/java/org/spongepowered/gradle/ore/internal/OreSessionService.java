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
package org.spongepowered.gradle.ore.internal;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class OreSessionService implements BuildService<OreSessionService.Parameters>, AutoCloseable {

    private static final Logger LOGGER = Logging.getLogger(OreSessionService.class);

    private final ExecutorService executor;
    private final Map<String, CompletableFuture<OreSession>> sessions = new ConcurrentHashMap<>();

    public OreSessionService() {
        this.executor = Executors.newCachedThreadPool();
    }

    public interface Parameters extends BuildServiceParameters {
        Property<Duration> getSessionDuration();
    }

    public CompletableFuture<OreSession> session(final String apiKey, final String endpoint) {
        return sessions.computeIfAbsent(endpoint, end -> OreSession.connect(this.executor, apiKey, end, this.getParameters().getSessionDuration().get().getSeconds()));
    }

    @Override
    public void close() {
        final List<CompletableFuture<?>> futures = new ArrayList<>();

        for (final CompletableFuture<OreSession> session : sessions.values()) {
            futures.add(session.thenCompose(OreSession::terminate));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).get();
        } catch (final InterruptedException | ExecutionException ex) {
            LOGGER.error("Failed to shut down an ore session", ex);
        }

        this.executor.shutdown();
        boolean success;
        try {
            success = this.executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            success = false;
        }

        if (!success) {
            LOGGER.warn("Failed to shut down Ore session executor pool in 10 seconds");
        }
    }
}
