package xyz.deftu.craftprocessor.commands

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import xyz.deftu.craftprocessor.DataHandler
import xyz.deftu.craftprocessor.utils.toMessageEmbed

object TermsCommand {
    fun initialize(client: JDA, action: CommandListUpdateAction) {
        client.addEventListener(this)
        action.addCommands(
            Commands.slash("terms", "Sends the Terms of Service message for the bot.")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
        ).queue()
    }

    @SubscribeEvent
    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.interaction.name != "terms") return

        fun fail() {
            event.reply("There was an error fetching the Terms of Service.")
                .setEphemeral(true)
                .queue()
        }

        try {
            val termsRaw = DataHandler.fetchData("terms.json", "rework") // We'll use the rework branch explicitly while in the testing phase.
            val termsJsonRaw = JsonParser.parseString(termsRaw)
            if (!termsJsonRaw.isJsonObject) fail().also { return }
            val termsJson = termsJsonRaw.asJsonObject
            event.channel.sendMessage(MessageBuilder()
                .setEmbeds(termsJson.toMessageEmbed())
                .build()).queue()
            event.reply("Sent the Terms of Service successfully.")
                .setEphemeral(true)
                .queue()
        } catch (e: Exception) {
            fail()
        }
    }
}
