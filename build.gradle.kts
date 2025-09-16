// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.binary.compatibility) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.licensee) apply false
    alias(libs.plugins.roborazzi) apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}

kover {
    currentProject {
        createVariant("coverage") {
            // Use a custom variant called "coverage" to generate the merged report. This variant only runs the tests
            // for the release variant.
        }
    }
}

dependencies {
    // Generate combined coverage report
    kover(project(":authenticator"))
    kover(project(":liveness"))
}
