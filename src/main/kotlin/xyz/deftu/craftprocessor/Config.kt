package xyz.deftu.craftprocessor

import java.io.File

data class Config(
    val token: String?
) {
    companion object {
        fun read(file: File): Config =
            CraftProcessor.gson
                .fromJson(file.apply {
                    if (!exists()) {
                        createNewFile()
                        file.writeText("{}")
                    }
                }.readText(), Config::class.java)
    }
}
