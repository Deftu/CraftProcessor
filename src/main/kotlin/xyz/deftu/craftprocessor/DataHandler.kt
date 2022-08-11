package xyz.deftu.craftprocessor

import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CompletableFuture

object DataHandler {
    val httpClient = OkHttpClient.Builder()
        .addInterceptor {
            it.proceed(it.request().newBuilder()
                .addHeader("User-Agent", "CraftProcessor/1.0.0") // TODO - Replace with templates later
                .build())
        }.build()

    fun fetchData(path: String): String {
        val path = if (path.startsWith("/")) path.substring(1) else path
        return httpClient.newCall(Request.Builder()
            .get()
            .cacheControl(CacheControl.Builder()
                .noCache()
                .build())
            .url("https://raw.githubusercontent.com/Deftu/CraftProcessor/main/data/$path")
            .build()).execute().body?.string() ?: ""
    }

    fun fetchDataAsync(path: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            fetchData(path)
        }
    }
}
