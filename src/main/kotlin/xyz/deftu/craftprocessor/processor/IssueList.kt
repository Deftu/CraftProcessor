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

    fun reload() {
        val data = DataHandler.fetchData("issues.json")
        if (data.isBlank()) return
        val json = JsonParser.parseString(data)
        if (!json.isJsonArray) return
        versions.clear()
        json.asJsonArray.forEach { element ->
            if (!element.isJsonObject) return@forEach
            CraftProcessor.gson
                .newBuilder()
                .registerTypeAdapter(List::class.java, (object : TypeAdapter<List<String>>() {
                    override fun write(output: JsonWriter, value: List<String>) {
                        output.beginArray()
                        value.forEach {
                            output.value(it)
                        }
                        output.endArray()
                    }

                    override fun read(input: JsonReader): List<String> {
                        val list = mutableListOf<String>()
                        input.beginArray()
                        while (input.hasNext()) {
                            list.add(input.nextString())
                        }
                        input.endArray()
                        return list
                    }
                })).create()
                .fromJson(element, IssueVersion::class.java)?.let { version ->
                    versions.add(version)
                }
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

enum class IssueSearchMethod {
    REGEX,
    CONTAINS;
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
