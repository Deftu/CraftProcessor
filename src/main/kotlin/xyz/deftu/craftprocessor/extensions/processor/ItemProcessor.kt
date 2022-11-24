package xyz.deftu.craftprocessor.extensions.processor

import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Attachment
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinInstant
import xyz.deftu.craftprocessor.util.PasteUpload
import java.time.OffsetDateTime

class ItemProcessor(
    val event: MessageCreateEvent,
    val attachments: Set<Attachment>,
    val sourceBinUrls: Set<String>
) : Runnable {
    private val versionRegex = "(?:Minecraft Version:|Loading Minecraft) ([0-9.]+)".toRegex()

    override fun run() {
        for (attachment in attachments) {
            runBlocking {
                println("Processing attachment")
                var content = attachment.download().decodeToString()
                if (!ProcessorData.isMinecraftFile(content)) return@runBlocking
                println("Processing minecraft file")

                content = ProcessorData.censor(content)
                val version = versionRegex.find(content)?.groupValues?.get(1) ?: return@runBlocking
                println("Version: $version")
                handle(version, content)
            }
        }

        if (sourceBinUrls.isNotEmpty()) {
            for (url in sourceBinUrls) {
                runBlocking {
                    println("Processing sourcebin link")
                    var content = PasteUpload.get(url)
                    println("Content: $content")
                    if (!ProcessorData.isMinecraftFile(content)) return@runBlocking
                    println("Processing minecraft file")

                    content = ProcessorData.censor(content)
                    val version = versionRegex.find(content)?.groupValues?.get(1) ?: return@runBlocking
                    println("Version: $version")
                    handle(version, content)
                }
            }
        }
    }

    suspend fun handle(
        version: String,
        content: String
    ) {
        val strippedVersion = stripVersion(version)
        val versions = ProcessorData.forVersion(strippedVersion) ?: return
        val fileUrl = PasteUpload.upload(content)
        var versionString = ""
        for (issueVersion in versions) {
            for (version in issueVersion.versions) {
                if (versionString.contains(version)) continue
                versionString += version
                if (issueVersion != versions.last()) versionString += ", "
                if (version != issueVersion.versions.last()) continue
            }
        }

        event.message.channel.createMessage {
            this.content = "**${event.message.author?.tag}** uploaded a log!\n"

            val messageContent = event.message.content.replace(PasteUpload.SOURCE_BIN_REGEX, "")
            if (messageContent.isNotBlank()) {
                this.content += "\"$messageContent\""
                this.content += "\n"
            }

            this.content += "\n"
            this.content += "**Minecraft Version:** $versionString\n"
            this.content += "**Log:** $fileUrl"

            fun applyEmbed(version: IssueVersion) {
                version.issues.forEach { issue ->
                    issue.causes.forEach { cause ->
                        if (cause.method.run(cause.text, content)) {
                            embed {
                                title = issue.title
                                description = issue.solution.let { solution ->
                                    if (cause.method == IssueSearchMethod.REGEX) {
                                        // replace all $1, $2, etc with the groups
                                        solution.replace(Regex("\\$\\d")) { match ->
                                            cause.text.toRegex().find(content)?.groupValues?.get(match.value.toInt()) ?: ""
                                        }
                                    }

                                    solution
                                }

                                color = Color(issue.severity.color.rgb)
                                timestamp = OffsetDateTime.now().toInstant().toKotlinInstant()
                                footer {
                                    text = ""

                                    if (event.message.author != null) {
                                        icon = event.message.author?.avatar?.url
                                        text += event.message.author?.id?.value
                                        text += " - "
                                    }

                                    text += issue.severity.text
                                }
                            }
                        }
                    }
                }
            }

            versions.forEach(::applyEmbed)
            ProcessorData.forVersion("global").forEach(::applyEmbed)

            actionRow {
                interactionButton(ButtonStyle.Danger, ProcessorExtension.DELETE_BUTTON_ID) {
                    label = "Delete"
                }
            }
        }

        event.message.delete("Minecraft item processed")
    }

    private fun stripVersion(content: String) =
        content.replace("[0-9]+\\.[0-9]+(\\..*)?".toRegex().find(content)?.groupValues?.get(1) ?: "", "")
}