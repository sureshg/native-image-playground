import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
  java
  application
  id("com.google.devtools.ksp") version "1.5.31-1.0.0"
  kotlin("jvm") version "1.5.31"
  kotlin("plugin.serialization") version "1.5.31"
  id("org.graalvm.buildtools.native") version "0.9.5"
  id("com.github.ben-manes.versions") version "0.39.0"
  id("com.diffplug.spotless") version "5.16.0"
  id("dev.zacsweers.redacted") version "0.8.0"
  id("fr.brouillard.oss.gradle.jgitver") version "0.10.0-rc03"
  id("org.jreleaser") version "0.7.0"
}

group = "dev.suresh"

val javaVersion = 16
val gjfVersion = "1.11.0"
val ktlintVersion = "0.42.1"

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
      apiVersion = "1.5"
      languageVersion = "1.5"
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
      languageVersion.set(java.toolchain.languageVersion.get())
      vendor.set(java.toolchain.vendor.get())
    }
  }
}

// Formatting
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
  // isEnforceCheck = false
}

jgitver {
  useSnapshot = true
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
      release.set(javaVersion)
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
      jvmTarget = javaVersion.toString()
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
    gradleVersion = "7.2"
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
