package xyz.deftu.craftprocessor.processor

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.DataHandler

object HasteUpload {
    val url: String
        get() {
            var baseUrl = CraftProcessor.config.hastebinUrl ?: "https://hastebin.com"
            if (!baseUrl.endsWith("/")) baseUrl += "/"
            return baseUrl
        }

    fun upload(input: String): String {
        val response = DataHandler.httpClient.newCall(Request.Builder()
            .url(url + "documents")
            .post(input.toRequestBody("text/plain".toMediaType()))
            .build()).execute()
        return if (response.isSuccessful) {
            val hasteResponse = CraftProcessor.gson
                .fromJson(response.body!!.string(), HastebinResponse::class.java)
                .key
            return "$url$hasteResponse"
        } else "Error uploading to Hastebin: ${response.message} (${response.code})"
    }
}

private data class HastebinResponse(
    val key: String
)
