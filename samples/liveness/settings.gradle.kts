pluginManagement {
    includeBuild("../../build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Liveness-Sample"
include(":app")

// Use the local liveness module from this repo so edits to liveness/ are picked up.
// Composite-build substitution swaps com.amplifyframework.ui:liveness for project(":liveness").
includeBuild("../../") {}
