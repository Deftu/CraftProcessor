package xyz.deftu.craftprocessor.extensions.processor

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.runBlocking
import xyz.deftu.deftils.Multithreader
import java.util.concurrent.TimeUnit

class ProcessorExtension : Extension() {
    private val multithreader = Multithreader(25)
    override val name = "processor"

    override suspend fun setup() {
        multithreader.schedule({
            println("processor reload")
            runBlocking {
                println("processor reload 2")
                ProcessorData.reload()
                println("processor reload 3")
            }
        }, 0, 15, TimeUnit.MINUTES)

        event<MessageCreateEvent> {
            action {
                if (event.message.author?.isBot == true) return@action
                println("Received message - not a bot")

                val attachments = event.message.attachments
                if (attachments.isEmpty()) return@action
                println("Received message - has attachments")

                if (attachments.any {
                    val fileExtension = it.filename.split(".").last()
                    println("Received message - attachment is $fileExtension")
                    !it.isImage && ProcessorData.isProcessableFile(fileExtension)
                }) {
                    println("Received message - attachment is processable")
                    multithreader.runAsync(ItemProcessor(event, attachments))
                }
            }
        }
    }
}
