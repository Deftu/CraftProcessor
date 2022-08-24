package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import xyz.deftu.MessageDecoration
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.utils.toFormattedTime
import xyz.deftu.embed
import xyz.deftu.jdac.BaseCommand
import xyz.deftu.reply
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class AboutCommand : BaseCommand() {
    override fun getName() = "about"
    override fun getDescription() = "Shows off some neat information about the bot."

    override fun handle(event: SlashCommandInteractionEvent) {
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
