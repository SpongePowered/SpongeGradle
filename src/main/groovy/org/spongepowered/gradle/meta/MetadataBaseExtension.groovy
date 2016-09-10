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
package org.spongepowered.gradle.meta

import groovy.transform.ToString
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.spongepowered.gradle.SpongeGradle
import org.spongepowered.plugin.meta.PluginMetadata

import java.util.function.Consumer

@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class MetadataBaseExtension {

    static final String EXTENSION_NAME = 'sponge'

    final Project project
    final NamedDomainObjectContainer<Plugin> plugins

    MetadataBaseExtension(Project project) {
        this.project = project
        this.plugins = project.container(Plugin, { name ->
            return new Plugin(this, name)
        })
    }

    void plugins(Closure closure) {
        project.configure(plugins, closure)
    }

    @ToString(includePackage = false, includeNames = true, ignoreNulls = true, excludes = 'extension')
    static class Plugin {

        final MetadataBaseExtension extension
        final String name
        Object id
        final Meta meta = new Meta()

        Plugin(MetadataBaseExtension extension, String name) {
            this.extension = extension
            this.name = name
            this.id = name.toLowerCase()
        }

        String getName() {
            return this.name
        }

        String getId() {
            return SpongeGradle.resolve(this.id)
        }

        void meta(@DelegatesTo(Meta) Closure<?> closure) {
            extension.project.configure(meta, closure)
        }

        @ToString(includePackage = false, includeNames = true, ignoreNulls = true)
        static class Meta implements Consumer<PluginMetadata> {

            Object name
            Object version
            Object description
            Object url
            Object minecraftVersion

            List<String> authors = []

            String getName() {
                return SpongeGradle.resolve(this.name)
            }

            String getVersion() {
                return SpongeGradle.resolve(this.version)
            }

            String getDescription() {
                return SpongeGradle.resolve(this.description)
            }

            String getUrl() {
                return SpongeGradle.resolve(this.url)
            }

            String getMinecraftVersion() {
                return SpongeGradle.resolve(this.minecraftVersion)
            }

            @Override
            void accept(PluginMetadata meta) {
                meta.name = getName()
                meta.version = getVersion()
                meta.description = getDescription()
                meta.url = getUrl()
                meta.minecraftVersion = getMinecraftVersion()
                meta.authors.addAll(this.authors)
            }

        }

    }

}
