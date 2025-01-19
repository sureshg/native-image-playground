@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    // Version catalog for build plugins
    register("blibs") {
      from(files("gradle/libs.versions.toml"))
      // from("dev.suresh.build:catalog:+")
      // version("java", "...")
    }
  }
}

plugins {
  val testVersion = extra["test.version"].toString()
  id("dev.suresh.plugin.repos") version testVersion
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "native-image-playground"
