package dev.suresh

import com.sun.management.OperatingSystemMXBean
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.suresh.model.Creds
import dev.suresh.model.Secret
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.WellKnownMimeType
import io.rsocket.kotlin.keepalive.KeepAlive
import io.rsocket.kotlin.ktor.client.RSocketSupport
import io.rsocket.kotlin.ktor.client.rSocket
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.PayloadMimeType
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import java.io.File
import java.lang.System.Logger.Level.INFO
import java.lang.management.ManagementFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.StandardSocketOptions
import java.net.URI
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.Security
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.io.use
import kotlin.jvm.optionals.getOrDefault
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.graalvm.nativeimage.ImageInfo

val logger = System.getLogger("Main")
val vtExec = Executors.newVirtualThreadPerTaskExecutor()
val viDispatcher = vtExec.asCoroutineDispatcher()
// val REQ_URI = ScopedValue.newInstance<String>()

fun main(args: Array<String>) {
  logger.log(INFO) { "This is from system logger!" }

  val debug = args.contains("--debug")
  val start = System.currentTimeMillis()
  val server =
      HttpServer.create(InetSocketAddress(9080), 0).apply {
        createContext("/") {
          val req = it.requestURI.toString()
          println("GET: $req")
          // ScopedValue.where(REQ_URI, req) {
          val res = summary(args, debug).encodeToByteArray()
          it.sendResponseHeaders(200, res.size.toLong())
          it.responseBody.use { os -> os.write(res) }
          // }
        }

        createContext("/shutdown") {
          stop(0)
          exitProcess(0)
        }

        createContext("/rsocket", ::rSocket)

        createContext("/reflect", ::reflect)

        createContext("/resources", ::resources)

        createContext("/uds", ::unixDomainSockets)

        executor = vtExec
        start()
      }

  println("Http Server started on http://localhost:${server.address.port}...")
  val vmTime =
      ProcessHandle.current().info().startInstant().getOrDefault(Instant.now()).toEpochMilli()
  val currTime = System.currentTimeMillis()
  // val vmTime = ManagementFactory.getRuntimeMXBean().startTime

  val type = if (ImageInfo.isExecutable()) "Binary" else "JVM"
  println(
      "Started in ${currTime - vmTime} ms ($type: ${start - vmTime} ms, Server: ${currTime - start} ms).",
  )
}

