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

import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor;

import javax.inject.Inject;

class SpongeRepositoryExtensionImpl implements SpongeRepositoryExtension {

    private static final String SPONGE_REPO_ROOT = "https://repo.spongepowered.org/repository/";

    private static final String SPONGE_MAVEN_PUBLIC = SPONGE_REPO_ROOT + "maven-public/";

    private static final String SPONGE_MAVEN_RELEASES = SPONGE_REPO_ROOT + "maven-releases/";

    private static final String SPONGE_MAVEN_SNAPSHOTS = SPONGE_REPO_ROOT + "maven-snapshots/";

    private final RepositoryHandler repositories;

    @Inject
    public SpongeRepositoryExtensionImpl(final RepositoryHandler repositories) {
        this.repositories = repositories;
    }

    @Override
    public MavenArtifactRepository all() {
        return this.repositories.maven(repo -> {
            repo.setName("spongeAll");
            repo.setUrl(SPONGE_MAVEN_PUBLIC);
        });
    }

    @Override
    public MavenArtifactRepository releases() {
        return this.repositories.maven(repo -> {
            repo.setName("spongeReleases");
            repo.setUrl(SPONGE_MAVEN_RELEASES);
            repo.mavenContent(MavenRepositoryContentDescriptor::releasesOnly);
        });
    }

    @Override
    public MavenArtifactRepository snapshots() {
        return this.repositories.maven(repo -> {
            repo.setName("spongeSnapshots");
            repo.setUrl(SPONGE_MAVEN_SNAPSHOTS);
            repo.mavenContent(MavenRepositoryContentDescriptor::snapshotsOnly);
        });
    }

}
