package dev.suresh

import dev.zacsweers.redacted.annotations.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.*

fun main() {
  println("Hello ${Secret("testsecret")}")
  val plugins = ServiceLoader.load(Callable::class.java)
  plugins.forEach {
    println(it.call())
  }

  runBlocking {
    delay(100)
    println(KotlinVersion.CURRENT)
  }

  println(Secret::class.java.getResourceAsStream("/message.txt")?.bufferedReader()?.readText())
}

@Redacted
data class Secret(val value: String)
