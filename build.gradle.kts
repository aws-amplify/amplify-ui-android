// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

// Plugin aliases are a warning in Gradle < 8.1, this suppress can be removed after updating
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint) apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

dependencies {
    // Generate combined coverage report
    kover(project(":authenticator"))
    kover(project(":liveness"))
}
