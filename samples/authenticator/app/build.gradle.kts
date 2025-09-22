plugins {
    id("amplify.android.application")
}

android {
    namespace = "com.amplifyframework.ui.sample.authenticator"
}

dependencies {
    val authenticatorVersion = "1.6.0"

    implementation(platform(libs.androidx.compose.bom))
    implementation("com.amplifyframework.ui:authenticator:$authenticatorVersion")

    implementation(libs.bundles.compose)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.samples.androidx.datastore.prefs)

    coreLibraryDesugaring(libs.android.desugar)
}
