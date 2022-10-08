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

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.ExtensionAware;
import org.jetbrains.annotations.NotNull;

public class RepositoryPlugin implements Plugin<Object> {

    @Override
    public void apply(final @NotNull Object target) {
        if (target instanceof Project) {
            this.applyToProject((Project) target);
        } else if (target instanceof Settings) {
            this.applyToSettings((Settings) target);
        } else if (target instanceof Gradle) {
            // no-op
        } else {
            throw new GradleException(
                    "Sponge repository plugin target '" + target
                            + "' is of unexpected type " + target.getClass()
                            + ", expecting a Project or Settings instance"
            );
        }
    }

    private void applyToProject(final Project project) {
        this.registerExtension(project.getRepositories());
    }

    private void applyToSettings(final Settings settings) {
        settings.getGradle().getPlugins().apply(RepositoryPlugin.class);
        this.registerExtension(settings.getDependencyResolutionManagement().getRepositories());
    }

    private void registerExtension(final RepositoryHandler repositories) {
        ((ExtensionAware) repositories).getExtensions().create(
            SpongeRepositoryExtension.class,
            "sponge",
            SpongeRepositoryExtensionImpl.class,
            repositories
        );
    }

}
