@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_SETTINGS
  versionCatalogs { register("applibs") { from(files("gradle/libs.versions.toml")) } }
}

plugins {
  val testVersion = extra["test.version"].toString()
  id("dev.suresh.plugin.repos") version testVersion
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "native-image-playground"
