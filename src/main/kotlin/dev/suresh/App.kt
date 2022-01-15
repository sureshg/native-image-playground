package dev.suresh

import dev.zacsweers.redacted.annotations.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.net.*
import java.util.*
import java.util.concurrent.*

fun main() {
  println("Hello ${Secret("testsecret")}")

  val plugins = ServiceLoader.load(Callable::class.java)
  plugins.forEach {
    println(it.call())
  }

  // Load plugins from current directory
  val loader = URLClassLoader.newInstance(arrayOf(URL("file://${System.getProperty("user.dir")}/plugins.jar")))
  val svcLoader = ServiceLoader.load(Runnable::class.java,loader)
  println("Found ${svcLoader.toList().size} plugins!")

  runBlocking {
    delay(100)
    println(KotlinVersion.CURRENT)
  }

  println(Secret::class.java.getResourceAsStream("/message.txt")?.bufferedReader()?.readText())
}

@Redacted
data class Secret(val value: String)
