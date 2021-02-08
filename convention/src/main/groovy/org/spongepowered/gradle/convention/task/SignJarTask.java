/*
 * This file is part of spongegradle-convention, licensed under the MIT License (MIT).
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
package org.spongepowered.gradle.convention.task;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.jvm.tasks.Jar;
import org.spongepowered.gradle.convention.invoker.SignJarInvoker;

public abstract class SignJarTask extends Jar {
    @Input
    public abstract Property<String> getAlias();

    @Input
    public abstract Property<String> getStorePassword();

    @InputFile
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public abstract RegularFileProperty getKeyStore();

    @Input
    public abstract Property<Boolean> getStrict();

    public SignJarTask() {
        this.getStrict().convention(false);
    }

    @Override
    public void copy() {
        super.copy();
        if (this.getDidWork()) {
            SignJarInvoker.signJar(
                this.getAnt(),
                this.getArchiveFile().get().getAsFile(),
                this.getAlias().get(),
                this.getStorePassword().get(),
                this.getKeyStore().get().getAsFile(),
                this.getLogger().isInfoEnabled(),
                this.getStrict().get()
            );
        }
    }

}
