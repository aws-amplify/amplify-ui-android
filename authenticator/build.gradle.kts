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

    packagingOptions {
        resources.excludes.add("META-INF/LICENSE.md")
    }

}

dependencies {
    api(libs.amplify.auth)

    implementation(libs.bundles.compose)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.compose.viewmodel)
    coreLibraryDesugaring(libs.android.desugar)

    testImplementation(projects.testing)
}
