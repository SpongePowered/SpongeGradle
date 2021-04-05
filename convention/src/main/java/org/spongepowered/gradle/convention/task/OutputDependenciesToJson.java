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
package org.spongepowered.gradle.convention.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.internal.impldep.org.apache.commons.codec.binary.Hex;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class OutputDependenciesToJson extends DefaultTask {

    // From http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
    private static final char[] hexArray = "0123456789abcdef".toCharArray();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * A single dependency.
     */
    static final class DependencyDescriptor {

        final String group;
        final String module;
        final String version;
        final String md5;

        DependencyDescriptor(final String group, final String module, final String version, final String md5) {
            this.group = group;
            this.module = module;
            this.version = version;
            this.md5 = md5;
        }
    }

    /**
     * A manifest containing a list of dependencies.
     *
     * <p>At runtime, transitive dependencies won't be traversed, so this needs to
     * include direct + transitive depends.</p>
     */
    static final class DependencyManifest {

        final List<DependencyDescriptor> dependencies;

        DependencyManifest(final List<DependencyDescriptor> dependencies) {
            this.dependencies = dependencies;
        }
    }

    /**
     * Configuration to gather dependency artifacts from.
     */
    @Input
    public abstract Property<Configuration> getConfiguration();

    /**
     * Excludes configuration, to remove certain entries from dependencies and transitive dependencies of [configuration]
     */
    @Input
    public abstract Property<Configuration> getExcludeConfiguration();

    /**
     * Classifiers to include in the dependency manifest. The empty string identifies no classifier.
     */
    @Input
    public abstract SetProperty<String> getAllowedClassifiers();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public OutputDependenciesToJson() {
        this.getAllowedClassifiers().add("");
    }

    @org.gradle.api.tasks.TaskAction
    public void generateDependenciesJson() {
        final Configuration foundConfig;
        if (this.getExcludeConfiguration().isPresent()) {
            final Configuration config = this.getConfiguration().get().copyRecursive();
            final Configuration excludes = this.getExcludeConfiguration().get();
            excludes.getAllDependencies().forEach(dep -> {
                final Map<String, String> params = new HashMap<>();
                params.put("group", dep.getGroup());
                params.put("module", dep.getName());
                config.exclude(params);
            });
            foundConfig = config;
        } else {
            foundConfig = this.getConfiguration().get();
        }

        final List<DependencyDescriptor> descriptors = foundConfig.getResolvedConfiguration().getFirstLevelModuleDependencies().stream()
            .flatMap(dep -> dep.getAllModuleArtifacts().stream())
            // only jars with the allowed classifiers
            .filter(it -> it.getExtension().equals("jar") && this.getAllowedClassifiers().get().contains(it.getClassifier() == null ? "" : it.getClassifier()))
            .filter(it -> !Objects.equals(it.getModuleVersion().getId().getName(), "SpongeAPI"))
            .distinct()
            .map(dependency -> {
                final ModuleVersionIdentifier ident = dependency.getModuleVersion().getId();
                final String version;
                final @Nullable ComponentIdentifier id = dependency.getId().getComponentIdentifier();
                if (id instanceof ModuleComponentIdentifier) {
                    version = ((ModuleComponentIdentifier) id).getVersion();
                } else {
                    version = ident.getVersion();
                }

                // Get file input stream for reading the file content
                final String md5hash;
                try (final InputStream in = new FileInputStream(dependency.getFile())) {
                    final MessageDigest hasher = MessageDigest.getInstance("MD5");
                    final byte[] buf = new byte[4096];
                    int read;
                    while ((read = in.read(buf)) != -1) {
                        hasher.update(buf, 0, read);
                    }

                    md5hash = OutputDependenciesToJson.toHexString(hasher.digest());
                } catch (final IOException | NoSuchAlgorithmException ex) {
                    throw new GradleException("Failed to create hash for " + dependency, ex);
                }

                // create descriptor
                return new DependencyDescriptor(
                    ident.getGroup(),
                    ident.getName(),
                    version,
                    md5hash
                );
            }).collect(Collectors.toList());
        final DependencyManifest manifest = new DependencyManifest(descriptors);

        this.getLogger().info("Writing to {}", this.getOutputFile().get().getAsFile());
        try (
            final BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(this.getOutputFile().get().getAsFile()), StandardCharsets.UTF_8))
        ) {
            OutputDependenciesToJson.GSON.toJson(manifest, writer);
        } catch (final IOException ex) {
            throw new GradleException("Failed to write dependencies manifest", ex);
        }
    }

    public static String toHexString(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = OutputDependenciesToJson.hexArray[v >>> 4];
            hexChars[j * 2 + 1] = OutputDependenciesToJson.hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
