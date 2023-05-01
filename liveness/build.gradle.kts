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

    api(dependency.amplify.api)
    api(dependency.amplify.predictions)

    implementation(dependency.bundles.camera)
    implementation(dependency.bundles.compose)

    implementation(dependency.androidx.futures)
    implementation(dependency.androidx.lifecycle)

    implementation(dependency.kotlin.serialization.json)

    implementation(dependency.tensorflow)
    implementation(dependency.tensorflow.support)

    testImplementation(projects.testing)
}
