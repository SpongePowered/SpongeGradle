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
                .setId(configuration.getName())
                .setLoader(configuration.loader().get())
                .setName(configuration.displayName().get())
                .setVersion(configuration.version().get())
                .setMainClass(configuration.mainClass().get())
                .setDescription(configuration.description().get())
            ;

            final PluginLinks.Builder linksBuilder = PluginLinks.builder();
            final PluginLinksConfiguration linksConfiguration = configuration.links();
            if (linksConfiguration.homepage().isPresent()) {
                linksBuilder.setHomepage(linksConfiguration.homepage().get());
            }
            if (linksConfiguration.source().isPresent()) {
                linksBuilder.setSource(linksConfiguration.source().get());
            }
            if (linksConfiguration.issues().isPresent()) {
                linksBuilder.setIssues(linksConfiguration.issues().get());
            }
            metadataBuilder.setLinks(linksBuilder.build());

            final List<PluginContributor> contributors = new ArrayList<>();

            for (final PluginContributorConfiguration contributor : configuration.contributors()) {
                final PluginContributor.Builder contributorBuilder = PluginContributor.builder();

                contributorBuilder.setName(contributor.getName());
                if (contributor.description().isPresent()) {
                    contributorBuilder.setDescription(contributor.description().get());
                }
                contributors.add(contributorBuilder.build());
            }

            metadataBuilder.setContributors(contributors);

            final List<PluginDependency> dependencies = new ArrayList<>();

            for (final PluginDependencyConfiguration dependency : configuration.dependencies()) {
                final PluginDependency.Builder dependencyBuilder = PluginDependency.builder();

                dependencyBuilder.setId(dependency.getName());
                dependencyBuilder.setVersion(dependency.version().get());
                if (dependency.loadOrder().isPresent()) {
                    dependencyBuilder.setLoadOrder(dependency.loadOrder().get());
                }
                if (dependency.optional().isPresent()) {
                    dependencyBuilder.setOptional(dependency.optional().get());
                }
                dependencies.add(dependencyBuilder.build());
            }

            metadataBuilder.setDependencies(dependencies);

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
