plugins {
    id("amplify.android.ui.component")
    alias(libs.plugins.roborazzi)
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
    implementation(platform(libs.androidx.compose.bom))

    api(libs.amplify.auth)

    implementation(libs.bundles.compose)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.compose.viewmodel)
    implementation(libs.zxing)
    coreLibraryDesugaring(libs.android.desugar)

    testImplementation(projects.testing)
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}
