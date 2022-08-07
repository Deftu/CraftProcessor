package xyz.deftu.craftprocessor.processor

import com.google.gson.JsonParser
import xyz.deftu.craftprocessor.DataHandler

object UrlCensor {
    private val urlRegex = "(?:https:\\/\\/|http:\\/\\/)[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/\\/=]*)".toRegex()
    private val whitelistedUrls = mutableListOf<String>()

    fun censor(input: String): String {
        val urls = urlRegex.findAll(input)
        var input = input
        urls.forEach { match ->
            if (whitelistedUrls.none {  url ->
                match.value.contains(url)
            }) {
                input = input.replace(match.value, "")
            }
        }
        return input
    }

    fun reload() {
        val data = DataHandler.fetchData("whitelisted_urls.json")
        if (data.isBlank()) return
        val json = JsonParser.parseString(data)
        if (!json.isJsonArray) return
        whitelistedUrls.clear()
        json.asJsonArray.forEach {
            if (!it.isJsonPrimitive) return@forEach
            whitelistedUrls.add(it.asString)
        }
    }
}
