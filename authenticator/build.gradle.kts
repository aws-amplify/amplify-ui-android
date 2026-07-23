plugins {
    id("amplify.android.ui.component")
}

android {
    namespace = "com.amplifyframework.ui.authenticator"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles += file("consumer-rules.pro")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    api(libs.amplify.auth)

    implementation(libs.bundles.compose)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.compose.viewmodel)
    implementation(libs.zxing)

    testImplementation(projects.testing)
}
