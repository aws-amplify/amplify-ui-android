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

import com.amplify.ui.configureAndroid
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * This convention plugin configures an Android library module
 */
@Suppress("LocalVariableName")
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("org.jetbrains.kotlin.android")
            apply("amplify.android.ktlint")
        }

        val POM_GROUP: String by target

        with(target) {
            group = POM_GROUP
            extensions.configure<LibraryExtension> {
                target.configureAndroid(this)
                defaultConfig {
                    consumerProguardFiles += rootProject.file("configuration/consumer-rules.pro")
                }
            }

            tasks.withType<JavaCompile>().configureEach {
                options.compilerArgs.apply {
                    add("-Xlint:all")
                    add("-Werror")
                }
            }

            tasks.withType<Test>().configureEach {
                minHeapSize = "128m"
                maxHeapSize = "4g"
            }

            configure<KotlinProjectExtension> {
                jvmToolchain(17)
            }
        }
    }
}
