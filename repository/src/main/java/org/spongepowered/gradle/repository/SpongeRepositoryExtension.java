/*
 * This file is part of spongegradle-repository, licensed under the MIT License (MIT).
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
package org.spongepowered.gradle.repository;

import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

/**
 * Extension for configuring Sponge's repositories.
 */
public interface SpongeRepositoryExtension {

    /**
     * Register the Maven repository group containing Sponge's own releases and snapshots, plus all proxied content.
     *
     * @return the created repository
     * @since 2.1.0
     */
    MavenArtifactRepository all();

    /**
     * Register the Maven repository group containing Sponge's own releases and snapshots, plus all proxied content.
     *
     * @param extraConfig an extra action to apply to the created repository
     * @return the created repository
     * @since 2.1.0
     */
    default MavenArtifactRepository all(final @NonNull Action<MavenArtifactRepository> extraConfig) {
        final MavenArtifactRepository ret = all();
        requireNonNull(extraConfig, "extraConfig").execute(ret);
        return ret;
    }

    /**
     * Register the Maven repository containing Sponge's own published releases.
     *
     * @return the created repository
     * @since 2.1.0
     */
    MavenArtifactRepository releases();

    /**
     * Register the Maven repository containing Sponge's own published releases.
     *
     * @param extraConfig an extra action to apply to the created repository
     * @return the created repository
     * @since 2.1.0
     */
    default MavenArtifactRepository releases(final @NonNull Action<MavenArtifactRepository> extraConfig) {
        final MavenArtifactRepository ret = releases();
        requireNonNull(extraConfig, "extraConfig").execute(ret);
        return ret;
    }

    /**
     * Register the Maven repository containing Sponge's own published snapshots.
     *
     * @return the created repository
     * @since 2.1.0
     */
    MavenArtifactRepository snapshots();

    /**
     * Register the Maven repository containing Sponge's own published snapshots.
     *
     * @param extraConfig an extra action to apply to the created repository
     * @return the created repository
     * @since 2.1.0
     */
    default MavenArtifactRepository snapshots(final @NonNull Action<MavenArtifactRepository> extraConfig) {
        final MavenArtifactRepository ret = snapshots();
        requireNonNull(extraConfig, "extraConfig").execute(ret);
        return ret;
    }

}
