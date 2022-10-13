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

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.gradle.ore.OreDeploymentExtension;
import org.spongepowered.gradle.ore.OrePublication;

import javax.inject.Inject;

public class OreDeploymentExtensionImpl implements OreDeploymentExtension {

    private static final String DEFAULT_ENDPOINT = "https://ore.spongepowered.org/";
    private static final String API_KEY_ENVIRONMENT_VARIABLE = "ORE_TOKEN";
    private final Property<String> oreEndpoint;
    private final Property<String> apiKey;

    private final NamedDomainObjectContainer<OrePublication> publications;

    @Inject
    public OreDeploymentExtensionImpl(final ObjectFactory objects, final ProviderFactory providers) {
        this.oreEndpoint = objects.property(String.class)
                .convention(OreDeploymentExtensionImpl.DEFAULT_ENDPOINT);

        this.apiKey = objects.property(String.class)
                .convention(providers.environmentVariable(OreDeploymentExtensionImpl.API_KEY_ENVIRONMENT_VARIABLE));

        this.publications = objects.domainObjectContainer(
            OrePublication.class,
            name -> objects.newInstance(OrePublicationImpl.class, name)
        );
    }

    @Override
    public @NotNull Property<String> oreEndpoint() {
        return this.oreEndpoint;
    }

    @Override
    public @NotNull Property<String> apiKey() {
        return this.apiKey;
    }

    @Override
    public @NotNull NamedDomainObjectContainer<OrePublication> publications() {
        return this.publications;
    }

}
