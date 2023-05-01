plugins {
    id("amplify.android.ui.component")
}

android {
    namespace = "com.amplifyframework.ui.authenticator"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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
