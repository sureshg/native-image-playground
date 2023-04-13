package dev.suresh.aot

import java.time.Instant
import java.time.ZoneId

/**
 * A config class to hold the build environment variables, which is explicitly initialized at build
 * time.
 */
object BuildEnv {

  val TIME_STAMP =
      Instant.ofEpochSecond(System.getenv("BUILD_TIMESTAMP").toLong())
          .atZone(ZoneId.systemDefault())
          .toLocalDateTime()
}
