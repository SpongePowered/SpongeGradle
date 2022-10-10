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

import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.gradle.ore.OrePublication;
import org.spongepowered.gradle.ore.internal.OreSession;
import org.spongepowered.gradle.ore.internal.model.DeployVersionInfo;
import org.spongepowered.gradle.ore.internal.model.Version;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Publish an artifact to the Ore plugin repository.
 */
public abstract class PublishToOreTask extends OreTask {

    @Nested
    public abstract Property<OrePublication> getPublication();

    @TaskAction
    public void doPublish() throws InterruptedException {
        final CompletableFuture<OreSession> session = this.session();

        final OrePublication pub = this.getPublication().get();
        final File toPublish = pub.getPublishArtifacts().getSingleFile();
        final boolean createForumPost = pub.getCreateForumPost().get();
        final String versionBody = pub.getVersionBody().get();
        final @Nullable String channel = pub.getChannel().getOrNull();

        try {
            // TODO: Log info about published version (like URL?)
            final Version result = session.thenCompose(api -> api.publishVersion(
                pub.getProjectId().get(),
                new DeployVersionInfo(
                    versionBody,
                    createForumPost,
                    Collections.singletonMap("Channel", Collections.singletonList(channel))
                ),
                toPublish.toPath()
            )).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof GradleException) {
                throw (GradleException) e.getCause();
            } else {
                throw new GradleException("Failed to publish to Ore: " + e.getMessage(), e.getCause());
            }
        }
    }

}
