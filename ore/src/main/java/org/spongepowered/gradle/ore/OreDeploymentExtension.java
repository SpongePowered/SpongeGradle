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
package org.spongepowered.gradle.ore;

import static java.util.Objects.requireNonNull;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

/**
 * Properties to configure all ore deployment tasks.
 *
 * <p>These options can be overridden per-task if needed.</p>
 *
 * @since 2.1.0
 */
public interface OreDeploymentExtension {

    /**
     * The Ore host to publish to.
     *
     * <p>Default: {@code https://ore.spongepowered.org/}</p>
     *
     * @return a property supplying the ore endpoint.
     * @since 2.1.0
     */
    @NotNull Property<String> oreEndpoint();

    /**
     * Set the ore host to publish to.
     *
     * @param endpoint the ore endpoint
     * @since 2.1.0
     * @see #oreEndpoint()
     */
    default void oreEndpoint(final @NotNull String endpoint) {
        this.oreEndpoint().set(endpoint);
    }

    /**
     * Get the API key to use for authentication.
     *
     * <p>This should be a v2 API key with {@code create_version} permissions.</p>
     *
     * <p>For security reasons, this property should be set to the value of a gradle property
     * or environment variable rather than the literal API key.</p>
     *
     * <p>Default: the value of the environment variable {@code ORE_TOKEN}.</p>
     *
     * @return the API key property
     * @since 2.1.0
     */
    @NotNull Property<String> apiKey();

    /**
     * Set the API key to use for authentication.
     *
     * @param apiKey the api key
     * @since 2.1.0
     * @see #apiKey()
     */
    default void apiKey(final @NotNull String apiKey) {
        this.apiKey().set(apiKey);
    }

    /**
     * Get publications for this project.
     *
     * @return publications collection
     * @since 2.1.0
     */
    @NotNull NamedDomainObjectContainer<OrePublication> publications();

    /**
     * Configure the publications on this project.
     *
     * @param configureAction the configure action
     */
    default void publications(final @NotNull Action<NamedDomainObjectContainer<OrePublication>> configureAction) {
        requireNonNull(configureAction, "configureAction").execute(this.publications());
    }

}
