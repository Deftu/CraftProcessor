package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.DataHandler
import xyz.deftu.craftprocessor.config.ConfigManager
import xyz.deftu.embed
import xyz.deftu.jdac.BaseCommand

class TermsCommand(
    client: JDA
) : BaseCommand() {
    init {
        client.addEventListener(this)
    }

    override fun getName() = "terms"
    override fun getDescription() = "Sends the Terms of Service message for the bot."
    override fun getDefaultMemberPermissions() = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)

    override fun handle(event: SlashCommandInteractionEvent) {
        fun fail() {
            event.reply("There was an error fetching the Terms of Service.")
                .setEphemeral(true)
                .queue()
        }

        try {
            val terms = DataHandler.fetchData("terms.txt")
                .replace("\$NAME", CraftProcessor.NAME)
                .replace("\$VERSION", CraftProcessor.VERSION)
            event.channel.sendMessage(
                MessageCreateBuilder()
                .setEmbeds(embed {
                    title("Terms of Service")
                    colorRaw(CraftProcessor.COLOR)
                    description(terms)
                }).addActionRow(
                    Button.danger("tos-opt-out", "Opt-out")
                ).build()).queue()
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
