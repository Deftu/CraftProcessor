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
import xyz.deftu.craftprocessor.commands.AboutCommand
import xyz.deftu.craftprocessor.commands.GuildConfigCommand
import xyz.deftu.craftprocessor.commands.PersonalConfigCommand
import xyz.deftu.craftprocessor.commands.TermsCommand
import xyz.deftu.craftprocessor.config.ConfigManager
import xyz.deftu.craftprocessor.config.LocalConfig
import xyz.deftu.craftprocessor.processor.ProcessorHandler
import xyz.deftu.jdac.CommandManager
import java.io.File
import java.time.OffsetDateTime

fun main() {
    CraftProcessor.start()
}

object CraftProcessor : Thread("CraftProcessor") {
    const val NAME = "@NAME@"
    const val VERSION = "@VERSION@"
    const val COLOR = 0x990000
    lateinit var startTime: OffsetDateTime
        private set

    private val shutdownListeners = mutableListOf<() -> Unit>()

    private lateinit var client: ShardManager
    private val commandManagers = mutableListOf<CommandManager>()

    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setLenient()
        .create()

    override fun run() {
        val token = LocalConfig.INSTANCE.token
        if (token.isNullOrBlank()) throw IllegalArgumentException("Token is blank")

        // Set the start time, this is used for uptime
        startTime = OffsetDateTime.now()

        // Create the client (shard manager)
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

        // Commands
        client.shards.forEach { client ->
            val commandManager = CommandManager(client)
            commandManager.register(AboutCommand())
            commandManager.register("config", "Entrypoint to the config system", GuildConfigCommand(client), PersonalConfigCommand(client))
            commandManager.register(TermsCommand(client))
            commandManager.start()
            commandManagers.add(commandManager)
        }

        // Config
        ConfigManager.initialize(File("data"))

        // Features
        ProcessorHandler.start(client)
        StatsTracker.initialize()

        Runtime.getRuntime().addShutdownHook(Thread({
            shutdownListeners.forEach { it() }
            client.shutdown()
        }, "$NAME Shutdown Thread"))
    }

    fun addShutdownListener(listener: () -> Unit) =
        shutdownListeners.add(listener)
}
