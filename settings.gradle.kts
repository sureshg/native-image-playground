pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }

  val testVersion = extra["test.version"].toString()
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("dev.suresh.plugin")) {
        useModule("dev.suresh.build:plugins:$testVersion")
      }
    }
  }
}

dependencyResolutionManagement {
  versionCatalogs { register("applibs") { from(files("gradle/libs.versions.toml")) } }
}

plugins { id("dev.suresh.plugin.repos") }

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "native-image-playground"
