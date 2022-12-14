package xyz.deftu.craftprocessor.config

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import xyz.deftu.MessageDecoration
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.utils.SQLiteHelper
import xyz.deftu.craftprocessor.utils.convertToMentions
import xyz.deftu.craftprocessor.utils.toButton
import xyz.deftu.craftprocessor.utils.toReadableString
import xyz.deftu.embed
import java.io.File
import java.sql.Connection
import java.time.OffsetDateTime

internal class GuildConfigManager {
    private val configs = mutableMapOf<String, GuildConfig>()
    private lateinit var connection: Connection

    fun initialize(file: File) {
        // Open our connection to the database file.
        connection = SQLiteHelper.openConnection(file)
        // Create the table if it doesn't exist.
        SQLiteHelper.createTable(
            connection = connection,
            tableName = "configs",
            columns = arrayOf(
                "id TEXT PRIMARY KEY",

                "channel_whitelist INTEGER",
                "whitelisted_channels TEXT",

                "channel_blacklist INTEGER",
                "blacklisted_channels TEXT",

                "role_whitelist INTEGER",
                "whitelisted_roles TEXT",

                "role_blacklist INTEGER",
                "blacklisted_roles TEXT"
            )
        )

        loadGuilds()
    }

    fun close() {
        connection.close()
    }

    fun getGuild(id: String, createIfNotExists: Boolean = false) =
        configs[id] ?: if (createIfNotExists) createGuild(id) else null

    fun saveGuild(config: GuildConfig) {
        configs[config.id] = config
        SQLiteHelper.update(
            connection = connection,
            tableName = "configs",
            values = arrayOf(
                "id" to config.id,

                "channel_whitelist" to config.channelWhitelist,
                "whitelisted_channels" to "'${config.whitelistedChannels.joinToString(",")}'",

                "channel_blacklist" to config.channelBlacklist,
                "blacklisted_channels" to "'${config.blacklistedChannels.joinToString(",")}'",

                "role_whitelist" to config.roleWhitelist,
                "whitelisted_roles" to "'${config.whitelistedRoles.joinToString(",")}'",

                "role_blacklist" to config.roleBlacklist,
                "blacklisted_roles" to "'${config.blacklistedRoles.joinToString(",")}'"
            )
        )
    }

    fun createMessage(config: GuildConfig, guild: Guild, member: Member? = null): MessageCreateData {
        val components = mutableListOf<List<ItemComponent>>()
        run {
            // Channels

            val channels = guild.textChannels.filter {
                member?.hasPermission(it, Permission.VIEW_CHANNEL) ?: true
            }.map {
                (if (it.parentCategory != null) {
                    "(${it.parentCategory!!.name}) "
                } else "") + "#${it.name}" to it.id
            }
            val roles = guild.roles.map {
                it.name to it.id
            }

            components.add(listOf(
                config.channelWhitelist.toButton("guild_config - channel_whitelist", "Channel whitelist"),
                config.channelBlacklist.toButton("guild_config - channel_blacklist", "Channel blacklist"),
                config.roleWhitelist.toButton("guild_config - role_whitelist", "Role whitelist"),
                config.roleBlacklist.toButton("guild_config - role_blacklist", "Role blacklist")
            ))

            components.add(listOf(
                SelectMenu.create("guild_config - channel_whitelist_edit").apply {
                    placeholder = "Edit whitelisted channels"
                    minValues = 0
                    maxValues = 10
                    channels.forEach {
                        addOption(it.first, it.second)
                    }
                }.build()
            ))

            components.add(listOf(
                SelectMenu.create("guild_config - channel_blacklist_edit").apply {
                    placeholder = "Edit blacklisted channels"
                    minValues = 0
                    maxValues = 10
                    channels.forEach {
                        addOption(it.first, it.second)
                    }
                }.build()
            ))

            components.add(listOf(
                SelectMenu.create("guild_config - role_whitelist_edit").apply {
                    placeholder = "Edit whitelisted roles"
                    minValues = 0
                    maxValues = 10
                    roles.forEach {
                        addOption(it.first, it.second)
                    }
                }.build()
            ))

            components.add(listOf(
                SelectMenu.create("guild_config - role_blacklistlist_edit").apply {
                    placeholder = "Edit blacklisted roles"
                    minValues = 0
                    maxValues = 10
                    roles.forEach {
                        addOption(it.first, it.second)
                    }
                }.build()
            ))
        }

        return MessageCreateBuilder()
            .setEmbeds(embed { 
                title("Guild Config")
                colorRaw(CraftProcessor.COLOR)
                timestamp(OffsetDateTime.now())
                footer(guild.name, guild.iconUrl)
                description {
                    append("Channel whitelist: ").append(config.channelWhitelist.toReadableString()).append("\n")
                    append("Whitelisted channels: ", MessageDecoration.BOLD).append(config.whitelistedChannels.convertToMentions {
                        guild.getTextChannelById(it)
                    }).append("\n\n")

                    append("Channel blacklist: ", MessageDecoration.BOLD).append(config.channelBlacklist.toReadableString()).append("\n")
                    append("Blacklisted channels: ", MessageDecoration.BOLD).append(config.blacklistedChannels.convertToMentions {
                        guild.getTextChannelById(it)
                    }).append("\n\n")

                    append("Role whitelist: ", MessageDecoration.BOLD).append(config.roleWhitelist.toReadableString()).append("\n")
                    append("Whitelisted roles: ", MessageDecoration.BOLD).append(config.whitelistedRoles.convertToMentions {
                        guild.getRoleById(it)
                    }).append("\n\n")

                    append("Role blacklist: ", MessageDecoration.BOLD).append(config.roleBlacklist.toReadableString()).append("\n")
                    append("Blacklisted roles: ", MessageDecoration.BOLD).append(config.blacklistedRoles.convertToMentions {
                        guild.getRoleById(it)
                    })
                }
            }).apply {
                components.forEach(::addActionRow)
            }.build()
    }

    private fun loadGuilds() {
        // Load all the guild configs from the database.
        val configs = SQLiteHelper.select(
            connection = connection,
            tableName = "configs",
            columns = arrayOf(
                "id",

                "channel_whitelist",
                "whitelisted_channels",

                "channel_blacklist",
                "blacklisted_channels",

                "role_whitelist",
                "whitelisted_roles",

                "role_blacklist",
                "blacklisted_roles"
            )
        )

        // Loop through all the guild configs.
        for (guildConfig in configs) {
            // Create a new guild config.
            val config = GuildConfig(
                id = guildConfig["id"]?.toString() ?: continue,

                channelWhitelist = guildConfig["channel_whitelist"] == 1,
                whitelistedChannels = guildConfig["whitelisted_channels"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue,

                channelBlacklist = guildConfig["channel_blacklist"] == 1,
                blacklistedChannels = guildConfig["blacklisted_channels"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue,

                roleWhitelist = guildConfig["role_whitelist"] == 1,
                whitelistedRoles = guildConfig["whitelisted_roles"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue,

                roleBlacklist = guildConfig["role_blacklist"] == 1,
                blacklistedRoles = guildConfig["blacklisted_roles"]?.toString()?.split(",")?.map {
                    it.toLongOrNull() ?: 0
                }?.toMutableList() ?: continue
            )
            // Add it to the array.
            this.configs[config.id] = config
        }
    }

    private fun createGuild(id: String): GuildConfig {
        val config = GuildConfig(id)
        configs[id] = config
        saveGuild(config)
        return config
    }
}
