// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("app.cash.licensee:licensee-gradle-plugin:1.7.0")
    }
}

subprojects {
    apply(plugin = "app.cash.licensee")
    configure<app.cash.licensee.LicenseeExtension> {
        allow("Apache-2.0")
        allow("MIT")
        allow("BSD-2-Clause")
        allowDependency("org.bouncycastle", "bcprov-jdk15on", "1.70") {
            "MIT License"
        }
        allowDependency("org.ow2.asm", "asm", "9.4") {
            "3-Clause BSD License"
        }
        allowDependency("org.ow2.asm", "asm-analysis", "9.4")
        allowDependency("org.ow2.asm", "asm-commons", "9.4")
        allowDependency("org.ow2.asm", "asm-tree", "9.4")
        allowDependency("org.ow2.asm", "asm-util", "9.4")
        allowDependency("com.ibm.icu", "icu4j", "70.1")
        allowUrl("http://aws.amazon.com/apache2.0")
        allowUrl("https://developer.android.com/studio/terms.html")

        ignoreDependencies("javax.annotation", "javax.annotation-api") {
            "Transitive dependency for androidx.test.espresso:espresso-core"
        }
        ignoreDependencies("org.junit", "junit-bom") {
            because("Unit Testing Dependency")
        }
        ignoreDependencies("org.junit", "jupiter") {
            because("Unit Testing Dependency")
        }
        ignoreDependencies("org.junit.jupiter", "junit-jupiter") {
            because("Unit Testing Dependency")
        }
        ignoreDependencies("org.junit.jupiter", "junit-jupiter-params") {
            because("Unit Testing Dependency")
        }
        ignoreDependencies("org.junit", "junit-jupiter-params") {
            because("Unit Testing Dependency")
        }
        ignoreDependencies("org.junit.platform", "junit-platform-commons") {
            because("Unit Testing Dependency")
        }
        ignoreDependencies("org.junit.platform", "junit-platform-engine") {
            because("Unit Testing Dependency")
        }
        ignoreDependencies("junit", "junit") {
            because("Unit Testing Dependency")
        }
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
