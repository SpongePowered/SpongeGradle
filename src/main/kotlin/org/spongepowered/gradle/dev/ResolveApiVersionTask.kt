package org.spongepowered.gradle.dev

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import java.io.ByteArrayOutputStream
import java.io.IOException

open class ResolveApiVersionTask: DefaultTask() {

    init {
        group = "sponge"
    }

    @TaskAction
    fun resolveApiVersion() {
        try {
            val byteOut = ByteArrayOutputStream()
            val apiVersionResult = project.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
                standardOutput = byteOut

            }
            val output = String(byteOut.toByteArray())

            val result = apiVersionResult.exitValue
            if (result == 0) {
                project.extra.set("apiVersion", output.trim())
            } else {
                logger.warn("Failed to resolve API revision (Processed returned error code $result")
            }
        } catch (e: IOException) {
            logger.warn("Failed to resolve API revision: $e")
        }
    }
}