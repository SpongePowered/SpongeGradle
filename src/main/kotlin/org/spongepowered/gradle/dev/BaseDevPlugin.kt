/*
 * This file is part of SpongeGradle, licensed under the MIT License (MIT).
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
package org.spongepowered.gradle.dev

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.plugins.ide.idea.IdeaPlugin

/**
 * Applies the java base plugins along with target compatibility and adds sponge
 * repositories for dependency resolution.
 */
open class BaseDevPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply {
            apply("java-library")
            apply("eclipse")
            apply("idea")
        }
        target.buildscript.repositories.apply {
            gradlePluginPortal()
            maven {
                name = "sponge"
                setUrl("https://repo.spongepowered.org/maven")
            }
            maven {
                name = "sponge v2"
                setUrl("https://repo-new.spongepowered.org/maven")
            }
            maven {
                name = "forge"
                setUrl("https://files.minecraftforge.net/maven")
            }
        }
        target.convention.getPlugin(JavaPluginConvention::class.java).apply {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        target.repositories.apply {
            mavenLocal()
            mavenCentral()
            maven {
                name = "sponge"
                setUrl("https://repo.spongepowered.org/maven")
            }
            maven {
                name = "sponge v2"
                setUrl("https://repo-new.spongepowered.org/maven")
            }
        }
        target.plugins.withType(IdeaPlugin::class.java) {
            model.module.inheritOutputDirs = true
        }
    }
}
