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
                arguments += "-DCMAKE_VERBOSE_MAKEFILE=ON"
            }
        }
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
}

dependencies {

    api(libs.amplify.api)
    api(libs.amplify.predictions)

    implementation(libs.bundles.camera)
    implementation(libs.bundles.compose)

    implementation(libs.androidx.futures)
    implementation(libs.androidx.lifecycle)

    implementation(libs.kotlin.serialization.json)

    implementation(libs.tensorflow)
    implementation(libs.tensorflow.support)

    testImplementation(projects.testing)
}
