package xyz.deftu.craftprocessor

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CompletableFuture

object DataHandler {
    private val httpClient = OkHttpClient.Builder()
        .build()

    fun fetchData(path: String): String {
        val path = if (path.startsWith("/")) path.substring(1) else path
        return httpClient.newCall(Request.Builder()
            .get()
            .url("https://raw.githubusercontent.com/Deftu/CraftProcessor/main/data/$path")
            .build()).execute().body?.string() ?: ""
    }

    fun fetchDataAsync(path: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            fetchData(path)
        }
    }
}
