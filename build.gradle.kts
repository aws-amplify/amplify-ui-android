// Top-level build file where you can add configuration options common to all sub-projects/modules.

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

// Plugin aliases are a warning in Gradle < 8.1, this suppress can be removed after updating
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(dependency.plugins.android.application) apply false
    alias(dependency.plugins.android.library) apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10" apply false
    id("org.jetbrains.kotlin.jvm") version "1.7.21" apply false
    alias(dependency.plugins.dokka)
    alias(dependency.plugins.ktlint) apply false
}

val optInAnnotations = listOf(
    "com.amplifyframework.annotations.InternalApiWarning",
    "com.amplifyframework.annotations.InternalAmplifyApi"
)

allprojects {
    gradle.projectsEvaluated {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.apply {
                add("-Xlint:all")
                add("-Werror")
            }
        }
        tasks.withType<Test>().configureEach {
            minHeapSize = "128m"
            maxHeapSize = "4g"
        }
        tasks.withType<KotlinCompile> {
            compilerOptions {
                freeCompilerArgs.addAll(optInAnnotations.map { "-opt-in=$it" })
            }
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(rootProject.buildDir)
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}
