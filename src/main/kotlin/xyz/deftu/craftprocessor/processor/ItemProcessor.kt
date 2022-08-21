package xyz.deftu.craftprocessor.processor

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import xyz.deftu.craftprocessor.StatsTracker
import xyz.deftu.craftprocessor.config.ConfigManager
import xyz.deftu.craftprocessor.utils.HasteUpload
import java.time.OffsetDateTime

class ItemProcessor(
    val event: MessageReceivedEvent,
    val attachments: List<Attachment>
) : Runnable {
    private val versionRegex = "Minecraft Version: (.*)".toRegex()

    override fun run() {
        val config = ConfigManager.getUser(event.author.id)
        if (config != null && !config.toggle) return

        if (event.isFromGuild) {
            val guild = event.guild
            val channelId = event.channel.idLong
            val member = event.member!!
            val config = ConfigManager.getGuild(guild.id)
            if (config != null) {
                if (
                    (config.channelWhitelist && !config.isChannelWhitelisted(channelId)) ||
                    (config.channelBlacklist && config.isChannelBlacklisted(channelId)) ||

                    (config.roleWhitelist && member.roles.none { config.isRoleWhitelisted(it.idLong) }) ||
                    (config.roleBlacklist && member.roles.any { config.isRoleBlacklisted(it.idLong) })
                ) return
            }
        }

        for (attachment in attachments) {
            var content = attachment.proxy.download().get()?.bufferedReader()?.readText() ?: continue
            if (!LogIdentifier.isLog(content)) continue
            content = UrlCensor.censor(content)
            val version = versionRegex.find(content)?.groupValues?.get(1) ?: continue
            handle(version, content)
        }
    }

    fun handle(version: String, content: String) {
        val strippedVersion = stripVersion(version)
        val versions = IssueList.fromVersion(strippedVersion) ?: return
        val fileUrl = HasteUpload.upload(content)
        var versionString = ""
        for (issueVersion in versions) {
            for (version in issueVersion.versions) {
                if (versionString.contains(version)) continue
                versionString += version
                if (issueVersion != versions.last()) versionString += ", "
                if (version != issueVersion.versions.last()) continue
            }
        }

        val message = MessageBuilder()
            .append("**${event.author.asTag}** uploaded a log!\n").apply {
                if (event.message.contentRaw.isNotBlank()) {
                    append("\"${event.message.contentRaw}\"")
                    append("\n")
                }
            }.append("\n")
            .append("**Version(s):** ${versionString}\n")
            .append("**File:** $fileUrl")
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
                        .setTimestamp(OffsetDateTime.now())
                        .build())
                }
            }
        }

        for (version in versions) applyWith(version)
        IssueList.fromVersion("global").getOrNull(0)?.let(::applyWith)

        message.setEmbeds(embeds)
        val sentMessage = event.channel.sendMessage(message
            .setActionRows(ActionRow.of(
                Button.danger(ProcessorHandler.ITEM_DELETE_ID, "Delete")
            )).build()).complete()
        if (event.isFromGuild) {
            fun editMessageDeleteError() {
                sentMessage.editMessage(MessageBuilder(sentMessage)
                    .append("\n\n")
                    .append("Failed to delete original log message, this may result in some sensitive info being leaked!")
                    .build()).queue()
            }

            if (event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) event.message.delete().queue({}, {
                if (it.message?.contains("Unknown") == false) {
                    editMessageDeleteError()
                }
            }) else editMessageDeleteError()
        }

        StatsTracker.incrementItemsProcessed()
        StatsTracker.incrementItemsProcessed(strippedVersion)
    }

    private fun stripVersion(content: String) =
        content.replace("[0-9]+\\.[0-9]+(\\..*)?".toRegex().find(content)?.groupValues?.get(1) ?: "", "")
}
