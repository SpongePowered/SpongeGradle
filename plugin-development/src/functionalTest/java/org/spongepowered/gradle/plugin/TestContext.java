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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Context information for individual tests
 */
public class TestContext {
    private final Class<?> resourceBase;
    private final String testName;
    private final Path outputDirectory;
    private final String gradleVersion;
    private final List<String> commonArguments;

    public TestContext(
        final Class<?> resourceBase, final String testName, final Path outputDirectory, final String gradleVersion,
        final List<String> commonArguments
    ) {
        this.resourceBase = resourceBase;
        this.testName = testName;
        this.outputDirectory = outputDirectory;
        this.gradleVersion = gradleVersion;
        this.commonArguments = commonArguments;
    }

    public Path outputDirectory() {
        return this.outputDirectory;
    }

    // copy a resource from the <testName>/in/ directory to the run directory with no changes
    public void copyInput(final String name) throws IOException {
        this.copyInput(name, name);
    }

    // copy a resource from the <testName>/in/ directory to the run directory with the provided new name
    public void copyInput(final String fromName, final String toName) throws IOException {
        try (final InputStream is = this.resourceBase.getResourceAsStream(this.testName + "/in/" + fromName)) {
            Assertions.assertNotNull(is, () -> "No resource found with name " + fromName);
            final Path destination = this.outputDirectory.resolve(toName);
            Files.createDirectories(destination.getParent());
            try (final OutputStream os = Files.newOutputStream(destination)) {
                final byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        }
    }

    /**
     * Expect that a file is present in the output directory with the provided path.
     *
     * @param fileName the file name
     * @return the contents of the file as a string
     * @throws IOException if
     */
    public String readOutput(final String fileName) throws IOException {
        final StringBuilder builder = new StringBuilder();
        try (final BufferedReader reader = Files.newBufferedReader(this.outputDirectory.resolve(fileName), StandardCharsets.UTF_8)) {
            final char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }
        return builder.toString();
    }

    /**
     * Expect that the contents of the output file {@code fileName} is equal to
     * the contents of the resource at  {@code <testName>/out/<resourceName>}.
     *
     * @param resourceName the name of the expected resource
     * @param fileName the name of the actual output file
     * @throws IOException if failed to read one of the files
     */
    public void assertOutputEquals(final String resourceName, final String fileName) throws IOException {
        final String actualOutput = this.readOutput(fileName);

        final StringBuilder builder = new StringBuilder();
        final InputStream is = this.resourceBase.getResourceAsStream(this.testName + "/out/" + resourceName);
        Assertions.assertNotNull(is, () -> "No resource found with name " + resourceName);

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            final char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }

        final String expected = builder.toString();

        Assertions.assertEquals(expected, actualOutput);
    }

    public GradleRunner runner(final String... extraArgs) {
        final List<String> args = new ArrayList<>(this.commonArguments.size() + extraArgs.length);
        args.addAll(this.commonArguments);
        Collections.addAll(args, extraArgs);

        return GradleRunner.create()
            // .forwardOutput()
            .withGradleVersion(this.gradleVersion)
            .withPluginClasspath()
            .withProjectDir(this.outputDirectory.toFile())
            .withArguments(args);
    }

    public BuildResult build(final String... extraArgs) {
        return this.runner(extraArgs).build();
    }
}
