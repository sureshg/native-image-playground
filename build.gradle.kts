plugins { plugins.graalvm }

dependencies {
  implementation(platform(libs.ktor.bom))
  implementation(platform(libs.helidon.nima.bom))
  implementation(libs.helidon.nima.webserver)
  implementation(libs.helidon.nima.static)
  implementation(libs.helidon.nima.service)
  implementation(libs.ajalt.clikt)
  implementation(libs.ajalt.mordant)
  implementation(libs.ajalt.colormath)
  // RSocket
  implementation(libs.ktor.client.cio)
  implementation(libs.rsocket.ktor.client)
  runtimeOnly(libs.slf4j.nop)
}
