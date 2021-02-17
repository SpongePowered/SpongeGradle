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
package org.spongepowered.gradle.convention;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import javax.inject.Inject;

public abstract class SpongeImplementationConventionPlugin implements Plugin<Project> {

    @Inject
    protected abstract ObjectFactory getObjects();

    @Override
    public void apply(final Project target) {
        target.getPlugins().apply(SpongeConventionPlugin.class);

        final NamedDomainObjectProvider<Configuration> parentProject = this.registerParentProjectConfiguration(target.getConfigurations());

        target.getPlugins().withType(JavaPlugin.class, $ -> this.registerSourceSets(
            target.getExtensions().getByType(SourceSetContainer.class),
            target.getConfigurations(),
            target.getDependencies(),
            parentProject
        ));

        // installer (isolated)
        // - can see: nothing
        // - on runtime classpath (for shading): all other source sets, parent project, and
        // - can be seen by: nothing

        /*
        sourceSets.configureEach {
         val sourceSet = this
         tasks.register(this.sourcesJarTaskName, Jar::class.java) {
         group = "build"
         val classifier = if ("main".equals(sourceSet.name)) "sources" else "${sourceSet.name}sources"
         archiveClassifier.set(classifier)
         from(sourceSet.allJava)
         }
         }
         */
    }

    private NamedDomainObjectProvider<Configuration> registerParentProjectConfiguration(final ConfigurationContainer configurations) {
        // A configuration used for declaration only -- the individual source set configurations will extend from this as appropriate.
        return configurations.register("parentProject", config -> {
            config.setCanBeResolved(false);
            config.setCanBeConsumed(false);
        });
    }

    private void registerSourceSets(
        final SourceSetContainer sourceSets,
        final ConfigurationContainer configurations,
        final DependencyHandler dependencies,
        final NamedDomainObjectProvider<Configuration> parentProject
    ) {
        dependencies.getAttributesSchema().attribute(ImplementationFacet.FACET_ATTRIBUTE);
        // applaunch
        // can see: libs (- MC)
        sourceSets.register("applaunch", this.configureSourceSetForFacet(ImplementationFacet.APPLAUNCH, configurations, parentProject));
        // launch
        // can see: accessor, main, main dependencies
        sourceSets.register("launch", this.configureSourceSetForFacet(ImplementationFacet.LAUNCH, configurations, parentProject));
        // mixins
        // can see: minecraft, `main` dependencies
        sourceSets.register("mixins", this.configureSourceSetForFacet(ImplementationFacet.MIXINS, configurations, parentProject));
        // accessor
        // can see: minecraft, main dependencies
        sourceSets.register("accessors", this.configureSourceSetForFacet(ImplementationFacet.ACCESSORS, configurations, parentProject));
        // Configure source sets:
        // main
        // can see: applaunch, launch, accessor
        // sourceSets.register("installer"); -- probably separate this?
    }

    private Action<SourceSet> configureSourceSetForFacet(final String facetName, final ConfigurationContainer configurations, final NamedDomainObjectProvider<Configuration> parentProject) {
        return set -> {
            // Bring in the parent project
            configurations.named(set.getImplementationConfigurationName(), conf -> conf.extendsFrom(parentProject.get()));

            // set attributes on runtime + compile-time classpaths (so we resolve the correct dependencies)
            configurations.named(set.getRuntimeClasspathConfigurationName(), conf -> conf.getAttributes().attribute(ImplementationFacet.FACET_ATTRIBUTE,
                this.getObjects().named(ImplementationFacet.class, facetName)));
            configurations.named(set.getCompileClasspathConfigurationName(), conf -> conf.getAttributes().attribute(ImplementationFacet.FACET_ATTRIBUTE,
                this.getObjects().named(ImplementationFacet.class, facetName)));
            // set attributes on outgoing of api + runtime elements (so consumers know who to find)
            configurations.named(set.getRuntimeElementsConfigurationName(), conf -> conf.getOutgoing().getAttributes().attribute(ImplementationFacet.FACET_ATTRIBUTE,
                this.getObjects().named(ImplementationFacet.class, facetName)));
            configurations.named(set.getApiElementsConfigurationName(), conf -> conf.getOutgoing().getAttributes().attribute(ImplementationFacet.FACET_ATTRIBUTE,
                this.getObjects().named(ImplementationFacet.class, facetName)));
        };
    }

    /*

fun generateImplementationVersionString(apiVersion: String, minecraftVersion: String, implRecommendedVersion: String, addedVersionInfo: String? = null): String {
    val apiSplit = apiVersion.replace("-SNAPSHOT", "").split(".")
    val minor = if (apiSplit.size > 1) apiSplit[1] else (if (apiSplit.size > 0) apiSplit.last() else "-1")
    val apiReleaseVersion = "${apiSplit[0]}.$minor"
    return listOfNotNull(minecraftVersion, addedVersionInfo, "$apiReleaseVersion.$implRecommendedVersion").joinToString("-")
}
fun generatePlatformBuildVersionString(apiVersion: String, minecraftVersion: String, implRecommendedVersion: String, addedVersionInfo: String? = null): String {
    val isRelease = !implRecommendedVersion.endsWith("-SNAPSHOT")
    println("Detected Implementation Version $implRecommendedVersion as ${if (isRelease) "Release" else "Snapshot"}")
    val apiSplit = apiVersion.replace("-SNAPSHOT", "").split(".")
    val minor = if (apiSplit.size > 1) apiSplit[1] else (if (apiSplit.size > 0) apiSplit.last() else "-1")
    val apiReleaseVersion = "${apiSplit[0]}.$minor"
    val buildNumber = Integer.parseInt(System.getenv("BUILD_NUMBER") ?: "0")
    val implVersionAsReleaseCandidateOrRecommended: String = if (isRelease) {
        "$apiReleaseVersion.$implRecommendedVersion"
    } else {
        "$apiReleaseVersion.${implRecommendedVersion.replace("-SNAPSHOT", "")}-RC$buildNumber"
    }
    return listOfNotNull(minecraftVersion, addedVersionInfo, implVersionAsReleaseCandidateOrRecommended).joinToString("-")
}
     */
}
