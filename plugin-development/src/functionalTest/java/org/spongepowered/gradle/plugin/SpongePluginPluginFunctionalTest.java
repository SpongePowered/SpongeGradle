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
import net.kyori.mammoth.test.TestContext;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.gradle.testkit.runner.UnexpectedBuildSuccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.spongepowered.gradle.build.SpongeGradleFunctionalTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;

/**
 * Simple functional tests for the Sponge plugin development plugin.
 *
 * <p>To capture errors from builds, it can be helpful to register exception
 * breakpoints on {@link UnexpectedBuildFailure} and {@link UnexpectedBuildSuccess}.
 * This allows viewing build reports before the temporary directories are cleared.</p>
 */
class SpongePluginPluginFunctionalTest {

    @DisplayName("simplebuild")
    @SpongeGradleFunctionalTest
    void testSimpleBuild(final TestContext ctx) throws IOException {
        ctx.copyInput("build.gradle.kts");
        ctx.copyInput("settings.gradle.kts");

        final BuildResult result = ctx.build("writePluginMetadata");

        Assertions.assertTrue(result.getOutput().contains("SpongePowered Plugin"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":writePluginMetadata").getOutcome());

        // Then make sure we actually generated a plugins file
        ctx.assertOutputEquals("sponge_plugins.json", "build/generated/sponge/plugin/META-INF/sponge_plugins.json");
    }

    @DisplayName("missingproperties")
    @SpongeGradleFunctionalTest
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
        assertTrue(result.getOutput().contains(".license"));
        assertTrue(result.getOutput().contains(".loader"));
        assertTrue(result.getOutput().contains(".entrypoint"));
    }

    @DisplayName("propertiesinferred")
    @SpongeGradleFunctionalTest
    void testPropertiesInferredFromProjectConfiguration(final TestContext ctx) throws IOException {
        ctx.copyInput("build.gradle.kts");
        ctx.copyInput("settings.gradle.kts");

        final BuildResult result = ctx.build("writePluginMetadata");
        assertEquals(TaskOutcome.SUCCESS, result.task(":writePluginMetadata").getOutcome());

        final JsonObject plugins = new Gson().fromJson(ctx.readOutput("build/generated/sponge/plugin/META-INF/sponge_plugins.json"), JsonObject.class);
        final JsonObject plugin = plugins.getAsJsonArray("plugins").get(0).getAsJsonObject();

        // Compare properties drawn from build
        assertEquals("1234", plugin.getAsJsonPrimitive("version").getAsString());
        assertEquals(
            "An example of properties coming from build configuration",
            plugin.getAsJsonPrimitive("description").getAsString()
        );
    }

    @DisplayName("complexbuild")
    @SpongeGradleFunctionalTest
    void testComplexBuild(final TestContext ctx) throws IOException {
        ctx.copyInput("build.gradle.kts");
        ctx.copyInput("settings.gradle.kts");
        ctx.copyInput("Example.java", "src/main/java/org/spongepowered/example/Example.java");

        final BuildResult result = ctx.build("build");
        assertEquals(TaskOutcome.SUCCESS, result.task(":writePluginMetadata").getOutcome());

        // Test that plugin metadata was included in the jar
        final Path jar = ctx.outputDirectory().resolve("build/libs/complexbuild-1.0-SNAPSHOT.jar");

        try (final JarFile jf = new JarFile(jar.toFile())) {
            assertNotNull(jf.getEntry("META-INF/sponge_plugins.json"));
        }
    }
}
