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

import groovy.json.JsonOutput
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
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskAction

import javax.net.ssl.SSLHandshakeException

class OreDeployTask extends DefaultTask {

    String instanceUrl = 'https://ore.spongepowered.org'
    Boolean recommended
    NamedDomainObjectContainer<String> tags
    String apiKey
    Boolean forumPost
    String changelog

    Configuration deploy = project.configurations.archives

    OreDeployTask(Project project) {
        this.tags = project.container(String)
    }

    @TaskAction
    void run() {
        Logger logger = project.logger
        def artifacts = deploy.allArtifacts
        PublishArtifact plugin = artifacts.find { a -> (a.type == "jar") }
        if (plugin == null) {
            throw new InvalidUserDataException("Plugin file not found.")
        }

        logger.quiet('Publishing ' + plugin.name + ' to ' + instanceUrl)
        logger.quiet('  Recommended: ' + recommended)
        logger.quiet('  Tags: ' + tags)
        logger.quiet('  Forum post: ' + forumPost)
        logger.quiet('  Changelog: "' + changelog ?: "" + '"')

        if (instanceUrl.endsWith("/")) {
            instanceUrl = instanceUrl.substring(0, instanceUrl.length() - 1)
        }
        URL projectUrl
        URL authUrl
        try {
            String pluginId = project.sponge.plugin.id
            String str = instanceUrl + '/api/v2/projects/' + pluginId + '/versions'
            projectUrl = new URL(str)
            authUrl = new URL(instanceUrl + '/api/v2/authenticate')
            logger.debug('POST ' + str)
        } catch (MalformedURLException e) {
            throw new InvalidUserDataException("Invalid project URL", e)
        }

        if (apiKey == null) {
            apiKey = project.property('oreDeploy.apiKey')
        }

        HttpClient http = HttpClients.createDefault()
        try {
            HttpPost authRequest = new HttpPost(authUrl.toURI())
            authRequest.addHeader('Authorization', 'ApiKey ' + apiKey)

            HttpResponse authResponse = null
            String session
            try {
                authResponse = http.execute(authRequest)

                //We could probably cache this in the file system somewhere, but don't want to deal with the security around that

                //TODO: Only get a session once per gradle run
                def result = new JsonSlurper().parse(authResponse.entity.content, 'UTF-8')
                if(result.error) {
                    throw new GradleException('Failed to authenticate with Ore: ' + result.error)
                }

                session = result.session

                EntityUtils.consume(authResponse.entity)

            } catch (InterruptedIOException | SocketException e) {
                throw new GradleException('Failed to connect to Ore.', e)
            } catch (SSLHandshakeException ignored) {
                throw new GradleException(
                        'Please update to Java version 1.8.0_121+ in order to connect to Sponge securely.')
            } catch (IOException e) {
                throw new IOException('An unexpected error occurred.', e)
            } finally {
                if (authResponse != null) {
                    authResponse.close()
                }
            }

            def deployInfo = JsonOutput.toJson([
                    tags: tags.asMap,
                    recommended: recommended,
                    create_forum_post: forumPost,
                    description: changelog
            ])

            HttpEntity deployRequestEntity = MultipartEntityBuilder.create()
                    .addPart('plugin-info', new StringBody(deployInfo))
                    .addPart('plugin-file', new FileBody(plugin.file))
                    .build()

            logger.debug(deployRequestEntity.toString())

            HttpPost deployRequest = new HttpPost(projectUrl.toURI())
            deployRequest.entity = deployRequestEntity
            deployRequest.addHeader('Authorization', 'ApiSession ' + session)
            HttpResponse deployResponse = null
            try {
                deployResponse = http.execute(deployRequest)
                def json = new JsonSlurper().parse(deployResponse.entity.content, 'UTF-8')
                def status = deployResponse.statusLine
                boolean created = status.statusCode == HttpURLConnection.HTTP_CREATED
                if (!created) {
                    logger.error('[failure] ' + status.statusCode + ' ' + status.reasonPhrase)
                    printErrorStream json.user_error
                } else {
                    logger.debug(json.toString())
                    logger.quiet("[success] $instanceUrl/${json.namespace.owner}/${json.namespace.slug}/${json.name}")
                }
                EntityUtils.consume(deployResponse.entity)
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
                if (deployResponse != null) {
                    deployResponse.close()
                }
            }

        } finally {
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
