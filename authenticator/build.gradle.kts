plugins {
    id("com.android.library")
    id("kotlin-android")
}
apply(from = rootProject.file("configuration/publishing.gradle"))

project.group = properties["POM_GROUP"].toString()

android {
    namespace = "com.amplifyframework.ui.authenticator"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(dependency.amplify.auth)

    implementation(dependency.bundles.compose)
    implementation(dependency.androidx.lifecycle)
    implementation(dependency.androidx.compose.viewmodel)
    coreLibraryDesugaring(dependency.android.desugar)

    testImplementation(projects.testing)
}
