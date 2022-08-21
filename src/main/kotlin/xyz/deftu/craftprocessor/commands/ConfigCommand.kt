package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import xyz.deftu.craftprocessor.config.ConfigManager

object ConfigCommand {
    fun initialize(client: JDA, action: CommandListUpdateAction) {
        client.addEventListener(this)
        action.addCommands(
            Commands.slash("config", "Displays a config menu.")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addOption(OptionType.STRING, "type", "The type of config you'd like to configure.", true, true)
        ).queue()
    }

    @SubscribeEvent
    fun onCommandAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        if (event.interaction.name != "config") return
        when (event.focusedOption.name) {
            "type" -> event.replyChoiceStrings(run {
                val choices = mutableListOf("Personal")
                if (event.isFromGuild) choices.add("Guild")
                choices
            }).queue()
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
        val config = ConfigManager.getGuild(guildId, true)!!

        event.reply(ConfigManager.createGuildMessage(config, guild))
            .queue()
    }

    fun handlePersonalConfig(event: SlashCommandInteractionEvent) {
        val user = event.user
        val userId = user.id
        val config = ConfigManager.getUser(userId, true)!!

        event.reply(ConfigManager.createUserMessage(config, user))
            .queue()
    }

    @SubscribeEvent
    fun onButtonClicked(event: ButtonInteractionEvent) {
        val id = event.interaction.componentId
        val type = id.substringBefore("-").replace("_config ", "")
        when (type) {
            "guild" -> handleGuildConfigButton(event)
            "personal" -> handlePersonalConfigButton(event)
        }
    }

    @SubscribeEvent
    fun onSelectMenuPicked(event: SelectMenuInteractionEvent) {
        val id = event.interaction.componentId
        val type = id.substringBefore("-").replace("_config ", "")
        when (type) {
            "guild" -> handleGuildConfigSelectMenu(event)
        }
    }

    private fun handleGuildConfigButton(event: ButtonInteractionEvent) {
        val id = event.interaction.componentId
        val type = id.substringAfter("-").replace(" ", "")
        val guild = event.guild!!
        val config = ConfigManager.getGuild(guild.id, true)!!
        when (type) {
            "channel_whitelist" -> config.channelWhitelist = !config.channelWhitelist
            "channel_blacklist" -> config.channelBlacklist = !config.channelBlacklist
            "role_whitelist" -> config.roleWhitelist = !config.roleWhitelist
            "role_blacklist" -> config.roleBlacklist = !config.roleBlacklist
        }
        event.message.editMessage(ConfigManager.createGuildMessage(config, guild))
            .queue()
        ConfigManager.saveGuild(config)
        event.deferEdit().queue()
    }

    private fun handleGuildConfigSelectMenu(event: SelectMenuInteractionEvent) {
        val id = event.interaction.componentId
        val type = id.substringAfter("-").replace(" ", "")
        val guild = event.guild!!
        val config = ConfigManager.getGuild(guild.id, true)!!
        val values = event.values
        when (type) {
            "channel_whitelist_edit" -> {
                config.channelWhitelist = values.isNotEmpty()
                config.whitelistedChannels = values.map {
                    it.toLong()
                }.toMutableList()
            }
            "channel_blacklist_edit" -> {
                config.channelBlacklist = values.isNotEmpty()
                config.blacklistedChannels = values.map {
                    it.toLong()
                }.toMutableList()
            }
            "role_whitelist_edit" -> {
                config.roleWhitelist = values.isNotEmpty()
                config.whitelistedRoles = values.map {
                    it.toLong()
                }.toMutableList()
            }
            "role_blacklist_edit" -> {
                config.roleBlacklist = values.isNotEmpty()
                config.blacklistedRoles = values.map {
                    it.toLong()
                }.toMutableList()
            }
        }
        event.message.editMessage(ConfigManager.createGuildMessage(config, guild))
            .queue()
        ConfigManager.saveGuild(config)
        event.deferEdit().queue()
    }

    private fun handlePersonalConfigButton(event: ButtonInteractionEvent) {
        val id = event.interaction.componentId
        val type = id.substringAfter("-").replace(" ", "")
        val user = event.user
        val config = ConfigManager.getUser(user.id, true)!!
        when (type) {
            "toggle" -> config.toggle = !config.toggle
        }
        event.message.editMessage(ConfigManager.createUserMessage(config, user))
            .queue()
        ConfigManager.saveUser(config)
        event.deferEdit().queue()
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
