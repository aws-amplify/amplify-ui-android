// Top-level build file where you can add configuration options common to all sub-projects/modules.

import com.android.build.gradle.LibraryExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath(kotlin("gradle-plugin", version = "1.8.10"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:11.0.0")
    }
}

plugins {
    id("com.android.application") version "7.3.1" apply false
    id("com.android.library") version "7.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10" apply false
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
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + optInAnnotations.map { "-opt-in=$it" }
            }
        }
    }
}

apply(plugin = "org.jetbrains.dokka")
tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(rootProject.buildDir)
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
    }

    afterEvaluate {
        configureAndroid()
    }

    if (!name.contains("test")) {
        apply(plugin = "org.jetbrains.dokka")
        tasks.withType<DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    includeNonPublic.set(false)
                    skipEmptyPackages.set(true)
                    skipDeprecated.set(true)
                    reportUndocumented.set(true)
                    jdkVersion.set(8)
                }
            }
        }
    }
}

fun Project.configureAndroid() {
    val sdkVersionName = findProperty("VERSION_NAME") ?: rootProject.findProperty("VERSION_NAME")

    if (hasProperty("signingKeyId")) {
        println("Getting signing info from protected source.")
        extra["signing.keyId"] = findProperty("signingKeyId")
        extra["signing.password"] = findProperty("signingPassword")
        extra["signing.inMemoryKey"] = findProperty("signingInMemoryKey")
    }

    configure<LibraryExtension> {
        compileSdk = 33

        defaultConfig {
            minSdk = 24
            targetSdk = 33
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            testInstrumentationRunnerArguments += "clearPackageData" to "true"
            consumerProguardFiles += rootProject.file("configuration/consumer-rules.pro")

            testOptions {
                animationsDisabled = true
                unitTests {
                    isIncludeAndroidResources = true
                }
            }

            buildConfigField("String", "VERSION_NAME", "\"$sdkVersionName\"")
        }

        lint {
            warningsAsErrors = true
            abortOnError = true
            enable += listOf("UnusedResources", "NewerVersionAvailable")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        // Needed when running integration tests. The oauth2 library uses relies on two
        // dependencies (Apache's httpcore and httpclient), both of which include
        // META-INF/DEPENDENCIES. Tried a couple other options to no avail.
        packagingOptions {
            resources.excludes.add("META-INF/DEPENDENCIES")
        }

        buildFeatures {
            compose = true
        }

        composeOptions {
            kotlinCompilerExtensionVersion = "1.4.3"
        }
    }
}
