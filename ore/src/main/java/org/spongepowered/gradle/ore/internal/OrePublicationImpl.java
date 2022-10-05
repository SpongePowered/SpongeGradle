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

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.gradle.ore.OrePublication;

import javax.inject.Inject;

public class OrePublicationImpl implements OrePublication {

    private final String name;
    private final Property<String> projectId;
    private final Property<Boolean> createForumPost;
    private final Property<String> versionBody;
    private final Property<String> channel;
    private final ConfigurableFileCollection publishArtifacts;

    @Inject
    public OrePublicationImpl(final ObjectFactory objects, final String name) {
        this.name = name;

        this.projectId = objects.property(String.class);
        this.createForumPost = objects.property(Boolean.class);
        this.versionBody = objects.property(String.class);
        this.channel = objects.property(String.class);
        this.publishArtifacts = objects.fileCollection();
    }

    @Override
    public @NotNull String getName() {
        return this.name;
    }

    @Override
    public Property<String> getProjectId() {
        return this.projectId;
    }

    @Override
    public Property<Boolean> getCreateForumPost() {
        return this.createForumPost;
    }

    @Override
    public Property<String> getVersionBody() {
        return this.versionBody;
    }

    @Override
    public Property<String> getChannel() {
        return this.channel;
    }

    @Override
    public ConfigurableFileCollection getPublishArtifacts() {
        return this.publishArtifacts;
    }

}
