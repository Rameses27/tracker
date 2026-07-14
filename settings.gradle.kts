pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Match Gradle 8.4 with AGP 8.4.x
        id("com.android.application") version "8.4.0"
        id("com.android.library") version "8.4.0"
        // Kotlin plugin for Android (adjust version if your project targets a different Kotlin)
        id("org.jetbrains.kotlin.android") version "1.9.22"
        // Google services plugin (if used in modules)
        id("com.google.gms.google-services") version "4.4.0"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ExpenseTracker"
include(":app")
