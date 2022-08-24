package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.utils.messages.MessageEditData
import xyz.deftu.craftprocessor.config.ConfigManager
import xyz.deftu.jdac.BaseCommand

class GuildConfigCommand(
    client: JDA
) : BaseCommand() {
    init {
        client.addEventListener(this)
    }

    override fun getName() = "guild"
    override fun getDescription() = "Displays the config menu for the current guild's settings."
    override fun getDefaultMemberPermissions() = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val guildId = guild.id
        val config = ConfigManager.getGuild(guildId, true)!!

        event.reply(ConfigManager.createGuildMessage(config, guild))
            .queue()
    }

    @SubscribeEvent
    fun onButtonClicked(event: ButtonInteractionEvent) {
        val id = event.interaction.componentId
        val type = id.substringBefore("-").replace("_config ", "")
        if (type != "guild") return

        val configType = id.substringAfter("-").replace(" ", "")
        val guild = event.guild!!
        val config = ConfigManager.getGuild(guild.id, true)!!
        when (configType) {
            "channel_whitelist" -> config.channelWhitelist = !config.channelWhitelist
            "channel_blacklist" -> config.channelBlacklist = !config.channelBlacklist
            "role_whitelist" -> config.roleWhitelist = !config.roleWhitelist
            "role_blacklist" -> config.roleBlacklist = !config.roleBlacklist
        }

        event.message.editMessage(MessageEditData.fromCreateData(ConfigManager.createGuildMessage(config, guild)))
            .queue()
        ConfigManager.saveGuild(config)
        event.deferEdit().queue()
    }

    @SubscribeEvent
    fun onSelectMenuPicked(event: SelectMenuInteractionEvent) {
        val id = event.interaction.componentId
        val type = id.substringBefore("-").replace("_config ", "")
        if (type != "guild") return

        val configType = id.substringAfter("-").replace(" ", "")
        val guild = event.guild!!
        val config = ConfigManager.getGuild(guild.id, true)!!
        val values = event.values
        when (configType) {
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

        event.message.editMessage(MessageEditData.fromCreateData(ConfigManager.createGuildMessage(config, guild)))
            .queue()
        ConfigManager.saveGuild(config)
        event.deferEdit().queue()
    }
}
