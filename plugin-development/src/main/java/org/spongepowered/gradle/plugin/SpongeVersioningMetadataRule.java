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
package org.spongepowered.gradle.plugin;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.artifacts.CacheableRule;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataDetails;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CacheableRule
public class SpongeVersioningMetadataRule implements ComponentMetadataRule {
    // <mc version>-<API target>-<RC<BUILD> // development
    // <mc version>-<API target> // stable
    private static final Pattern SPONGE_IMPL_VERSION = Pattern.compile("(?<mc>[^-]+)-(?<api>[\\d.]+)(?:-RC(?<rc>\\d+))?");
    private static final List<String> STATUS_SCHEME = Collections.unmodifiableList(Arrays.asList("rc", "release"));

    public static final Attribute<String> MINECRAFT_TARGET = Attribute.of("org.spongepowered.minecraft-target", String.class);
    public static final Attribute<String> API_TARGET = Attribute.of("org.spongepowered.api-target", String.class);

    @Override
    public void execute(final ComponentMetadataContext ctx) {
        final ComponentMetadataDetails details = ctx.getDetails();
        final String version = details.getId().getVersion();
        final Matcher match = SpongeVersioningMetadataRule.SPONGE_IMPL_VERSION.matcher(version);
        if (!match.matches()) {
            return;
        }

        final String mcVersion = match.group("mc");
        final String apiTarget = match.group("api");
        final @Nullable String rc = match.group("rc");

        details.setStatusScheme(STATUS_SCHEME);
        if (rc != null) {
            details.setStatus("rc");
        }

        final AttributeContainer attrs = details.getAttributes();
        attrs.attribute(SpongeVersioningMetadataRule.MINECRAFT_TARGET, mcVersion);
        attrs.attribute(SpongeVersioningMetadataRule.API_TARGET, apiTarget);
    }
}
