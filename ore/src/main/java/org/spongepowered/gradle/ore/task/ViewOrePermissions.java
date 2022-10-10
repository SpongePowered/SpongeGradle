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
package org.spongepowered.gradle.ore.task;

import org.gradle.api.tasks.TaskAction;
import org.spongepowered.gradle.ore.internal.OreSession;
import org.spongepowered.gradle.ore.internal.model.KeyPermissions;

import java.util.concurrent.CompletableFuture;

public abstract class ViewOrePermissions extends OreTask {

    @TaskAction
    void printOrePermissions() {
        final CompletableFuture<OreSession> session = this.session();

        final KeyPermissions perms = this.responseOrThrow(session.thenCompose(OreSession::globalPermissions));

        this.getLogger().lifecycle("Your permissions (in scope {}) are:", perms.type());
        for (final String permission : perms.permissions()) {
            this.getLogger().lifecycle("  - {}", permission);
        }
    }

}
