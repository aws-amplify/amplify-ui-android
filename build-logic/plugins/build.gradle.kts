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
    compileOnly(libs.plugin.binary.compatibility)
    compileOnly(libs.plugin.kotlin.android)
    compileOnly(libs.plugin.kover)
    compileOnly(libs.plugin.ktlint)
    compileOnly(libs.plugin.licensee)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "amplify.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("apiValidator") {
            id = "amplify.android.api.validator"
            implementationClass = "ApiValidatorConventionPlugin"
        }
        register("component") {
            id = "amplify.android.ui.component"
            implementationClass = "ComponentConventionPlugin"
        }
        register("kover") {
            id = "amplify.android.kover"
            implementationClass = "KoverConventionPlugin"
        }
        register("ktlint") {
            id = "amplify.android.ktlint"
            implementationClass = "KtLintConventionPlugin"
        }
        register("licenses") {
            id = "amplify.android.licenses"
            implementationClass = "LicensesConventionPlugin"
        }
        register("publishing") {
            id = "amplify.android.publishing"
            implementationClass = "PublishingConventionPlugin"
        }
    }
}

abstract class GenerateTypeSafeCatalogTask : DefaultTask() {

    @get:Input
    abstract val catalogName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val catalogs = project.extensions.getByType<VersionCatalogsExtension>()
        val catalog = catalogs.named(catalogName.get())

        val pluginExtension = project.extensions.getByType<GradlePluginDevelopmentExtension>()

        val generatedCode = buildString {
            appendLine("package com.example.catalog")
            appendLine()
            appendLine("import org.gradle.api.artifacts.MinimalExternalModuleDependency")
            appendLine("import org.gradle.api.provider.Provider")
            appendLine("import org.gradle.plugin.use.PluginDependency")
            appendLine("import org.gradle.api.artifacts.VersionCatalog")
            appendLine("import org.gradle.api.artifacts.ExternalModuleDependencyBundle") // Ensure correct import
            appendLine("import java.util.Optional")
            appendLine()

            // Define the extension function
            appendLine("fun <T> Optional<T>.orElseThrowIllegalArgs(alias: String, type: String): T {")
            appendLine("    return this.orElseThrow { IllegalArgumentException(\"\$type alias '\$alias' not found\") }")
            appendLine("}")
            appendLine()

            // Generate the main wrapper class
            appendLine("data class GeneratedCatalog(val catalog: VersionCatalog) {")
            appendLine("    val versions = Versions(catalog)")
            appendLine("    val libraries = Libraries(catalog)")
            appendLine("    val bundles = Bundles(catalog)")
            appendLine("    val plugins = Plugins(catalog)")
            appendLine("}")
            appendLine()

            // Generate the Versions data class
            appendLine("data class Versions(val catalog: VersionCatalog) {")
            catalog.versionAliases.forEach { alias ->
                appendLine("    val ${alias.toCamelCase()}: String")
                appendLine("        get() = catalog.findVersion(\"$alias\").orElseThrowIllegalArgs(\"$alias\", \"Version\").requiredVersion")
            }
            appendLine("}")
            appendLine()

            // Generate the Libraries data class
            appendLine("data class Libraries(val catalog: VersionCatalog) {")
            catalog.libraryAliases.forEach { alias ->
                appendLine("    val ${alias.toCamelCase()}: Provider<MinimalExternalModuleDependency>")
                appendLine("        get() = catalog.findLibrary(\"$alias\").orElseThrowIllegalArgs(\"$alias\", \"Library\")")
            }
            appendLine("}")
            appendLine()

            // Generate the Bundles data class
            appendLine("data class Bundles(val catalog: VersionCatalog) {")
            catalog.bundleAliases.forEach { alias ->
                appendLine("    val ${alias.toCamelCase()}: Provider<ExternalModuleDependencyBundle>")
                appendLine("        get() = catalog.findBundle(\"$alias\").orElseThrowIllegalArgs(\"$alias\", \"Bundle\")")
            }
            appendLine("}")
            appendLine()

            // Generate the Plugins data class
            appendLine("data class Plugins(val catalog: VersionCatalog) {")
            catalog.pluginAliases.forEach { alias ->
                appendLine("    val ${alias.toCamelCase()}: Provider<PluginDependency>")
                appendLine("        get() = catalog.findPlugin(\"$alias\").orElseThrowIllegalArgs(\"$alias\", \"Plugin\")")
            }
            appendLine("    val conventions = ConventionPlugins()")
            appendLine("}")
            appendLine()

            appendLine("class ConventionPlugins {")
            pluginExtension.plugins.forEach { pluginDeclaration ->
                appendLine("    val ${pluginDeclaration.name}: String = \"${pluginDeclaration.id}\"")
            }
            appendLine("}")
            appendLine()
        }

        val outputFile = outputDir.get().file("GeneratedCatalog.kt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(generatedCode)
    }

    private fun String.toCamelCase(): String = split("-", "_", ".")
        .joinToString("") { it.capitalize() }
        .decapitalize()
}

tasks.register<GenerateTypeSafeCatalogTask>("generateTypeSafeCatalog") {
    outputDir.set(layout.buildDirectory.dir("generated/sources/versionCatalog"))
    catalogName.set("libs")
}

sourceSets {
    main {
        kotlin.srcDir("build/generated/sources/versionCatalog")
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateTypeSafeCatalog")
}
