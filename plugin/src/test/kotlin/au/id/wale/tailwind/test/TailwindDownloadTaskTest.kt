package au.id.wale.tailwind.test

import au.id.wale.tailwind.tasks.TailwindDownloadTask
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TailwindDownloadTaskTest {

    private lateinit var tempDir: File

    @BeforeEach
    fun setup(@TempDir tempDirPath: Path) {
        tempDir = tempDirPath.toFile()
    }

    @Test
    fun `parseChecksum extracts correct checksum from sha256sums file`() {
        val checksums = """
            f391d5c6ac7d18fa9f6ba0a04818b8dac5bd2db9aa2ab2aa8c7c84363adbe073  ./tailwindcss-linux-arm64
            8537a4e4dd1816d600ccc17f5eb1a8971c6e0f0bce6a70017bff29e77f3944d9  ./tailwindcss-linux-x64
            83d1a3fcb378b44251acd8c08ed7e15fee65df93e82f43bf6b27423f5bd2b93b  ./tailwindcss-macos-arm64
        """.trimIndent()

        val result = TailwindDownloadTask.parseChecksum(checksums, "tailwindcss-linux-x64")

        assertEquals("8537a4e4dd1816d600ccc17f5eb1a8971c6e0f0bce6a70017bff29e77f3944d9", result)
    }

    @Test
    fun `parseChecksum returns null for non-existent file`() {
        val checksums = """
            f391d5c6ac7d18fa9f6ba0a04818b8dac5bd2db9aa2ab2aa8c7c84363adbe073  ./tailwindcss-linux-arm64
        """.trimIndent()

        val result = TailwindDownloadTask.parseChecksum(checksums, "non-existent-file")

        assertEquals(null, result)
    }

    @Test
    fun `calculateSHA256 computes correct hash`() {
        // Create a test file with known content
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("Hello, World!")

        val result = TailwindDownloadTask.calculateSHA256(testFile.toPath())

        // SHA256 of "Hello, World!" is a known value
        assertEquals("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f", result)
    }

    @Test
    fun `calculateSHA256 handles empty file`() {
        val testFile = tempDir.resolve("empty.txt")
        testFile.writeText("")

        val result = TailwindDownloadTask.calculateSHA256(testFile.toPath())

        // SHA256 of empty string
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", result)
    }

    @Test
    fun `calculateSHA256 handles binary content`() {
        val testFile = tempDir.resolve("binary.bin")
        testFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0xFF.toByte()))

        val result = TailwindDownloadTask.calculateSHA256(testFile.toPath())

        // Verify it returns a valid SHA256 hash (64 hex characters)
        assertEquals(64, result.length)
        assertTrue(result.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `download task fails when version is not set`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("au.id.wale.tailwind")

        val task = project.tasks.getByName("tailwindDownload") as au.id.wale.tailwind.tasks.TailwindDownloadTask

        val exception = assertFailsWith<org.gradle.api.GradleException> {
            task.downloadTailwind()
        }

        assertTrue(
            exception.message!!.contains("version is not configured"),
            "Should fail with version not configured message"
        )
    }

    @Test
    fun `download task fails with invalid version format`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("au.id.wale.tailwind")

        val task = project.tasks.getByName("tailwindDownload") as au.id.wale.tailwind.tasks.TailwindDownloadTask

        val extension = project.extensions.getByType(au.id.wale.tailwind.TailwindExtension::class.java)
        extension.version.set("invalid")

        val exception = assertFailsWith<org.gradle.api.GradleException> {
            task.downloadTailwind()
        }

        assertTrue(
            exception.message!!.contains("Invalid Tailwind version format"),
            "Should fail with invalid version format message"
        )
    }

    @Test
    fun `download task fails with empty version`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("au.id.wale.tailwind")

        val task = project.tasks.getByName("tailwindDownload") as au.id.wale.tailwind.tasks.TailwindDownloadTask

        val extension = project.extensions.getByType(au.id.wale.tailwind.TailwindExtension::class.java)
        extension.version.set("")

        val exception = assertFailsWith<org.gradle.api.GradleException> {
            task.downloadTailwind()
        }

        assertTrue(
            exception.message!!.contains("cannot be empty"),
            "Should fail with empty version message"
        )
    }
}
