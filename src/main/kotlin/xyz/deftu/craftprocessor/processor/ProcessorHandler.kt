package xyz.deftu.craftprocessor.processor

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.sharding.ShardManager
import xyz.deftu.deftils.Multithreader
import java.util.concurrent.TimeUnit

object ProcessorHandler {
    private val multithreader = Multithreader(25)
    const val ITEM_DELETE_ID = "item-remove"

    fun start(client: ShardManager) {
        client.addEventListener(this)
        multithreader.schedule({
            IssueList.reload()
            LogIdentifier.reload()
            ProcessableExtensions.reload()
            UrlCensor.reload()
        }, 0, 10, TimeUnit.MINUTES)
    }

    /**
     * Event handler for running the attachment/item
     * processor in a seperate thread.
     */
    @SubscribeEvent
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        val message = event.message
        val attachments = event.message.attachments
        if (attachments.isEmpty()) return
        if (attachments.any {
            !it.isImage && !it.isVideo && ProcessableExtensions.isProcessable(it.fileExtension ?: "")
        }) {
            multithreader.runAsync(ItemProcessor(event, attachments))
        }
    }

    /**
     * Event handler for the delete button on processed
     * items.
     */
    @SubscribeEvent
    fun onButtonInteractionReceived(event: ButtonInteractionEvent) {
        if (event.button.id != ITEM_DELETE_ID) return
        val message = event.message
        if (!message.author.isBot || message.author.idLong != event.jda.selfUser.idLong) return
        if (
            // Check if this is being done in a DM, if true, pass
            !event.isFromGuild ||
            // Check if the user is the same as the author of the original message, if true, pass (this is not very reliable...)
            message.contentStripped.startsWith(event.user.asTag) ||
            // Check if the user has the permission to manage messages, if true, pass
            event.member?.hasPermission(Permission.MESSAGE_MANAGE) == true
        ) {
            message.delete().queue()
        } else event.reply("You do not have permission to delete this message.")
            .setEphemeral(true)
            .queue()
    }
}
