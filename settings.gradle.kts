@file:Suppress("UnstableApiUsage")

pluginManagement {
  val buildPluginVer =
      settingsDir
          .resolve("gradle/libs.versions.toml")
          .readLines()
          .first { it.contains("bc-plugins") }
          .split("\"")[1]
          .trim()

  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("dev.suresh.plugin")) {
        useVersion(buildPluginVer)
      }
    }
  }

  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins { id("dev.suresh.plugin.repos") }

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "native-image-playground"
