package au.id.wale.tailwind.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import javax.inject.Inject
import java.io.File
import java.nio.file.Files
import java.time.Duration
import kotlin.io.path.Path

abstract class TailwindInitTask @Inject constructor(
    execOperations: ExecOperations
) : BaseTailwindTask(execOperations) {
    init {
        description = "Initialises a Tailwind configuration file."
    }

    @get:Input
    @get:Option(option = "config", description = "The desired folder path of the `tailwind.config.js` file.")
    val configPath: Property<String> = project.objects.property(String::class.java)

    @get:Input
    @get:Option(option = "full", description = "Initialize with a full configuration file that includes all default options.")
    val full: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @get:Input
    @get:Option(option = "postcss", description = "Initialize PostCSS configuration alongside Tailwind config.")
    val postcss: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @get:Input
    @get:Option(option = "esm", description = "Generate an ESM format configuration file.")
    val esm: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @get:Input
    @get:Option(option = "ts", description = "Generate a TypeScript configuration file.")
    val typescript: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @TaskAction
    fun initTailwind() {
        // Validate required properties
        validateVersion()
        validateCacheDir()

        if (!configPath.isPresent) {
            throw GradleException("Config path is not configured. Please set 'tailwind.configPath' in your build.gradle.kts")
        }

        val file = RelativePath.parse(false, configPath.get()).getFile(project.projectDir)

        // Validate path is within project boundaries (prevent path traversal)
        validatePathWithinProject(file)

        if (Files.notExists(file.toPath())) {
            throw GradleException(
                "The config path does not exist: ${file.absolutePath}\n" +
                "Please create the directory before running tailwindInit."
            )
        }

        if (!file.isDirectory) {
            throw GradleException(
                "The config path is a file, not a directory: ${file.absolutePath}\n" +
                "Please specify a directory path for 'tailwind.configPath'."
            )
        }

        val args = arrayListOf<String>()
        args += "init"

        // Add user-configurable options
        if (full.get()) {
            args += "--full"
        }
        if (postcss.get()) {
            args += "--postcss"
        }
        if (esm.get()) {
            args += "--esm"
        }
        if (typescript.get()) {
            args += "--ts"
        }

        try {
            execOperations.exec {
                it.workingDir = file
                it.executable = getBinary()
                it.args = args
                // Note: Timeout would require custom implementation with Future/timeout handling
                // For now, rely on CI/CD timeout mechanisms
            }
        } catch (e: Exception) {
            throw GradleException(
                "Tailwind CSS initialization failed.\n" +
                "Command: ${getBinary()} ${args.joinToString(" ")}\n" +
                "Working directory: ${file.absolutePath}\n" +
                "Error: ${e.message}",
                e
            )
        }
    }

    private fun validatePathWithinProject(file: File) {
        val canonicalPath = file.canonicalPath
        val projectPath = project.projectDir.canonicalPath

        if (!canonicalPath.startsWith(projectPath)) {
            throw GradleException(
                "Config path escapes project directory.\n" +
                "Path: $canonicalPath\n" +
                "Project: $projectPath\n" +
                "Path traversal is not allowed for security reasons."
            )
        }
    }
}
