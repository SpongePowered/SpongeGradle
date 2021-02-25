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
package org.spongepowered.gradle.common;

public final class Constants {

    public static final String NAME = "SpongeGradle";
    public static final String VERSION = Constants.version();
    public static final String TASK_GROUP = "sponge gradle";
    public static final String GITHUB_ORGANIZATION = "SpongePowered";

    public static final class Repositories {
        public static final String SPONGE = "https://repo.spongepowered.org/repository/maven-public/";

        private Repositories() {
        }
    }

    public static final class Dependencies {
        public static final String SPONGE_GROUP = "org.spongepowered";
        public static final String SPONGE_API = "spongeapi";

        private Dependencies() {
        }
    }

    private Constants() {
    }

    private static String version() {
        final String rawVersion = Constants.class.getPackage().getImplementationVersion();
        if (rawVersion == null) {
            return "dev";
        } else {
            return rawVersion;
        }
    }
}
