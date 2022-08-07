package xyz.deftu.craftprocessor.processor

import com.google.gson.JsonParser
import xyz.deftu.craftprocessor.DataHandler

object LogIdentifier {
    private val identifiers = mutableListOf<String>()

    fun isLog(input: String): Boolean {
        return identifiers.any { identifier ->
            input.contains(identifier)
        }
    }

    fun reload() {
        val data = DataHandler.fetchData("log_identifiers.json")
        if (data.isBlank()) return
        val json = JsonParser.parseString(data)
        if (!json.isJsonArray) return
        identifiers.clear()
        json.asJsonArray.forEach {
            if (!it.isJsonPrimitive) return@forEach
            identifiers.add(it.asString)
        }
    }
}
