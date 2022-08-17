package xyz.deftu.craftprocessor

import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CompletableFuture

object DataHandler {
    val httpClient = OkHttpClient.Builder()
        .addInterceptor {
            it.proceed(it.request().newBuilder()
                .addHeader("User-Agent", "${CraftProcessor.NAME}/${CraftProcessor.VERSION}")
                .build())
        }.build()

    /**
     * Fetches data from a file path under the
     * "data" directory inside the GitHub
     * repository for the project. This is very
     * useful for files that need to be updated
     * on-the-go.
     */
    fun fetchData(path: String, branch: String = "main"): String {
        val path = if (path.startsWith("/")) path.substring(1) else path
        return httpClient.newCall(Request.Builder()
            .get()
            .cacheControl(CacheControl.Builder()
                .noCache()
                .build())
            .url("https://raw.githubusercontent.com/Deftu/CraftProcessor/$branch/data/$path")
            .build()).execute().body?.string() ?: ""
    }

    fun fetchDataAsync(path: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            fetchData(path)
        }
    }
}
