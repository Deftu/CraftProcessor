package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.DataHandler
import xyz.deftu.craftprocessor.config.ConfigManager

object TermsCommand {
    fun initialize(client: JDA, action: CommandListUpdateAction) {
        client.addEventListener(this)
        action.addCommands(
            Commands.slash("terms", "Sends the Terms of Service message for the bot.")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
        )
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
            val terms = DataHandler.fetchData("terms.txt", "rework")
                .replace("\$NAME", CraftProcessor.NAME)
                .replace("\$VERSION", CraftProcessor.VERSION)
            event.channel.sendMessage(MessageBuilder()
                .setEmbeds(CraftProcessor.createEmbed()
                    .setTitle("Terms of Service")
                    .setDescription(terms)
                    .build())
                .setActionRows(ActionRow.of(
                    Button.danger("tos-opt-out", "Opt-out")
                )).build()).queue()
            event.reply("Sent the Terms of Service successfully.")
                .setEphemeral(true)
                .queue()
        } catch (e: Exception) {
            fail()
        }
    }

    @SubscribeEvent
    fun onButtonClicked(event: ButtonInteractionEvent) {
        if (event.interaction.componentId != "tos-opt-out") return

        val action = event.deferReply()
            .setEphemeral(true)
            .complete()
        val config = ConfigManager.getUser(event.user.id, true)!!
        config.toggle = false
        ConfigManager.saveUser(config)
        action.editOriginal("Opted you out of the bot.").queue()
    }
}
