package com.hyeonslab.tailwind.tasks

import com.hyeonslab.tailwind.TailwindPlugin
import com.hyeonslab.tailwind.platform.TailwindPlatform
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import javax.inject.Inject
import java.nio.file.Path

abstract class BaseTailwindTask @Inject constructor(
    @get:Internal protected val execOperations: ExecOperations
) : DefaultTask() {
    init {
        group = TailwindPlugin.PLUGIN_GROUP
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

    @Input
    protected fun getBinary(): String {
        val file = cacheDir.map { dir ->
            dir.resolve(version.get())
                .resolve(formattedName)
                .toFile()
        }
        return file.get().absolutePath
    }

    protected fun validateVersion() {
        if (!version.isPresent) {
            throw GradleException("Tailwind version is not configured. Please set 'tailwind.version' in your build.gradle.kts")
        }

        val versionStr = version.get()
        if (versionStr.isBlank()) {
            throw GradleException("Tailwind version cannot be empty")
        }

        // Validate version format (supports MAJOR.MINOR.PATCH, pre-release, and SNAPSHOT versions)
        // Examples: 4.1.0, 4.1.0-beta.1, 4.1.0-SNAPSHOT, 4.1.0-alpha.1-SNAPSHOT
        if (!versionStr.matches(Regex("\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.]+-?SNAPSHOT|-[a-zA-Z0-9.]+)?"))) {
            throw GradleException(
                "Invalid Tailwind version format: '$versionStr'\n" +
                "Version must be in format: MAJOR.MINOR.PATCH (e.g., '4.1.0', '4.1.0-beta.1', or '4.1.0-SNAPSHOT')"
            )
        }
    }

    protected fun validateCacheDir() {
        if (!cacheDir.isPresent) {
            throw GradleException("Cache directory is not configured")
        }
    }
}
