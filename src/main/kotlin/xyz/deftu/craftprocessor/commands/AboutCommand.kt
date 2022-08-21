package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.utils.reply
import xyz.deftu.craftprocessor.utils.toFormattedTime
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

        val embed = CraftProcessor.createEmbed()
            .setTitle(CraftProcessor.NAME)
            .setTimestamp(OffsetDateTime.now())
        embed.descriptionBuilder.apply {
            append("CraftProcessor is a Discord bot made by [UnifyCraft](https://github.com/UnifyCraft) that parses and handles Minecraft crash reports and logs. It is written in the [Kotlin programming language](https://kotlinlang.org/) and uses the [JDA library](https://github.com/DV8FromTheWorld/JDA).").append("\n\n")
            append("**").append("Version: ").append("**").append(CraftProcessor.VERSION).append("\n")
            append("**").append("Uptime: ").append("**").append(CraftProcessor.startTime.until(OffsetDateTime.now(), ChronoUnit.MILLIS).toFormattedTime()).append("\n")
            append("**").append("Source: ").append("**").append("https://github.com/UnifyCraft/${CraftProcessor.NAME}")
        }

        event.reply(embed.build()).queue()
    }
}
