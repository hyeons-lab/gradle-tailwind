package com.hyeonslab.tailwind.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import javax.inject.Inject
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.Path

abstract class TailwindCompileTask @Inject constructor(
    execOperations: ExecOperations
) : BaseTailwindTask(execOperations) {
    init {
        description = "Compiles Tailwind sources to a CSS file"
    }

    @get:Input
    @get:Option(option = "input", description = "The path of the Tailwind-compatible CSS file.")
    val input: Property<String> = project.objects.property(String::class.java)

    @get:Input
    @get:Option(option = "output", description = "The desired output path for the built CSS file.")
    val output: Property<String> = project.objects.property(String::class.java)

    @get:Input
    @get:Option(option = "config", description = "The desired folder path of the `tailwind.config.js` file.")
    val configPath: Property<String> = project.objects.property(String::class.java)

    @TaskAction
    fun compileTailwind() {
        // Validate required properties
        validateVersion()
        validateCacheDir()

        if (!input.isPresent) {
            throw GradleException("Input file is not configured. Please set 'tailwind.input' in your build.gradle.kts")
        }
        if (!output.isPresent) {
            throw GradleException("Output file is not configured. Please set 'tailwind.output' in your build.gradle.kts")
        }

        val inputFile = RelativePath.parse(true, input.get()).getFile(project.projectDir)
        val outputFile = RelativePath.parse(true, output.get()).getFile(project.projectDir)

        // Validate paths are within project boundaries (prevent path traversal)
        validatePathWithinProject(inputFile, "Input file")
        validatePathWithinProject(outputFile, "Output file")

        if (!inputFile.exists()) {
            throw GradleException("The input file ${inputFile.path} does not exist.")
        }

        // Ensure output directory exists
        outputFile.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
                logger.info("Created output directory: ${parent.absolutePath}")
            }
        }

        val args = arrayListOf<String>()
        args += "-i"
        args += inputFile.absolutePath
        args += "-o"
        args += outputFile.absolutePath

        // Config file is optional in Tailwind v4 (uses CSS-based config)
        // Only add -c flag if configPath is set and the file exists
        if (configPath.isPresent) {
            val configPathFile = RelativePath.parse(true, configPath.get()).getFile(project.projectDir)

            // Check for all supported config file formats
            val configExtensions = listOf("js", "cjs", "mjs", "ts")
            val configFile = configExtensions
                .map { ext -> Path(configPathFile.path, "tailwind.config.$ext").toFile() }
                .firstOrNull { it.exists() }

            if (configFile != null) {
                args += "-c"
                args += configFile.absolutePath
                logger.info("Using Tailwind config file: ${configFile.absolutePath}")
            } else {
                logger.info("No tailwind.config.{js,cjs,mjs,ts} found - using CSS-based configuration (Tailwind v4)")
            }
        } else {
            logger.info("No config path specified - using CSS-based configuration (Tailwind v4)")
        }

        try {
            execOperations.exec {
                it.workingDir = project.projectDir
                it.executable = getBinary()
                it.args = args
                // Note: Timeout would require custom implementation with Future/timeout handling
                // For now, rely on CI/CD timeout mechanisms
            }
        } catch (e: Exception) {
            throw GradleException(
                "Tailwind CSS compilation failed.\n" +
                "Command: ${getBinary()} ${args.joinToString(" ")}\n" +
                "Working directory: ${project.projectDir}\n" +
                "Error: ${e.message}\n" +
                "Make sure the input file exists and contains valid Tailwind CSS syntax.",
                e
            )
        }
    }

    private fun validatePathWithinProject(file: File, fileType: String) {
        val canonicalPath = file.canonicalPath
        val projectPath = project.projectDir.canonicalPath

        if (!canonicalPath.startsWith(projectPath)) {
            throw GradleException(
                "$fileType path escapes project directory.\n" +
                "File: $canonicalPath\n" +
                "Project: $projectPath\n" +
                "Path traversal is not allowed for security reasons."
            )
        }
    }
}
