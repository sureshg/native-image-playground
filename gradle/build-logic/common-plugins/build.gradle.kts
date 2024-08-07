import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  alias(libs.plugins.jte)
  alias(libs.plugins.bestpractices)
}

/**
 * Java version used in Java toolchains and Kotlin compile JVM target for Gradle precompiled script
 * plugins.
 */
val dslJavaVersion = libs.versions.kotlin.dsl.jvmtarget

// java { toolchain { languageVersion = dslJavaVersion.map(JavaLanguageVersion::of) } }

tasks {
  withType<KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget = dslJavaVersion.map(JvmTarget::fromTarget)
      freeCompilerArgs.addAll("-Xcontext-receivers")
    }
  }
}

kotlin {
  sourceSets.all {
    languageSettings.apply {
      optIn("kotlin.ExperimentalStdlibApi")
      optIn("kotlin.io.path.ExperimentalPathApi")
      optIn("kotlin.time.ExperimentalTime")
      optIn("org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi")
      optIn("org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl")
      optIn("org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDceDsl")
    }
  }
}

gradlePlugin {
  plugins {

    // Re-exposure of plugin from dependency. Gradle doesn't expose the plugin itself.
    create("com.gradle.develocity") {
      id = "com.gradle.develocity"
      implementationClass = "com.gradle.develocity.agent.gradle.DevelocityPlugin"
      displayName = "Develocity Gradle Plugin"
      description = "Develocity gradle settings plugin re-exposed from dependency"
    }

    // Uncomment the id to change plugin id for this pre-compiled plugin
    named("plugins.common") {
      // id = "build.plugins.common"
      displayName = "Common build-logic plugin"
      description = "Common pre-compiled script plugin"
      tags = listOf("Common Plugin", "build-logic")
    }

    // val settingsPlugin by creating {}
  }
}

// Jte is used for generating build config.
jte {
  contentType = gg.jte.ContentType.Plain
  sourceDirectory = sourceSets.main.get().resources.srcDirs.firstOrNull()?.toPath()
  generate()
  // jteExtension("gg.jte.models.generator.ModelExtension")
  // jteExtension("gg.jte.nativeimage.NativeResourcesExtension")
  // binaryStaticContent = true
}

dependencies {
  implementation(platform(libs.kotlin.bom))
  implementation(kotlin("stdlib"))
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(libs.ajalt.mordant.coroutines)
  implementation(libs.jte.runtime)
  implementation(libs.build.zip.prefixer)
  // jteGenerate(libs.jte.models)
  // compileOnly(libs.jte.kotlin)

  implementation(libs.build.kotlin)
  // OR implementation(kotlin("gradle-plugin"))
  implementation(libs.build.kotlin.ksp)
  implementation(libs.build.kotlinx.atomicfu)
  implementation(libs.build.kotlinx.serialization)
  implementation(libs.build.kotlinx.kover)
  implementation(libs.build.dokka)
  implementation(libs.build.ksp.redacted)
  implementation(libs.build.gradle.develocity)
  implementation(libs.build.nexus.plugin)
  implementation(libs.build.spotless.plugin)
  implementation(libs.build.shadow.plugin)
  implementation(libs.build.semver.plugin)
  implementation(libs.build.benmanesversions)
  implementation(libs.build.dependencyanalysis)
  implementation(libs.build.foojay.resolver)
  implementation(libs.build.nativeimage.plugin)
  testImplementation(gradleTestKit())

  // implementation(libs.build.jte.plugin)
  // implementation(libs.build.includegit.plugin)
}
