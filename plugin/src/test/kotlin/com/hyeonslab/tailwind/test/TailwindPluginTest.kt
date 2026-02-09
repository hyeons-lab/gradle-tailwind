package com.hyeonslab.tailwind.test

import com.hyeonslab.tailwind.TailwindExtension
import com.hyeonslab.tailwind.tasks.TailwindDownloadTask
import com.hyeonslab.tailwind.tasks.TailwindInitTask
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TailwindPluginTest {
    @Test
    fun `tailwindDownload task is applied correctly to the project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.hyeons-lab.tailwind")

        assert(project.tasks.getByName("tailwindDownload") is TailwindDownloadTask)
    }

    @Test
    fun `tailwindInit task is applied correctly to the project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.hyeons-lab.tailwind")

        assert(project.tasks.getByName("tailwindInit") is TailwindInitTask)
    }

    @Test
    fun `tailwindInit task depends on tailwindDownload`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.hyeons-lab.tailwind")

        val initTask = project.tasks.getByName("tailwindInit")
        val downloadTask = project.tasks.getByName("tailwindDownload")

        assertTrue(
            initTask.dependsOn.contains(downloadTask) ||
            initTask.taskDependencies.getDependencies(initTask).contains(downloadTask),
            "tailwindInit should depend on tailwindDownload"
        )
    }

    @Test
    fun `tailwindCompile task depends on tailwindDownload`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.hyeons-lab.tailwind")

        val compileTask = project.tasks.getByName("tailwindCompile")
        val downloadTask = project.tasks.getByName("tailwindDownload")

        assertTrue(
            compileTask.dependsOn.contains(downloadTask) ||
            compileTask.taskDependencies.getDependencies(compileTask).contains(downloadTask),
            "tailwindCompile should depend on tailwindDownload"
        )
    }
}
