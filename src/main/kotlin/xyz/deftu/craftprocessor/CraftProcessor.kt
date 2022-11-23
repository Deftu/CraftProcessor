package xyz.deftu.craftprocessor

import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.event.Event
import dev.kord.gateway.DefaultGateway
import dev.kord.gateway.ratelimit.IdentifyRateLimiter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import xyz.deftu.craftprocessor.extensions.processor.ProcessorExtension
import xyz.deftu.craftprocessor.util.ConstantRetry
import xyz.deftu.craftprocessor.util.PasteUpload
import java.io.File

val customEventFlow = MutableSharedFlow<Event>()
lateinit var kord: Kord
    private set
lateinit var httpClient: OkHttpClient
    private set

suspend fun main(args: Array<String>) {
    val argMap = ArgumentMap.parse(args)
    val configFile = argMap["config"]?.let { File(it) } ?: File("config.json")
    Config.read(configFile)
    Config.watch(configFile)

    val token = argMap.removeAndGet("token")
    if (Config.token.isEmpty()) {
        if (token == null) throw IllegalArgumentException("No token provided!")

        Config.token = token
    }

    start(argMap)
}

suspend fun start(argMap: ArgumentMap) {
    httpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .addHeader("User-Agent", "${NAME}/${VERSION}")
                .build())
        }.build()

    val dataHandlerUrl = argMap.removeAndGet("dataHandlerUrl")
    if (Config.dataHandlerUrl.isEmpty()) {
        if (dataHandlerUrl == null) throw IllegalArgumentException("No data handler url provided!")

        Config.dataHandlerUrl = dataHandlerUrl
    }

    val pasteUploadUrl = argMap.removeAndGet("pasteUploadUrl")
    if (Config.pasteUploadUrl.isEmpty()) {
        if (pasteUploadUrl == null) throw IllegalArgumentException("No paste upload url provided!")

        Config.pasteUploadUrl = pasteUploadUrl
    }

    DataHandler.setup(httpClient, Config.dataHandlerUrl)
    PasteUpload.setup(httpClient, Config.pasteUploadUrl)

    val bot = ExtensibleBot(Config.token) {
        kord {
            eventFlow = customEventFlow
            gateways { resources, shards ->
                val rateLimiter = IdentifyRateLimiter(resources.maxConcurrency, defaultDispatcher)
                shards.map {
                    DefaultGateway {
                        client = resources.httpClient
                        reconnectRetry = ConstantRetry()
                        identifyRateLimiter = rateLimiter
                    }
                }
            }
        }

        presence {
            status = PresenceStatus.DoNotDisturb
            watching("your Minecraft issues!")
        }

        extensions {
            add(::ProcessorExtension)
        }
    }

    kord = bot.kordRef
    bot.start()
}

suspend fun shutdown(e: Exception? = null) {
    LOGGER.warn("Shutting down...")
    e?.printStackTrace()
    kord.editPresence {
        playing("shutting down..." + (e?.let { "(${e::class.java.simpleName})" } ?: ""))
    }

    delay(10_000)
    kord.shutdown()
    e?.let {
        throw it
    }
}
