# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

This is **gradle-tailwind** - a Gradle plugin for managing TailwindCSS in JVM projects. It automatically downloads the TailwindCSS standalone CLI binary for your platform and provides tasks to initialize, compile, and manage Tailwind CSS files.

**Key Features:**
- Automatic platform-specific binary downloads (Windows/macOS/Linux, x64/ARM64)
- Gradle tasks: `tailwindDownload`, `tailwindInit`, `tailwindCompile`
- Caches binaries in Gradle user home directory
- Published to Gradle Plugin Portal as `au.id.wale.tailwind`

**Current Status:** Active development (v0.2.0)

---

## Build System

**Technology:**
- Gradle 9.3.1 (latest stable)
- Kotlin 2.3.0
- JVM Target 17
- Gradle plugin-publish 2.0.0

**Key commands:**
```bash
# Build the plugin
./gradlew build

# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Publish plugin (requires credentials)
./gradlew publishPlugins

# Test the example
./gradlew :example:tailwindDownload
./gradlew :example:tailwindCompile
```

**Project Structure:**
- `plugin/` - Main plugin source code
- `example/` - Example project demonstrating usage
- Root project - Parent build configuration

---

## Architecture

### Core Concept

The plugin works by:
1. Detecting user's platform (OS + architecture)
2. Downloading TailwindCSS standalone binary from GitHub releases
3. Caching binary in `~/.gradle/caches/au.id.wale.tailwind/<version>/`
4. Providing tasks that execute the binary with user-configured options

### Package Structure

```
au.id.wale.tailwind/
├── TailwindPlugin.kt           # Main plugin entry point
├── TailwindExtension.kt        # DSL configuration interface
├── platform/
│   ├── TailwindPlatform.kt     # Platform detection and binary naming
│   ├── TailwindOS.kt           # OS enum (Windows/macOS/Linux)
│   └── TailwindArch.kt         # Architecture enum (x64/ARM64/ARMv7)
└── tasks/
    ├── BaseTailwindTask.kt     # Base class with ExecOperations injection
    ├── TailwindDownloadTask.kt # Downloads and caches binary
    ├── TailwindInitTask.kt     # Runs `tailwind init`
    └── TailwindCompileTask.kt  # Compiles CSS from input to output
```

### Key Design Patterns

**1. Service Injection (Gradle 9 Requirement)**
- Tasks use `@Inject constructor(execOperations: ExecOperations)` pattern
- Required because `project.exec()` was deprecated/removed in Gradle 9
- ExecOperations marked as `@Internal` (not a task input)

**2. Platform Detection**
- `TailwindPlatform.platformOS` - Detects Windows/macOS/Linux via `System.getProperty("os.name")`
- `TailwindPlatform.platformArch` - Detects x64/ARM64/ARMv7 via `System.getProperty("os.arch")`
- Binary name format: `tailwindcss-{os}-{arch}` (`.exe` on Windows)

**3. Binary Caching**
- Downloads to `~/.gradle/caches/au.id.wale.tailwind/{version}/{binary-name}`
- Task marked `onlyIf { !binary.exists() }` to skip if already downloaded
- Binary set executable on Unix systems after download

**4. Configuration DSL**
- `TailwindExtension` is an interface (Gradle best practice)
- Properties are `Property<String>` for lazy evaluation
- Gradle 8.1+ supports direct property assignment: `version = "4.1.0"`

---

## Security Considerations

**All critical security issues have been fixed!** ✅

### ✅ FIXED: Binary Checksum Verification
**Status:** FIXED - Task #1 COMPLETED
- ✅ Downloads and verifies SHA256 checksums from GitHub releases
- ✅ Prevents MITM attacks and corrupted downloads
- ✅ Automatic checksum verification before using binary
- ✅ Clear error messages on checksum mismatch
- ✅ 3 retries with backoff for network resilience

### ✅ FIXED: Internal Gradle API Usage
**Status:** FIXED - Task #2 COMPLETED
**File:** `TailwindPlatform.kt:19`
- ✅ Replaced `org.gradle.internal.os.OperatingSystem` with public API
- ✅ Uses `System.getProperty("os.name")` for OS detection
- ✅ Uses `System.getProperty("os.arch")` for architecture detection
- ✅ Throws clear errors for unsupported platforms

### ✅ FIXED: Deprecated URL Constructor
**Status:** FIXED - Task #3 COMPLETED
- ✅ Replaced deprecated `URL(String)` with `URI.create().toURL()`
- ✅ No deprecation warnings
- ✅ Better error handling for network failures

**Security features:**
- ✅ SHA256 checksum verification for all downloads
- ✅ Network retry logic (3 attempts with 1s delay)
- ✅ Input validation (version format, required properties)
- ✅ Platform validation (unsupported OS/arch detection)
- ✅ Only uses public, stable APIs

