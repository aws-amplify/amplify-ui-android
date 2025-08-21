plugins {
    id("amplify.android.application")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.amplifyframework.ui.sample.liveness"
    defaultConfig {
        buildConfigField("boolean", "SHOW_DEBUG_UI", "false")
    }
}

dependencies {
    val livenessVersion = "1.6.0"

    implementation(platform(libs.androidx.compose.bom))
    implementation("com.amplifyframework.ui:liveness:$livenessVersion")

    implementation(libs.amplify.core.kotlin)
    implementation(libs.amplify.api)
    implementation(libs.amplify.auth)
    implementation(libs.amplify.predictions)

    implementation(libs.bundles.compose)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist)

    coreLibraryDesugaring(libs.android.desugar)
}
