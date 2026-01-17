pluginManagement {
    repositories {
        google {
            mavenContent { includeGroupByRegex("com\\.android.*") }
            mavenContent { includeGroupByRegex("com\\.google.*") }
            mavenContent { includeGroupByRegex("androidx.*") }
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
    }
}

rootProject.name = "VisionairRadioPlayer"
include(":app")
