package au.id.wale.tailwind.test

import au.id.wale.tailwind.TailwindExtension
import au.id.wale.tailwind.tasks.TailwindCompileTask
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TailwindCompileTaskTest {

    private lateinit var tempDir: File

    @BeforeEach
    fun setup(@TempDir tempDirPath: Path) {
        tempDir = tempDirPath.toFile()
    }

    @Test
    fun `compile task fails when version is not set`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("au.id.wale.tailwind")

        val task = project.tasks.getByName("tailwindCompile") as TailwindCompileTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.input.set("src/main/css/input.css")
        extension.output.set("build/css/output.css")
        extension.configPath.set("src/main/resources")

        val exception = assertFailsWith<GradleException> {
            task.compileTailwind()
        }

        assertTrue(
            exception.message!!.contains("version is not configured"),
            "Should fail with version not configured message"
        )
    }

    @Test
    fun `compile task fails when input is not set`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("au.id.wale.tailwind")

        val task = project.tasks.getByName("tailwindCompile") as TailwindCompileTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.version.set("4.1.0")
        extension.output.set("build/css/output.css")
        extension.configPath.set("src/main/resources")

        val exception = assertFailsWith<GradleException> {
            task.compileTailwind()
        }

        assertTrue(
            exception.message!!.contains("Input file is not configured"),
            "Should fail with input not configured message"
        )
    }

    @Test
    fun `compile task fails when output is not set`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("au.id.wale.tailwind")

        val task = project.tasks.getByName("tailwindCompile") as TailwindCompileTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.version.set("4.1.0")
        extension.input.set("src/main/css/input.css")
        extension.configPath.set("src/main/resources")

        val exception = assertFailsWith<GradleException> {
            task.compileTailwind()
        }

        assertTrue(
            exception.message!!.contains("Output file is not configured"),
            "Should fail with output not configured message"
        )
    }

    @Test
    fun `compile task works without configPath for Tailwind v4`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("au.id.wale.tailwind")

        // Create input file
        val inputDir = File(tempDir, "src/main/css")
        inputDir.mkdirs()
        val inputFile = File(inputDir, "input.css")
        inputFile.writeText("@tailwind base;")

        val task = project.tasks.getByName("tailwindCompile") as TailwindCompileTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.version.set("4.1.0")
        extension.input.set("src/main/css/input.css")
        extension.output.set("build/css/output.css")

        // This should not throw - configPath is optional in v4
        // Note: This will fail when trying to execute since we don't have the binary,
        // but validation should pass
        try {
            task.compileTailwind()
        } catch (e: Exception) {
            // Expected to fail during execution, but not during validation
            assertTrue(
                !e.message!!.contains("Config path is not configured"),
                "Should not fail with config path validation error"
            )
        }
    }

    @Test
    fun `compile task fails when input file does not exist`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("au.id.wale.tailwind")

        val task = project.tasks.getByName("tailwindCompile") as TailwindCompileTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.version.set("4.1.0")
        extension.input.set("src/main/css/nonexistent.css")
        extension.output.set("build/css/output.css")
        extension.configPath.set("src/main/resources")

        val exception = assertFailsWith<GradleException> {
            task.compileTailwind()
        }

        assertTrue(
            exception.message!!.contains("does not exist"),
            "Should fail when input file doesn't exist"
        )
    }

    @Test
    fun `compile task fails with invalid version format`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("au.id.wale.tailwind")

        val task = project.tasks.getByName("tailwindCompile") as TailwindCompileTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.version.set("invalid-version")
        extension.input.set("src/main/css/input.css")
        extension.output.set("build/css/output.css")
        extension.configPath.set("src/main/resources")

        val exception = assertFailsWith<GradleException> {
            task.compileTailwind()
        }

        assertTrue(
            exception.message!!.contains("Invalid Tailwind version format"),
            "Should fail with invalid version format message"
        )
    }
}
