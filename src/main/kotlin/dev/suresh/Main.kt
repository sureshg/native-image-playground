package dev.suresh

import com.sun.management.OperatingSystemMXBean
import dev.suresh.config.BuildEnv
import dev.suresh.model.Creds
import dev.suresh.model.Secret
import io.ktor.server.application.ServerReady
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.io.File
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
import kotlin.io.path.Path
import kotlin.io.use
import kotlin.jvm.optionals.getOrDefault
import kotlin.system.exitProcess
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.graalvm.nativeimage.ImageInfo

val vtDispatcher by lazy { Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher() }
val REQ_URI = ScopedValue.newInstance<String>()

fun main(args: Array<String>) {
  val serverStart = System.currentTimeMillis()
  val processStart =
      ProcessHandle.current().info().startInstant().getOrDefault(Instant.now()).toEpochMilli()
  val commandArgs = args.toList()

  println(
      """
    | Build info,
    | version       : ${BuildEnv.BUILD_NUMBER}
    | Commit Hash   : ${BuildEnv.COMMIT_HASH}
    | Built on      : ${BuildEnv.TIME_STAMP}
    """
          .trimMargin()
  )
  Runtime.getRuntime().addShutdownHook(Thread { println("Shutting down...") })

  embeddedServer(CIO, port = 9080) {
        monitor.subscribe(ServerReady) {
          val ready = System.currentTimeMillis()
          val type = if (ImageInfo.isExecutable()) "Native Image" else "JVM App"
          val startup = serverStart - processStart
          val server = ready - serverStart

          log.info(
              "$type ready in ${startup + server} ms = $startup ms (process start ➟ main) + $server ms (main ➟ server ready)"
          )
        }

        install(CallLogging) {
          format { call -> "${call.request.httpMethod.value}: ${call.request.path()}" }
        }

        install(StatusPages) {
          exception<Throwable> { call, cause ->
            println("ERROR: ${call.request.path()} - ${cause.message}")
            call.respondText("", status = InternalServerError)
          }
        }

        routing {
          route("/") {
            handle {
              val report =
                  withContext(vtDispatcher) {
                    ScopedValue.where(REQ_URI, call.request.path()).call<_, Throwable> {
                      summary(commandArgs)
                    }
                  }
              call.respondText(report)
            }
          }

          get("/shutdown") { exitProcess(0) }

          get("/reflect/{type}") {
            ServiceLoader.load(Callable::class.java).forEach {
              println("ServiceLoader Plugin: ${it.call()}")
            }
            println("Redacted: ${Secret("abc")}, ${Creds("user", "pass")}")

            val className =
                when (call.requirePathParameter("type").trim()) {
                  "java" -> "dev.suresh.model.JVersion"
                  "kotlin" -> "dev.suresh.model.KtVersion"
                  else -> null
                }
            val data =
                className?.let { Class.forName(it).getConstructor().newInstance().toString() }
                    ?: "NativeImage Playground!"
            call.respondText(data)
          }

          get("/resources") {
            URLClassLoader.newInstance(
                    arrayOf(URI("file://${System.getProperty("user.dir")}/plugins.jar").toURL())
                )
                .use { loader ->
                  val plugins = ServiceLoader.load(Runnable::class.java, loader).toList()
                  println("Found ${plugins.size} Runnable plugins!")
                }
            val resources =
                Secret::class.java.getResourceAsStream("/message.txt")?.readBytes()
                    ?: "Resource not found!".encodeToByteArray()
            call.respondBytes(resources)
          }

          get("/uds") { call.respondText("wip!") }

          route("/ui") { handle { call.respondRedirect("/", permanent = true) } }

          staticResources("/img", "static")
        }
      }
      .start(wait = true)
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
      "✧✧✧ [JVM-CPU] JVM CPU Time(Sec)    : ${Duration.ofNanos(osMxBean.processCpuTime).toSeconds()}"
  )
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
    cs.forEach { appendLine("${it.key}: ${it.value}") }
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
  env.forEach { [k, v] -> appendLine("$k : $v") }

  appendLine("✧✧✧ System Properties ✧✧✧")
  val props = System.getProperties()
  props.forEach { k: Any, v: Any -> appendLine("$k : $v") }

  val fmt = HexFormat.ofDelimiter(", ").withUpperCase().withPrefix("0x")
  appendLine("✧✧✧ I ❤️ Kotlin = ${fmt.formatHex("I ❤️ Kotlin".encodeToByteArray())}")
  appendLine("✧✧✧ LineSeparator  = ${fmt.formatHex(System.lineSeparator().encodeToByteArray())}")
  appendLine("✧✧✧ File PathSeparator = ${fmt.formatHex(File.pathSeparator.encodeToByteArray())}")
  appendLine("✧✧✧ File Separator = ${fmt.formatHex(File.separator.encodeToByteArray())}")

  appendLine("✧✧✧ Additional info in exception ✧✧✧")
  val ex = runCatching {
    Security.setProperty("jdk.includeInExceptions", "hostInfo,jar")
    Socket().use { s ->
      s.setOption(StandardSocketOptions.SO_REUSEADDR, true)
      s.setOption(StandardSocketOptions.SO_REUSEPORT, true)
      s.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
      // Disable the Nagle algorithm as using it would hurt latency.
      s.setOption(StandardSocketOptions.TCP_NODELAY, true)
      // s.setOption(StandardSocketOptions.SO_RCVBUF, 4096)
      s.soTimeout = 100
      s.connect(InetSocketAddress("localhost", 12345), 100)
    }
  }
      .exceptionOrNull()
  appendLine(ex?.message)
  // Host info is not available on native-image
  if (ImageInfo.isExecutable().not()) {
    println(ex?.message)
    check(ex?.message?.contains("localhost/127.0.0.1:12345") == true)
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

val udsServer by
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      val addr =
          UnixDomainSocketAddress.of(
              Path(System.getProperty("java.io.tmpdir")).resolve("native-image-server.socket")
          )

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

private val Int.fmt
  get() = "%-5d".format(this)

val Long.compactFmt: String
  get() = NumberFormat.getCompactNumberInstance().format(this)
