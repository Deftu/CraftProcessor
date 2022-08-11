package xyz.deftu.craftprocessor.processor

import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.DataHandler
import java.awt.Color

object IssueList {
    private val versions = mutableListOf<IssueVersion>()

    fun fromVersion(version: String): IssueVersion? {
        return versions.find {
            it.versions.contains(version)
        }
    }

    fun reload() {
        val data = DataHandler.fetchData("issues.json")
        if (data.isBlank()) return
        val json = JsonParser.parseString(data)
        if (!json.isJsonObject) return
        versions.clear()
        json.asJsonObject.entrySet().forEach { entry ->
            val (name, element) = entry
            if (!element.isJsonArray) return@forEach
            val json = element.asJsonArray
            val versions = name.trim().split(",")
            val issues = mutableListOf<Issue>()
            json.forEach { element ->
                if (!element.isJsonObject) return@forEach
                issues.add(CraftProcessor.gson
                    .fromJson(element, Issue::class.java))
            }
            val version = IssueVersion(versions, issues)
            this.versions.add(version)
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
    val run: (String, String) -> Boolean
) {
    REGEX({ value, input ->
        value.toRegex().matches(input)
    }),
    CONTAINS({ value, input ->
        input.contains(value)
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
