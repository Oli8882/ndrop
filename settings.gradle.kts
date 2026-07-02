// NDrop — Gradle Settings
// Signature: Olii-8882
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // MapLibre Compose (rallista) is published to Maven Central
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "NDrop"
include(":app")
