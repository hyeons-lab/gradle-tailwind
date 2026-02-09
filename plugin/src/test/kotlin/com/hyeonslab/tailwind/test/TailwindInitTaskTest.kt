package com.hyeonslab.tailwind.test

import com.hyeonslab.tailwind.TailwindExtension
import com.hyeonslab.tailwind.tasks.TailwindInitTask
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TailwindInitTaskTest {

    private lateinit var tempDir: File

    @BeforeEach
    fun setup(@TempDir tempDirPath: Path) {
        tempDir = tempDirPath.toFile()
    }

    @Test
    fun `init task fails when version is not set`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("com.hyeons-lab.tailwind")

        val task = project.tasks.getByName("tailwindInit") as TailwindInitTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.configPath.set("src/main/resources")

        val exception = assertFailsWith<GradleException> {
            task.initTailwind()
        }

        assertTrue(
            exception.message!!.contains("version is not configured"),
            "Should fail with version not configured message"
        )
    }

    @Test
    fun `init task fails when configPath is not set`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("com.hyeons-lab.tailwind")

        val task = project.tasks.getByName("tailwindInit") as TailwindInitTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.version.set("4.1.0")

        val exception = assertFailsWith<GradleException> {
            task.initTailwind()
        }

        assertTrue(
            exception.message!!.contains("Config path is not configured"),
            "Should fail with config path not configured message"
        )
    }

    @Test
    fun `init task fails when config directory does not exist`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("com.hyeons-lab.tailwind")

        val task = project.tasks.getByName("tailwindInit") as TailwindInitTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.version.set("4.1.0")
        extension.configPath.set("nonexistent/directory")

        val exception = assertFailsWith<GradleException> {
            task.initTailwind()
        }

        assertTrue(
            exception.message!!.contains("does not exist"),
            "Should fail when config directory doesn't exist"
        )
    }

    @Test
    fun `init task fails when config path is a file not a directory`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("com.hyeons-lab.tailwind")

        // Create a file instead of a directory
        val configFile = File(tempDir, "config.txt")
        configFile.writeText("test")

        val task = project.tasks.getByName("tailwindInit") as TailwindInitTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.version.set("4.1.0")
        extension.configPath.set("config.txt")

        val exception = assertFailsWith<GradleException> {
            task.initTailwind()
        }

        assertTrue(
            exception.message!!.contains("is a file, not a directory"),
            "Should fail when config path is a file"
        )
    }

    @Test
    fun `init task fails with invalid version format`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("com.hyeons-lab.tailwind")

        // Create config directory
        val configDir = File(tempDir, "config")
        configDir.mkdirs()

        val task = project.tasks.getByName("tailwindInit") as TailwindInitTask

        val extension = project.extensions.getByType(TailwindExtension::class.java)
        extension.version.set("not-a-version")
        extension.configPath.set("config")

        val exception = assertFailsWith<GradleException> {
            task.initTailwind()
        }

        assertTrue(
            exception.message!!.contains("Invalid Tailwind version format"),
            "Should fail with invalid version format message"
        )
    }
}
