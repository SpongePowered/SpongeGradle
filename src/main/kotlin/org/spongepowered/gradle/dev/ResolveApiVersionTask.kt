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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import java.io.ByteArrayOutputStream
import java.io.IOException

open class ResolveApiVersionTask : DefaultTask() {

    init {
        group = "sponge"
    }

    @TaskAction
    fun resolveApiVersion() {
        try {
            val byteOut = ByteArrayOutputStream()
            val apiVersionResult = project.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
                standardOutput = byteOut
            }
            val output = String(byteOut.toByteArray())

            val result = apiVersionResult.exitValue
            if (result == 0) {
                project.extra.set("apiVersion", output.trim())
            } else {
                logger.warn("Failed to resolve API revision (Processed returned error code $result")
            }
        } catch (e: IOException) {
            logger.warn("Failed to resolve API revision: $e")
        }
    }
}
