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
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.util.zip.Inflater

class ItemProcessor(
    val event: MessageCreateEvent,
    val attachments: Set<Attachment>,
    val sourceBinUrls: Set<String>
) : Runnable {
    private val versionRegex = "(?:Minecraft Version:|Loading Minecraft) ([0-9.]+)".toRegex()

    override fun run() {
        println("Processing item for ${event.message.author?.username}")
        for (attachment in attachments) {
            runBlocking {
                val bytes: ByteArray = if (attachment.filename.endsWith(".log.gz")) {
                    // If the file is a .gz file, we need to decompress it
                    println("Decompressing ${attachment.filename}")
                    val decompressed = decompress(attachment.download())
                    decompressed
                } else if (attachment.filename.endsWith(".gz")) {
                    println("Skipping ${attachment.filename} as it is not a log file")
                    return@runBlocking
                } else attachment.download()

                var content = bytes.decodeToString()
                if (!ProcessorData.isMinecraftFile(content)) return@runBlocking

                content = ProcessorData.censor(content)
                val version = versionRegex.find(content)?.groupValues?.get(1) ?: return@runBlocking
                handle(version, content)
            }
        }

        if (sourceBinUrls.isNotEmpty()) {
            for (url in sourceBinUrls) {
                runBlocking {
                    var content = PasteUpload.get(url)
                    if (!ProcessorData.isMinecraftFile(content)) return@runBlocking

                    content = ProcessorData.censor(content)
                    val version = versionRegex.find(content)?.groupValues?.get(1) ?: return@runBlocking
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

    private fun decompress(bytes: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(bytes)

        val outputStream = ByteArrayOutputStream(bytes.size)
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }

        outputStream.close()
        val output = outputStream.toByteArray()
        println("output: ${outputStream.toByteArray().decodeToString()}")

        inflater.end()
        return output
    }

    private fun stripVersion(content: String) =
        content.replace("[0-9]+\\.[0-9]+(\\..*)?".toRegex().find(content)?.groupValues?.get(1) ?: "", "")
}