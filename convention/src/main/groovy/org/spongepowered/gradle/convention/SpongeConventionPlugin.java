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
import net.kyori.indra.git.GitPlugin;
import net.kyori.indra.git.IndraGitExtension;
import org.cadixdev.gradle.licenser.LicenseExtension;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;
import org.gradle.api.tasks.testing.logging.TestLoggingContainer;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;
import org.gradle.plugins.signing.signatory.pgp.PgpSignatoryProvider;
import org.spongepowered.gradle.convention.task.SignJarTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public abstract class SpongeConventionPlugin implements Plugin<Project> {
    private @MonotonicNonNull Project project;

    @Override
    public void apply(final Project target) {
        this.project = target;
        this.applyPlugins(target.getPlugins());
        final IndraExtension indra = Indra.extension(target.getExtensions());
        final SpongeConventionExtension sponge = target.getExtensions().create(
            "spongeConvention",
            SpongeConventionExtension.class,
            indra,
            target.getExtensions().getByType(LicenseExtension.class),
            target.getConvention().getPlugin(JavaPluginConvention.class)
        );

        this.configurePublicationMetadata(indra);
        this.configureStandardTasks();
        this.configureLicenseHeaders(target.getExtensions().getByType(LicenseExtension.class));
        this.configureJarSigning();

        target.getPlugins().withType(SigningPlugin.class, $ ->
            target.afterEvaluate(p -> this.configureSigning(p.getExtensions().getByType(SigningExtension.class))));

        final Manifest manifest = sponge.sharedManifest();
        this.project.getTasks().withType(Jar.class).configureEach(task -> task.getManifest().from(manifest));
        target.afterEvaluate(proj -> {
            // Only configure manifest after evaluate so we can capture project version properly
            this.configureJarTasks(manifest, proj.getExtensions().getByType(IndraGitExtension.class));
        });
    }

    private void configureJarTasks(final Manifest manifest, final IndraGitExtension git) {
        // Add some standard attributes
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("Specification-Title", this.project.getName());
        attributes.put("Specification-Vendor", "SpongePowered");
        attributes.put("Specification-Version", this.project.getVersion());
        attributes.put("Implementation-Title", this.project.getName());
        attributes.put("Implementation-Vendor", "SpongePowered");
        attributes.put("Implementation-Version", this.project.getVersion());
        manifest.attributes(attributes);
        git.applyVcsInformationToManifest(manifest);
    }

    private void configureStandardTasks() {
        final TaskContainer tasks = this.project.getTasks();

        tasks.withType(JavaCompile.class).configureEach(compile -> {
            compile.getOptions().getCompilerArgs().addAll(Arrays.asList("-Xmaxerrs", "1000"));
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
        plugins.apply(GitPlugin.class);
    }

    private void configurePublicationMetadata(final IndraExtension indra) {
        indra.configurePublications(pub -> pub.pom(pom -> {
            pom.developers(devs -> devs.developer(dev -> {
                dev.getName().set("SpongePowered Team");
                dev.getEmail().set("staff@spongepowered.org");
            }));
            pom.organization(org -> {
                org.getName().set("SpongePowered");
                org.getUrl().set("https://spongepowered.org/");
            });
        }));

        final @Nullable String spongeSnapshotRepo = (String) this.project.findProperty(ConventionConstants.ProjectProperties.SPONGE_SNAPSHOT_REPO);
        final @Nullable String spongeReleaseRepo = (String) this.project.findProperty(ConventionConstants.ProjectProperties.SPONGE_RELEASE_REPO);
        if (spongeReleaseRepo != null && spongeSnapshotRepo != null) {
            indra.publishSnapshotsTo("sponge", spongeSnapshotRepo);
            indra.publishReleasesTo("sponge", spongeReleaseRepo);
        }

    }

    private void configureLicenseHeaders(final LicenseExtension licenses) {
        licenses.setHeader(this.project.getRootProject().file(ConventionConstants.Locations.LICENSE_HEADER));
        licenses.properties(ext -> {
            ext.set("name", this.project.getRootProject().getName());
        });
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

    private void configureJarSigning() {
        if (!this.project.hasProperty(ConventionConstants.ProjectProperties.SPONGE_KEY_STORE)) {
            return;
        }

        // We have to replace the default artifact which is a bit ugly
        // https://github.com/gradle/gradle/pull/13650 should make it easier
        final String[] outgoingConfigurations = {JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME};
        final String keyStoreProp = (String) this.project.property(ConventionConstants.ProjectProperties.SPONGE_KEY_STORE);
        final File fileTemp = new File(keyStoreProp);
        final File keyStoreFile;
        if (fileTemp.exists()) {
            keyStoreFile = fileTemp;
        } else {
            // Write keystore to a temporary file
            final Path dest = this.project.getLayout().getProjectDirectory().file(".gradle/signing-key").getAsFile().toPath();
            try {
                Files.createDirectories(dest.getParent());
                Files.createFile(dest, PosixFilePermissions.asFileAttribute(new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))));
            } catch (final IOException ignored) {
                // oh well
            }

            final byte[] decoded = Base64.getDecoder().decode(keyStoreProp);
            try (final OutputStream out = Files.newOutputStream(dest)) {
                out.write(decoded);
            } catch (final IOException ex) {
                throw new GradleException("Unable to write key file to disk", ex);
            }

            // Delete the temporary file when the runtime exits
            keyStoreFile = dest.toFile();
            keyStoreFile.deleteOnExit();
        }

        this.project.getTasks().matching(it -> it.getName().equals("jar") && it instanceof Jar).whenTaskAdded(task -> {
            final Jar jarTask = (Jar) task;
            jarTask.getArchiveClassifier().set("unsigned");
            final TaskProvider<SignJarTask> sign = this.project.getTasks().register("signJar", SignJarTask.class, config -> {
                config.dependsOn(jarTask);
                config.from(this.project.zipTree(jarTask.getOutputs().getFiles().getSingleFile()));
                config.setManifest(jarTask.getManifest());
                config.getArchiveClassifier().set("");
                config.getKeyStore().set(keyStoreFile);
                config.getAlias().set((String) this.project.property(ConventionConstants.ProjectProperties.SPONGE_KEY_STORE_ALIAS));
                config.getStorePassword().set((String) this.project.property(ConventionConstants.ProjectProperties.SPONGE_KEY_STORE_PASSWORD));
            });

            for (final String configName : outgoingConfigurations) {
                this.project.getConfigurations().named(configName, conf -> {
                    conf.getOutgoing().artifact(sign);
                });
            }

            this.project.getTasks().named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, t -> t.dependsOn(sign));
        });

        this.project.afterEvaluate(p -> {
            // Remove the unsigned artifact from publications
            for (final String outgoing : outgoingConfigurations) {
                p.getConfigurations().named(outgoing, conf -> {
                    conf.getOutgoing().getArtifacts().removeIf(it -> Objects.equals(it.getClassifier(), "unsigned"));
                });
            }
        });
    }
}
