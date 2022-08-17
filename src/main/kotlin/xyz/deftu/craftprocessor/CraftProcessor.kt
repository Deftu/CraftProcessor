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
import xyz.deftu.craftprocessor.commands.TermsCommand
import xyz.deftu.craftprocessor.config.LocalConfig
import xyz.deftu.craftprocessor.processor.ProcessorHandler

fun main() {
    CraftProcessor.start()
}

object CraftProcessor : Thread("CraftProcessor") {
    const val NAME = "@NAME@"
    const val VERSION = "@VERSION@"

    private lateinit var client: ShardManager

    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setLenient()
        .create()

    override fun run() {
        val token = LocalConfig.INSTANCE.token
        if (token.isNullOrBlank()) throw IllegalArgumentException("Token is blank")

        val eventManager = AnnotatedEventManager()
        client = DefaultShardManagerBuilder.createDefault(token)
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

        TermsCommand.initialize(client)
        ProcessorHandler.start()
        client.addEventListener(ProcessorHandler)
    }
}
