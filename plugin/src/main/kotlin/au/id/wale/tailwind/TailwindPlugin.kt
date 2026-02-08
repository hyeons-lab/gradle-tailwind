/**
 *    Copyright 2023-2026 Duale Siad
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package au.id.wale.tailwind

import au.id.wale.tailwind.tasks.TailwindCompileTask
import au.id.wale.tailwind.tasks.TailwindDownloadTask
import au.id.wale.tailwind.tasks.TailwindInitTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class TailwindPlugin : Plugin<Project> {

    companion object {
        const val PLUGIN_GROUP = "Tailwind"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create("tailwind", TailwindExtension::class.java)

        // Use Kotlin nullable types instead of Java Optional
        val cacheDirectory: Path = (project.findProperty("au.id.wale.tailwind.cache.dir") as? String)
            ?.let { Paths.get(it) }
            ?: project.gradle
                .gradleUserHomeDir
                .toPath()
                .resolve("caches")
                .resolve("au.id.wale.tailwind")

        if (!cacheDirectory.exists()) {
            cacheDirectory.createDirectories()
        }

        // Validate cache directory is writable
        if (!cacheDirectory.toFile().canWrite()) {
            throw org.gradle.api.GradleException(
                "Cache directory is not writable: ${cacheDirectory.toAbsolutePath()}\n" +
                "Please check file permissions or specify a different cache directory using:\n" +
                "gradle.properties: au.id.wale.tailwind.cache.dir=/path/to/writable/dir"
            )
        }

        val downloadTask = project.tasks.register("tailwindDownload", TailwindDownloadTask::class.java) {
            it.cacheDir.set(cacheDirectory)
            it.version.set(extension.version)
        }

        project.tasks.register("tailwindInit", TailwindInitTask::class.java) {
            it.configPath.set(extension.configPath)
            it.version.set(extension.version)
            it.cacheDir.set(cacheDirectory)
            it.dependsOn(downloadTask)
        }

        project.tasks.register("tailwindCompile", TailwindCompileTask::class.java) {
            it.configPath.set(extension.configPath)
            it.version.set(extension.version)
            it.input.set(extension.input)
            it.output.set(extension.output)
            it.cacheDir.set(cacheDirectory)
            it.dependsOn(downloadTask)
        }
    }
}
