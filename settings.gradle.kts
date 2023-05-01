pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

    // Set the prefix used for dependencies declared in gradle/libs.versions.toml
    defaultLibrariesExtensionName.set("dependency")
}

rootProject.name = "amplify-ui-android"
include(":liveness")
include(":authenticator")
include(":testing")
include(":authenticator-screenshots")

// Enable typesafe accessor generation for cross-project references
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
