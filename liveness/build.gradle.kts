plugins {
    id("amplify.android.ui.component")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.amplifyframework.ui.liveness"
    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf("-DCMAKE_VERBOSE_MAKEFILE=ON", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
        buildConfigField(
            "String",
            "LIVENESS_VERSION_NAME",
            "\"${project.properties["VERSION_NAME"]}\""
        )
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    externalNativeBuild {
        cmake {
            // AGP doesn"t allow us to use project.buildDir (or subdirs) for CMake"s generated
            // build files (ninja build files, CMakeCache.txt, etc.). Use a staging directory that
            // lives alongside the project"s buildDir.
            buildStagingDirectory = file("${project.buildDir}/../buildNative")
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        mlModelBinding = true
    }

    androidResources {
        noCompress += "tflite"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons)

    api(libs.amplify.api)
    api(libs.amplify.predictions)

    implementation(libs.bundles.camera)
    implementation(libs.bundles.compose)

    implementation(libs.androidx.futures)
    implementation(libs.androidx.lifecycle)

    implementation(libs.kotlin.serialization.json)

    implementation(libs.litert)
    implementation(libs.litert.support)

    coreLibraryDesugaring(libs.android.desugar)

    testImplementation(projects.testing)
}
