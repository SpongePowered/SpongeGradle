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

import net.kyori.indra.Indra;
import net.kyori.indra.IndraExtension;
import net.kyori.indra.IndraLicenseHeaderPlugin;
import net.kyori.indra.IndraPlugin;
import org.cadixdev.gradle.licenser.LicenseExtension;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;
import org.gradle.api.tasks.testing.logging.TestLoggingContainer;
import org.gradle.jvm.tasks.Jar;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;
import org.gradle.plugins.signing.signatory.pgp.PgpSignatoryProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SpongeConventionPlugin implements Plugin<Project> {
    private @MonotonicNonNull Project project;

    @Override
    public void apply(final Project target) {
        this.project = target;
        this.applyPlugins(target.getPlugins());
        final IndraExtension indra = Indra.extension(target);
        target.getExtensions().create(
            "spongeConvention",
            SpongeConventionExtension.class,
            indra,
            target.getExtensions().getByType(LicenseExtension.class)
        );

        this.configurePublicationMetadata(indra);
        this.configureStandardTasks();
        this.configureLicenseHeaders(target.getExtensions().getByType(LicenseExtension.class));

        target.getPlugins().withType(SigningPlugin.class, $ -> {
            this.configureSigning(this.project.getExtensions().getByType(SigningExtension.class));
        });
    }

    private void configureStandardTasks() {
        final TaskContainer tasks = this.project.getTasks();
        tasks.withType(Jar.class).configureEach(jar -> {
            final Map<String, Object> attributes = new HashMap<>();
            attributes.put("Specification-Title", this.project.getName());
            attributes.put("Specification-Vendor", "SpongePowered");
            attributes.put("Specification-Version", this.project.getVersion());
            attributes.put("Implementation-Title", this.project.getName());
            attributes.put("Implementation-Vendor", "SpongePowered");
            attributes.put("Implementation-Version", this.project.getVersion());
            jar.getManifest().attributes(attributes);
        });

        tasks.withType(Test.class).configureEach(test -> {
            final TestLoggingContainer testLogging = test.getTestLogging();
            testLogging.setExceptionFormat(TestExceptionFormat.FULL);
            testLogging.setShowStandardStreams(true);
            testLogging.setShowStackTraces(true);
        });
    }

    private void applyPlugins(final PluginContainer plugins) {
        plugins.apply(IndraPlugin.class);
        plugins.apply(IndraLicenseHeaderPlugin.class);
    }

    private void configurePublicationMetadata(final IndraExtension indra) {
        indra.configurePublications(pub -> pub.pom(pom -> pom.developers(devs -> devs.developer(dev -> {
            dev.getName().set("SpongePowered Team");
            dev.getEmail().set("staff@spongepowered.org");
        }))));

        final @Nullable String spongeSnapshotRepo = (String) this.project.findProperty(ConventionConstants.ProjectProperties.SPONGE_SNAPSHOT_REPO);
        final @Nullable String spongeReleaseRepo = (String) this.project.findProperty(ConventionConstants.ProjectProperties.SPONGE_RELEASE_REPO);
        if (spongeReleaseRepo != null && spongeSnapshotRepo != null) {
            indra.publishSnapshotsTo("sponge", spongeSnapshotRepo);
            indra.publishReleasesTo("sponge", spongeReleaseRepo);
        }

    }

    private void configureLicenseHeaders(final LicenseExtension licenses) {
        licenses.setHeader(this.project.getRootProject().file("HEADER.txt"));
        final ExtraPropertiesExtension ext = ((ExtensionAware) licenses).getExtensions().getExtraProperties();
        ext.set("name", this.project.getRootProject().getName());
    }

    private void configureSigning(final SigningExtension extension) {
        final String spongeSigningKey = (String) this.project.findProperty(ConventionConstants.ProjectProperties.SPONGE_SIGNING_KEY);
        final String spongeSigningPassword = (String) this.project.findProperty(ConventionConstants.ProjectProperties.SPONGE_SIGNING_PASSWORD);
        if (spongeSigningKey != null && spongeSigningPassword != null) {
            final File keyFile = this.project.file(spongeSigningKey);
            if (keyFile.exists()) {
                final StringBuilder contents = new StringBuilder();
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(keyFile), StandardCharsets.UTF_8))) {
                    final char[] buf = new char[2048];
                    int read;
                    while ((read = reader.read(buf)) != -1) {
                        contents.append(buf, 0, read);
                    }
                } catch (final IOException ex) {
                    throw new GradleException("Failed to read Sponge key file", ex);
                }
                extension.useInMemoryPgpKeys(contents.toString(), spongeSigningPassword);
            } else {
                extension.useInMemoryPgpKeys(spongeSigningKey, spongeSigningPassword);
            }
        } else {
            extension.setSignatories(new PgpSignatoryProvider()); // don't use gpg agent
        }
    }
}
