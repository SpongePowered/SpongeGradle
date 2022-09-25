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
import net.kyori.indra.api.model.ApplyTo;
import org.gradle.api.Action;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.bundling.Jar;
import org.spongepowered.gradle.common.Constants;

import java.util.Objects;

import javax.inject.Inject;

public class SpongeConventionExtension {
    private final IndraExtension indra;

    private final ExtensionAware licensePropertyExtension;
    private final Manifest sharedManifest;

    @Inject
    public SpongeConventionExtension(
        final ObjectFactory objects,
        final ProviderFactory providers,
        final IndraExtension indra,
        final MapProperty<String, Object> licenseProperties,
        final JavaPluginExtension extension
    ) {
        this.indra = indra;
        this.licensePropertyExtension = (ExtensionAware) objects.newInstance(EmptyExtension.class);
        licenseProperties.putAll(providers.provider(() -> this.licensePropertyExtension.getExtensions().getExtraProperties().getProperties()));
        this.sharedManifest = extension.manifest();
    }

    /**
     * Set the repository used in Maven metadata.
     *
     * @param repositoryName the target repository name
     */
    public void repository(final String repositoryName) {
        this.indra.github(Constants.GITHUB_ORGANIZATION, repositoryName);
    }

    /**
     * Set the repository used in Maven metadata.
     *
     * @param repositoryName the target repository name
     * @param extraConfig extra options to apply to the repository
     */
    public void repository(final String repositoryName, final Action<ApplyTo> extraConfig) {
        this.indra.github(Constants.GITHUB_ORGANIZATION, repositoryName, extraConfig);
    }

    public void mitLicense() {
        this.indra.mitLicense();
    }

    /**
     * Get template parameters used by the license plugin for headers.
     *
     * @return the parameters
     */
    public ExtraPropertiesExtension licenseParameters() {
        return this.licensePropertyExtension.getExtensions().getExtraProperties();
    }

    /**
     * Act on template parameters used by the license plugin for headers.
     *
     * @param configureAction the action to use to configure license header properties
     */
    public void licenseParameters(final Action<ExtraPropertiesExtension> configureAction) {
        Objects.requireNonNull(configureAction, "configureAction").execute(this.licensePropertyExtension.getExtensions().getExtraProperties());
    }

    /**
     * Get a manifest that will be included in all {@link Jar} tasks.
     *
     * <p>This allows applying project-wide identifying metadata.</p>
     *
     * @return the shared manifest
     */
    public Manifest sharedManifest() {
        return this.sharedManifest;
    }

    /**
     * Configure a manifest that will be included in all {@link Jar} tasks.
     *
     * <p>This allows applying project-wide identifying metadata.</p>
     *
     * @param configureAction action to configure with
     */
    public void sharedManifest(final Action<Manifest> configureAction) {
        Objects.requireNonNull(configureAction, "configureAction").execute(this.sharedManifest);
    }

}
