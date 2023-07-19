package dev.suresh

import com.sun.management.OperatingSystemMXBean
import dev.suresh.config.BuildEnv
import dev.suresh.model.Creds
import dev.suresh.model.Secret
import io.helidon.common.http.Http
import io.helidon.common.http.Http.Header
import io.helidon.common.http.NotFoundException
import io.helidon.nima.webserver.WebServer
import io.helidon.nima.webserver.http.HttpRouting
import io.helidon.nima.webserver.http.ServerRequest
import io.helidon.nima.webserver.http.ServerResponse
import io.helidon.nima.webserver.staticcontent.StaticContentService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.WellKnownMimeType
import io.rsocket.kotlin.keepalive.KeepAlive
import io.rsocket.kotlin.ktor.client.RSocketSupport
import io.rsocket.kotlin.ktor.client.rSocket
import io.rsocket.kotlin.payload.PayloadMimeType
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import java.io.File
import java.lang.System.Logger.Level.INFO
import java.lang.management.ManagementFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.StandardProtocolFamily
import java.net.StandardSocketOptions
import java.net.URI
import java.net.URLClassLoader
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
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
import jdk.incubator.concurrent.ScopedValue
import kotlin.io.path.Path
import kotlin.io.use
import kotlin.jvm.optionals.getOrDefault
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.graalvm.nativeimage.ImageInfo

val logger = System.getLogger("Main")
val vtDispatcher by lazy { Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher() }
val REQ_URI = ScopedValue.newInstance<String>()

lateinit var cmdArgs: List<String>
lateinit var webServer: WebServer

