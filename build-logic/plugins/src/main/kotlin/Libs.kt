/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.example.catalog.GeneratedCatalog
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency

fun Project.libs(): GeneratedCatalog {
    val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    return GeneratedCatalog(catalog)
}

fun PluginManager.apply(provider: Provider<PluginDependency>) = apply(provider.get().pluginId)

fun PluginManager.withPlugin(
    provider: Provider<PluginDependency>,
    action: (AppliedPlugin) -> Unit,
) = withPlugin(provider.get().pluginId, action)
