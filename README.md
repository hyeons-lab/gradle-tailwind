![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/au.id.wale.tailwind)
# `gradle-tailwind`
`gradle-tailwind` is a plugin for the [Gradle](https://gradle.org) build manager for the [TailwindCSS](https://tailwindcss.com) framework.

> **NOTE**
> This Gradle plugin is currently in _alpha_, so some information listed here does not apply fully yet.

## How To Use
### Requirements
- Gradle 7.4+
- Tailwind 3.0.3+ (v4.x supported)

### Plugin Configuration
The plugin configuration is simple, just see the following section for the relevant DSL for your project:

#### Groovy DSL
First, add your plugin like so:
```groovy
plugins {
    id "au.id.wale.tailwind" version "0.2.0"
}
```
And configure your Tailwind application as desired, like so:
```groovy
tailwind {
    version = "4.1.0"
    configPath = "src/main/resources"  // Optional for v4; required for v3
    input = "src/main/resources/tailwind/tailwind.css"
    output = "src/main/resources/css/example.css"
}
```
#### Kotlin DSL
First, add your plugin like so:
```kts
plugins {
    id("au.id.wale.tailwind") version "0.1.0"
}
```
And configure your Tailwind application as desired, like so:
- Gradle <8.1
    ```kts
    tailwind {
        version.set("4.1.0")
        configPath.set("src/main/resources")  // Optional for v4; required for v3
        input.set("src/main/resources/tailwind/tailwind.css")
        output.set("src/main/resources/css/example.css")
    }
    ```
- Gradle >8.1
    ```kotlin
    tailwind {
        version = "4.1.0"
        configPath = "src/main/resources"  // Optional for v4; required for v3
        input = "src/main/resources/tailwind/tailwind.css"
        output = "src/main/resources/css/example.css"
    }
    ```

### Tailwind CSS v4 Support

This plugin supports both Tailwind CSS v3 and v4.

**Tailwind v4** uses a CSS-based configuration system instead of `tailwind.config.js`. When using v4:
- The `configPath` property is **optional** - if not set or if `tailwind.config.js` doesn't exist, the plugin will use CSS-based configuration
- Configure your theme directly in your CSS file using `@theme` and other v4 directives
- See the [Tailwind v4 documentation](https://tailwindcss.com/blog/tailwindcss-v4) for details on CSS-based configuration

**Tailwind v3** uses JavaScript configuration:
- Set `configPath` to the directory containing your `tailwind.config.js`
- The plugin will automatically use the config file if it exists

### Available Tasks
- `:tailwindDownload` - Downloads the TailwindCSS binary for the configured version. Automatically runs before compile/init tasks if the binary is missing. Includes SHA256 checksum verification for security.
- `:tailwindInit` - Initialises the `tailwind.config.js` file (for Tailwind v3) with the input and output provided in the config.
- `:tailwindCompile` - Compiles the given Tailwind CSS file to the path provided in `output`. Automatically downloads the binary if needed.

### Example
There is a working example containing a rudimentary Tailwind project. To compile the CSS and view the HTML properly, run the following task:
```bash
./gradlew :example:tailwindCompile
```

## Development
Because Gradle sucks, the only LTS versions that this build allows for are 8 and 11, due to a known issue [with the tests](https://github.com/gradle/gradle/issues/18647). The plugin itself should build with Java 17 in the meantime.

## License
This project is licensed under the [Apache 2.0](https://github.com/wale/gradle-tailwind/blob/main/LICENSE) license.