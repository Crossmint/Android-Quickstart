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
        maven("https://maven.crossmint.com/releases") {
            content { includeGroup("com.crossmint") }
        }
        maven("https://maven.crossmint.com/snapshots") {
            mavenContent {
                includeGroup("com.crossmint")
                snapshotsOnly()
            }
        }
    }
}

rootProject.name = "Crossmint Quickstart"
include(":app")
include(":checkout")
