rootProject.name = "native-image-playground"

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}
