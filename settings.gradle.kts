pluginManagement {
  includeBuild("gradle/build-logic")
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins { id("settings.repos") }

rootProject.name = "native-image-playground"
