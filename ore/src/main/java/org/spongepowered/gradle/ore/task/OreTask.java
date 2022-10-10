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
package org.spongepowered.gradle.ore.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.spongepowered.gradle.ore.internal.OreSession;
import org.spongepowered.gradle.ore.internal.OreSessionService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class OreTask extends DefaultTask {

    @Input
    public abstract Property<String> getOreEndpoint();

    @Input
    public abstract Property<String> getOreApiKey();

    @Internal
    public abstract Property<OreSessionService> getOreSessions();

    protected CompletableFuture<OreSession> session() {
        return this.getOreSessions().get().session(
            this.getOreApiKey().get(),
            this.getOreEndpoint().get()
        );
    }

    protected <V> V responseOrThrow(final CompletableFuture<V> request) {
        try {
            return request.get(5, TimeUnit.MINUTES);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof GradleException) {
                throw (GradleException) e.getCause();
            } else {
                throw new GradleException("Failed to publish to Ore: " + e.getMessage(), e.getCause());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new GradleException("Ore API request in " + this.getName() + " timed out!");
        }
    }
}
