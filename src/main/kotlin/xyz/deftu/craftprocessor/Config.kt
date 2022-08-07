package xyz.deftu.craftprocessor

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import java.io.File

data class Config(
    val token: String?
) {
    companion object {
        fun read(file: File): Config =
            GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setLenient()
                .create()
                .fromJson(file.apply {
                    if (!exists()) {
                        createNewFile()
                        file.writeText("{}")
                    }
                }.readText(), Config::class.java)
    }
}
