package xyz.deftu.craftprocessor.config

import net.dv8tion.jda.api.sharding.ShardManager
import xyz.deftu.craftprocessor.utils.SQLiteHelper
import java.io.File
import java.sql.Connection

// Each guild has its own config.
// The config is stored in a sqlite database.
// The config has the following values:
// - has_channel_whitelist (boolean)
// - has_channel_blacklist (boolean)
// - whitelisted_channels (long array)
// - blacklisted_channels (long array)
// - has_user_whitelist (boolean)
// - has_user_blacklist (boolean)
// - whitelisted_users (long array)
// - blacklisted_users (long array)
// - has_role_whitelist (boolean)
// - has_role_blacklist (boolean)
// - whitelisted_roles (long array)
// - blacklisted_roles (long array)

object GuildConfig {
    // Our connection to the database.
    private lateinit var databaseConnection: Connection
    // A map of all the guild config data.
    private val guildConfigs = mutableMapOf<String, GuildConfigData>()

    fun initialize(client: ShardManager) {
        // Open our connection to the database file.
        databaseConnection = SQLiteHelper.openConnection(File("guild_config.db"))
        // Create the table if it doesn't exist.
        SQLiteHelper.createTable(
            connection = databaseConnection,
            tableName = "guild_config",
            columns = arrayOf(
                "guild_id TEXT PRIMARY KEY",
                "has_channel_whitelist INTEGER",
                "has_channel_blacklist INTEGER",
                "whitelisted_channels TEXT",
                "blacklisted_channels TEXT",
                "has_user_whitelist INTEGER",
                "has_user_blacklist INTEGER",
                "whitelisted_users TEXT",
                "blacklisted_users TEXT",
                "has_role_whitelist INTEGER",
                "has_role_blacklist INTEGER",
                "whitelisted_roles TEXT",
                "blacklisted_roles TEXT"
            )
        )
        // Load all the guild configs.
        loadGuildConfigs()

        guildConfigs.forEach { id, data ->
            println("Loaded config for guild $id:\n$data")
        }
    }

    fun getGuildConfig(guildId: String): GuildConfigData? {
        return guildConfigs[guildId]
    }

    fun createGuildConfig(guildId: String): GuildConfigData {
        // Create a new guild config.
        val guildConfig = GuildConfigData(guildId)
        // Add it to the array.
        guildConfigs[guildId] = guildConfig
        // Save it to the database.
        saveGuildConfig(guildConfig)
        // Return the guild config.
        return guildConfig
    }

    fun saveGuildConfig(guildConfig: GuildConfigData) {
        // Save the guild config to the database.
        SQLiteHelper.update(
            connection = databaseConnection,
            tableName = "guild_config",
            values = arrayOf(
                "guild_id" to guildConfig.guildId,
                "has_channel_whitelist" to guildConfig.hasChannelWhitelist,
                "has_channel_blacklist" to guildConfig.hasChannelBlacklist,
                "whitelisted_channels" to guildConfig.whitelistedChannels.joinToString(","),
                "blacklisted_channels" to guildConfig.blacklistedChannels.joinToString(","),
                "has_user_whitelist" to guildConfig.hasUserWhitelist,
                "has_user_blacklist" to guildConfig.hasUserBlacklist,
                "whitelisted_users" to guildConfig.whitelistedUsers.joinToString(","),
                "blacklisted_users" to guildConfig.blacklistedUsers.joinToString(","),
                "has_role_whitelist" to guildConfig.hasRoleWhitelist,
                "has_role_blacklist" to guildConfig.hasRoleBlacklist,
                "whitelisted_roles" to guildConfig.whitelistedRoles.joinToString(","),
                "blacklisted_roles" to guildConfig.blacklistedRoles.joinToString(",")
            )
        )
    }

    fun deleteGuildConfig(guildId: String) {
        guildConfigs.remove(guildId)
        SQLiteHelper.deleteRow(
            connection = databaseConnection,
            tableName = "guild_config",
            where = "guild_id = ?",
            whereArgs = arrayOf(guildId)
        )
    }

    private fun loadGuildConfigs() {
        // Load all the guild configs from the database.
        val guildConfigs = SQLiteHelper.select(
            connection = databaseConnection,
            tableName = "guild_config",
            columns = arrayOf(
                "guild_id",
                "has_channel_whitelist",
                "has_channel_blacklist",
                "whitelisted_channels",
                "blacklisted_channels",
                "has_user_whitelist",
                "has_user_blacklist",
                "whitelisted_users",
                "blacklisted_users",
                "has_role_whitelist",
                "has_role_blacklist",
                "whitelisted_roles",
                "blacklisted_roles"
            )
        )
        // Loop through all the guild configs.
        for (guildConfig in guildConfigs) {
            // Create a new guild config.
            val guildConfigData = GuildConfigData(
                guildId = guildConfig["guild_id"]?.toString() ?: continue,
                hasChannelWhitelist = guildConfig["has_channel_whitelist"] == "1",
                hasChannelBlacklist = guildConfig["has_channel_blacklist"] == "1",
                whitelistedChannels = guildConfig["whitelisted_channels"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue,
                blacklistedChannels = guildConfig["blacklisted_channels"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue,
                hasUserWhitelist = guildConfig["has_user_whitelist"] == "1",
                hasUserBlacklist = guildConfig["has_user_blacklist"] == "1",
                whitelistedUsers = guildConfig["whitelisted_users"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue,
                blacklistedUsers = guildConfig["blacklisted_users"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue,
                hasRoleWhitelist = guildConfig["has_role_whitelist"] == "1",
                hasRoleBlacklist = guildConfig["has_role_blacklist"] == "1",
                whitelistedRoles = guildConfig["whitelisted_roles"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue,
                blacklistedRoles = guildConfig["blacklisted_roles"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue
            )
            // Add it to the array.
            this.guildConfigs[guildConfigData.guildId] = guildConfigData
        }
    }
}

data class GuildConfigData(
    val guildId: String,
    var hasChannelWhitelist: Boolean = false,
    var hasChannelBlacklist: Boolean = false,
    var whitelistedChannels: MutableList<Long> = mutableListOf(),
    var blacklistedChannels: MutableList<Long> = mutableListOf(),
    var hasUserWhitelist: Boolean = false,
    var hasUserBlacklist: Boolean = false,
    var whitelistedUsers: MutableList<Long> = mutableListOf(),
    var blacklistedUsers: MutableList<Long> = mutableListOf(),
    var hasRoleWhitelist: Boolean = false,
    var hasRoleBlacklist: Boolean = false,
    var whitelistedRoles: MutableList<Long> = mutableListOf(),
    var blacklistedRoles: MutableList<Long> = mutableListOf()
) {
    fun isChannelWhitelisted(channelId: Long) =
        hasChannelWhitelist && whitelistedChannels.contains(channelId)
    fun isChannelBlacklisted(channelId: Long) =
        hasChannelBlacklist && blacklistedChannels.contains(channelId)
    fun isUserWhitelisted(userId: Long) =
        hasUserWhitelist && whitelistedUsers.contains(userId)
    fun isUserBlacklisted(userId: Long) =
        hasUserBlacklist && blacklistedUsers.contains(userId)
    fun isRoleWhitelisted(roleId: Long) =
        hasRoleWhitelist && whitelistedRoles.contains(roleId)
    fun isRoleBlacklisted(roleId: Long) =
        hasRoleBlacklist && blacklistedRoles.contains(roleId)
}
