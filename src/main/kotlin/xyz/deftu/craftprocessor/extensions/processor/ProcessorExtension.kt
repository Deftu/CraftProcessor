package xyz.deftu.craftprocessor.extensions.processor

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.hasPermission
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import xyz.deftu.craftprocessor.util.PasteUpload
import xyz.deftu.deftils.Multithreader
import java.util.concurrent.TimeUnit

class ProcessorExtension : Extension() {
    companion object {
        const val DELETE_BUTTON_ID = "processs-item-delete"
    }

    private val multithreader = Multithreader(25)
    override val name = "processor"

    override suspend fun setup() {
        multithreader.schedule({
            ProcessorData.reload()
        }, 0, 15, TimeUnit.MINUTES)

        event<MessageCreateEvent> {
            action {
                if (event.message.author?.isBot == true) return@action

                val attachments = event.message.attachments
                if (attachments.any {
                    val fileExtension = it.filename.split(".").last()
                    !it.isImage && ProcessorData.isProcessableFile(fileExtension)
                } || PasteUpload.SOURCE_BIN_REGEX.containsMatchIn(event.message.content)) {
                    val sourceBinLinks = PasteUpload.SOURCE_BIN_REGEX.findAll(event.message.content).map {
                        it.groupValues[0]
                    }.toSet()
                    multithreader.runAsync(ItemProcessor(event, attachments, sourceBinLinks))
                }
            }
        }

        event<ButtonInteractionCreateEvent> {
            check {
                failIf(event.interaction.componentId != DELETE_BUTTON_ID)
            }

            action {
                val message = event.interaction.message
                if (message.author?.isBot == false || message.author?.id?.value != event.kord.selfId.value) return@action

                val channel = event.interaction.channel.asChannel().type
                val embed = message.embeds.firstOrNull()
                val footer = embed?.footer
                if (
                    // Check if this is being done in a DM, if true, pass
                    (channel == ChannelType.DM || channel == ChannelType.GroupDM) ||
                    // Check if the user is the same as the author of the original message, if true, pass (this is not very reliable...)
                    footer?.text?.startsWith(event.interaction.user.id.value.toString()) == true ||
                    // As a last resort to see if the requester is the original user, check if the tag at the start of the message is the same as the user's tag, if true, pass
                    message.content.startsWith(event.interaction.user.tag) ||
                    // Check if the user has the permission to manage messages, if true, pass
                    event.interaction.user.asMember((event.interaction.channel.asChannel() as GuildChannel).guildId).hasPermission(Permission.ManageMessages)
                ) {
                    message.delete("Deleted by ${event.interaction.user.asMember((event.interaction.channel.asChannel() as GuildChannel).guildId).mention}")
                } else event.interaction.respondEphemeral {
                    content = "You do not have permission to delete this message."
                }
            }
        }
    }
}
