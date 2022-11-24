package xyz.deftu.craftprocessor.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.deftu.craftprocessor.GSON

object PasteUpload {
    val SOURCE_BIN_REGEX = "(?:https?:\\/\\/)?(paste\\.ee|pastebin\\.com|has?tebin\\.com|(?:www\\.)?toptal\\.com\\/developers\\/hastebin|hasteb\\.in|hst\\.sh)\\/(?:raw\\/|p\\/)?([\\w-\\.]+)".toRegex()

    private lateinit var client: OkHttpClient
    private lateinit var url: String

    fun setup(client: OkHttpClient, url: String) {
        if (!::client.isInitialized) {
            this.client = client
        } else throw IllegalStateException("PasteUpload's HTTP client already initialized!")

        if (!::url.isInitialized) {
            this.url = url
        } else throw IllegalStateException("PasteUpload's URL already initialized!")
    }

    fun upload(input: String): String {
        if (!::client.isInitialized) throw IllegalStateException("PasteUpload's HTTP client not initialized!")
        if (!::url.isInitialized) throw IllegalStateException("PasteUpload's URL not initialized!")

        val url = if (url.endsWith("/")) url.substring(0, url.length - 1) else url
        val response = client.newCall(Request.Builder()
            .url("$url/documents")
            .post(input.toRequestBody("text/plain".toMediaType()))
            .build()).execute()
        return if (response.isSuccessful) {
            val hasteResponse = GSON.fromJson(response.body!!.string(), PasteResponse::class.java)
            return "$url/${hasteResponse.key}"
        } else "Error uploading to paste service: ${response.message} (${response.code})"
    }

    fun get(url: String): String {
        if (!::client.isInitialized) throw IllegalStateException("PasteUpload's HTTP client not initialized!")

        val response = client.newCall(Request.Builder()
            .get()
            .url(makeRaw(url))
            .build()).execute()
        return if (response.isSuccessful) {
            response.body!!.string()
        } else "Error getting paste: ${response.message} (${response.code})"
    }

    private fun makeRaw(input: String): String {
        val match = SOURCE_BIN_REGEX.matchEntire(input) ?: return input
        val (_, host, key) = match.groupValues
        return when (host) {
            "paste.ee" -> "https://paste.ee/r/$key"
            "pastebin.com" -> "https://pastebin.com/raw/$key"
            "hastebin.com", "hasteb.in", "hst.sh" -> "https://$host/raw/$key"
            "toptal.com/developers/hastebin" -> "https://toptal.com/developers/hastebin/raw/$key"
            else -> input
        }
    }

    private data class PasteResponse(
        val key: String
    )
}
