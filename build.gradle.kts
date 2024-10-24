import common.jvmArguments

plugins {
  dev.suresh.plugin.root
  dev.suresh.plugin.kotlin.jvm
  dev.suresh.plugin.graalvm
  application
}

application {
  mainClass = libs.versions.app.mainclass
  applicationDefaultJvmArgs += jvmArguments()
}

dependencies {
  implementation(platform(libs.ktor.bom))
  implementation(platform(applibs.helidon.bom))
  implementation(applibs.helidon.webserver)
  implementation(applibs.helidon.static)
  implementation(applibs.helidon.service)
  implementation(libs.ajalt.clikt)
  implementation(libs.ajalt.mordant)
  implementation(libs.ajalt.colormath)
  implementation(libs.ktor.client.cio)
  implementation(libs.rsocket.ktor.client)
  runtimeOnly(libs.slf4j.nop)
  graalImplementation(libs.classgraph)
}