---

## Testing

**Test Framework:**
- **JUnit 5** - Test runner
- **kotlin.test** - Assertions

**Current Test Coverage: Comprehensive** (28 tests across 5 files)
- ✅ TailwindDownloadTask (8 tests) - Checksum verification, validation, error cases
- ✅ TailwindPlatformTest (5 tests) - Platform/arch detection, binary naming
- ✅ TailwindCompileTaskTest (6 tests) - Validation, error handling, v4 support
- ✅ TailwindInitTaskTest (5 tests) - Validation, error handling
- ✅ TailwindPluginTest (4 tests) - Task registration, dependencies

**Test locations:**
- `plugin/src/test/kotlin/au/id/wale/tailwind/test/TailwindPluginTest.kt`
- `plugin/src/test/kotlin/au/id/wale/tailwind/test/TailwindDownloadTaskTest.kt`
- `plugin/src/test/kotlin/au/id/wale/tailwind/test/TailwindCompileTaskTest.kt`
- `plugin/src/test/kotlin/au/id/wale/tailwind/test/TailwindInitTaskTest.kt`
- `plugin/src/test/kotlin/au/id/wale/tailwind/test/TailwindPlatformTest.kt`

**Running tests:**
```bash
./gradlew test                              # Run all tests
./gradlew test --rerun-tasks                # Force re-run
./gradlew test --tests "TailwindPluginTest" # Specific test
```

**Writing new tests:**
```kotlin
class MyTest {
    @Test
    fun `test description`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("au.id.wale.tailwind")

        assert(project.tasks.getByName("taskName") is TaskClass)
    }
}
```

