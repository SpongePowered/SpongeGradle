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
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskAction
import org.gradle.plugins.signing.Signature

import javax.net.ssl.SSLHandshakeException

class OreDeployTask extends DefaultTask {

    String instanceUrl = 'https://ore.spongepowered.org'
    boolean recommended = true
    String channel
    String apiKey
    Boolean forumPost
    String changelog

    Configuration deploy = project.configurations.archives

    @TaskAction
    void run() {
        Logger logger = project.logger
        def artifacts = deploy.allArtifacts
        PublishArtifact plugin = artifacts.find { a -> (a.type == "jar") }
        PublishArtifact sig = artifacts.find { a -> (a instanceof Signature) }
        if (plugin == null) {
            throw new InvalidUserDataException("Plugin file not found.")
        }
        if (sig == null) {
            throw new InvalidUserDataException("Signature file not found.")
        }

        logger.quiet('Publishing ' + plugin.name + ' to ' + instanceUrl + '.')
        logger.quiet('  Recommended: ' + recommended)
        logger.quiet('  Channel: ' + channel)

        if (instanceUrl.endsWith("/")) {
            instanceUrl = instanceUrl.substring(0, instanceUrl.length() - 1)
        }
        URL projectUrl
        try {
            String pluginId = project.sponge.plugin.id
            String str = instanceUrl + '/api/projects/' + pluginId + '/versions/' + project.version
            projectUrl = new URL(str)
            logger.debug('POST ' + str)
        } catch (MalformedURLException e) {
            throw new InvalidUserDataException("Invalid project URL", e)
        }

        if (apiKey == null) {
            apiKey = project.property('oreDeploy.apiKey')
        }
        if (channel == null) {
            throw new InvalidUserDataException("Missing channel name.")
        }
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
                .addPart('apiKey', new StringBody(apiKey))
                .addPart('channel', new StringBody(channel))
                .addPart('recommended', new StringBody(recommended as String))
                .addPart('pluginFile', new FileBody(plugin.file))
                .addPart('pluginSig', new FileBody(sig.file))

        if(forumPost != null) {
            entityBuilder.addPart('forumPost', new StringBody(forumPost as String))
        }

        if(changelog != null) {
            entityBuilder.addPart('changelog', new StringBody(changelog))
        }

        HttpEntity requestEntity = entityBuilder.build()
        logger.debug(requestEntity.toString())

        HttpClient http = HttpClients.createDefault()
        HttpPost post = new HttpPost(projectUrl.toURI())
        post.entity = requestEntity
        HttpResponse response = null
        try {
            response = http.execute(post)
            HttpEntity responseEntity = response.entity
            def status = response.statusLine
            boolean created = status.statusCode == HttpURLConnection.HTTP_CREATED
            if (!created) {
                logger.error('[failure] ' + status.statusCode + ' ' + status.reasonPhrase)
                printErrorStream responseEntity.content
            } else {
                def json = new JsonSlurper().parse(responseEntity.content, 'UTF-8')
                logger.debug(json.toString())
                logger.quiet('[success] ' + instanceUrl + (json.href as String))
            }
            EntityUtils.consume(responseEntity)
            if (!created) {
                throw new GradleException('Deployment failed.')
            }
        } catch (InterruptedIOException | SocketException e) {
            throw new GradleException('Failed to connect to Ore.', e)
        } catch (SSLHandshakeException ignored) {
            throw new GradleException(
                'Please update to Java version 1.8.0_121+ in order to connect to Sponge securely.')
        } catch (IOException e) {
            throw new IOException('An unexpected error occurred.', e)
        } finally {
            if (response != null) {
                response.close()
            }
            http.close()
        }
    }

    private void printErrorStream(InputStream is) {
        def json = new JsonSlurper().parse(is, 'UTF-8')
        logger.debug(json.toString())
        if (json.errors != null) {
            for (Map.Entry<String, ?> error : ((Map<String, ?>) json.errors).entrySet()) {
                project.logger.error("* " + error.key)
                for (String msg : error.value) {
                    project.logger.error("  - " + msg)
                }
            }
        }
    }

}
