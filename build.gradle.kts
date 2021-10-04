import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.gradle.tasks.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  java
  application
  alias(libs.plugins.google.ksp)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.graalvm.nativeimage)
  alias(libs.plugins.benmanes)
  alias(libs.plugins.spotless)
  alias(libs.plugins.ksp.redacted)
  alias(libs.plugins.jgitver)
  alias(libs.plugins.jreleaser)
}

group = "dev.suresh"

val javaVersion = libs.versions.java.get()
val kotlinApi = libs.versions.kotlin.api.get()
val gjfVersion = libs.versions.google.javaformat.get()
val ktlintVersion = libs.versions.ktlint.get()

java {
  withSourcesJar()
  withJavadocJar()

  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
    vendor.set(JvmVendorSpec.GRAAL_VM)
  }
}

kotlin {
  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinApi
      languageVersion = kotlinApi
      progressiveMode = true
      enableLanguageFeature(LanguageFeature.JvmRecordSupport.name)
      optIn("kotlin.RequiresOptIn")
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
    (this as JavaToolchainSpec).apply {
      languageVersion.set(JavaLanguageVersion.of(javaVersion))
      vendor.set(java.toolchain.vendor.get())
    }
  }
}

spotless {
  java {
    googleJavaFormat(gjfVersion)
    // Exclude sealed types until it supports.
    targetExclude("**/ResultType.java", "build/generated-sources/**/*.java")
    importOrder()
    removeUnusedImports()
    toggleOffOn()
    trimTrailingWhitespace()
  }

  val ktlintConfig = mapOf(
    "disabled_rules" to "no-wildcard-imports",
    "insert_final_newline" to "true",
    "end_of_line" to "lf",
    "indent_size" to "2",
  )

  kotlin {
    ktlint(ktlintVersion).userData(ktlintConfig)
    targetExclude("$buildDir/**/*.kt", "bin/**/*.kt", "build/generated-sources/**/*.kt")
    endWithNewline()
    indentWithSpaces()
    trimTrailingWhitespace()
    // licenseHeader(rootProject.file("gradle/license-header.txt"))
  }

  kotlinGradle {
    ktlint(ktlintVersion).userData(ktlintConfig)
    target("*.gradle.kts")
  }

  format("misc") {
    target("**/*.md", "**/.gitignore")
    trimTrailingWhitespace()
    endWithNewline()
  }
}

jgitver {
  nonQualifierBranches = "main"
}

redacted {
  redactedAnnotation.set("Redacted")
  enabled.set(true)
}

tasks {
  withType<JavaCompile>().configureEach {
    options.apply {
      encoding = "UTF-8"
      release.set(javaVersion.toInt())
      isIncremental = true
      isFork = true
      compilerArgs.addAll(
        listOf("-Xlint:all", "-parameters")
      )
    }
  }

  withType<KotlinCompile>().configureEach {
    usePreciseJavaTracking = true
    kotlinOptions {
      verbose = true
      jvmTarget = javaVersion
      javaParameters = true
      incremental = true
      allWarningsAsErrors = false
      freeCompilerArgs += listOf(
        "-progressive",
        "-Xjsr305=strict",
        "-Xjvm-default=enable",
        "-Xassertions=jvm",
        "-Xallow-result-return-type",
        "-Xstrict-java-nullability-assertions",
        "-Xgenerate-strict-metadata-version",
        "-Xemit-jvm-type-annotations",
      )
    }
  }

  test {
    useJUnitPlatform()
  }

  wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.BIN
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  // implementation("org.graalvm.sdk:graal-sdk:21.2.0")
  // implementation("org.graalvm.nativeimage:svm:21.2.0")
  // implementation("org.graalvm.nativeimage:svm-libffi:21.2.0")
}
