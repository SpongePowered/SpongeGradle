/*
 * This file is part of spongegradle-plugin-development, licensed under the MIT License (MIT).
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
package org.spongepowered.gradle.plugin.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.spongepowered.gradle.common.Constants;
import org.spongepowered.gradle.plugin.config.PluginConfiguration;
import org.spongepowered.gradle.plugin.config.PluginContributorConfiguration;
import org.spongepowered.gradle.plugin.config.PluginDependencyConfiguration;
import org.spongepowered.gradle.plugin.config.PluginLinksConfiguration;
import org.spongepowered.plugin.metadata.PluginContributor;
import org.spongepowered.plugin.metadata.PluginDependency;
import org.spongepowered.plugin.metadata.PluginLinks;
import org.spongepowered.plugin.metadata.PluginMetadata;
import org.spongepowered.plugin.metadata.util.PluginMetadataHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CacheableTask
public abstract class WritePluginMetadataTask extends DefaultTask {

    public WritePluginMetadataTask() {
        this.setGroup(Constants.TASK_GROUP);
    }
    
    @Nested
    public abstract NamedDomainObjectContainer<PluginConfiguration> getConfigurations();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void execute() throws IOException {
        final PluginMetadataHelper helper = PluginMetadataHelper.builder().build();
        final List<PluginMetadata> metadata = new ArrayList<>();

        for (final PluginConfiguration configuration : this.getConfigurations()) {
            final PluginMetadata.Builder metadataBuilder = PluginMetadata.builder();

            metadataBuilder
                .id(configuration.getName())
                .loader(configuration.getLoader().get())
                .name(configuration.getDisplayName().get())
                .version(configuration.getVersion().get())
                .mainClass(configuration.getMainClass().get())
                .description(configuration.getDescription().get())
            ;

            final PluginLinks.Builder linksBuilder = PluginLinks.builder();
            final PluginLinksConfiguration linksConfiguration = configuration.getLinks();
            if (linksConfiguration.getHomepage().isPresent()) {
                linksBuilder.homepage(linksConfiguration.getHomepage().get());
            }
            if (linksConfiguration.getSource().isPresent()) {
                linksBuilder.source(linksConfiguration.getSource().get());
            }
            if (linksConfiguration.getIssues().isPresent()) {
                linksBuilder.issues(linksConfiguration.getIssues().get());
            }
            metadataBuilder.links(linksBuilder.build());

            for (final PluginContributorConfiguration contributor : configuration.getContributors()) {
                final PluginContributor.Builder contributorBuilder = PluginContributor.builder();

                contributorBuilder.name(contributor.getName());
                if (contributor.getDescription().isPresent()) {
                    contributorBuilder.description(contributor.getDescription().get());
                }
                metadataBuilder.addContributor(contributorBuilder.build());
            }

            for (final PluginDependencyConfiguration dependency : configuration.getDependencies()) {
                final PluginDependency.Builder dependencyBuilder = PluginDependency.builder();

                dependencyBuilder.id(dependency.getName());
                dependencyBuilder.version(dependency.getVersion().get());
                if (dependency.getLoadOrder().isPresent()) {
                    dependencyBuilder.loadOrder(dependency.getLoadOrder().get());
                }
                if (dependency.getOptional().isPresent()) {
                    dependencyBuilder.optional(dependency.getOptional().get());
                }

                metadataBuilder.addDependency(dependencyBuilder.build());
            }

            metadata.add(metadataBuilder.build());
        }

        final String json = helper.toJson(metadata);
        final Path outputDirectory = this.getOutputDirectory().getAsFile().get().toPath().resolve("META-INF");
        Files.createDirectories(outputDirectory);
        final Path outputFile = outputDirectory.resolve("plugins.json");
        Files.deleteIfExists(outputFile);
        Files.write(outputFile, json.getBytes(StandardCharsets.UTF_8));
    }
}
