/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

plugins {
    id("amplify.android.library")
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.amplifyframework.ui.authenticator.screenshots"

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.bundles.compose)
    implementation(libs.test.mockk)
    implementation(projects.authenticator)

    coreLibraryDesugaring(libs.android.desugar)
}

// Verify screenshots when running the check task
tasks.named("check").configure {
    dependsOn(tasks.first { it.name == "verifyPaparazzi" })
}
