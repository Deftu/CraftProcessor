package xyz.deftu.craftprocessor.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.utils.messages.MessageEditData
import xyz.deftu.craftprocessor.config.ConfigManager
import xyz.deftu.jdac.BaseCommand

class PersonalConfigCommand(
    client: JDA
) : BaseCommand() {
    init {
        client.addEventListener(this)
    }

    override fun getName() = "personal"
    override fun getDescription() = "Displays the config menu for your personal settings."

    override fun handle(event: SlashCommandInteractionEvent) {
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
        if (type != "personal") return

        val configType = id.substringAfter("-").replace(" ", "")
        val user = event.user
        val config = ConfigManager.getUser(user.id, true)!!
        when (configType) {
            "toggle" -> config.toggle = !config.toggle
        }

        event.message.editMessage(MessageEditData.fromCreateData(ConfigManager.createUserMessage(config, user)))
            .queue()
        ConfigManager.saveUser(config)
        event.deferEdit().queue()
    }
}
