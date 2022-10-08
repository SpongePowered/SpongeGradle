/*
 * This file is part of spongegradle-repository, licensed under the MIT License (MIT).
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
package org.spongepowered.gradle.repository;

import net.kyori.mammoth.test.TestContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.spongepowered.gradle.build.FunctionalTestDisplayNameGenerator;
import org.spongepowered.gradle.build.SpongeGradleFunctionalTest;

import java.io.IOException;

// These all essentially just test evaluation of the buildscript

@DisplayNameGeneration(FunctionalTestDisplayNameGenerator.class)
public class RepositoriesFunctionalTest {

    @SpongeGradleFunctionalTest
    void testProject(final TestContext ctx) throws IOException {
        ctx.copyInput("build.gradle");
        ctx.copyInput("settings.gradle");

        ctx.build("help");
    }

    @SpongeGradleFunctionalTest
    void testSettings(final TestContext ctx) throws IOException {
        ctx.copyInput("settings.gradle");

        ctx.build("help");
    }

    @SpongeGradleFunctionalTest
    void testKotlinProject(final TestContext ctx) throws IOException {
        ctx.copyInput("build.gradle.kts");
        ctx.copyInput("settings.gradle.kts");

        ctx.build("help");
    }

    @SpongeGradleFunctionalTest
    void testKotlinSettings(final TestContext ctx) throws IOException {
        ctx.copyInput("settings.gradle.kts");

        ctx.build("help");
    }

}
