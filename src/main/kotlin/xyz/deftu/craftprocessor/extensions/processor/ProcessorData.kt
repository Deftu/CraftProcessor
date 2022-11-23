package xyz.deftu.craftprocessor.extensions.processor

import com.google.gson.JsonParser
import xyz.deftu.craftprocessor.DataHandler
import java.awt.Color

object ProcessorData {
    private val URL_REGEX = "(?:https:\\/\\/|http:\\/\\/)[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/\\/=]*)".toRegex()

    private val identifiers = mutableListOf<String>()
    private val extensions = mutableListOf<String>()
    private val urlWhitelist = mutableListOf<String>()
    private val issues = mutableListOf<IssueVersion>()

    fun isMinecraftFile(input: String): Boolean {
        if (input.isBlank()) return false
        return identifiers.any { extensions ->
            input.contains(extensions)
        }
    }

    fun isProcessableFile(input: String): Boolean {
        if (input.isBlank()) return true
        println("input: $input")
        println("extensions: $extensions")
        return extensions.any { extensions ->
            input.contains(extensions)
        }
    }

    fun censor(input: String): String {
        val urls = URL_REGEX.findAll(input)
        var input = input
        urls.forEach { match ->
            if (urlWhitelist.none {  url ->
                match.value.contains(url)
            }) {
                input = input.replace(match.value, "[URL CENSORED]")
            }
        }
        return input
    }

    fun forVersion(version: String): List<IssueVersion> {
        return issues.filter {
            it.versions.contains(version)
        }
    }

    suspend fun reload() {
        identifiers.clear()
        extensions.clear()
        urlWhitelist.clear()
        issues.clear()

        reloadIdentifiers()
        reloadExtensions()
        reloadUrlWhitelist()
        reloadIssues()
    }

    private fun reloadIdentifiers() {
        val data = DataHandler.fetch("log_identifiers.json")
        if (data.isBlank()) return

        val json = JsonParser.parseString(data)
        if (!json.isJsonArray) return

        json.asJsonArray.forEach {
            if (!it.isJsonPrimitive) return@forEach
            identifiers.add(it.asString)
        }
    }

    private fun reloadExtensions() {
        val data = DataHandler.fetch("processable.json")
        println("data: $data")
        if (data.isBlank()) return

        val json = JsonParser.parseString(data)
        println("json: $json")
        if (!json.isJsonArray) return

        json.asJsonArray.forEach {
            if (!it.isJsonPrimitive) return@forEach
            println("it: $it")
            extensions.add(it.asString)
        }
    }

    private fun reloadUrlWhitelist() {
        val data = DataHandler.fetch("whitelisted_urls.json")
        if (data.isBlank()) return

        val json = JsonParser.parseString(data)
        if (!json.isJsonArray) return

        json.asJsonArray.forEach {
            if (!it.isJsonPrimitive) return@forEach
            urlWhitelist.add(it.asString)
        }
    }

    private fun reloadIssues() {
        val data = DataHandler.fetch("issues.json")
        if (data.isBlank()) return

        val json = JsonParser.parseString(data)
        if (!json.isJsonObject) return

        IssueParser.parse(json.toString()).forEach {
            issues.add(it)
        }
    }
}

data class IssueVersion(
    val versions: List<String>,
    val issues: List<Issue>
)

data class Issue(
    val title: String,
    val solution: String,
    val severity: IssueSeverity,
    val causes: List<IssueCause>
)

enum class IssueSeverity(
    val text: String,
    val color: Color
) {
    CRITICAL("Critical", Color.RED),
    MAJOR("Major", Color.ORANGE),
    MINOR("Minor", Color.YELLOW),
    TRIVIAL("Trivial", Color.GREEN);
    companion object {
        fun from(input: String) = values().firstOrNull() {
            it.name.equals(input, true)
        } ?: TRIVIAL
    }
}

enum class IssueSearchMethod(
    val run: (text: String, log: String) -> Boolean
) {
    REGEX({ text, log ->
        text.toRegex().matches(log)
    }),
    CONTAINS({ text, log ->
        log.contains(text)
    });

    companion object {
        fun from(input: String) = values().firstOrNull {
            it.name.equals(input, true)
        } ?: CONTAINS
    }
}

data class IssueCause(
    val method: IssueSearchMethod,
    val text: String
)
