import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.plugins.signing.SigningExtension
import java.net.URI

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
 * Apply the publishing plugin to modules that are published for public consumption
 */
class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Apply the publishing plugins
        with(target.pluginManager) {
            apply("signing")
            apply("maven-publish")
        }

        // Setup the publishing extension for the android plugin
        target.configureAndroidPublishing()
        // Configure the signing and maven publish plugins
        target.afterEvaluate {
            configureMavenPublishing()
        }
    }

    // Configure the publishing block in the android extension
    private fun Project.configureAndroidPublishing() {
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> {
                publishing {
                    singleVariant("release") {
                        withSourcesJar()
                    }
                }
            }
        }
    }

    // Configure the publishing extension in the project
    @Suppress("LocalVariableName")
    private fun Project.configureMavenPublishing() {
        configure<PublishingExtension> {
            publications {
                create("maven", MavenPublication::class.java) {
                    val POM_GROUP: String by project
                    val POM_ARTIFACT_ID: String by project
                    val VERSION_NAME: String by project

                    groupId = POM_GROUP
                    artifactId = POM_ARTIFACT_ID
                    version = VERSION_NAME

                    from(components["release"])

                    pom {
                        val POM_NAME: String? by project
                        val POM_PACKAGING: String? by project
                        val POM_DESCRIPTION: String? by project
                        val POM_URL: String? by project
                        name.set(POM_NAME)
                        packaging = POM_PACKAGING
                        description.set(POM_DESCRIPTION)
                        url.set(POM_URL)

                        scm {
                            val POM_SCM_URL: String? by project
                            val POM_SCM_CONNECTION: String? by project
                            val POM_SCM_DEV_CONNECTION: String? by project
                            url.set(POM_SCM_URL)
                            connection.set(POM_SCM_CONNECTION)
                            developerConnection.set(POM_SCM_DEV_CONNECTION)
                        }

                        licenses {
                            license {
                                val POM_LICENSE_NAME: String? by project
                                val POM_LICENSE_URL: String? by project
                                val POM_LICENSE_DIST: String? by project
                                name.set(POM_LICENSE_NAME)
                                url.set(POM_LICENSE_URL)
                                distribution.set(POM_LICENSE_DIST)
                            }
                        }

                        developers {
                            developer {
                                val POM_DEVELOPER_ID: String? by project
                                val POM_DEVELOPER_ORGANIZATION_URL: String? by project
                                id.set(POM_DEVELOPER_ID)
                                organizationUrl.set(POM_DEVELOPER_ORGANIZATION_URL)
                                roles.set(listOf("developer"))
                            }
                        }
                    }
                }
            }

            repositories {
                maven {
                    url = if (isReleaseBuild) releaseRepositoryUrl else snapshotRepositoryUrl
                    credentials {
                        username = sonatypeUsername
                        password = sonatypePassword
                    }
                }
            }
        }

        configure<SigningExtension> {
            isRequired = isReleaseBuild && gradle.taskGraph.hasTask("publish")
            if (hasProperty("signing.inMemoryKey")) {
                val signingKey = findProperty("signing.inMemoryKey").toString().replace("\\n", "\n")
                val signingPassword = findProperty("signing.password").toString()
                val keyId = findProperty("signing.keyId").toString()
                useInMemoryPgpKeys(keyId, signingKey, signingPassword)
            }
            sign(extensions.findByType(PublishingExtension::class.java)?.publications?.get("maven"))
        }
    }

    private val Project.versionName: String
        get() = properties["VERSION_NAME"]!!.toString()

    private val Project.isReleaseBuild: Boolean
        get() = !versionName.contains("SNAPSHOT")

    private val Project.releaseRepositoryUrl: URI
        get() = URI.create(
            getPropertyOrDefault(
                "RELEASE_REPOSITORY_URL",
                "https://aws.oss.sonatype.org/service/local/staging/deploy/maven2/"
            )
        )

    private val Project.snapshotRepositoryUrl: URI
        get() = URI.create(
            getPropertyOrDefault(
                "SNAPSHOT_REPOSITORY_URL",
                "https://aws.oss.sonatype.org/content/repositories/snapshots/"
            )
        )

    private val Project.sonatypeUsername: String
        get() = getPropertyOrDefault("SONATYPE_NEXUS_USERNAME", "")

    private val Project.sonatypePassword: String
        get() = getPropertyOrDefault("SONATYPE_NEXUS_PASSWORD", "")

    private fun Project.getPropertyOrDefault(
        property: String,
        default: String
    ) = propertyString(property) ?: default

    private fun Project.propertyString(property: String) = properties[property]?.toString()
}
