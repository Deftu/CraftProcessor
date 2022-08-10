package xyz.deftu.craftprocessor.processor

import com.google.gson.JsonParser
import xyz.deftu.craftprocessor.DataHandler

object ProcessableExtensions {
    private val extensions = mutableListOf<String>()

    fun isProcessable(input: String): Boolean {
        if (input.isBlank()) return true
        return extensions.any { extensions ->
            input.contains(extensions)
        }
    }

    fun reload() {
        val data = DataHandler.fetchData("processable.json")
        if (data.isBlank()) return
        val json = JsonParser.parseString(data)
        if (!json.isJsonArray) return
        extensions.clear()
        json.asJsonArray.forEach {
            if (!it.isJsonPrimitive) return@forEach
            extensions.add(it.asString)
        }
    }
}