/** Get the system summary report */
fun summary(args: Array<String>, debug: Boolean = false) = buildString {
  val rt = Runtime.getRuntime()
  val unit = 1024 * 1024L
  val heapSize = rt.totalMemory()
  val heapFreeSize = rt.freeMemory()
  val heapUsedSize = heapSize - heapFreeSize
  val heapMaxSize = rt.maxMemory()
  val osMxBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)

  appendLine("✧✧✧ Time: ${LocalDateTime.now()}")
  appendLine("✧✧✧ [JVM] JVM Version              : ${System.getProperty("java.version")}")

  appendLine("✧✧✧ [SYS-OS]  Operating System     : ${System.getProperty("os.name")}")
  appendLine("✧✧✧ [SYS-CPU] CPU Arch             : ${System.getProperty("os.arch")}")
  appendLine("✧✧✧ [SYS-CPU] Available Processors : ${rt.availableProcessors()}")
  appendLine("✧✧✧ [SYS-CPU] System CPU Usage     : ${osMxBean.cpuLoad}")
  appendLine("✧✧✧ [JVM-CPU] JVM CPU Usage        : ${osMxBean.processCpuLoad}")
  appendLine(
      "✧✧✧ [JVM-CPU] JVM CPU Time(Sec)    : ${Duration.ofNanos(osMxBean.processCpuTime).toSeconds()}")
  appendLine("✧✧✧ [SYS-MEM] Total Memory                  : ${osMxBean.totalMemorySize / unit} MiB")
  appendLine("✧✧✧ [SYS-MEM] Free  Memory                  : ${osMxBean.freeMemorySize / unit} MiB")
  appendLine("✧✧✧ [JVM-MEM] Current Heap Size (Committed) : ${heapSize / unit} MiB")
  appendLine("✧✧✧ [JVM-MEM] Current Free memory in Heap   : ${heapFreeSize/unit} MiB")
  appendLine("✧✧✧ [JVM-MEM] Currently used memory         : ${heapUsedSize/unit} MiB")
  appendLine("✧✧✧ [JVM-MEM] Max Heap Size (-Xmx)          : ${heapMaxSize/unit} MiB")
  appendLine("✧✧✧ [JVM-CMD] Command Args                  : ${args.joinToString()}")

  appendLine("✧✧✧ Processes ✧✧✧")
  val ps = ProcessHandle.allProcesses().sorted(ProcessHandle::compareTo).toList()
  ps.forEach { appendLine("${it.pid()} : ${it.info()}") }

  appendLine("✧✧✧ Trust stores ✧✧✧")
  val caCerts =
      TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
        init(null as KeyStore?)
        trustManagers.filterIsInstance<X509TrustManager>().flatMap { it.acceptedIssuers.toList() }
      }
  caCerts.forEach { appendLine(it.issuerX500Principal) }

  appendLine("✧✧✧ Dns Resolution ✧✧✧")
  val dns = InetAddress.getAllByName("google.com").toList()
  dns.forEach { appendLine(it) }

  appendLine("✧✧✧ TimeZones ✧✧✧")
  val tz = ZoneId.getAvailableZoneIds()
  if (debug) {
    tz.forEach { appendLine(it) }
  } else {
    appendLine("Found ${tz.size} timezones.")
  }

  appendLine("✧✧✧ Charsets ✧✧✧")
  val cs = Charset.availableCharsets()
  if (debug) {
    cs.forEach { (name, charset) -> appendLine("$name: $charset") }
  } else {
    appendLine("Found ${cs.size} charsets.")
  }

  appendLine("✧✧✧ System Locales ✧✧✧")
  val locales = Locale.getAvailableLocales()
  if (debug) {
    locales.forEach { appendLine(it) }
  } else {
    appendLine("Found ${locales.size} locales.")
  }

  appendLine("✧✧✧ System Countries ✧✧✧")
  val countries = Locale.getISOCountries()
  if (debug) {
    countries.forEach { appendLine(it) }
  } else {
    appendLine("Found ${countries.size} countries.")
  }

  appendLine("✧✧✧ System Currencies ✧✧✧")
  val currencies = Currency.getAvailableCurrencies()
  if (debug) {
    currencies.forEach { appendLine(it) }
  } else {
    appendLine("Found ${currencies.size} currencies.")
  }

  appendLine("✧✧✧ System Languages ✧✧✧")
  val languages = Locale.getISOLanguages()
  if (debug) {
    languages.forEach { appendLine(it) }
  } else {
    appendLine("Found ${languages.size} languages.")
  }

  appendLine("✧✧✧ Env Variables ✧✧✧")
  val env = System.getenv()
  env.forEach { (k: String, v: String) -> appendLine("$k : $v") }

  appendLine("✧✧✧ System Properties ✧✧✧")
  val props = System.getProperties()
  props.forEach { k: Any, v: Any -> appendLine("$k : $v") }

  val fmt = HexFormat.ofDelimiter(", ").withUpperCase().withPrefix("0x")
  appendLine("✧✧✧ I ❤️ Kotlin = ${fmt.formatHex("I ❤️ Kotlin".encodeToByteArray())}")
  appendLine("✧✧✧ LineSeparator  = ${fmt.formatHex(System.lineSeparator().encodeToByteArray())}")
  appendLine("✧✧✧ File PathSeparator = ${fmt.formatHex(File.pathSeparator.encodeToByteArray())}")
  appendLine("✧✧✧ File Separator = ${fmt.formatHex(File.separator.encodeToByteArray())}")

  // Somehow, socket connect is hanging on native image.
  if (ImageInfo.isExecutable().not()) {
    appendLine("✧✧✧ Additional info in exception ✧✧✧")
    val ex =
        runCatching {
              Security.setProperty("jdk.includeInExceptions", "hostInfo,jar")
              Socket().use { s ->
                s.setOption(StandardSocketOptions.SO_REUSEADDR, true)
                s.setOption(StandardSocketOptions.SO_REUSEPORT, true)
                s.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
                s.soTimeout = 100
                s.connect(InetSocketAddress("localhost", 12345), 100)
              }
            }
            .exceptionOrNull()
    appendLine(ex?.message)
    check(ex?.message?.contains("localhost:12345") == true)
  }

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
    | Virtual Thread : ${Thread.currentThread().isVirtual} |
    +-----------------------+
    """
          .trimIndent(),
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
                  metadata = WellKnownMimeType.MessageRSocketCompositeMetadata,
              )
        }
      }
    }
  }
}

private fun String.newInstance() = Class.forName(this).getConstructor().newInstance()

/** Test reflection and [ServiceLoader] plugins. */
fun reflect(ex: HttpExchange) {
  val plugins = ServiceLoader.load(Callable::class.java)
  plugins.forEach { println("ServiceLoader Plugin: ${it.call()}") }

  println("Redacted: ${Secret("abc")}, ${Creds("user", "pass")}")

  val path = ex.requestURI.path.substringAfterLast("/").lowercase()
  val res =
      when (path) {
            "java" -> "dev.suresh.model.JVersion".newInstance()
            "kotlin" -> "dev.suresh.model.KtVersion".newInstance()
            else -> "NativeImage Playground!"
          }
          .toString()
          .encodeToByteArray()

  ex.sendResponseHeaders(200, res.size.toLong())
  ex.responseBody.use { os -> os.write(res) }
}

/** Embed resources in binary. */
fun resources(ex: HttpExchange) {

  // Load plugins from current directory
  val loader =
      URLClassLoader.newInstance(
          arrayOf(URI("file://${System.getProperty("user.dir")}/plugins.jar").toURL()),
      )
  val svcLoader = ServiceLoader.load(java.lang.Runnable::class.java, loader)
  println("Found ${svcLoader.toList().size} Runnable plugins!")

  val res =
      Secret::class.java.getResourceAsStream("/message.txt")?.readBytes()
          ?: "Resource not found!".encodeToByteArray()

  ex.sendResponseHeaders(200, res.size.toLong())
  ex.responseBody.use { os -> os.write(res) }
}

fun rSocket(ex: HttpExchange) {
  println("Starting new rSocket connection!")
  runBlocking(viDispatcher) {
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
      stream.take(10).flowOn(viDispatcher).collect { payload ->
        os.write(payload.data.readBytes())
        os.write("\n".encodeToByteArray())
        os.flush()
      }
    }
  }
}

fun unixDomainSockets(httpExchange: HttpExchange) {}

private val Int.fmt
  get() = "%-5d".format(this)

val Long.compactFmt: String
  get() = NumberFormat.getCompactNumberInstance().format(this)
