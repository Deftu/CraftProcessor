package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.sharding.ShardManager
import xyz.deftu.craftprocessor.config.GuildConfig
import xyz.deftu.craftprocessor.config.GuildConfigData
import xyz.deftu.craftprocessor.utils.toButton
import xyz.deftu.craftprocessor.utils.toReadableString
import java.time.OffsetDateTime

object ConfigCommand {
    fun initialize(client: ShardManager) {
        client.addEventListener(this)
        client.shards.forEach(this::register)
    }

    private fun register(client: JDA) {
        client.updateCommands().addCommands(
            Commands.slash("config", "Displays a config menu.")
                .addOption(OptionType.STRING, "type", "The type of config you'd like to configure.", true, true)
        ).queue()
    }

    @SubscribeEvent
    fun onCommandAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        if (event.interaction.name != "config") return
        when (event.focusedOption.name) {
            "type" -> event.replyChoiceStrings("Guild", "Personal").queue()
        }
    }

    @SubscribeEvent
    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.interaction.name != "config") return
        val type = ConfigType.from(event.getOption("type")!!.asString.lowercase())
        if (type == ConfigType.GUILD && !event.isFromGuild) {
            event.reply("You can only configure guild configs in a guild.").queue()
            return
        }
        when (type) {
            ConfigType.GUILD -> handleGuildConfig(event)
            ConfigType.PERSONAL -> handlePersonalConfig(event)
        }
    }

    fun handleGuildConfig(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val guildId = guild.id
        var config = GuildConfig.getGuildConfig(guildId)
        if (config == null) config = GuildConfig.createGuildConfig(guildId)

        event.reply(config.createMessage(guild))
            .queue()
    }

    fun handlePersonalConfig(event: SlashCommandInteractionEvent) {

    }

    @SubscribeEvent
    fun onButtonClicked(event: ButtonInteractionEvent) {
        val id = event.interaction.componentId
        val type = id.substringBefore("-").replace("_config ", "")
        when (type) {
            "guild" -> handleGuildConfigButton(event)
            //"personal" -> handlePersonalConfigButton(event)
        }
    }

    private fun handleGuildConfigButton(event: ButtonInteractionEvent) {
        val id = event.interaction.componentId
        val type = id.substringAfter("-").replace(" ", "")
        val config = GuildConfig.getGuildConfig(event.guild!!.id) ?: return
        when (type) {
            "channel_whitelist" -> config.hasChannelWhitelist = !config.hasChannelWhitelist
            "channel_blacklist" -> config.hasChannelBlacklist = !config.hasChannelBlacklist
            "user_whitelist" -> config.hasUserWhitelist = !config.hasUserWhitelist
            "user_blacklist" -> config.hasUserBlacklist = !config.hasUserBlacklist
            "role_whitelist" -> config.hasRoleWhitelist = !config.hasRoleWhitelist
            "role_blacklist" -> config.hasRoleBlacklist = !config.hasRoleBlacklist
        }
        event.message.editMessage(config.createMessage(event.guild!!)).queue()
        GuildConfig.saveGuildConfig(config)
        event.reply("Updated config.")
            .setEphemeral(true)
            .queue()
    }

    fun GuildConfigData.createMessage(guild: Guild): Message {
        val embed = EmbedBuilder()
            .setTitle("Guild Config")
            .setDescription("View and edit the settings for this guild.")
            .setColor(0x990000)
            .setFooter("Made by Deftu - https://deftu.xyz")
            .setTimestamp(OffsetDateTime.now())

        run {
            fun List<Long>.toMentionedString(map: (Long) -> IMentionable?) = mapNotNull {
                map(it)
            }.joinToString(", ", transform = IMentionable::getAsMention).ifBlank { "None" }

            // Create the config menu
            embed.appendDescription("\n\n")

            // Channel whitelist
            embed.appendDescription("**Channel whitelist:** ${hasChannelWhitelist.toReadableString()}")
            embed.appendDescription("\n")
            embed.appendDescription("**Channel blacklist:** ${hasChannelBlacklist.toReadableString()}")
            embed.appendDescription("\n")
            embed.appendDescription("**Whitelisted channels:** ${whitelistedChannels.toMentionedString { guild.getTextChannelById(it) }}")
            embed.appendDescription("\n")
            embed.appendDescription("**Blacklisted channels:** ${blacklistedChannels.toMentionedString { guild.getTextChannelById(it) }}")
            embed.appendDescription("\n\n")

            // User whitelist
            embed.appendDescription("**User whitelist:** ${hasUserWhitelist.toReadableString()}")
            embed.appendDescription("\n")
            embed.appendDescription("**User blacklist:** ${hasUserBlacklist.toReadableString()}")
            embed.appendDescription("\n")
            embed.appendDescription("**Whitelisted users:** ${whitelistedUsers.toMentionedString { guild.getMemberById(it) }}")
            embed.appendDescription("\n")
            embed.appendDescription("**Blacklisted users:** ${blacklistedUsers.toMentionedString { guild.getMemberById(it) }}")
            embed.appendDescription("\n\n")

            // Role whitelist
            embed.appendDescription("**Role whitelist:** ${hasRoleWhitelist.toReadableString()}")
            embed.appendDescription("\n")
            embed.appendDescription("**Role blacklist:** ${hasRoleBlacklist.toReadableString()}")
            embed.appendDescription("\n")
            embed.appendDescription("**Whitelisted roles:** ${whitelistedRoles.toMentionedString { guild.getRoleById(it) }}")
            embed.appendDescription("\n")
            embed.appendDescription("**Blacklisted roles:** ${blacklistedRoles.toMentionedString { guild.getRoleById(it) }}")
        }

        val actionRows = mutableListOf<ActionRow>()
        run {
            // Channels
            run {
                actionRows.add(ActionRow.of(
                    hasChannelWhitelist.toButton("guild_config - channel_whitelist", "Channel whitelist"),
                    hasChannelBlacklist.toButton("guild_config - channel_blacklist", "Channel blacklist"),
                    Button.secondary("guild_config - channel_whitelist_edit", "Channel whitelist edit"),
                    Button.secondary("guild_config - channel_blacklist_edit", "Channel blacklist edit")
                ))
            }

            // Users
            run {
                actionRows.add(ActionRow.of(
                    hasUserWhitelist.toButton("guild_config - user_whitelist", "User whitelist"),
                    hasUserBlacklist.toButton("guild_config - user_blacklist", "User blacklist"),
                    Button.secondary("guild_config - user_whitelist_edit", "User whitelist edit"),
                    Button.secondary("guild_config - user_blacklist_edit", "User blacklist edit")
                ))
            }

            // Roles
            run {
                actionRows.add(ActionRow.of(
                    hasRoleWhitelist.toButton("guild_config - role_whitelist", "Role whitelist"),
                    hasRoleBlacklist.toButton("guild_config - role_blacklist", "Role blacklist"),
                    Button.secondary("guild_config - role_whitelist_edit", "Role whitelist edit"),
                    Button.secondary("guild_config - role_blacklist_edit", "Role blacklist edit")
                ))
            }
        }

        return MessageBuilder()
            .setEmbeds(embed.build())
            .setActionRows(actionRows)
            .build()
    }
}

private enum class ConfigType {
    GUILD,
    PERSONAL;

    companion object {
        fun from(input: String): ConfigType = values().firstOrNull {
            it.name.equals(input, true)
        } ?: throw IllegalArgumentException("Invalid config type: $input")
    }
}