fun main(args: Array<String>) {
  val start = System.currentTimeMillis()
  val type = if (ImageInfo.isExecutable()) "Native Image" else "JVM App"
  logger.log(INFO) {
    """
    | Build info,
    | $type version : ${BuildEnv.BUILD_NUMBER}
    | Commit Hash   : ${BuildEnv.COMMIT_HASH}
    | Built on      : ${BuildEnv.TIME_STAMP}
    """
        .trimMargin()
  }
  Runtime.getRuntime().addShutdownHook(Thread { println("Shutting down...") })

  cmdArgs = args.toList()
  webServer = WebServer.builder().port(9080).routing(::routes).build().start()

  val vmTime =
      ProcessHandle.current().info().startInstant().getOrDefault(Instant.now()).toEpochMilli()
  val currTime = System.currentTimeMillis()
  // val vmTime = ManagementFactory.getRuntimeMXBean().startTime
  // println("Started in ${currTime - vmTime} ms ($type: ${start - vmTime} ms, Server: ${currTime -
  // start} ms).")
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

val SERVER_HEADER = Header.createCached(Header.SERVER, "Nima")
val UI_REDIRECT = Header.createCached(Header.LOCATION, "/")

fun routes(rules: HttpRouting.Builder) {
  rules
      .addFilter { chain, req, res ->
        println("${req.prologue().method()}: ${req.prologue().uriPath().path()}")
        res.header(SERVER_HEADER)
        chain.proceed()
      }
      .any("/", ::root)
      .get("/shutdown", ::shutdown)
      .get("/rsocket", ::rSocket)
      .get("/reflect/{type}", ::reflect)
      .get("/resources", ::resources)
      .get("/uds", ::unixDomainSockets)
      .any("/ui", ::redirect)
      .register(
          "/img", StaticContentService.builder("static").welcomeFileName("favicon.ico").build())
      .error(Throwable::class.java, ::error)
}

fun root(req: ServerRequest, res: ServerResponse) {
  ScopedValue.where(REQ_URI, req.path().path()) { res.send(summary(cmdArgs)) }
}

fun error(req: ServerRequest, res: ServerResponse, ex: Throwable) {
  println("ERROR: ${req.path().path()} - ${ex.message}")
  when (ex) {
    is NotFoundException -> res.status(Http.Status.NOT_FOUND_404)
    else -> res.status(Http.Status.INTERNAL_SERVER_ERROR_500)
  }
  res.send()
}

fun shutdown(req: ServerRequest, res: ServerResponse) {
  webServer.stop()
  exitProcess(0)
}

fun redirect(req: ServerRequest, res: ServerResponse) {
  res.status(Http.Status.MOVED_PERMANENTLY_301)
  res.headers().set(UI_REDIRECT)
  res.send()
}

/** Get the system summary report */
fun summary(args: List<String>) = buildString {
  val debug = args.contains("--debug")
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
  if (debug) {
    ps.forEach { appendLine("${it.pid()} : ${it.info()}") }
  } else {
    appendLine("Found ${ps.size} processes.")
  }

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

  appendLine("✧✧✧ Additional info in exception ✧✧✧")
  val ex =
      runCatching {
            Security.setProperty("jdk.includeInExceptions", "hostInfo,jar")
            Socket().use { s ->
              s.setOption(StandardSocketOptions.SO_REUSEADDR, true)
              s.setOption(StandardSocketOptions.SO_REUSEPORT, true)
              s.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
              // Disable the Nagle algorithm
              // s.setOption(StandardSocketOptions.TCP_NODELAY, true)
              // s.setOption(StandardSocketOptions.SO_RCVBUF, 4096)
              s.soTimeout = 100
              s.connect(InetSocketAddress("localhost", 12345), 100)
            }
          }
          .exceptionOrNull()
  appendLine(ex?.message)
  // Host info is not available on native image
  if (ImageInfo.isExecutable().not()) {
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
    | ScopedValue    : ${REQ_URI.orElse("n/a")}    |
    +-----------------------+
    """
          .trimIndent(),
  )
}

/** Test reflection and [ServiceLoader] plugins. */
fun reflect(req: ServerRequest, res: ServerResponse) {
  val plugins = ServiceLoader.load(Callable::class.java)
  plugins.forEach { println("ServiceLoader Plugin: ${it.call()}") }

  println("Redacted: ${Secret("abc")}, ${Creds("user", "pass")}")
  val type = req.path().pathParameters().value("type").trim()
  val data =
      when (type) {
        "java" -> "dev.suresh.model.JVersion".newInstance()
        "kotlin" -> "dev.suresh.model.KtVersion".newInstance()
        else -> "NativeImage Playground!"
      }.toString()
  res.send(data)
}

/** Embed resources in binary. */
fun resources(req: ServerRequest, res: ServerResponse) {
  // Load plugins from the current directory
  val loader =
      URLClassLoader.newInstance(
          arrayOf(URI("file://${System.getProperty("user.dir")}/plugins.jar").toURL()),
      )
  val svcLoader = ServiceLoader.load(java.lang.Runnable::class.java, loader)
  println("Found ${svcLoader.toList().size} Runnable plugins!")

  val resources =
      Secret::class.java.getResourceAsStream("/message.txt")?.readBytes()
          ?: "Resource not found!".encodeToByteArray()
  res.send(resources)
}

fun rSocket(req: ServerRequest, res: ServerResponse) =
    runBlocking(vtDispatcher) {
      println("Starting new rSocket connection!")
      val rSocket: RSocket = rsClient.rSocket("wss://demo.rsocket.io/rsocket")
      val stream = rSocket.requestStream(buildPayload { data("""{ "data": "Kotlin rSocket!" }""") })
      res.header(Header.CONTENT_TYPE, "text/event-stream")
      res.header(Header.CACHE_CONTROL, "no-cache")
      // Chunked transfer encoding will be set if the response length is zero.
      // res.header(Header.TRANSFER_ENCODING,"chunked")
      // Streaming binary response - res.header(Header.CONTENT_TYPE,"application/octet-stream")
      res.outputStream().buffered().use { bos ->
        stream.take(10).flowOn(vtDispatcher).collect { payload ->
          bos.write(payload.data.readBytes())
          bos.write("\n".encodeToByteArray())
          bos.flush()
        }
      }
    }

val udsServer by
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      val addr =
          UnixDomainSocketAddress.of(
              Path(System.getProperty("java.io.tmpdir")).resolve("native-image-server.socket"))

      ServerSocketChannel.open(StandardProtocolFamily.UNIX).use {
        it.bind(addr)
        while (true) {
          val client = it.accept()
          Thread.startVirtualThread {
            client.use {
              val buf = ByteBuffer.allocate(1024)
              while (client.read(buf) > 0) {
                buf.flip()
                client.write(buf)
                buf.clear()
              }
            }
          }
        }
      }
    }

fun unixDomainSockets(req: ServerRequest, res: ServerResponse) {
  res.send("wip!")
}

private val Int.fmt
  get() = "%-5d".format(this)

val Long.compactFmt: String
  get() = NumberFormat.getCompactNumberInstance().format(this)
