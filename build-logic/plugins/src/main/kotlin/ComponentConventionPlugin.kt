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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val amplifyInternalMarkers = listOf(
    "com.amplifyframework.annotations.InternalApiWarning",
    "com.amplifyframework.annotations.InternalAmplifyApi"
)

/**
 * Shared plugin for UI component libraries
 */
class ComponentConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = libs()
            pluginManager.apply(libs.plugins.conventions.androidLibrary)
            pluginManager.apply(libs.plugins.conventions.publishing)
            pluginManager.apply(libs.plugins.conventions.kover)
            pluginManager.apply(libs.plugins.conventions.apiValidator)
            pluginManager.apply(libs.plugins.conventions.licenses)

            tasks.withType<KotlinCompile>().configureEach {
                kotlinOptions {
                    freeCompilerArgs = freeCompilerArgs + amplifyInternalMarkers.map { "-opt-in=$it" }
                }
            }
        }
    }
}
