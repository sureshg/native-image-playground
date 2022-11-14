package dev.suresh.service

import com.google.auto.service.*
import java.util.concurrent.*

@AutoService(Callable::class)
class KotlinPlugin : Callable<String> {
  override fun call() = "Kotlin ${KotlinVersion.CURRENT}"
}

@AutoService(Callable::class)
class JavaPlugin : Callable<String> {
  override fun call() = "Java ${System.getProperty("java.version")}"
}
