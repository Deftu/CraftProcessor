package xyz.deftu.craftprocessor.processor

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

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
        val version = IssueList.fromVersion(version.replace("[0-9]+\\.[0-9]+(\\..*)?".toRegex().find(version)?.groupValues?.get(1) ?: return, "")) ?: return
        val message = MessageBuilder()
            .append("**${event.author.asTag}** uploaded a log!\n").apply {
                if (event.message.contentRaw.isNotBlank()) {
                    append("\"${event.message.contentRaw}\"")
                    append("\n")
                }
            }.append("\n")
            .append("**Version(s):** ${version.versions.joinToString(", ")}\n")
            .append("**File:** ${HasteUpload.upload(content)}")
        val embeds = mutableListOf<MessageEmbed>()

        fun applyWith(version: IssueVersion) {
            version.issues.forEach { issue ->
                if (issue.causes.all {
                    it.method.run(it.text, content)
                }) {
                    embeds.add(EmbedBuilder()
                        .setTitle(issue.title)
                        .setDescription(issue.solution)
                        .setColor(issue.severity.color)
                        .setFooter(issue.severity.text)
                        .build())
                }
            }
        }

        applyWith(version)
        IssueList.fromVersion("global")?.let(::applyWith)

        message.setEmbeds(embeds)
        val sentMessage = event.channel.sendMessage(message
            .setActionRows(ActionRow.of(
                Button.danger(ProcessorHandler.ITEM_DELETE_ID, "Delete")
            )).build()).complete()
        if (!event.isFromGuild || event.guild?.selfMember?.hasPermission(Permission.MESSAGE_MANAGE) == true) event.message.delete().queue()
        else sentMessage.editMessage(MessageBuilder(sentMessage)
            .append("\n\n")
            .append("Failed to delete original log message, this may result in some sensitive info being leaked!")
            .build()).queue()
    }
}
