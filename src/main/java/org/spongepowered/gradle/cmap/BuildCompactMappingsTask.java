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
package org.spongepowered.gradle.cmap;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public class BuildCompactMappingsTask extends DefaultTask {

    private Object minecraftJar;
    private Object srg;
    private Object output;

    @InputFile
    public File getMinecraftJar() {
        return getProject().file(this.minecraftJar);
    }

    public void setMinecraftJar(Object minecraftJar) {
        this.minecraftJar = minecraftJar;
    }

    @InputFile
    public File getSrg() {
        return getProject().file(this.srg);
    }

    public void setSrg(Object srg) {
        this.srg = srg;
    }

    @OutputFile
    public File getOutput() {
        return getProject().file(this.output);
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    @TaskAction
    public void buildMappings() throws IOException {
        // Read original SRG mappings
        SrgReader reader = new SrgReader();
        reader.read(getSrg().toPath());

        // Process mappings
        VanillaCompactMappingProcessor processor = new VanillaCompactMappingProcessor(reader);
        processor.prepare(getMinecraftJar().toPath());
        processor.process();

        // Write mappings
        CompactMappingsWriter.write(getOutput().toPath(), processor.getMappings());
    }

}
