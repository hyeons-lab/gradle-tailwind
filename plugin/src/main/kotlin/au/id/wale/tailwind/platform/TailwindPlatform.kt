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

package au.id.wale.tailwind.platform

import org.gradle.api.GradleException

open class TailwindPlatform {
    companion object {
        // Cache platform detection results to avoid repeated System.getProperty() calls
        private val _platformOS: TailwindOS by lazy {
            val osName = System.getProperty("os.name")?.lowercase()
                ?: throw GradleException("Unable to detect operating system (os.name property is null)")

            when {
                osName.contains("mac") || osName.contains("darwin") -> TailwindOS.MAC
                osName.contains("linux") -> TailwindOS.LINUX
                osName.contains("windows") -> TailwindOS.WINDOWS
                else -> throw GradleException(
                    "Unsupported operating system: $osName\n" +
                    "TailwindCSS standalone binary is only available for macOS, Linux, and Windows.\n" +
                    "Detected OS: $osName"
                )
            }
        }

        private val _platformArch: TailwindArch by lazy {
            val arch = System.getProperty("os.arch")
            when (arch) {
                "x86_64", "amd64" -> TailwindArch.X86_64
                "aarch32", "arm" -> TailwindArch.AARCH32
                "aarch64", "arm64" -> TailwindArch.AARCH64
                else -> throw GradleException(
                    "Unsupported architecture: $arch\n" +
                    "TailwindCSS standalone binary is only available for x86_64, ARM32, and ARM64 architectures.\n" +
                    "Detected architecture: $arch"
                )
            }
        }

        val platformOS: TailwindOS
            get() = _platformOS

        val platformArch: TailwindArch
            get() = _platformArch
    }

    /**
     * Formats a given TailwindCSS version to match the name with the Tailwind binary.
     */
    fun format(): String {
        val archName = platformArch.binaryArch
        val osName = platformOS.binaryOS
        val format = "tailwindcss-$osName-$archName"
        return if (platformOS == TailwindOS.WINDOWS) "$format.exe" else format
    }
}
