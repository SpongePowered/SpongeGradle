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

import org.gradle.api.Named;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.jetbrains.annotations.NotNull;

// Data that could be applied to a single ore task, or multiple
public interface OrePublication extends Named {

    @Override
    @Internal
    @NotNull String getName();

    /**
     * The project ID to publish to.
     *
     * @return the project ID property
     * @since 2.1.0
     */
    @Input
    Property<String> getProjectId();

    /**
     * Get whether a forum post should be created for this project.
     *
     * <p>Default: {@code true}</p>
     *
     * @return the property for creating a forum post
     * @since 2.1.0
     */
    @Input
    Property<Boolean> getCreateForumPost();

    /**
     * Get contents to provide as the body/changelog for a published version.
     *
     * <p>Default: (empty)</p>
     *
     * @return the body property
     * @since 2.1.0
     */
    @Input
    Property<String> getVersionBody();

    /**
     * Get the channel to publish a release to.
     *
     * @return the release channel.
     * @since 2.1.0
     */
    @Input
    @Optional
    Property<String> getChannel();

    /**
     * Get the artifact to publish.
     *
     * <p>This must evaluate to a single file.</p>
     *
     * <p>Default: the output of the project's {@code jar} task</p>
     *
     * @return the artifact collection
     * @since 2.1.0
     */
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    ConfigurableFileCollection getPublishArtifacts();

}
