/*
 * This file is part of spongegradle-convention, licensed under the MIT License (MIT).
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
package org.spongepowered.gradle.convention;

import net.kyori.indra.IndraExtension;
import net.kyori.indra.data.ApplyTo;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.spongepowered.gradle.common.Constants;

import javax.inject.Inject;

public class SpongeConventionExtension {
    private final IndraExtension extension;

    @Inject
    public SpongeConventionExtension(final ObjectFactory objects, final IndraExtension extension) {
        this.extension = extension;
    }

    public void repository(final String repositoryName) {
        this.extension.github(Constants.GITHUB_ORGANIZATION, repositoryName);
    }

    public void repository(final String repositoryName, final Action<ApplyTo> extraConfig) {
        this.extension.github(Constants.GITHUB_ORGANIZATION, repositoryName, extraConfig);
    }

}
