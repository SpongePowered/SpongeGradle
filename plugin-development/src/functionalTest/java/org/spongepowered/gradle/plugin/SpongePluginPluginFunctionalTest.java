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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.gradle.testkit.runner.UnexpectedBuildSuccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Simple functional tests for the Sponge plugin development plugin.
 *
 * <p>To capture errors from builds, it can be helpful to register exception
 * breakpoints on {@link UnexpectedBuildFailure} and {@link UnexpectedBuildSuccess}.
 * This allows viewing build reports before the temporary directories are cleared.</p>
 */
class SpongePluginPluginFunctionalTest {

    void testSimpleBuild(final TestContext ctx) throws IOException {
        ctx.copyInput("build.gradle.kts");
        ctx.copyInput("settings.gradle.kts");

        final BuildResult result = ctx.build("writePluginMetadata");

        Assertions.assertTrue(result.getOutput().contains("SpongePowered Plugin"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":writePluginMetadata").getOutcome());

        // Then make sure we actually generated a plugins file
        ctx.assertOutputEquals("plugins.json", "build/generated/sponge/plugin/META-INF/plugins.json");
    }

    void testBuildFailsWhenMissingProperties(final TestContext ctx) throws IOException {
        ctx.copyInput("build.gradle.kts");
        ctx.copyInput("settings.gradle.kts");

        final BuildResult result = ctx.runner("writePluginMetadata")
            .buildAndFail();
        assertEquals(TaskOutcome.FAILED, result.task(":writePluginMetadata").getOutcome());

        assertTrue(
            result.getOutput().contains("No value has been specified for property") // Gradle 6.x
                || result.getOutput().contains("doesn't have a configured value") // Gradle 7.x
        );
        assertTrue(result.getOutput().contains(".loader"));
        assertTrue(result.getOutput().contains(".mainClass"));
    }

    void testPropertiesInferredFromProjectConfiguration(final TestContext ctx) throws IOException {
        ctx.copyInput("build.gradle.kts");
        ctx.copyInput("settings.gradle.kts");

        final BuildResult result = ctx.build("writePluginMetadata");
        assertEquals(TaskOutcome.SUCCESS, result.task(":writePluginMetadata").getOutcome());

        final JsonObject plugins = new Gson().fromJson(ctx.readOutput("build/generated/sponge/plugin/META-INF/plugins.json"), JsonObject.class);
        final JsonObject plugin = plugins.getAsJsonArray("plugins").get(0).getAsJsonObject();

        // Compare properties drawn from build
        assertEquals("1234", plugin.getAsJsonPrimitive("version").getAsString());
        assertEquals(
            "An example of properties coming from build configuration",
            plugin.getAsJsonPrimitive("description").getAsString()
        );
    }

    void testComplexBuild(final TestContext ctx) throws IOException {
        ctx.copyInput("build.gradle.kts");
        ctx.copyInput("settings.gradle.kts");
        ctx.copyInput("Example.java", "src/main/java/org/spongepowered/example/Example.java");

        final BuildResult result = ctx.build("build");
        assertEquals(TaskOutcome.SUCCESS, result.task(":writePluginMetadata").getOutcome());

        // Test that plugin metadata was included in the jar
        final Path jar = ctx.outputDirectory().resolve("build/libs/complexbuild-1.0-SNAPSHOT.jar");

        try (final JarFile jf = new JarFile(jar.toFile())) {
            assertNotNull(jf.getEntry("META-INF/plugins.json"));
        }
    }

    // the test framework itself (still fairly basic)

    @TestFactory
    Stream<DynamicTest> functionalTests(@TempDir final Path runDirectory) {
        // Common arguments for Gradle
        final List<String> commonArgs = Arrays.asList("--warning-mode", "fail", "--stacktrace");

        // Test variants
        final String[][] variants = {
            {"6.8.3", ""},
            {"7.0", ""},
            {"6.8.3", "--configuration-cache"},
            {"7.0", "--configuration-cache"},
        };

        // The actual tests to execute
        return Stream.of(
            new Pair("simplebuild", this::testSimpleBuild),
            new Pair("missingproperties", this::testBuildFailsWhenMissingProperties),
            new Pair("propertiesinferred", this::testPropertiesInferredFromProjectConfiguration),
            new Pair("complexbuild", this::testComplexBuild)
        ).flatMap(test -> Arrays.stream(variants)
            .map(variant -> {
                final List<String> extraArgs = SpongePluginPluginFunctionalTest.processArgs(commonArgs, variant[1]);
                final TestContext context;
                try {
                    context = new TestContext(
                        this.getClass(),
                        test.name,
                        Files.createTempDirectory(runDirectory, test.name + System.nanoTime()),
                        variant[0],
                        extraArgs
                    );
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                return DynamicTest.dynamicTest(test.name + " (gradle " + variant[0] + ", args=" + extraArgs + ")", () -> test.method.accept(context));
            }));
    }

    private static List<String> processArgs(final List<String> common, final String extra) {
        final List<String> ret = new ArrayList<>(common);
        if (!extra.isEmpty()) {
            Collections.addAll(ret, extra.split(" ", -1));
        }
        return ret;
    }

    static class Pair {
        final String name;
        final ThrowingConsumer<TestContext> method;

        public Pair(final String name, final ThrowingConsumer<TestContext> method) {
            this.name = name;
            this.method = method;
        }
    }

}
