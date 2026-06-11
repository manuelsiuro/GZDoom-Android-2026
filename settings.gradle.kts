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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GZDoom-Android"
include(":doom")
include(":touchcontrols")

// PNG->WAD generator SDK (native libpng2wad.so + Png2WadConverter). Source lives
// in the sibling png2wad repo; included here so the map editor can generate maps
// and launch them in-engine for end-to-end testing.
include(":png2wad-sdk")
project(":png2wad-sdk").projectDir = file("../png2wad/android")
