/**
 *    Copyright 2023-present Duale Siad
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = "com.hyeons-lab"
version = "0.3.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    this.jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

gradlePlugin {
    website.set("https://github.com/hyeons-lab/gradle-tailwind")
    vcsUrl.set("https://github.com/hyeons-lab/gradle-tailwind")
    plugins {
        create("tailwind") {
            id = "com.hyeons-lab.tailwind"
            displayName = "TailwindCSS Gradle Plugin"
            description = "A Gradle plugin to manage TailwindCSS files."
            tags.set(listOf("tailwind", "css", "web", "frontend"))
            implementationClass = "com.hyeonslab.tailwind.TailwindPlugin"
        }
    }
}