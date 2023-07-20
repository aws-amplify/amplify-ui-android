import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.DokkaDefaults.includeNonPublic
import org.jetbrains.dokka.DokkaDefaults.jdkVersion
import org.jetbrains.dokka.DokkaDefaults.reportUndocumented
import org.jetbrains.dokka.DokkaDefaults.skipDeprecated
import org.jetbrains.dokka.DokkaDefaults.skipEmptyPackages
import org.jetbrains.dokka.gradle.DokkaTask

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

/**
 * Configures Dokka for API documentation
 */
class DokkaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.dokka")
            tasks.withType<DokkaTask>().configureEach {
                dokkaSourceSets.configureEach {
                    includeNonPublic.set(false)
                    skipEmptyPackages.set(true)
                    skipDeprecated.set(true)
                    reportUndocumented.set(true)
                    jdkVersion.set(8)
                }
            }
        }
    }
}
