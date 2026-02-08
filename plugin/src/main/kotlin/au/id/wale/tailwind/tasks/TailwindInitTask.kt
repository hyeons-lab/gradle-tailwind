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

        try {
            execOperations.exec {
                it.workingDir = file
                it.executable = getBinary()
                it.args = args // TODO: add user-configurable options to the TailwindCSS `init` task.
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
