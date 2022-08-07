package xyz.deftu.craftprocessor.processor

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import xyz.deftu.deftils.Multithreader
import java.util.concurrent.TimeUnit

object ProcessorHandler : Thread("Processor") {
    private val multithreader = Multithreader(25)
    private val versionRegex = "Minecraft Version: (.*)".toRegex()

    override fun run() {
        multithreader.schedule({
            LogIdentifier.reload()
        }, 0, 10, TimeUnit.SECONDS)
    }

    @SubscribeEvent
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        val attachments = event.message.attachments
        if (attachments.isEmpty()) return
        multithreader.runAsync {
            for (attachment in attachments) {
                if (attachment.isImage || attachment.isVideo) continue
                val content = attachment.proxy.download().get()?.bufferedReader()?.readText() ?: continue
                if (!LogIdentifier.isLog(content)) continue
                val version = versionRegex.find(content)?.groupValues?.get(1) ?: continue
                println("Found version: $version")
                println("Content: $content")
            }
        }
    }
}
