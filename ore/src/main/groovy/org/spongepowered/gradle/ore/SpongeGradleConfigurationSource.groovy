/*
 * This file is part of spongegradle-ore, licensed under the MIT License (MIT).
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
package org.spongepowered.gradle.ore

import groovy.transform.PackageScope
import org.gradle.api.Project
import org.spongepowered.gradle.ore.internal.OrePublicationImpl

@PackageScope
class SpongeGradleConfigurationSource {

    private SpongeGradleConfigurationSource() {
    }

    static def configureSpongeGradle(Project project, OreDeploymentExtension extension) {
        project.plugins.withId('org.spongepowered.gradle.plugin') {
            def first = true
            def jar = project.tasks.named('jar').flatMap { it.archiveFile }
            extension.defaultPublication {}
            def defPub = extension.publications().named(OrePublicationImpl.DEFAULT_NAME)
            project.sponge.plugins.whenObjectAdded { plugin ->
                if (first) {
                    first = false
                    defPub.configure {
                        projectId.set(plugin.name)
                        publishArtifacts.from(jar)
                    }
                }
            }
        }
    }
}
