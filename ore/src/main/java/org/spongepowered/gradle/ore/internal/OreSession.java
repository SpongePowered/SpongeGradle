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

import com.google.gson.Gson;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.FormBodyPartBuilder;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.gradle.api.GradleException;
import org.spongepowered.gradle.ore.internal.http.AsyncLegacyEntityProducer;
import org.spongepowered.gradle.ore.internal.http.HttpWrapper;
import org.spongepowered.gradle.ore.internal.http.JsonEntityConsumer;
import org.spongepowered.gradle.ore.internal.http.JsonEntityProducer;
import org.spongepowered.gradle.ore.internal.model.ApiSessionProperties;
import org.spongepowered.gradle.ore.internal.model.AuthenticationResponse;
import org.spongepowered.gradle.ore.internal.model.DeployVersionInfo;
import org.spongepowered.gradle.ore.internal.model.Version;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Represents a session with the Ore API
 */
public class OreSession implements AutoCloseable {

    private static final String API_V2 = "api/v2";

    public static final Gson GSON = new Gson();

    private final Executor executor;
    private final String apiBase;
    private final String apiKey;
    private final long sessionDurationSeconds;
    private CompletableFuture<OreResponse<AuthenticationResponse>> sessionFuture;

    private volatile String sessionKey;

    private final HttpWrapper http = new HttpWrapper(builder -> {
        builder.addRequestInterceptorFirst((request, entity, context) -> {
            if (this.sessionKey != null && !request.containsHeader(HttpHeaders.AUTHORIZATION)) {
                request.setHeader(HttpHeaders.AUTHORIZATION, "OreApi session=\"" + this.sessionKey + "\"");
            }
        });
    });

    public static CompletableFuture<OreSession> connect(final Executor executor, final String apiKey, final String apiBase, final long sessionDurationSeconds) {
        final OreSession session = new OreSession(executor, apiKey, apiBase, sessionDurationSeconds);
        return session.authenticate().thenApply(result -> {
            result.asSuccessOrThrow(RuntimeException::new);
            return session;
        });
    }

    private static URI make(final String apiBase, final String endpoint) {
        final StringBuilder builder = new StringBuilder(apiBase.length() + API_V2.length() + endpoint.length());
        builder.append(apiBase);
        if (!apiBase.endsWith("/")) {
            builder.append("/");
        }
        builder.append(API_V2);
        if (!endpoint.startsWith("/")) {
            builder.append("/");
        }
        builder.append(endpoint);

        return URI.create(builder.toString());
    }


    OreSession(final Executor executor, final String apiKey, final String apiBase, final long sessionDurationSeconds) {
        this.executor = executor;
        this.apiKey = apiKey;
        this.apiBase = apiBase;
        this.sessionDurationSeconds = sessionDurationSeconds;
    }

    CompletableFuture<OreResponse<AuthenticationResponse>> authenticate() {
        final AsyncRequestProducer request = AsyncRequestBuilder.post(OreSession.make(this.apiBase, "authenticate"))
            .setHeader(HttpHeaders.AUTHORIZATION, "OreApi apikey=\"" + this.apiKey + "\"")
            .setEntity(new JsonEntityProducer(GSON, new ApiSessionProperties(false, this.sessionDurationSeconds)))
            .build();
        return this.sessionFuture = this.http.request(request, new JsonEntityConsumer<>(GSON, AuthenticationResponse.class)).thenApply(response -> {
            if (response.wasSuccessful()) {
                final AuthenticationResponse auth = response.asSuccessOrThrow(IllegalStateException::new).value();
                this.sessionKey = auth.session();
            }
            return response;
        });
    }

    public CompletableFuture<OreResponse<Void>> terminate() {
        if (this.sessionFuture == null) {
            return CompletableFuture.completedFuture(OreResponse.failure(404, null));
        }
        return this.sessionFuture.thenCompose(session -> {
            final CompletableFuture<OreResponse<Void>> result = this.http.request(
                SimpleHttpRequest.create(Method.DELETE, OreSession.make(this.apiBase, "sessions/current")),
                new NoopEntityConsumer()
            );
            this.sessionFuture = null;
            return result;
        });
    }

    public CompletableFuture<Version> publishVersion(final String pluginId, final DeployVersionInfo info, final Path pluginFile) {
        final HttpEntity entity = MultipartEntityBuilder.create()
            .addPart(FormBodyPartBuilder.create("plugin-info", new StringBody(GSON.toJson(info), ContentType.APPLICATION_JSON)).build())
            .addPart(FormBodyPartBuilder.create("plugin-file", new FileBody(pluginFile.toFile())).build())
            .build();

        return doRequest(() -> this.http.request(
            AsyncRequestBuilder.post(OreSession.make(this.apiBase, "projects/" + pluginId + "/versions"))
                .setEntity(new AsyncLegacyEntityProducer(entity, this.executor))
                .build(),
            new JsonEntityConsumer<>(GSON, Version.class)
        ));
    }

    private <V> CompletableFuture<V> doRequest(final Supplier<CompletableFuture<OreResponse<V>>> action) {
        return this.sessionFuture.thenCompose(session -> {
            System.out.println("Session expires " + session.asSuccessOrThrow(GradleException::new).value().expires());
            return action.get().thenCompose(response -> {
                if (response instanceof OreResponse.Reauthenticate<?>) {
                    this.authenticate();
                    return this.doRequest(action);
                } else {
                    final CompletableFuture<V> result = new CompletableFuture<>();
                    if (response instanceof OreResponse.Success<?>) {
                        result.complete(((OreResponse.Success<V>) response).value());
                    } else {
                        final OreResponse.Failure<V> error = (OreResponse.Failure<V>) response;
                        result.completeExceptionally(new GradleException("Encountered error while performing Ore API request [" + error.responseCode() + "]: " + error.errorMessage()));
                    }
                    return result;
                }
            });
        });
    }

    @Override
    public void close() throws IOException {
        this.http.close();
    }
}
