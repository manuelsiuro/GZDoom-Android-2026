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

// PNG->WAD generator SDK (native libpng2wad.so + Png2WadConverter). Vendored under
// png2wad-sdk/ so the map editor can generate maps and launch them in-engine without
// depending on a sibling checkout. Upstream sources: github.com/akaAgar/png2wad (GPLv3).
include(":png2wad-sdk")
project(":png2wad-sdk").projectDir = file("png2wad-sdk")
