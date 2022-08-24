package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import xyz.deftu.MessageDecoration
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.utils.toFormattedTime
import xyz.deftu.embed
import xyz.deftu.reply
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

object AboutCommand {
    fun initialize(client: JDA, action: CommandListUpdateAction) {
        client.addEventListener(this)
        action.addCommands(
            Commands.slash("about", "Shows off some neat information about the bot.")
        )
    }

    @SubscribeEvent
    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.interaction.name != "about") return

        event.reply(embed {
            title(CraftProcessor.NAME)
            colorRaw(CraftProcessor.COLOR)
            timestamp(OffsetDateTime.now())
            description {
                append("${CraftProcessor.NAME} is a Discord bot made by [Deftu](https://github.com/Deftu) that parses and handles Minecraft crash reports and logs. It is written in the [Kotlin programming language](https://kotlinlang.org/) and uses the [JDA library](https://github.com/DV8FromTheWorld/JDA).").append("\n\n")
                append("Version: ", MessageDecoration.BOLD).append(CraftProcessor.VERSION).append("\n")
                append("Uptime: ", MessageDecoration.BOLD).append(CraftProcessor.startTime.until(OffsetDateTime.now(), ChronoUnit.MILLIS).toFormattedTime()).append("\n")
                append("Source: ", MessageDecoration.BOLD).append("https://github.com/Deftu/${CraftProcessor.NAME}")
            }
        }).queue()
    }
}
