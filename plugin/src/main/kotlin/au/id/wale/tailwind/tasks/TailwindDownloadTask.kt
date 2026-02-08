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

package au.id.wale.tailwind.tasks

import au.id.wale.tailwind.TailwindPlugin
import au.id.wale.tailwind.platform.TailwindOS
import au.id.wale.tailwind.platform.TailwindPlatform
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest


abstract class TailwindDownloadTask : DefaultTask() {

    init {
        description = "Downloads and caches a given TailwindCSS binary."
        group = TailwindPlugin.PLUGIN_GROUP

        onlyIf {
            // Check version is present before calling getBinary which requires version
            version.isPresent && cacheDir.isPresent && !getBinary().get().asFile.exists()
        }
    }

    @get:Input
    @get:Option(option = "version", description = "The current Tailwind version to download.")
    val version: Property<String> = project.objects.property(String::class.java)

    @get:InputDirectory
    @get:Option(option = "cacheDir", description = "The directory where the Tailwind binaries are cached")
    val cacheDir: Property<Path> = project.objects.property(Path::class.java)

    // Lazy evaluation to avoid platform detection during configuration phase
    private val formattedName: String
        get() = TailwindPlatform().format()

    private val BASE_URL = "https://github.com/tailwindlabs/tailwindcss/releases/download"

    @TaskAction
    fun downloadTailwind() {
        // Validate required properties
        validateVersion()
        validateCacheDir()

        Files.createDirectories(getBinary().get().asFile.toPath().parent)

        val binaryUrl = "${BASE_URL}/v${version.get()}/${formattedName}"
        val checksumUrl = "${BASE_URL}/v${version.get()}/sha256sums.txt"
        val binaryPath = getBinary().get().asFile.toPath()

        // Download the checksums file with retry logic
        val checksums = downloadWithRetry(checksumUrl, "checksums file") { url ->
            URI.create(url).toURL().openStream().use { stream ->
                stream.bufferedReader().readText()
            }
        }

        // Parse checksums to find the expected checksum for our binary
        val expectedChecksum = parseChecksum(checksums, formattedName)
            ?: throw GradleException("Checksum not found for $formattedName in sha256sums.txt")

        // Download the binary with retry logic
        downloadWithRetry(binaryUrl, "Tailwind binary") { url ->
            URI.create(url).toURL().openStream().use { `in` ->
                Files.copy(
                    `in`,
                    binaryPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        // Verify the checksum
        val actualChecksum = calculateSHA256(binaryPath)
        if (actualChecksum != expectedChecksum) {
            Files.deleteIfExists(binaryPath)
            throw GradleException(
                "Checksum verification failed for $formattedName!\n" +
                "Expected: $expectedChecksum\n" +
                "Actual:   $actualChecksum\n" +
                "The downloaded file may be corrupted or tampered with."
            )
        }

        logger.info("Checksum verification passed for $formattedName")

        // Set the downloaded Tailwind executable to be `true`.
        if ((TailwindPlatform.platformOS == TailwindOS.LINUX) || (TailwindPlatform.platformOS == TailwindOS.MAC)) {
            getBinary().get().asFile.setExecutable(true)
        }
    }

    private fun <T> downloadWithRetry(url: String, resourceName: String, downloadFn: (String) -> T): T {
        val maxRetries = 3
        val baseDelayMs = 1000L
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                logger.info("Downloading $resourceName (attempt ${attempt + 1}/$maxRetries)...")
                return downloadFn(url)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    // Exponential backoff: 1s, 2s, 4s
                    val delayMs = baseDelayMs * (1 shl attempt)
                    logger.warn("Download failed, retrying in ${delayMs}ms: ${e.message}")
                    // Note: Thread.sleep blocks the Gradle daemon, but acceptable for short delays
                    // Alternative would require WorkQueue/async handling which adds complexity
                    Thread.sleep(delayMs)
                }
            }
        }
        throw GradleException(
            "Failed to download $resourceName from $url after $maxRetries attempts.\n" +
            "Last error: ${lastException?.message}\n" +
            "Please check your internet connection and try again.",
            lastException
        )
    }

    private fun validateVersion() {
        if (!version.isPresent) {
            throw GradleException("Tailwind version is not configured. Please set 'tailwind.version' in your build.gradle.kts")
        }

        val versionStr = version.get()
        if (versionStr.isBlank()) {
            throw GradleException("Tailwind version cannot be empty")
        }

        // Validate version format (supports MAJOR.MINOR.PATCH, pre-release, and SNAPSHOT versions)
        // Examples: 4.1.0, 4.1.0-beta.1, 4.1.0-SNAPSHOT, 4.1.0-alpha.1-SNAPSHOT
        if (!versionStr.matches(Regex("\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.]+-?SNAPSHOT|[a-zA-Z0-9.]+)?"))) {
            throw GradleException(
                "Invalid Tailwind version format: '$versionStr'\n" +
                "Version must be in format: MAJOR.MINOR.PATCH (e.g., '4.1.0', '4.1.0-beta.1', or '4.1.0-SNAPSHOT')"
            )
        }
    }

    private fun validateCacheDir() {
        if (!cacheDir.isPresent) {
            throw GradleException("Cache directory is not configured")
        }
    }

    companion object {
        internal fun parseChecksum(checksums: String, filename: String): String? {
            // Format: <checksum>  ./<filename> or <checksum>  <filename>
            // Use exact matching to avoid partial filename matches (security issue)
            return checksums.lines()
                .firstOrNull { line ->
                    val trimmed = line.trim()
                    // Match exact filename with various formats:
                    // "checksum  ./filename", "checksum  filename", "checksum *filename"
                    trimmed.endsWith("  ./$filename") ||
                    trimmed.endsWith("  $filename") ||
                    trimmed.endsWith(" *$filename") ||
                    trimmed.matches(Regex("^[a-fA-F0-9]+\\s+\\*?\\./?" + Regex.escape(filename) + "$"))
                }
                ?.split("\\s+".toRegex())
                ?.firstOrNull()
        }

        internal fun calculateSHA256(path: Path): String {
            val digest = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(path).use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }


    @OutputFile
    fun getBinary(): Provider<RegularFile> {
        return project.layout.file(cacheDir.map { dir ->
            dir.resolve(version.get())
                .resolve(formattedName)
                .toFile()
        })
    }
}
