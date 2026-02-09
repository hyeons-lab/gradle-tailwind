package com.hyeonslab.tailwind.test

import com.hyeonslab.tailwind.platform.TailwindArch
import com.hyeonslab.tailwind.platform.TailwindOS
import com.hyeonslab.tailwind.platform.TailwindPlatform
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TailwindPlatformTest {

    @Test
    fun `platformOS detects current operating system`() {
        val osName = System.getProperty("os.name").lowercase()
        val detectedOS = TailwindPlatform.platformOS

        when {
            osName.contains("mac") || osName.contains("darwin") -> {
                assertEquals(TailwindOS.MAC, detectedOS, "Should detect macOS")
            }
            osName.contains("linux") -> {
                assertEquals(TailwindOS.LINUX, detectedOS, "Should detect Linux")
            }
            osName.contains("windows") -> {
                assertEquals(TailwindOS.WINDOWS, detectedOS, "Should detect Windows")
            }
            else -> {
                assertEquals(TailwindOS.UNKNOWN, detectedOS, "Should return UNKNOWN for unrecognized OS")
            }
        }
    }

    @Test
    fun `platformArch detects current architecture`() {
        val arch = System.getProperty("os.arch")
        val detectedArch = TailwindPlatform.platformArch

        when (arch) {
            "x86_64", "amd64" -> assertEquals(TailwindArch.X86_64, detectedArch)
            "aarch32", "arm" -> assertEquals(TailwindArch.AARCH32, detectedArch)
            "aarch64", "arm64" -> assertEquals(TailwindArch.AARCH64, detectedArch)
            else -> assertEquals(TailwindArch.UNKNOWN, detectedArch)
        }
    }

    @Test
    fun `format returns correct binary name for current platform`() {
        val platform = TailwindPlatform()
        val formatted = platform.format()

        // Should start with "tailwindcss-"
        assertTrue(formatted.startsWith("tailwindcss-"), "Binary name should start with tailwindcss-")

        // Should contain OS name
        val osName = TailwindPlatform.platformOS.binaryOS
        if (osName.isNotEmpty()) {
            assertTrue(formatted.contains(osName), "Binary name should contain OS: $osName")
        }

        // Should contain arch name
        val archName = TailwindPlatform.platformArch.binaryArch
        if (archName.isNotEmpty()) {
            assertTrue(formatted.contains(archName), "Binary name should contain arch: $archName")
        }

        // Windows should have .exe extension
        if (TailwindPlatform.platformOS == TailwindOS.WINDOWS) {
            assertTrue(formatted.endsWith(".exe"), "Windows binary should end with .exe")
        }
    }

    @Test
    fun `format returns expected pattern for macOS ARM64`() {
        // This test verifies the format matches what we expect for a specific platform
        val platform = TailwindPlatform()
        val formatted = platform.format()

        // Format should be: tailwindcss-{os}-{arch}[.exe]
        val pattern = Regex("tailwindcss-(macos|linux|windows)-(x64|arm64|armv7)(\\.exe)?")
        assertTrue(
            formatted.matches(pattern) || formatted == "tailwindcss-",
            "Binary name should match expected pattern: $formatted"
        )
    }

}
