package xyz.deftu.craftprocessor.config

import xyz.deftu.craftprocessor.CraftProcessor
import java.io.File

data class LocalConfig(
    val token: String?,
    val statsTracker: StatsTrackerConfig?,
    val hastebinUrl: String?,
) {
    companion object {
        @JvmStatic
        val INSTANCE by lazy {
            read(File("config.json"))
        }

        fun read(file: File): LocalConfig =
            CraftProcessor.gson
                .fromJson(file.apply {
                    if (!exists()) {
                        createNewFile()
                        file.writeText("{}")
                    }
                }.readText(), LocalConfig::class.java)
    }
}

data class StatsTrackerConfig(
    val port: Int
)
