import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.gradle.tasks.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  java
  application
  `test-suite-base`
  alias(libs.plugins.jgitver)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.graalvm.nativeimage)
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
val javaVersion = libs.versions.java.get()
val kotlinApi = libs.versions.kotlin.api.get()

application {
  mainClass.set("$group.MainKt")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }

  withSourcesJar()
  // withJavadocJar()
}

kotlin {
  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinApi
      languageVersion = kotlinApi
      progressiveMode = true
      enableLanguageFeature(LanguageFeature.JvmRecordSupport.name)
      enableLanguageFeature(LanguageFeature.ContextReceivers.name)
      optIn("kotlin.ExperimentalStdlibApi")
      optIn("kotlin.ExperimentalUnsignedTypes")
      optIn("kotlin.io.path.ExperimentalPathApi")
      optIn("kotlin.time.ExperimentalTime")
      optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
      optIn("kotlinx.coroutines.FlowPreview")
      optIn("kotlinx.serialization.ExperimentalSerializationApi")
    }
  }

  jvmToolchain {
    languageVersion.set(java.toolchain.languageVersion.get())
    vendor.set(java.toolchain.vendor.get())
  }
}

ksp {
  arg("autoserviceKsp.verify", "true")
  arg("autoserviceKsp.verbose", "true")
}

kapt {
  javacOptions {
    option("-Xmaxerrs", 200)
  }
}

spotless {
  java {
    googleJavaFormat()
    targetExclude("build/generated-sources/**/*.java")
    importOrder()
    removeUnusedImports()
    toggleOffOn()
    trimTrailingWhitespace()
  }

  val editorConfig = mapOf(
    "indent_size" to 2,
    "indent_style" to "space",
    "disabled_rules" to "no-wildcard-imports,filename"
  )

  kotlin {
    ktlint().setUseExperimental(true).editorConfigOverride(editorConfig)
    targetExclude("$buildDir/**/*.kt", "bin/**/*.kt", "build/generated-sources/**/*.kt")
    endWithNewline()
    indentWithSpaces()
    trimTrailingWhitespace()
    // licenseHeader(rootProject.file("gradle/license-header.txt"))
  }

  kotlinGradle {
    ktlint().setUseExperimental(true).editorConfigOverride(editorConfig)
    target("*.gradle.kts")
  }

  format("misc") {
    target("**/*.md", "**/.gitignore")
    trimTrailingWhitespace()
    endWithNewline()
  }
  // isEnforceCheck = false
}

jgitver {
  nonQualifierBranches = "main"
}

redacted {
  enabled.set(true)
}

tasks {
  withType<JavaCompile>().configureEach {
    options.apply {
      encoding = "UTF-8"
      release.set(javaVersion.toInt())
      isIncremental = true
      isFork = true
      debugOptions.debugLevel = "source,lines,vars"
      // Compiling module-info in the 'main/java' folder needs to see already compiled Kotlin code
      compilerArgs.addAll(
        listOf(
          "-Xlint:all",
          "-parameters"
          // "--patch-module",
          // "$moduleName=${sourceSets.main.get().output.asPath}"
        )
      )
    }
  }

  withType<KotlinCompile>().configureEach {
    kotlinOptions {
      verbose = true
      jvmTarget = javaVersion
      javaParameters = true
      incremental = false
      allWarningsAsErrors = false
      freeCompilerArgs += listOf(
        "-Xjsr305=strict",
        "-Xjvm-default=all",
        "-Xassertions=jvm",
        "-Xallow-result-return-type",
        "-Xemit-jvm-type-annotations",
        "-Xjspecify-annotations=strict"
        // "-Xgenerate-strict-metadata-version",
      )
    }
  }

  test {
    useJUnitPlatform()
  }

  dependencyAnalysis {
    issues {
      all {
        onAny {
          severity("warn")
        }
      }
    }
  }

  wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.ALL
  }

  defaultTasks("clean", "tasks", "--all")
}

val graal by sourceSets.creating

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation(enforcedPlatform(libs.ktor.bom))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.json.okio)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.ajalt.clikt)
  implementation(libs.ajalt.mordant)
  implementation(libs.ajalt.colormath)
  // RSocket
  implementation(libs.ktor.client.cio)
  implementation(libs.rsocket.ktor.client)
  runtimeOnly(libs.logback.classic)
  // Auto-service
  ksp(libs.ksp.auto.service)
  implementation(libs.google.auto.annotations)

  kapt(libs.graalvm.hint.processor)
  compileOnly(libs.graalvm.hint.annotations)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.junit.jupiter)

  // nativeImageCompileOnly(libs.graalvm.sdk)
  // nativeImageCompileOnly(libs.graalvm.svm)
  // nativeImageCompileOnly(libs.graalvm.svmlibffi)
}
