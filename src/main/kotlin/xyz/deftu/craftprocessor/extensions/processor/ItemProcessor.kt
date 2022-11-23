package xyz.deftu.craftprocessor.extensions.processor

import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Attachment
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinInstant
import xyz.deftu.craftprocessor.util.PasteUpload
import java.time.OffsetDateTime

class ItemProcessor(
    val event: MessageCreateEvent,
    val attachments: Set<Attachment>
) : Runnable {
    private val versionRegex = "Minecraft Version: (.*)".toRegex()

    override fun run() {
        println("Processing message")
        for (attachment in attachments) {
            runBlocking {
                var content = attachment.download().decodeToString()
                if (!ProcessorData.isMinecraftFile(content)) return@runBlocking

                content = ProcessorData.censor(content)
                val version = versionRegex.find(content)?.groupValues?.get(1) ?: return@runBlocking
                handle(version, content)
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
            if (event.message.content.isNotBlank()) {
                this.content += "\"${event.message.content}\""
                this.content += "\n"
            }

            this.content += "\n"
            this.content += "**Minecraft Version:** $versionString\n"
            this.content += "**Log:** $fileUrl"

            fun applyEmbed(version: IssueVersion) {
                version.issues.forEach { issue ->
                    if (issue.causes.all {
                            it.method.run(it.text, content)
                        }) {
                        embed {
                            title = issue.title
                            description = issue.solution
                            color = Color(issue.severity.color.rgb)
                            timestamp = OffsetDateTime.now().toInstant().toKotlinInstant()
                            footer {
                                text = issue.severity.text
                            }
                        }
                    }
                }
            }

            versions.forEach(::applyEmbed)
            ProcessorData.forVersion("global").forEach(::applyEmbed)
        }
    }

    private fun stripVersion(content: String) =
        content.replace("[0-9]+\\.[0-9]+(\\..*)?".toRegex().find(content)?.groupValues?.get(1) ?: "", "")
}