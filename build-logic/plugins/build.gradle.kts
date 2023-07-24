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

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

ktlint {
    android.set(true)
}

dependencies {
    compileOnly(libs.plugin.android.gradle)
    compileOnly(libs.plugin.kotlin.android)
    compileOnly(libs.plugin.dokka)
    compileOnly(libs.plugin.kover)
    compileOnly(libs.plugin.ktlint)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "amplify.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("publishing") {
            id = "amplify.android.publishing"
            implementationClass = "PublishingConventionPlugin"
        }
        register("dokka") {
            id = "amplify.android.dokka"
            implementationClass = "DokkaConventionPlugin"
        }
        register("ktlint") {
            id = "amplify.android.ktlint"
            implementationClass = "KtLintConventionPlugin"
        }
        register("component") {
            id = "amplify.android.ui.component"
            implementationClass = "ComponentConventionPlugin"
        }
        register("kover") {
            id = "amplify.android.kover"
            implementationClass = "KoverConventionPlugin"
        }
    }
}
