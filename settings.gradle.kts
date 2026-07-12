import org.gradle.authentication.http.BasicAuthentication

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
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                // Secret token (scope: DOWNLOADS:READ) from https://account.mapbox.com/access-tokens/
                // Set this in ~/.gradle/gradle.properties (NOT this repo's gradle.properties) as:
                // MAPBOX_DOWNLOADS_TOKEN=sk.xxxxx
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").getOrElse("")
            }
        }
    }
}

rootProject.name = "ShadowMap"
include(":app")
