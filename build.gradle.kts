@file:Suppress("UnstableApiUsage")

import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.toolchain.JvmVendorSpec.GRAAL_VM
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom

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

// Check if debug property is enabled
val debugEnabled by lazy {
  val debug: String? by project
  debug.toBoolean()
}

// Check if quickBuild property is enabled
val quickBuildEnabled by lazy {
  val quick: String? by project
  quick.toBoolean()
}

application {
  mainClass = "$group.MainKt"
  applicationDefaultJvmArgs +=
      listOf("--show-version", "--enable-preview", "--add-modules=ALL-SYSTEM")
}

java {
  toolchain {
    languageVersion = javaVersion.map { JavaLanguageVersion.of(it.majorVersion) }
    vendor = GRAAL_VM
  }

  withSourcesJar()
  withJavadocJar()
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
    languageVersion = java.toolchain.languageVersion.get()
    vendor = java.toolchain.vendor.get()
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

redacted { enabled = true }

graalvmNative {
  binaries {
    named("main") {
      imageName = project.name
      mainClass = application.mainClass
      useFatJar = false
      sharedLibrary = false
      verbose = false
      fallback = false
      quickBuild = false
      buildArgs = buildList {
        add("--enable-preview")
        add("--native-image-info")
        add("--enable-monitoring=heapdump,jfr,jvmstat")
        add("--install-exit-handlers")
        add("--features=dev.suresh.aot.RuntimeFeature")
        add("-march=native")
        add("-R:MaxHeapSize=64m")
        add("-H:+ReportExceptionStackTraces")
        add("-EBUILD_NUMBER=${project.version}")
        // add("-H:IncludeResources=.*(message\\.txt|\\app.properties)\$")

        if (OperatingSystem.current().isLinux) {
          add("-H:+StaticExecutableWithDynamicLibC")
          add("-H:+StripDebugInfo")
        }

        if (quickBuildEnabled) {
          add("-Ob")
        }

        if (debugEnabled) {
          add("-H:+TraceNativeToolUsage")
        }
      }
      jvmArgs = listOf("--add-modules=ALL-SYSTEM")
      systemProperties = mapOf("java.awt.headless" to "false")
      resources { autodetect() }
    }
  }
  metadataRepository { enabled = true }
  toolchainDetection = false
}

tasks {
  withType<JavaCompile>().configureEach {
    options.apply {
      encoding = "UTF-8"
      release = javaVersion.map { it.majorVersion.toInt() }
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
      jvmTarget = kotlinJvmTarget
      apiVersion = kotlinApiVersion
      languageVersion = kotlinLangVersion
      verbose = true
      javaParameters = true
      allWarningsAsErrors = false
      suppressWarnings = false
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

  shadowJar { mergeServiceFiles() }

  test { useJUnitPlatform() }

  dependencyAnalysis { issues { this.all { onAny { severity("warn") } } } }

  wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.ALL
  }

  defaultTasks("clean", "tasks", "--all")
}

/**
 * Creates a custom sourceset(`graal`) for GraalVM native image build time configurations. The
 * following configurations will
 * - Creates a `graal` source set.
 * - Add `main` output to `graal` compile and runtime classpath.
 * - Add `main` dependencies to `graal` compile and runtime classpath.
 * - Add `graal` dependencies (graalImplementation) to native-image classpath.
 * - Add `graal` output to native-image classpath.
 *
 * For each source set added to the project, the Java plugins add a few
 * [dependency configurations](https://docs.gradle.org/current/userguide/java_plugin.html#java_source_set_configurations)
 * - graalImplementation
 * - graalCompileOnly
 * - graalRuntimeOnly
 * - graalCompileClasspath (CompileOnly + Implementation)
 * - graalRuntimeClasspath (RuntimeOnly + Implementation)
 *
 * [Configure Custom
 * SourceSet](https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests)
 */
val graal by
    sourceSets.creating {
      compileClasspath += sourceSets.main.get().output
      runtimeClasspath += sourceSets.main.get().output
    }

configurations {
  val graalImplementation by existing
  val graalRuntimeOnly by existing

  // graalImplementation extendsFrom main source set implementation
  graalImplementation.extendsFrom(implementation)
  graalRuntimeOnly.extendsFrom(runtimeOnly)

  // Finally, nativeImage classpath extendsFrom graalImplementation
  // This way all main + graal dependencies are also available at native image build time.
  nativeImageClasspath.extendsFrom(graalImplementation)
}

dependencies {
  implementation(platform(libs.kotlin.bom))
  implementation(platform(libs.ktor.bom))
  implementation(platform(libs.helidon.nima.bom))
  implementation(kotlin("stdlib"))
  implementation(libs.helidon.nima.webserver)
  implementation(libs.helidon.nima.static)
  implementation(libs.helidon.nima.service)
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

  // Dependencies required for native-image build. Use "graalCompileOnly" for compile only deps.
  // "graalCompileOnly"(libs.graalvm.sdk)
  "graalImplementation"(libs.classgraph)
  nativeImageCompileOnly(graal.output)

  // kapt(libs.graalvm.hint.processor)
  // compileOnly(libs.graalvm.hint.annotations)
}
