@file:Suppress("UnstableApiUsage", "DSL_SCOPE_VIOLATION")

import org.gradle.jvm.toolchain.JvmVendorSpec.GRAAL_VM
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  java
  application
  `test-suite-base`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.graalvm.nativeimage)
  alias(libs.plugins.semver)
  alias(libs.plugins.benmanes)
  alias(libs.plugins.shadow)
  alias(libs.plugins.spotless)
  alias(libs.plugins.ksp.redacted)
  alias(libs.plugins.versioncatalog.update)
  alias(libs.plugins.dependency.analysis)
  alias(libs.plugins.ksp.powerassert)
  alias(libs.plugins.champeau.includegit) apply false
}

group = "dev.suresh"

val moduleName = "$group.nativeimage"
val javaVersion = libs.versions.java.map { JavaVersion.toVersion(it) }
val kotlinJvmTarget = libs.versions.kotlin.jvm.target.map { JvmTarget.fromTarget(it) }
val kotlinApiVersion = libs.versions.kotlin.api.version.map { KotlinVersion.fromVersion(it) }
val kotlinLangVersion = libs.versions.kotlin.lang.version.map { KotlinVersion.fromVersion(it) }

application { mainClass.set("$group.MainKt") }

java {
  toolchain {
    languageVersion.set(javaVersion.map { JavaLanguageVersion.of(it.majorVersion) })
    vendor.set(GRAAL_VM)
  }

  withSourcesJar()
  // withJavadocJar()
}

kotlin {
  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinApiVersion.get().version
      languageVersion = kotlinLangVersion.get().version
      progressiveMode = true
      optIn("kotlin.ExperimentalStdlibApi")
      optIn("kotlin.ExperimentalUnsignedTypes")
      optIn("kotlin.io.path.ExperimentalPathApi")
      optIn("kotlin.time.ExperimentalTime")
      optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
      optIn("kotlinx.coroutines.FlowPreview")
      optIn("kotlinx.serialization.ExperimentalSerializationApi")
    }
    // kotlin.setSrcDirs(listOf("src/kotlin"))
  }

  jvmToolchain {
    languageVersion.set(java.toolchain.languageVersion.get())
    vendor.set(java.toolchain.vendor.get())
  }

  kotlinDaemonJvmArgs = listOf("--show-version", "--enable-preview")
}

ksp {
  arg("autoserviceKsp.verify", "true")
  arg("autoserviceKsp.verbose", "true")
}

spotless {
  java {
    googleJavaFormat()
    target("**/*.java")
    targetExclude("**/build/**", "**/.gradle/**")
  }
  // if(plugins.hasPlugin(JavaPlugin::class.java)){ }

  kotlin {
    ktfmt(libs.versions.ktfmt.get())
    target("**/*.kt")
    trimTrailingWhitespace()
    endWithNewline()
    targetExclude("**/build/**", "**/.gradle/**")
    // licenseHeader(rootProject.file("gradle/license-header.txt"))
  }

  kotlinGradle {
    ktfmt(libs.versions.ktfmt.get())
    target("**/*.kts")
    trimTrailingWhitespace()
    endWithNewline()
    targetExclude("**/build/**")
  }

  format("misc") {
    target("**/*.md", "**/.gitignore")
    trimTrailingWhitespace()
    indentWithSpaces(2)
    endWithNewline()
  }
  isEnforceCheck = true
}

redacted { enabled.set(true) }

tasks {
  withType<JavaCompile>().configureEach {
    options.apply {
      encoding = "UTF-8"
      release.set(javaVersion.map { it.majorVersion.toInt() })
      isIncremental = true
      isFork = true
      debugOptions.debugLevel = "source,lines,vars"
      compilerArgs.addAll(
          listOf(
              "--enable-preview",
              "-Xlint:all",
              "-parameters",
          ))
    }
  }
  withType<KotlinCompile>().configureEach {
    usePreciseJavaTracking = true
    compilerOptions {
      jvmTarget.set(kotlinJvmTarget)
      apiVersion.set(kotlinApiVersion)
      languageVersion.set(kotlinLangVersion)
      verbose.set(true)
      javaParameters.set(true)
      allWarningsAsErrors.set(false)
      suppressWarnings.set(false)
      freeCompilerArgs.addAll(
          "-Xjsr305=strict",
          "-Xjvm-default=all",
          "-Xassertions=jvm",
          "-Xallow-result-return-type",
          "-Xemit-jvm-type-annotations",
          "-Xjspecify-annotations=strict",
      )
    }

    finalizedBy("spotlessApply")
  }

  test { useJUnitPlatform() }

  dependencyAnalysis { issues { all { onAny { severity("warn") } } } }

  wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.ALL
  }

  defaultTasks("clean", "tasks", "--all")
}

val graal by sourceSets.creating

dependencies {
  implementation(kotlin("stdlib"))
  implementation(platform(libs.ktor.bom))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.json.okio)
  implementation(libs.kotlinx.datetime)
  implementation(libs.ajalt.clikt)
  implementation(libs.ajalt.mordant)
  implementation(libs.ajalt.colormath)
  // RSocket
  implementation(libs.ktor.client.cio)
  implementation(libs.rsocket.ktor.client)
  runtimeOnly(libs.slf4j.nop)
  // Auto-service
  ksp(libs.ksp.auto.service)
  implementation(libs.google.auto.annotations)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.junit.jupiter)

  // kapt(libs.graalvm.hint.processor)
  // compileOnly(libs.graalvm.hint.annotations)
  // nativeImageCompileOnly(libs.graalvm.sdk)
  // "graalCompileOnly"(libs.graalvm.sdk) // For graal source set
}
