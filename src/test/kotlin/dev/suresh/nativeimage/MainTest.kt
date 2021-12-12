package dev.suresh.nativeimage

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

internal class MainTest {

  @Test
  fun main() {
    assertEquals("1.6.10", KotlinVersion.CURRENT.toString())
  }
}
