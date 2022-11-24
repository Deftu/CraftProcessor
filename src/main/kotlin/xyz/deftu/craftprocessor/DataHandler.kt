package xyz.deftu.craftprocessor

import okhttp3.OkHttpClient
import okhttp3.Request

object DataHandler {
    private lateinit var client: OkHttpClient
    private lateinit var url: String

    fun setup(client: OkHttpClient, url: String) {
        if (!::client.isInitialized) {
            this.client = client
        } else throw IllegalStateException("DataHandler client already initialized!")

        if (!::url.isInitialized) {
            this.url = url
        } else throw IllegalStateException("DataHandler url already initialized!")
    }

    fun fetch(path: String) = client.newCall(
        // no cache
        Request.Builder()
            .get()
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Pragma", "no-cache")
            .addHeader("User-Agent", "${NAME}/${VERSION}")
            .url(buildString {
                append(url.replaceUrlTemplates())
                if (!url.endsWith("/")) append("/")
                append(path)
            }).build()
    ).execute().body?.string() ?: ""

    private fun String.replaceUrlTemplates() =
        replace("{{git_branch}}", if (GIT_BRANCH == "LOCAL") "main" else GIT_BRANCH)
}
