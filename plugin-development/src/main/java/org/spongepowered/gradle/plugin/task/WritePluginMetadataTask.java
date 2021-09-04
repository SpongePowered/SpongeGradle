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

import com.google.gson.Gson;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.spongepowered.gradle.common.Constants;
import org.spongepowered.gradle.plugin.config.MetadataContainerConfiguration;
import org.spongepowered.gradle.plugin.config.PluginBrandingConfiguration;
import org.spongepowered.gradle.plugin.config.PluginConfiguration;
import org.spongepowered.gradle.plugin.config.PluginContributorConfiguration;
import org.spongepowered.gradle.plugin.config.PluginDependencyConfiguration;
import org.spongepowered.gradle.plugin.config.PluginInheritableConfiguration;
import org.spongepowered.gradle.plugin.config.PluginLinksConfiguration;
import org.spongepowered.gradle.plugin.config.ContainerLoaderConfiguration;
import org.spongepowered.plugin.metadata.builtin.MetadataContainer;
import org.spongepowered.plugin.metadata.builtin.MetadataParser;
import org.spongepowered.plugin.metadata.builtin.StandardInheritable;
import org.spongepowered.plugin.metadata.builtin.model.StandardPluginBranding;
import org.spongepowered.plugin.metadata.builtin.model.StandardPluginContributor;
import org.spongepowered.plugin.metadata.builtin.model.StandardPluginDependency;
import org.spongepowered.plugin.metadata.builtin.model.StandardPluginLinks;
import org.spongepowered.plugin.metadata.builtin.StandardPluginMetadata;
import org.spongepowered.plugin.metadata.builtin.model.StandardContainerLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public abstract class WritePluginMetadataTask extends DefaultTask {

    private static final Gson GSON = MetadataParser.gsonBuilder().create();

    public WritePluginMetadataTask() {
        this.setGroup(Constants.TASK_GROUP);
    }
    
    @Nested
    public abstract Property<MetadataContainerConfiguration> getSourceContainer();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void execute() throws IOException {
        final MetadataContainerConfiguration src = this.getSourceContainer().get();
        final MetadataContainer.Builder container = new MetadataContainer.Builder();
        container.license(src.getLicense().get())
            .loader(this.convertLoader(src.getLoader()))
            .globalMetadata(this.populateBuilder(src.getGlobal(), new StandardInheritable.Builder()).build());
        if (src.getMappings().isPresent()) {
            container.mappings(src.getMappings().get());
        }

        for (final PluginConfiguration configuration : src.getPlugins()) {
            final StandardPluginMetadata.Builder metadataBuilder = new StandardPluginMetadata.Builder();

            this.populateBuilder(configuration, metadataBuilder)
                    .entrypoint(configuration.getEntrypoint().get());

            metadataBuilder
                .id(configuration.getName())
                .name(configuration.getDisplayName().get())
                .entrypoint(configuration.getEntrypoint().get())
                .description(configuration.getDescription().get())
            ;

            container.addMetadata(metadataBuilder.build());
        }

        final Path outputDirectory = this.getOutputDirectory().getAsFile().get().toPath().resolve("META-INF");
        Files.createDirectories(outputDirectory);
        final Path outputFile = outputDirectory.resolve("sponge_plugins.json");
        Files.deleteIfExists(outputFile);
        try {
            MetadataParser.write(outputFile, container.build(), WritePluginMetadataTask.GSON, true);
        } catch (final InvalidVersionSpecificationException ex) {
            throw new InvalidUserDataException(ex.getMessage(), ex);
        }
    }

    private StandardContainerLoader convertLoader(final ContainerLoaderConfiguration src) {
        return StandardContainerLoader.builder()
            .name(src.getName().get())
            .version(src.getVersion().get())
            .build();
    }

    private <T extends StandardInheritable.AbstractBuilder<?, T>> T populateBuilder(final PluginInheritableConfiguration src, final T builder) {
        builder.version(src.getVersion().get());

        final StandardPluginLinks.Builder linksBuilder = StandardPluginLinks.builder();
        final PluginLinksConfiguration linksConfiguration = src.getLinks();
        if (linksConfiguration.getHomepage().isPresent()) {
            linksBuilder.homepage(linksConfiguration.getHomepage().get());
        }
        if (linksConfiguration.getSource().isPresent()) {
            linksBuilder.source(linksConfiguration.getSource().get());
        }
        if (linksConfiguration.getIssues().isPresent()) {
            linksBuilder.issues(linksConfiguration.getIssues().get());
        }
        builder.links(linksBuilder.build());

        // TODO: validate paths here?
        final StandardPluginBranding.Builder brandingBuilder = StandardPluginBranding.builder();
        final PluginBrandingConfiguration brandingConfiguration = src.getBranding();
        if (brandingConfiguration.getIcon().isPresent()) {
            brandingBuilder.icon(brandingConfiguration.getIcon().get());
        }
        if (brandingConfiguration.getLogo().isPresent()) {
            brandingBuilder.icon(brandingConfiguration.getLogo().get());
        }
        builder.branding(brandingBuilder.build());

        for (final PluginContributorConfiguration contributor : src.getContributors()) {
            final StandardPluginContributor.Builder contributorBuilder = StandardPluginContributor.builder();

            contributorBuilder.name(contributor.getName());
            if (contributor.getDescription().isPresent()) {
                contributorBuilder.description(contributor.getDescription().get());
            }
            builder.addContributor(contributorBuilder.build());
        }

        for (final PluginDependencyConfiguration dependency : src.getDependencies()) {
            final StandardPluginDependency.Builder dependencyBuilder = StandardPluginDependency.builder();

            dependencyBuilder.id(dependency.getName());
            dependencyBuilder.version(dependency.getVersion().get());
            if (dependency.getLoadOrder().isPresent()) {
                dependencyBuilder.loadOrder(dependency.getLoadOrder().get());
            }
            if (dependency.getOptional().isPresent()) {
                dependencyBuilder.optional(dependency.getOptional().get());
            }

            builder.addDependency(dependencyBuilder.build());
        }
        return builder;
    }
}
