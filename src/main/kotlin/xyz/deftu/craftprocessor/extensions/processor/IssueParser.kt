package xyz.deftu.craftprocessor.extensions.processor

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

object IssueParser {
    fun parse(input: String) = JsonReader(input.reader()).use { reader ->
        val token = reader.peek()
        if (token != JsonToken.BEGIN_OBJECT) throw IllegalStateException("Expected BEGIN_OBJECT, got $token")
        reader.beginObject()

        val versions = mutableListOf<IssueVersion>()

        var currentName = ""
        while (reader.hasNext()) {
            val token = reader.peek()
            if (token == JsonToken.NAME) {
                currentName = reader.nextName()
                continue
            }

            versions.add(parseIssueVersion(currentName, reader, token))
        }

        reader.endObject()
        versions.toList()
    }

    private fun parseIssueVersion(
        name: String,
        reader: JsonReader,
        token: JsonToken
    ) = reader.let { reader ->
        reader.beginArray()
        val issues = mutableListOf<Issue>()
        while (reader.hasNext()) issues.add(parseIssue(reader, reader.peek()))
        reader.endArray()
        IssueVersion(name.split(",").map {
            it.replace(" ", "")
        }.toList(), issues)
    }

    private fun parseIssue(
        reader: JsonReader,
        token: JsonToken
    ) = reader.let { reader ->
        if (token != JsonToken.BEGIN_OBJECT) throw IllegalStateException("Expected BEGIN_OBJECT, got $token")
        reader.beginObject()

        var title = ""
        var solution = ""
        var severity = IssueSeverity.TRIVIAL
        val causes = mutableListOf<IssueCause>()

        var currentName = ""
        while (reader.hasNext()) {
            val token = reader.peek()
            if (token == JsonToken.NAME) {
                currentName = reader.nextName()
                continue
            }

            when (currentName) {
                "title" -> {
                    if (token != JsonToken.STRING) throw IllegalStateException("Expected STRING, got $token")
                    title = reader.nextString()
                }
                "solution" -> {
                    if (token != JsonToken.STRING) throw IllegalStateException("Expected STRING, got $token")
                    solution = reader.nextString()
                }
                "severity" -> {
                    if (token != JsonToken.STRING) throw IllegalStateException("Expected STRING, got $token")
                    severity = IssueSeverity.from(reader.nextString())
                }
                "causes" -> {
                    if (token != JsonToken.BEGIN_ARRAY) throw IllegalStateException("Expected BEGIN_ARRAY, got $token")
                    reader.beginArray()
                    while (reader.hasNext()) causes.add(parseIssueCause(reader, reader.peek()))
                    reader.endArray()
                }
            }
        }

        reader.endObject()
        Issue(title, solution, severity, causes)
    }

    private fun parseIssueCause(
        reader: JsonReader,
        token: JsonToken
    ) = reader.let { reader ->
        if (token != JsonToken.BEGIN_OBJECT) throw IllegalStateException("Expected BEGIN_OBJECT, got $token")
        reader.beginObject()

        var method = IssueSearchMethod.CONTAINS
        var text = ""

        var currentName = ""
        while (reader.hasNext()) {
            val token = reader.peek()
            if (token == JsonToken.NAME) {
                currentName = reader.nextName()
                continue
            }

            when (currentName) {
                "method" -> {
                    if (token != JsonToken.STRING) throw IllegalStateException("Expected STRING, got $token")
                    method = IssueSearchMethod.from(reader.nextString())
                }
                "text" -> {
                    if (token != JsonToken.STRING) throw IllegalStateException("Expected STRING, got $token")
                    text = reader.nextString()
                }
            }
        }

        reader.endObject()
        IssueCause(method, text)
    }
}
