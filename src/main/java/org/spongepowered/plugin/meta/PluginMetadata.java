/*
 * This file is part of plugin-meta, licensed under the MIT License (MIT).
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

package org.spongepowered.plugin.meta;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public final class PluginMetadata {

    private String id;
    @Nullable private String name;
    @Nullable private String version;

    @Nullable private String description;
    @Nullable private String url;
    @Nullable private String assets;

    private final List<String> authors = new ArrayList<>();

    private final Set<Dependency> dependencies = new HashSet<>();
    private final Set<Dependency> loadBefore = new HashSet<>();
    private final Set<Dependency> loadAfter = new HashSet<>();

    private final Map<String, Object> extensions = new LinkedHashMap<>();

    public PluginMetadata(String id) {
        setId(id);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        checkNotNull(id, "id");
        checkArgument(!id.isEmpty(), "id cannot be empty");
        this.id = id;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public void setName(@Nullable String name) {
        this.name = emptyToNull(name);
    }

    @Nullable
    public String getVersion() {
        return this.version;
    }

    public void setVersion(@Nullable String version) {
        this.version = emptyToNull(version);
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public void setDescription(@Nullable String description) {
        this.description = emptyToNull(description);
    }

    @Nullable
    public String getUrl() {
        return this.url;
    }

    public void setUrl(@Nullable String url) {
        this.url = emptyToNull(url);
    }

    @Nullable
    public String getAssetDirectory() {
        return this.assets;
    }

    public void setAssetDirectory(String assets) {
        this.assets = emptyToNull(assets);
    }

    public List<String> getAuthors() {
        return this.authors;
    }

    public void addAuthor(String author) {
        checkNotNull(author, "author");
        if (author.isEmpty()) {
            return;
        }

        this.authors.add(author);
    }

    public Set<Dependency> getRequiredDependencies() {
        return this.loadAfter;
    }

    public Set<Dependency> getLoadBefore() {
        return this.loadBefore;
    }

    public Set<Dependency> getLoadAfter() {
        return this.loadAfter;
    }

    public void addRequiredDependency(Dependency dependency) {
        checkNotNull(dependency, "dependency");
        this.dependencies.add(dependency);
    }

    public void loadBefore(Dependency dependency) {
        checkNotNull(dependency, "dependency");
        this.loadBefore.add(dependency);
    }

    public void loadBefore(Dependency dependency, boolean required) {
        loadBefore(dependency);
        if (required) {
            addRequiredDependency(dependency);
        }
    }

    public void loadAfter(Dependency dependency) {
        checkNotNull(dependency, "dependency");
        this.loadAfter.add(dependency);
    }

    public void loadAfter(Dependency dependency, boolean required) {
        loadAfter(dependency);
        if (required) {
            addRequiredDependency(dependency);
        }
    }

    public Map<String, Object> getExtensions() {
        return this.extensions;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getExtension(String key) {
        return (T) this.extensions.get(key);
    }

    public void setExtension(String key, Object extension) {
       this.extensions.put(key, extension);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .omitNullValues()
                .add("id", this.id)
                .add("name", this.name)
                .add("version", this.version)
                .add("description", this.description)
                .add("url", this.url)
                .add("assets", this.assets)
                .add("authors", this.authors)
                .add("dependencies", this.dependencies)
                .add("loadBefore", this.loadBefore)
                .add("loadAfter", this.loadAfter)
                .add("extensions", this.extensions)
                .toString();
    }

    public static final class Dependency {

        private final String id;
        @Nullable private final String version;

        public Dependency(String id, String version) {
            this.id = checkNotNull(id, "id");
            checkArgument(!id.isEmpty(), "id cannot be empty");
            this.version = emptyToNull(version);
        }

        public String getId() {
            return this.id;
        }

        @Nullable
        public String getVersion() {
            return this.version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Dependency)) {
                return false;
            }

            Dependency that = (Dependency) o;
            return this.id.equals(that.id)
                    && Objects.equal(this.version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.id, this.version);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .omitNullValues()
                    .add("id", this.id)
                    .add("version", this.version)
                    .toString();
        }

    }


}
