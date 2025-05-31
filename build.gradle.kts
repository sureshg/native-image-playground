import common.*

plugins {
  id("dev.suresh.plugin.root")
  id("dev.suresh.plugin.kotlin.jvm")
  id("dev.suresh.plugin.graalvm")
  application
}

application {
  mainClass = libs.versions.app.mainclass
  applicationDefaultJvmArgs += runJvmArgs
}

dependencies {
  implementation(platform(libs.ktor.bom))
  implementation(platform(libs.helidon.bom))
  implementation(libs.helidon.webserver)
  implementation(libs.helidon.static)
  implementation(libs.helidon.service)
  implementation(libs.ajalt.clikt)
  implementation(libs.ajalt.mordant)
  implementation(libs.ajalt.colormath)
  implementation(libs.ktor.client.cio)
  implementation(libs.rsocket.ktor.client)
  runtimeOnly(libs.slf4j.nop)
  graalImplementation(libs.classgraph)
}
