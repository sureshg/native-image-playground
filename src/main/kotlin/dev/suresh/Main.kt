package dev.suresh

import com.sun.net.httpserver.*
import java.io.*
import java.net.*
import java.nio.charset.*
import java.security.*
import java.time.*
import java.util.*
import javax.net.ssl.*

fun main() {
  val start = System.currentTimeMillis()
  val server = HttpServer.create(InetSocketAddress(80), 0).apply {
    createContext("/") {
      println("GET: ${it.requestURI}")
      val res = summary().encodeToByteArray()
      it.sendResponseHeaders(200, res.size.toLong())
      it.responseBody.use { os -> os.write(res) }
    }
    createContext("/shutdown") { stop(0) }
    start()
  }

  val currTime = System.currentTimeMillis()
  println("Http Server started on http://localhost:${server.address.port}...")
  val vmTime = ProcessHandle.current().info().startInstant().get().toEpochMilli()
  // val vmTime = ManagementFactory.getRuntimeMXBean().startTime

  val isNativeMode = System.getProperty("org.graalvm.nativeimage.kind", "jvm") == "executable"
  val type = if (isNativeMode) "Binary" else "JVM"
  println("Started in ${currTime - vmTime} ms ($type: ${start - vmTime} ms, Server: ${currTime - start} ms).")
}

fun summary() = buildString {
  val rt = Runtime.getRuntime()
  appendLine("✧✧✧✧✧ Time: ${LocalDateTime.now()} ✧✧✧✧✧")
  appendLine("✧✧✧✧✧ Available Processors: ${rt.availableProcessors()} ✧✧✧✧✧")
  appendLine("✧✧✧✧✧ JVM Memory, Total Allocated: ${rt.totalMemory().gibiByte}, Free: ${rt.freeMemory().gibiByte}, Max Configured: ${rt.maxMemory().gibiByte} ✧✧✧✧✧")

  appendLine("✧✧✧✧✧ Processes ✧✧✧✧✧")
  val ps = ProcessHandle.allProcesses().sorted(ProcessHandle::compareTo).toList()
  ps.forEach {
    appendLine("${it.pid()} : ${it.info()}")
  }

  appendLine("✧✧✧✧✧ Trust stores ✧✧✧✧✧")
  val caCerts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
    init(null as KeyStore?)
    trustManagers.filterIsInstance<X509TrustManager>().flatMap { it.acceptedIssuers.toList() }
  }
  caCerts.forEach {
    appendLine(it.issuerX500Principal)
  }

  appendLine("✧✧✧✧✧ Dns Resolution ✧✧✧✧✧")
  val dns = InetAddress.getAllByName("google.com").toList()
  dns.forEach { appendLine(it) }


  appendLine("✧✧✧✧✧ TimeZones ✧✧✧✧✧")
  val tz = ZoneId.getAvailableZoneIds()
  tz.forEach { appendLine(it) }

  appendLine("✧✧✧✧✧ Charsets ✧✧✧✧✧")
  val cs = Charset.availableCharsets()
  cs.forEach { (name, charset) -> appendLine("$name: $charset") }

  appendLine("✧✧✧✧✧ System Locales ✧✧✧✧✧")
  val locales = Locale.getAvailableLocales()
  locales.forEach { appendLine(it) }

  appendLine("✧✧✧✧✧ System Countries ✧✧✧✧✧")
  val countries = Locale.getISOCountries()
  countries.forEach { appendLine(it) }

  appendLine("✧✧✧✧✧ System Currencies ✧✧✧✧✧")
  val currencies = Currency.getAvailableCurrencies()
  currencies.forEach { appendLine(it) }

  appendLine("✧✧✧✧✧ System Languages ✧✧✧✧✧")
  val languages = Locale.getISOLanguages()
  languages.forEach { appendLine(it) }

  appendLine("✧✧✧✧✧ Env Variables ✧✧✧✧✧")
  val env = System.getenv()
  env.forEach { (k: String, v: String) ->
    appendLine("$k : $v")
  }

  appendLine("✧✧✧✧✧ System Properties ✧✧✧✧✧")
  val props = System.getProperties()
  props.forEach { k: Any, v: Any ->
    appendLine("$k : $v")
  }

  val fmt = HexFormat.ofDelimiter(", ").withUpperCase().withPrefix("0x")
  appendLine("✧✧✧✧✧ I ❤️ Kotlin = ${fmt.formatHex("I ❤️ Kotlin".encodeToByteArray())}")
  appendLine("✧✧✧✧✧ LineSeparator  = ${fmt.formatHex(System.lineSeparator().encodeToByteArray())}")
  appendLine("✧✧✧✧✧ File PathSeparator = ${fmt.formatHex(File.pathSeparator.encodeToByteArray())}")

  appendLine("✧✧✧✧✧ Additional info in exception ✧✧✧✧✧")
  val ex = runCatching {
    Security.setProperty("jdk.includeInExceptions", "hostInfo,jar")
    Socket().use { s ->
      s.soTimeout = 100
      s.connect(InetSocketAddress("localhost", 12345), 100)
    }
  }.exceptionOrNull()
  appendLine(ex?.message)
  // ex?.message?.contains("localhost:12345")

  appendLine(
    """
    +---------Summary-------+
    | Processes      : ${ps.size.fmt}|
    | Dns Addresses  : ${dns.size.fmt}|
    | Trust Stores   : ${caCerts.size.fmt}|
    | TimeZones      : ${tz.size.fmt}|
    | CharSets       : ${cs.size.fmt}|
    | Locales        : ${locales.size.fmt}|
    | Countries      : ${countries.size.fmt}|
    | Languages      : ${languages.size.fmt}|
    | Currencies     : ${currencies.size.fmt}|
    | Env Vars       : ${env.size.fmt}|
    | Sys Props      : ${props.size.fmt}|
    +-----------------------+
    """.trimIndent()
  )
}

private val Int.fmt get() = "%-5d".format(this)

val Long.gibiByte get() = "%.2f GiB".format(this / (1024f * 1024 * 1024))


