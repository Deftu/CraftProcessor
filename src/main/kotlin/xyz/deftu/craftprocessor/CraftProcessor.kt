package xyz.deftu.craftprocessor

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import net.dv8tion.jda.api.GatewayEncoding
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.AnnotatedEventManager
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.Compression
import xyz.deftu.craftprocessor.processor.ProcessorHandler
import java.io.File

fun main() {
    CraftProcessor.start()
}

object CraftProcessor : Thread("CraftProcessor") {
    lateinit var config: Config
        private set
    private lateinit var client: ShardManager

    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setLenient()
        .create()

    override fun run() {
        config = Config.read(File("config.json"))
        if (config.token.isNullOrBlank()) throw IllegalArgumentException("Token is blank")

        val eventManager = AnnotatedEventManager()
        client = DefaultShardManagerBuilder.createDefault(config.token)
            .setActivityProvider {
                Activity.watching("your Minecraft crashes and logs | $it")
            }.setBulkDeleteSplittingEnabled(true)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .setCompression(Compression.ZLIB)
            .setGatewayEncoding(GatewayEncoding.JSON)
            .setStatus(OnlineStatus.DO_NOT_DISTURB)
            .setEventManagerProvider {
                eventManager
            }.build()
        client.shards.forEach(JDA::awaitReady)

        ProcessorHandler.start()
        client.addEventListener(ProcessorHandler)
    }
}
