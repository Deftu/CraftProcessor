package xyz.deftu.craftprocessor.processor

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import xyz.deftu.deftils.Multithreader
import java.util.concurrent.TimeUnit

object ProcessorHandler : Thread("Processor") {
    private val multithreader = Multithreader(25)

    override fun run() {
        multithreader.schedule({
            LogIdentifier.reload()
            ProcessableExtensions.reload()
            UrlCensor.reload()
        }, 0, 10, TimeUnit.SECONDS)
    }

    @SubscribeEvent
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        val message = event.message
        val attachments = event.message.attachments
        if (attachments.isEmpty()) return
        if (attachments.any {
            !it.isImage && !it.isVideo && ProcessableExtensions.isProcessable(it.fileExtension ?: "")
        }) {
            multithreader.runAsync(ItemProcessor(event, message, attachments))
        }
    }
}
