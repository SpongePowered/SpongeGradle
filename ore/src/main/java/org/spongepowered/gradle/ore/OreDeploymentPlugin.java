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

import net.kyori.mammoth.ProjectPlugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.HelpTasksPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.gradle.ore.internal.OreDeploymentExtensionImpl;
import org.spongepowered.gradle.ore.internal.OreSessionService;
import org.spongepowered.gradle.ore.task.OreTask;
import org.spongepowered.gradle.ore.task.PublishToOreTask;
import org.spongepowered.gradle.ore.task.ViewOrePermissions;

import java.time.Duration;

public class OreDeploymentPlugin implements ProjectPlugin {

    private static final String ORE_DEPLOYMENT_EXTENSION = "oreDeployment";
    private static final String PUBLISH_TO_ORE_TASK = "publishToOre";

    private static final String ORE_GROUP = "ore";

    @Override
    public void apply(
        final @NotNull Project project,
        final @NotNull PluginContainer plugins,
        final @NotNull ExtensionContainer extensions,
        final @NotNull TaskContainer tasks
    ) {
        final OreDeploymentExtension extension = extensions.create(OreDeploymentExtension.class, ORE_DEPLOYMENT_EXTENSION, OreDeploymentExtensionImpl.class);

        final Provider<OreSessionService> ore = project.getGradle().getSharedServices().registerIfAbsent(
            "oreSessions",
            OreSessionService.class,
            params -> {
                params.getParameters().getSessionDuration().set(Duration.ofHours(3));
            }
        );

        tasks.withType(OreTask.class).configureEach(task -> {
            task.getOreSessions().set(ore);
            task.getOreEndpoint().set(extension.oreEndpoint());
            task.getOreApiKey().set(extension.apiKey());
        });

        this.registerPublicationTasks(extension, tasks);

        tasks.register("orePermissions", ViewOrePermissions.class, task -> {
            task.setGroup(HelpTasksPlugin.HELP_GROUP);
        });
    }

    private void registerPublicationTasks(final OreDeploymentExtension extension, final TaskContainer tasks) {
        tasks.register(PUBLISH_TO_ORE_TASK, task -> {
            task.dependsOn(tasks.withType(PublishToOreTask.class));
            task.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
        });

        extension.publications().all(publication -> {
            tasks.register(publishTaskName(publication.getName()), PublishToOreTask.class, task -> {
                task.getPublication().set(publication);
                task.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
            });
        });
    }

    private void registerDefaultPublication(final OreDeploymentExtension extension, final PluginContainer plugins) {
        // todo: grab plugin ID from SG?
    }

    private String publishTaskName(final String publicationName) {
        final String capitalized;
        if (publicationName.length() > 0 && Character.isLowerCase(publicationName.codePointAt(0))) {
            final StringBuilder builder = new StringBuilder(publicationName.length());
            capitalized = builder.appendCodePoint(Character.toUpperCase(publicationName.codePointAt(0)))
                .append(publicationName, builder.length(), publicationName.length())
                .toString();
        } else {
            capitalized = publicationName;
        }
        return "publish" + capitalized + "PublicationToOre";
    }
}
