package xyz.deftu.craftprocessor.config

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.utils.ensureExists
import java.io.File

object ConfigManager {
    private var initialized = false
    private val guildConfigManager = GuildConfigManager()
    private val userConfigManager = UserConfigManager()

    fun initialize(directory: File) {
        if (initialized) return
        directory.ensureExists()

        guildConfigManager.initialize(directory.resolve("guilds.db"))
        userConfigManager.initialize(directory.resolve("users.db"))

        CraftProcessor.addShutdownListener {
            guildConfigManager.close()
            userConfigManager.close()
        }

        initialized = true
    }

    fun getGuild(id: String, createIfNotExists: Boolean = false) =
        guildConfigManager.getGuild(id, createIfNotExists)
    fun saveGuild(guild: GuildConfig) =
        guildConfigManager.saveGuild(guild)
    fun createGuildMessage(config: GuildConfig, guild: Guild) =
        guildConfigManager.createMessage(config, guild)

    fun getUser(id: String, createIfNotExists: Boolean = false) =
        userConfigManager.getUser(id, createIfNotExists)
    fun saveUser(user: UserConfig) =
        userConfigManager.saveUser(user)
    fun createUserMessage(config: UserConfig, user: User) =
        userConfigManager.createMessage(config, user)
}
