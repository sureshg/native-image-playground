@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement { repositories { mavenCentral() } }

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "native-image-playground"