**Priority tests needed (Task #5):**
- Download with checksum verification
- Compile task execution
- Init task execution
- Platform detection (all OS/arch combinations)
- Error handling (missing binary, network failures, invalid config)
- Property validation

---

## Common Gotchas

### 1. Gradle 9+ ExecOperations Requirement
- **ALWAYS** inject `ExecOperations` via constructor
- **NEVER** use `project.exec()` directly (removed in Gradle 9)
- Mark injected services as `@Internal`

```kotlin
// ✅ CORRECT
abstract class MyTask @Inject constructor(
    @get:Internal protected val execOperations: ExecOperations
) : DefaultTask() {
    @TaskAction
    fun run() {
        execOperations.exec { /* ... */ }
    }
}

// ❌ WRONG (Gradle 9+)
abstract class MyTask : DefaultTask() {
    @TaskAction
    fun run() {
        project.exec { /* ... */ }  // Compilation error!
    }
}
```

### 2. Platform Detection on Apple Silicon
- `os.arch` returns `aarch64` on Apple Silicon Macs
- Tailwind binary name is `arm64` not `aarch64`
- Mapping handled in `TailwindArch.kt`

### 3. Task Dependencies ✅ FIXED
- ✅ `tailwindCompile` automatically depends on `tailwindDownload`
- ✅ `tailwindInit` automatically depends on `tailwindDownload`
- ✅ Binary is downloaded automatically before tasks execute

### 4. Tailwind v4 Support ✅ FIXED
- ✅ Plugin fully supports both v3 and v4
- ✅ `configPath` is optional for v4 (CSS-based config)
- ✅ Auto-detects config file presence
- ✅ v4 uses `@import "tailwindcss"` and `@theme` directives
- ✅ Tested and working with v4.1.0

### 5. Security & Reliability ✅ FIXED
- ✅ SHA256 checksum verification for all downloads
- ✅ Network retry logic (3 attempts)
- ✅ Comprehensive input validation
- ✅ No deprecated APIs used

---

## Gradle Configuration

**Multi-Module Build:**
- Root project: Parent configuration
- `plugin/` - Composite build (included via `includeBuild()`)
- `example/` - Regular subproject

**Plugin Publication:**
```kotlin
gradlePlugin {
    website.set("https://github.com/wale/gradle-tailwind")
    vcsUrl.set("https://github.com/wale/gradle-tailwind")
    plugins {
        create("tailwind") {
            id = "au.id.wale.tailwind"
            displayName = "TailwindCSS Gradle Plugin"
            description = "A Gradle plugin to manage TailwindCSS files."
            tags.set(listOf("tailwind", "css", "web", "frontend"))
            implementationClass = "au.id.wale.tailwind.TailwindPlugin"
        }
    }
}
```

**Version Management:**
- Version defined in `build.gradle.kts` and `plugin/build.gradle.kts`
- Should be externalized to `gradle.properties` (Task #18)

---

## Development Workflow

### Making Changes

1. Modify source in `plugin/src/main/kotlin/`
2. Run `./gradlew build` to verify
3. Test with example: `./gradlew :example:tailwindCompile`
4. Update `TASKS.md` to track completion
5. Commit with descriptive message

### Code Style

**File Formatting:**
- ✅ All files MUST end with a newline character
- ✅ Use UTF-8 encoding
- ✅ Use LF line endings (not CRLF)
- ✅ Trim trailing whitespace
- ✅ Kotlin files: 4 spaces indentation
- ✅ Configuration follows `.editorconfig`

**Import Rules:**
- ✅ NO star imports (wildcard imports) - use explicit imports only
- ✅ Remove unused imports
- ✅ Organize imports alphabetically
- ❌ NEVER use `import foo.bar.*`
- ✅ ALWAYS use `import foo.bar.SpecificClass`

**Why no star imports:**
- Makes it clear where classes come from
- Prevents naming conflicts
- Makes refactoring safer
- Easier to track dependencies

Most IDEs automatically respect the `.editorconfig` file in the repository root.

### Before Committing

- ✅ Ensure build succeeds: `./gradlew clean build`
- ✅ Run tests: `./gradlew test`
- ✅ Test example project still works
- ✅ Update `TASKS.md` if completing a task
- ✅ Follow commit message format (below)
- ✅ Verify code style compliance (files end with newline)

### Commit Messages

Follow conventional commits format:
```
<type>: <description>

[optional body with bullet points]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `security`: Security fix
- `refactor`: Code refactoring
- `test`: Add/update tests
- `docs`: Documentation only
- `chore`: Maintenance (deps, build config)

**Examples:**
```
security: add SHA256 checksum verification for downloads

- Fetch checksums from GitHub releases API
- Verify downloaded binary before use
- Throw clear error on checksum mismatch
- Add tests for verification logic
```

```
fix: add task dependencies between compile and download

- Make tailwindCompile depend on tailwindDownload
- Make tailwindInit depend on tailwindDownload
- Add integration test for task ordering
```

### Pull Request Descriptions

**IMPORTANT:** When creating or updating PRs, the PR description MUST match the commit message body exactly. This ensures consistency when squashing commits.

Format:
```
<Summary paragraph describing what was changed>

- Bullet point detailing specific change
- Another bullet point for another change
- Additional changes as needed
```

### Git Workflow

- **Main branch:** `main`
- **Forked org:** `hyeons-lab/gradle-tailwind`
- **Original:** `wale/gradle-tailwind`

**Remotes:**
```bash
origin       git@github.com:wale/gradle-tailwind.git
hyeons-lab   git@github.com:hyeons-lab/gradle-tailwind.git
```

---

## Known Technical Debt

See `TASKS.md` for complete list.

### ✅ COMPLETED - Phase 1 & 2 (Critical Items)
1. ✅ **Checksum verification** - Added SHA256 verification (Task #1)
2. ✅ **Internal Gradle API** - Replaced with public APIs (Task #2)
3. ✅ **Deprecated URL constructor** - Fixed (Task #3)
4. ✅ **Task dependencies** - Added automatic download dependencies (Task #4)
5. ✅ **Error handling** - Comprehensive validation & retry logic (Task #5)
6. ✅ **Test coverage** - Increased from 2 to 28 tests (Task #6)
7. ✅ **Tailwind v4 compatibility** - Fully supported with CSS-based config (Task #7)

### 🟢 Medium Priority (Remaining)
- No CI/CD pipeline (Task #8 in TASKS.md)
- Incomplete .gitignore (Task #9)
- Missing OSS docs (CONTRIBUTING, CHANGELOG, etc.) (Task #10)
- TODO: Init task options not implemented (Task #11)
- No configuration cache support (Task #12)

### 🔵 Low Priority
- Copyright headers outdated (Task #13)
- Missing advanced features (watch, minify, source maps) (Task #14)
- Minimal example project (Task #15)

**Recommended Next Steps:**
1. ✅ ~~Security & stability fixes~~ (COMPLETED)
2. ✅ ~~Test coverage & quality~~ (COMPLETED)
3. CI/CD pipeline (Task #8)
4. Documentation & housekeeping (Tasks #9, #10)
5. Additional features (Tasks #11-15)

---

## Documentation

- **README.md** - User-facing getting started guide
- **TASKS.md** - Complete task list from code review
- **CLAUDE.md** - This file (AI agent instructions)
- **LICENSE** - Apache 2.0

**Missing (Task #10):**
- `CONTRIBUTING.md` - Contribution guidelines
- `CHANGELOG.md` - Version history
- `CODE_OF_CONDUCT.md` - Community standards
- `SECURITY.md` - Security policy

---

## Dependencies

**Gradle Plugins:**
- `kotlin("jvm"):2.3.0` - Kotlin JVM plugin
- `com.gradle.plugin-publish:2.0.0` - Publishing to Gradle Plugin Portal

**Runtime Dependencies:**
- None (plugin has no external dependencies)

**Binary Downloads:**
- TailwindCSS standalone CLI from GitHub releases
- URL: `https://github.com/tailwindlabs/tailwindcss/releases/download/v{version}/{binary-name}`

**Consumer Projects Need:**
- Gradle 7.4+ (minimum for plugin-publish 2.0.0)
- Java 17+ (for plugin JVM target)

---

## Usage Example

```kotlin
// Consumer's build.gradle.kts
plugins {
    id("au.id.wale.tailwind") version "0.2.0"
}

tailwind {
    version = "4.1.0"
    configPath = "src/main/resources"
    input = "src/main/resources/tailwind/tailwind.css"
    output = "src/main/resources/css/example.css"
}
```

**Typical workflow:**
```bash
# Download Tailwind binary (automatic if not cached)
./gradlew tailwindDownload

# Initialize tailwind.config.js
./gradlew tailwindInit

# Compile CSS
./gradlew tailwindCompile
```

**Output:**
- Binary cached in `~/.gradle/caches/au.id.wale.tailwind/4.1.0/tailwindcss-{os}-{arch}`
- Config file: `src/main/resources/tailwind.config.js`
- Output CSS: `src/main/resources/css/example.css`

---

## Version Strategy

- **Current:** 0.2.0 (released)
- **Next:** 0.3.0 - Security fixes + Tailwind v4 support
- **Future (1.0):** Stable API, comprehensive features

**Breaking Changes Planned:**
- Task #2 may change platform detection behavior
- Task #8 may require config changes for Tailwind v4

---

## Special Instructions

### When Working on Security Issues (Tasks #1, #2, #3)

**Binary Checksum Verification (Task #1):**
- DO fetch SHA256 from GitHub releases API or checksums file
- DO verify before marking executable
- DO provide clear error messages
- DON'T skip verification even for development

**Internal API Replacement (Task #2):**
- DO test on all platforms (Windows, macOS, Linux)
- DO handle edge cases (unknown OS)
- DO throw clear error for unsupported platforms
- DON'T use regex on `os.name` (fragile)

### When Adding Tests (Task #5)

**High-value tests to add first:**
1. Platform detection for all OS/arch combinations
2. Download task with mocked network (or test mode)
3. Compile task with real Tailwind binary
4. Error cases (missing config, invalid paths, network failures)
5. Property validation (required fields, version format)

**Test patterns:**
```kotlin
// Platform detection
@Test
fun `detects macOS ARM64 correctly`() {
    // Mock System.getProperty calls
    val platform = TailwindPlatform()
    assertEquals(TailwindOS.MAC, platform.platformOS)
    assertEquals(TailwindArch.AARCH64, platform.platformArch)
}

// Task execution
@Test
fun `compile task executes with valid config`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply("au.id.wale.tailwind")

    val task = project.tasks.getByName("tailwindCompile") as TailwindCompileTask
    task.version.set("4.1.0")
    task.input.set("input.css")
    task.output.set("output.css")

    // Assert task configuration
}
```

### When Adding Features

**DO:**
- Add configuration properties to `TailwindExtension`
- Use `Property<T>` for lazy evaluation
- Add tests for new features
- Document in README with examples
- Consider backward compatibility

**DON'T:**
- Add features before fixing critical security issues (#1, #2, #3)
- Add features without tests (coverage already low)
- Break existing user configurations
- Add features not supported by Tailwind CLI

---

## Debugging Tips

**Binary download failures:**
```bash
# Check what URL is being constructed
# Should be: https://github.com/tailwindlabs/tailwindcss/releases/download/v4.1.0/tailwindcss-macos-arm64

# Check binary exists after download
ls -la ~/.gradle/caches/au.id.wale.tailwind/4.1.0/

# Check if executable on Unix
ls -l ~/.gradle/caches/au.id.wale.tailwind/4.1.0/tailwindcss-*
```

**Task not running:**
```bash
# Check task dependencies
./gradlew :example:tailwindCompile --dry-run

# Check if download task was skipped
./gradlew :example:tailwindDownload --info
```

**Platform detection issues:**
```kotlin
// Add logging to TailwindPlatform
println("Detected OS: ${System.getProperty("os.name")}")
println("Detected arch: ${System.getProperty("os.arch")}")
```

---

## Resources

- **Gradle Plugin Development:** https://docs.gradle.org/current/userguide/custom_plugins.html
- **Tailwind CLI:** https://tailwindcss.com/blog/standalone-cli
- **Gradle ExecOperations:** https://docs.gradle.org/current/javadoc/org/gradle/process/ExecOperations.html
- **Service Injection:** https://docs.gradle.org/current/userguide/service_injection.html