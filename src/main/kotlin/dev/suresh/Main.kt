package dev.suresh

import com.sun.net.httpserver.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.keepalive.*
import io.rsocket.kotlin.ktor.client.*
import io.rsocket.kotlin.payload.*
import java.io.*
import java.net.*
import java.nio.charset.*
import java.security.*
import java.text.*
import java.time.*
import java.util.*
import java.util.concurrent.Executors
import javax.net.ssl.*
import kotlin.io.use
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() {
  val start = System.currentTimeMillis()
  val server =
    HttpServer.create(InetSocketAddress(9080), 0).apply {
      createContext("/") {
        println("GET: ${it.requestURI}")
        val res = summary().encodeToByteArray()
        it.sendResponseHeaders(200, res.size.toLong())
        it.responseBody.use { os -> os.write(res) }
      }
      createContext("/shutdown") {
        stop(0)
        exitProcess(0)
      }
      createContext("/rsocket", ::rSocket)
      executor = Executors.newCachedThreadPool()
      start()
    }

  val currTime = System.currentTimeMillis()
  println("Http Server started on http://localhost:${server.address.port}...")
  val vmTime = ProcessHandle.current().info().startInstant().get().toEpochMilli()
  // val vmTime = ManagementFactory.getRuntimeMXBean().startTime

  val isNativeMode = System.getProperty("org.graalvm.nativeimage.kind", "jvm") == "executable"
  val type = if (isNativeMode) "Binary" else "JVM"
  println(
    "Started in ${currTime - vmTime} ms ($type: ${start - vmTime} ms, Server: ${currTime - start} ms)."
  )
}

fun summary() = buildString {
  val rt = Runtime.getRuntime()
  appendLine("✧✧✧✧✧ Time: ${LocalDateTime.now()} ✧✧✧✧✧")
  appendLine("✧✧✧✧✧ Available Processors: ${rt.availableProcessors()} ✧✧✧✧✧")
  appendLine(
    "✧✧✧✧✧ JVM Memory, Total Allocated: ${rt.totalMemory().compactFmt}, Free: ${rt.freeMemory().compactFmt}, Max Configured: ${rt.maxMemory().compactFmt} ✧✧✧✧✧"
  )

  appendLine("✧✧✧✧✧ Processes ✧✧✧✧✧")
  val ps = ProcessHandle.allProcesses().sorted(ProcessHandle::compareTo).toList()
  ps.forEach { appendLine("${it.pid()} : ${it.info()}") }

  appendLine("✧✧✧✧✧ Trust stores ✧✧✧✧✧")
  val caCerts =
    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
      init(null as KeyStore?)
      trustManagers.filterIsInstance<X509TrustManager>().flatMap { it.acceptedIssuers.toList() }
    }
  caCerts.forEach { appendLine(it.issuerX500Principal) }

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
  env.forEach { (k: String, v: String) -> appendLine("$k : $v") }

  appendLine("✧✧✧✧✧ System Properties ✧✧✧✧✧")
  val props = System.getProperties()
  props.forEach { k: Any, v: Any -> appendLine("$k : $v") }

  val fmt = HexFormat.ofDelimiter(", ").withUpperCase().withPrefix("0x")
  appendLine("✧✧✧✧✧ I ❤️ Kotlin = ${fmt.formatHex("I ❤️ Kotlin".encodeToByteArray())}")
  appendLine("✧✧✧✧✧ LineSeparator  = ${fmt.formatHex(System.lineSeparator().encodeToByteArray())}")
  appendLine("✧✧✧✧✧ File PathSeparator = ${fmt.formatHex(File.pathSeparator.encodeToByteArray())}")

  appendLine("✧✧✧✧✧ Additional info in exception ✧✧✧✧✧")
  val ex =
    runCatching {
        Security.setProperty("jdk.includeInExceptions", "hostInfo,jar")
        Socket().use { s ->
          s.soTimeout = 100
          s.connect(InetSocketAddress("localhost", 12345), 100)
        }
      }
      .exceptionOrNull()
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
    """.trimIndent(
    )
  )
}

val rsClient by lazy {
  HttpClient(CIO) {
    install(WebSockets) // rsocket requires websockets plugin installed
    install(RSocketSupport) {
      // configure rSocket connector (all values have defaults)
      connector {
        maxFragmentSize = 1024

        connectionConfig {
          keepAlive = KeepAlive(interval = 30.seconds, maxLifetime = 2.minutes)

          // payload for setup frame
          setupPayload { buildPayload { data("""{ "data": "setup" }""") } }

          // mime types
          payloadMimeType =
            PayloadMimeType(
              data = WellKnownMimeType.ApplicationJson,
              metadata = WellKnownMimeType.MessageRSocketCompositeMetadata
            )
        }
      }
    }
  }
}

fun rSocket(ex: HttpExchange) {
  println("Starting new rSocket connection!")
  runBlocking {
    val rSocket: RSocket = rsClient.rSocket("wss://demo.rsocket.io/rsocket")

    // request stream
    val stream: Flow<Payload> =
      rSocket.requestStream(buildPayload { data("""{ "data": "Kotlin rSocket!" }""") })

    val headers = ex.responseHeaders
    headers["Content-Type"] = "text/event-stream"
    headers["Cache-Control"] = "no-cache"
    // Chunked transfer encoding will be set if response length is zero.
    // headers["Transfer-encoding"] = "chunked"
    // Streaming binary response
    // headers["Content-Type"] = "application/octet-stream"
    ex.sendResponseHeaders(200, 0)

    ex.responseBody.buffered().use { os ->
      stream.take(10).flowOn(Dispatchers.IO).collect { payload ->
        os.write(payload.data.readBytes())
        os.write("\n".encodeToByteArray())
        os.flush()
      }
    }
  }
}

private val Int.fmt
  get() = "%-5d".format(this)

val Long.compactFmt: String
  get() = NumberFormat.getCompactNumberInstance().format(this)
