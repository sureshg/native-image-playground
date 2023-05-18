package dev.suresh.config

import java.time.ZonedDateTime

/**
 * A config class to hold the build environment variables, which is explicitly initialized at build
 * time.
 */
object BuildEnv {

  val BUILD_NUMBER = System.getenv().getOrElse("BUILD_NUMBER") { "0.0.0" }

  val TIME_STAMP = ZonedDateTime.now().toLocalDateTime()
}
