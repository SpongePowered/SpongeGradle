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
package org.spongepowered.gradle.ore

import groovy.json.JsonSlurper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.plugins.signing.Signature
import org.spongepowered.gradle.meta.MetadataExtension
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketException
import java.net.URL
import javax.net.ssl.SSLHandshakeException

open class OreDeployTask : DefaultTask() {

    var instanceUrl = "https://ore.spongepowered.org"
    var recommended = true
    var channel: String? = null
    var apiKey: String? = null
    var forumPost: Boolean? = null
    var changelog: String? = null

    var deploy: Configuration = project.configurations["archives"]

    @TaskAction
    fun run() {
        val logger = project.logger
        val artifacts = deploy.allArtifacts
        val plugin = artifacts.find { a -> (a.type == "jar") }
        val sig = artifacts.find { a -> (a is Signature) }
        if (plugin == null) {
            throw InvalidUserDataException("Plugin file not found.")
        }
        if (sig == null) {
            throw InvalidUserDataException("Signature file not found.")
        }

        logger.quiet("Publishing " + plugin.name + " to " + instanceUrl + ".")
        logger.quiet("  Recommended: " + recommended)
        logger.quiet("  Channel: " + channel)

        if (instanceUrl.endsWith("/")) {
            instanceUrl = instanceUrl.substring(0, instanceUrl.length - 1)
        }
        val projectUrl: URL
        try {

            val pluginId = project.extensions.getByType(MetadataExtension::class.java).plugin.id
            val str = instanceUrl + "/api/projects/" + pluginId + "/versions/" + project.version
            projectUrl = URL(str)
            logger.debug("POST " + str)
        } catch (e: MalformedURLException) {
            throw InvalidUserDataException("Invalid project URL", e)
        }

        if (apiKey == null) {
            apiKey = project.property("oreDeploy.apiKey") as String?
        }
        if (channel == null) {
            throw InvalidUserDataException("Missing channel name.")
        }
        val entityBuilder = MultipartEntityBuilder.create()
            .addTextBody("apiKey", apiKey)
            .addTextBody("channel", channel)
            .addTextBody("recommended", recommended.toString())
            .addBinaryBody("pluginFile", plugin.file)
            .addBinaryBody("pluginSig", sig.file)

        if (forumPost != null) {
            entityBuilder.addTextBody("forumPost", forumPost.toString())
        }

        if (changelog != null) {
            entityBuilder.addTextBody("changelog", changelog)
        }

        val requestEntity = entityBuilder.build()
        logger.debug(requestEntity.toString())

        val http = HttpClients.createDefault()
        val post = HttpPost(projectUrl.toURI())
        post.entity = requestEntity
        var response: CloseableHttpResponse? = null
        try {
            response = http.execute(post)
            val responseEntity = response.entity
            val status = response.statusLine
            val created: Boolean = status.statusCode == HttpURLConnection.HTTP_CREATED
            if (!created) {
                logger.error("[failure] " + status.statusCode + " " + status.reasonPhrase)
                printErrorStream(responseEntity.content)
            } else {
                val json = JsonSlurper().parse(responseEntity.content, "UTF-8")
                logger.debug(json.toString())
                logger.quiet("[success] " + instanceUrl + ((json as Map<*, *>)["href"] as String))
            }
            EntityUtils.consume(responseEntity)
            when {
                !created -> throw GradleException("Deployment failed")
            }
        } catch (e: Exception) {
            when (e) {
                is InterruptedIOException,
                is SocketException -> {
                    throw GradleException("Failed to connect to Ore.", e)
                }
                is SSLHandshakeException -> {
                    throw GradleException("Please update to Java version 1.8.0_121+ in order to connect to Sponge securely.")
                }
                is IOException -> {
                    throw IOException("An unexpected error occurred", e)
                }
            }
        } finally {
            if (response != null) {
                response.close()
            }
            http.close()
        }
    }

    fun printErrorStream(stream: InputStream) {
        val json = JsonSlurper().parse(stream, "UTF-8")
        logger.debug(json.toString())
        if ((json as Map<String, *>)["errors"] != null) {
            (json["errors"] as Map<String, *>).entries.forEach {
                project.logger.error("* " + it.key)
                (it.value as Iterable<String>).forEach {
                    project.logger.error("  - " + it)
                }
            }
        }
    }
}
