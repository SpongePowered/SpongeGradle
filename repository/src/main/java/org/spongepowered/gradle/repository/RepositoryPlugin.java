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

import net.kyori.mammoth.ProjectOrSettingsPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RepositoryPlugin implements ProjectOrSettingsPlugin {

    private static final GradleVersion MINIMUM_VERSION = GradleVersion.version("7.4"); // when kts codegen was added for extensions on repositories

    @Override
    public void applyToProject(
        final @NotNull Project target,
        final @NotNull PluginContainer plugins,
        final @NotNull ExtensionContainer extensions,
        final @NotNull TaskContainer tasks
    ) {
        this.registerExtension(target.getRepositories());
    }

    @Override
    public void applyToSettings(
        final @NotNull Settings target,
        final @NotNull PluginContainer plugins,
        final @NotNull ExtensionContainer extensions
    ) {
        this.registerExtension(target.getDependencyResolutionManagement().getRepositories());
    }

    private void registerExtension(final RepositoryHandler repositories) {
        ((ExtensionAware) repositories).getExtensions().create(
                SpongeRepositoryExtension.class,
                "sponge",
                SpongeRepositoryExtensionImpl.class,
                repositories
        );
    }

    @Override
    public @Nullable GradleVersion minimumGradleVersion() {
        return MINIMUM_VERSION;
    }
}
