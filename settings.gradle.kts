pluginManagement {
    includeBuild("build-logic")
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
    // Downloads the JDK named in gradle/gradle-daemon-jvm.properties and the Kotlin toolchain when it is missing.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Hemisphere"

// The module list in docs/architecture.md §2. The module-graph check (build-logic) holds the allowed edges.
include(":app")
include(":core:model")
include(":core:designsystem")
include(":core:input")
include(":core:platform")
include(":core:domain")
include(":core:fs")
include(":core:jobs")
include(":core:data")
include(":core:testing")
include(":feature:browser")
include(":feature:jobs")
include(":feature:settings")
include(":feature:trash")
