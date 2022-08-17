package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.sharding.ShardManager
import xyz.deftu.craftprocessor.config.GuildConfig
import xyz.deftu.craftprocessor.config.GuildConfigData

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
            ConfigType.GUILD -> {
                val guild = event.guild!!
                var config = GuildConfig.getGuildConfig(guild.id)
                if (config == null) config = GuildConfig.createNewGuildConfig(guild)
                event.reply(config.toMessage(guild))
                    .setEphemeral(true)
                    .queue()
            }
            ConfigType.PERSONAL -> println("SoonTM")
        }
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
