package xyz.deftu.craftprocessor.processor

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ItemProcessor(
    val event: MessageReceivedEvent,
    val message: Message,
    val attachments: List<Attachment>
) : Runnable {
    private val versionRegex = "Minecraft Version: (.*)".toRegex()

    override fun run() {
        for (attachment in attachments) {
            var content = attachment.proxy.download().get()?.bufferedReader()?.readText() ?: continue
            if (!LogIdentifier.isLog(content)) continue
            content = UrlCensor.censor(content)
            val version = versionRegex.find(content)?.groupValues?.get(1) ?: continue
            handle(version, content)
        }
    }

    fun handle(version: String, content: String) {

    }
}
