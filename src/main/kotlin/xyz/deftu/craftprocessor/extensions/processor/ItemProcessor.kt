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
import java.util.zip.GZIPInputStream

class ItemProcessor(
    val event: MessageCreateEvent,
    val attachments: Set<Attachment>,
    val sourceBinUrls: Set<String>
) : Runnable {
    private val versionRegex = "(?:Minecraft Version:|Loading Minecraft) ([0-9.]+)".toRegex()
    private val timeRegex = "^time: (?<date>[\\w \\/\\.:\\-]+)\$".toRegex(setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
    private val modLoaderRegexes = listOf(
        "Forge" to "Forge version ([0-9.]+) for Minecraft ([0-9.]+)".toRegex(),
        "Fabric" to "Loading Minecraft ([0-9.]+) with Fabric Loader ([0-9.]+)|Fabric Mods:".toRegex(),
        "Quilt" to "Loading Minecraft ([0-9.]+) with Quilt Loader ([0-9.]+)|Quilt Mods:".toRegex()
    )
    private val devEnvUserRegex = "Player\\d{3}".toRegex()

    private val userDirectoryRegex = "(\\/Users\\/[\\w\\s]+|\\/home\\/\\w+|C:\\\\Users\\\\[\\w\\s]+)".toRegex()
    private val emailRegex = "[a-zA-Z0-9_.+-]{1,50}@[a-zA-Z0-9-]{1,50}\\.[a-zA-Z-.]{1,10}".toRegex()
    private val usernameRegex = "(?:\\/INFO]: Setting user: (\\w{1,16})|--username, (\\w{1,16}))".toRegex()

    override fun run() {
        for (attachment in attachments) {
            runBlocking {
                val bytes: ByteArray = if (attachment.filename.endsWith(".log.gz")) {
                    // If the file is a .gz file, we need to decompress it
                    val decompressed = decompressGzip(attachment.download())
                    decompressed
                } else if (attachment.filename.endsWith(".gz")) {
                    return@runBlocking
                } else attachment.download()

                var content = bytes.decodeToString()
                if (!ProcessorData.isMinecraftFile(content)) return@runBlocking

                content = censor(content)
                val version = versionRegex.find(content)?.groupValues?.get(1) ?: return@runBlocking
                handle(version, content)
            }
        }

        if (sourceBinUrls.isNotEmpty()) {
            for (url in sourceBinUrls) {
                runBlocking {
                    var content = PasteUpload.get(url)
                    if (!ProcessorData.isMinecraftFile(content)) return@runBlocking

                    content = censor(content)
                    val version = versionRegex.find(content)?.groupValues?.get(1) ?: return@runBlocking
                    handle(version, content)
                }
            }
        }
    }

    private fun censor(input: String): String {
        var input = ProcessorData.censorUrls(input)
        input = userDirectoryRegex.replace(input, "[USER DIRECTORY]")
        input = emailRegex.replace(input, "[EMAIL]")
        input = usernameRegex.replace(input, "[USERNAME]")
        return input
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
            if (devEnvUserRegex.containsMatchIn(content)) this.content += "This file came from a development environment.\n"
            val time = timeRegex.find(content)?.groupValues?.get(1)
            if (time != null) this.content += "This file was generated at $time.\n"
            val modLoader = modLoaderRegexes.find { (_, regex) -> regex.containsMatchIn(content) }
            if (modLoader != null) {
                this.content += "**Mod loader:** ${modLoader.first}\n"
            } else this.content += "Couldn't tell what mod loader this log was produced by...\n"
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

    private fun decompressGzip(bytes: ByteArray): ByteArray {
        val inputStream = GZIPInputStream(bytes.inputStream())
        val outputStream = ByteArrayOutputStream()
        inputStream.copyTo(outputStream)
        return outputStream.toByteArray()
    }

    private fun stripVersion(content: String) =
        content.replace("[0-9]+\\.[0-9]+(\\..*)?".toRegex().find(content)?.groupValues?.get(1) ?: "", "")
}