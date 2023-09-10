plugins { plugins.graalvm }

dependencies {
  implementation(platform(libs.ktor.bom))
  implementation(platform(libs.helidon.bom))
  implementation(libs.helidon.webserver)
  implementation(libs.helidon.static)
  implementation(libs.helidon.service)
  implementation(libs.ajalt.clikt)
  implementation(libs.ajalt.mordant)
  implementation(libs.ajalt.colormath)
  // RSocket
  implementation(libs.ktor.client.cio)
  implementation(libs.rsocket.ktor.client)
  runtimeOnly(libs.slf4j.nop)
}
