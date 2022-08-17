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
import xyz.deftu.craftprocessor.config.GuildConfig
import xyz.deftu.craftprocessor.config.UserConfig
import xyz.deftu.craftprocessor.utils.HasteUpload
import java.time.OffsetDateTime

class ItemProcessor(
    val event: MessageReceivedEvent,
    val attachments: List<Attachment>
) : Runnable {
    private val versionRegex = "Minecraft Version: (.*)".toRegex()

    override fun run() {
        val config = UserConfig.getUserConfig(event.author.id)
        if (config != null && config.hasDisabled) return

        if (event.isFromGuild) {
            val guild = event.guild
            val channelId = event.channel.id
            val member = event.member!!
            val userId = member.id
            val config = GuildConfig.getGuildConfig(guild.id)
            if (config != null) {
                if (
                    (config.hasChannelBlacklist && config.isChannelBlacklisted(channelId)) ||
                    (config.hasChannelWhitelist && !config.isChannelWhitelisted(channelId)) ||
                    (config.hasUserBlacklist && config.isUserBlacklisted(userId)) ||
                    (config.hasUserWhitelist && !config.isUserWhitelisted(userId)) ||
                    (config.hasRoleBlacklist && member.roles.any { config.isRoleBlacklisted(it.id) }) ||
                    (config.hasRoleWhitelist && member.roles.none { config.isRoleWhitelisted(it.id) })
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
        val version = IssueList.fromVersion(stripVersion(version)) ?: return
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
                        .setTimestamp(OffsetDateTime.now())
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
    }

    private fun stripVersion(content: String) =
        content.replace("[0-9]+\\.[0-9]+(\\..*)?".toRegex().find(content)?.groupValues?.get(1) ?: "", "")
}
