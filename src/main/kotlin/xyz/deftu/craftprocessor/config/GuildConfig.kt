package xyz.deftu.craftprocessor.config

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.sharding.ShardManager
import xyz.deftu.craftprocessor.utils.SQLiteHelper
import xyz.deftu.craftprocessor.utils.toReadableString
import java.io.File
import java.sql.Connection

object GuildConfig {
    private lateinit var databaseConnection: Connection
    var guildConfigs = mutableMapOf<String, GuildConfigData>()
        private set

    fun initialize(client: ShardManager) {
        databaseConnection = SQLiteHelper.openConnection(File("guild_config.db"))
        // Create table if it doesn't exist
        // The table will have the following columns:
        // - guild_id (text, primary key)
        // - has_channel_whitelist (boolean)
        // - has_channel_blacklist (boolean)
        // - whitelisted_channels (text)
        // - blacklisted_channels (text)
        // - has_user_whitelist (boolean)
        // - has_user_blacklist (boolean)
        // - whitelisted_users (text)
        // - blacklisted_users (text)
        // - has_role_whitelist (boolean)
        // - has_role_blacklist (boolean)
        // - whitelisted_roles (text)
        // - blacklisted_roles (text)
        databaseConnection.createStatement()
            .executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS guild_config (
                    guild_id TEXT PRIMARY KEY,
                    
                    has_channel_whitelist BOOLEAN,
                    has_channel_blacklist BOOLEAN,
                    whitelisted_channels TEXT,
                    blacklisted_channels TEXT,
                    
                    has_user_whitelist BOOLEAN,
                    has_user_blacklist BOOLEAN,
                    whitelisted_users TEXT,
                    blacklisted_users TEXT,
                    
                    has_role_whitelist BOOLEAN,
                    has_role_blacklist BOOLEAN,
                    whitelisted_roles TEXT,
                    blacklisted_roles TEXT
                )
                """.trimIndent())

        // Load all guild configs
        val resultSet = databaseConnection.createStatement().executeQuery("SELECT * FROM guild_config")
        while (resultSet.next()) {
            val guildId = resultSet.getString("guild_id")
            val guildConfig = GuildConfigData(
                guildId,
                resultSet.getBoolean("has_channel_whitelist"),
                resultSet.getBoolean("has_channel_blacklist"),
                resultSet.getString("whitelisted_channels"),
                resultSet.getString("blacklisted_channels"),
                resultSet.getBoolean("has_user_whitelist"),
                resultSet.getBoolean("has_user_blacklist"),
                resultSet.getString("whitelisted_users"),
                resultSet.getString("blacklisted_users"),
                resultSet.getBoolean("has_role_whitelist"),
                resultSet.getBoolean("has_role_blacklist"),
                resultSet.getString("whitelisted_roles"),
                resultSet.getString("blacklisted_roles")
            )
            guildConfigs[guildId] = guildConfig
        }
    }

    fun createNewGuildConfig(guild: Guild): GuildConfigData {
        val guildConfig = GuildConfigData(
            guild.id,
            false,
            false,
            "",
            "",
            false,
            false,
            "",
            "",
            false,
            false,
            "",
            "")
        guildConfigs[guild.id] = guildConfig
        saveGuildConfig(guildConfig)
        return guildConfig
    }

    fun saveGuildConfig(guildConfig: GuildConfigData) {
        val statement = databaseConnection.createStatement()
        statement.executeUpdate("""
            INSERT OR REPLACE INTO guild_config (
                guild_id,
                has_channel_whitelist,
                has_channel_blacklist,
                whitelisted_channels,
                blacklisted_channels,
                has_user_whitelist,
                has_user_blacklist,
                whitelisted_users,
                blacklisted_users,
                has_role_whitelist,
                has_role_blacklist,
                whitelisted_roles,
                blacklisted_roles
            ) VALUES (
                '${guildConfig.guildId}',
                ${guildConfig.hasChannelWhitelist},
                ${guildConfig.hasChannelBlacklist},
                '${guildConfig.whitelistedChannels}',
                '${guildConfig.blacklistedChannels}',
                ${guildConfig.hasUserWhitelist},
                ${guildConfig.hasUserBlacklist},
                '${guildConfig.whitelistedUsers}',
                '${guildConfig.blacklistedUsers}',
                ${guildConfig.hasRoleWhitelist},
                ${guildConfig.hasRoleBlacklist},
                '${guildConfig.whitelistedRoles}',
                '${guildConfig.blacklistedRoles}'
            )
        """.trimIndent())
    }

    fun getGuildConfig(guildId: String): GuildConfigData? {
        return guildConfigs[guildId]
    }
}

data class GuildConfigData(
    val guildId: String,
    val hasChannelWhitelist: Boolean,
    val hasChannelBlacklist: Boolean,
    val whitelistedChannels: String,
    val blacklistedChannels: String,
    val hasUserWhitelist: Boolean,
    val hasUserBlacklist: Boolean,
    val whitelistedUsers: String,
    val blacklistedUsers: String,
    val hasRoleWhitelist: Boolean,
    val hasRoleBlacklist: Boolean,
    val whitelistedRoles: String,
    val blacklistedRoles: String
) {
    fun isChannelWhitelisted(channelId: String): Boolean {
        return if (hasChannelWhitelist) {
            whitelistedChannels.contains(channelId)
        } else {
            false
        }
    }

    fun isChannelBlacklisted(channelId: String): Boolean {
        return if (hasChannelBlacklist) {
            blacklistedChannels.contains(channelId)
        } else {
            false
        }
    }

    fun isUserWhitelisted(userId: String): Boolean {
        return if (hasUserWhitelist) {
            whitelistedUsers.contains(userId)
        } else {
            false
        }
    }

    fun isUserBlacklisted(userId: String): Boolean {
        return if (hasUserBlacklist) {
            blacklistedUsers.contains(userId)
        } else {
            false
        }
    }

    fun isRoleWhitelisted(roleId: String): Boolean {
        return if (hasRoleWhitelist) {
            whitelistedRoles.contains(roleId)
        } else {
            false
        }
    }

    fun isRoleBlacklisted(roleId: String): Boolean {
        return if (hasRoleBlacklist) {
            blacklistedRoles.contains(roleId)
        } else {
            false
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "guild_id" to guildId,
            "has_channel_whitelist" to hasChannelWhitelist,
            "has_channel_blacklist" to hasChannelBlacklist,
            "whitelisted_channels" to whitelistedChannels,
            "blacklisted_channels" to blacklistedChannels,
            "has_user_whitelist" to hasUserWhitelist,
            "has_user_blacklist" to hasUserBlacklist,
            "whitelisted_users" to whitelistedUsers,
            "blacklisted_users" to blacklistedUsers,
            "has_role_whitelist" to hasRoleWhitelist,
            "has_role_blacklist" to hasRoleBlacklist,
            "whitelisted_roles" to whitelistedRoles,
            "blacklisted_roles" to blacklistedRoles
        )
    }

    fun toMessage(guild: Guild): Message {
        return MessageBuilder()
            .setContent("""
            **Guild Config for $guildId:**
            
            - Channel whitelist: ${hasChannelWhitelist.toReadableString()}
            - Channel blacklist: ${hasChannelBlacklist.toReadableString()}
            - Whitelisted channels: ${whitelistedChannels.channelsToReadableString(guild)}
            - Blacklisted channels: ${blacklistedChannels.channelsToReadableString(guild)}
            
            - User whitelist: ${hasUserWhitelist.toReadableString()}
            - User blacklist: ${hasUserBlacklist.toReadableString()}
            - Whitelisted users: ${whitelistedUsers.usersToReadableString(guild)}
            - Blacklisted users: ${blacklistedUsers.usersToReadableString(guild)}
            
            - Role whitelist: ${hasRoleWhitelist.toReadableString()}
            - Role blacklist: ${hasRoleBlacklist.toReadableString()}
            - Whitelisted roles: ${whitelistedRoles.rolesToReadableString(guild)}
            - Blacklisted roles: ${blacklistedRoles.rolesToReadableString(guild)}
            """.trimIndent())
            .setActionRows(ActionRow.of(

            )).build()
    }

    private fun String.channelsToReadableString(guild: Guild) =
        if (isNotBlank()) {
            split(",").joinToString(", ") {
                guild.getTextChannelById(it)?.asMention ?: "Not Found"
            }
        } else "None"
    private fun String.usersToReadableString(guild: Guild) =
        if (isNotBlank()) {
            split(",").joinToString(", ") {
                guild.getMemberById(it)?.asMention ?: "Not Found"
            }
        } else "None"
    private fun String.rolesToReadableString(guild: Guild) =
        if (isNotBlank()) {
            split(",").joinToString(", ") {
                guild.getRoleById(it)?.asMention ?: "Not Found"
            }
        } else "None"
}
