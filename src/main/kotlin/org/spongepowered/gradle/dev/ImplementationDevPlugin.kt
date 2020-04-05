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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.spongepowered.gradle.util.Constants
import javax.inject.Inject


open class ImplementationDevPlugin : CommonImplementationDevPlugin() {
    override fun apply(project: Project) {
        val common = project.project(":SpongeCommon")
        val api = common.project("SpongeAPI")
        val mcVersion = common.property("minecraftVersion")!! as String
        val addedSourceSets = project.container(AddedSourceSet::class);

        val impl = project.extensions.create(Constants.SPONGE_DEV_EXTENSION, SpongeImpl::class.java, project, addedSourceSets, common, api)
        // This is basically to ensure that common can be configured with the appropriate
        // conventions before we continue adding more.
        super.apply(project)


        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.apply {
            val mainImpl = getByName("main")
            findByName("java6")?.apply {
                compileClasspath = project.files(mainImpl.compileClasspath, mainImpl.output)
            }
        }
        project.dependencies.apply {
            add("implementation", common)
        }
        project.repositories {
            maven("https://files.minecraftforge.net/maven")
        }


        common.afterEvaluate {
            impl.parent.dependencies.apply {
                add("implementation", common)
                add("runtime", "net.minecraftforge:forgeflower:1.5.380.23")
                impl.extraDeps.forEach {
                    add("implementation", it)
                }
            }
        }

        project.configurations.getByName("compile") {
            exclude(mapOf("module" to "asm"))
            exclude(mapOf("module" to "asm-commons"))
            exclude(mapOf("module" to "asm-tree"))
        }

        // TODO - create nested dependency of metas.
        project.plugins.apply("com.github.johnrengelman.shadow")


        project.tasks.apply {
            getting(Jar::class) {
                classifier = "base"
            }
            // TODO - figure out ForgeGradle 3 reobf stuff

        }
        project.tasks.apply {
            getting(ShadowJar::class) {
                project.artifacts.add("archives", this)
            }
        }

    }
}

open class SpongeImpl(val parent: Project, addedSourceSets: NamedDomainObjectContainer<AddedSourceSet>, common: Project, api: Project) : CommonDevExtension(addedSourceSets, common, api) {

    val extraDeps: MutableList<SourceSetOutput> = mutableListOf()

}
