package xyz.deftu.craftprocessor

import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.StringReader
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object Config {
    private lateinit var watcher: ConfigFileWatcher

    var token = ""
    var dataHandlerUrl = ""
    var pasteUploadUrl = ""

    suspend fun read(file: File) {
        if (!file.exists()) {
            if (!withContext(Dispatchers.IO) {
                    file.createNewFile()
                }) {
                shutdown(FileNotFoundException("Could not create config file!"))
                return
            }

            file.writeText(GSON.toJson(JsonObject()))
        }

        val startTime = System.currentTimeMillis()
        JsonReader(StringReader(file.readText())).use { reader ->
            val token = reader.peek()
            if (token != JsonToken.BEGIN_OBJECT) throw IllegalArgumentException("Config file must be a JSON object!")
            reader.beginObject()

            var currentName = ""
            while (reader.hasNext()) {
                val token = reader.peek()
                if (token == JsonToken.NAME) {
                    currentName = reader.nextName()
                    continue
                }

                when (currentName) {
                    "token" -> {
                        if (token != JsonToken.STRING) throw IllegalArgumentException("Configured token must be a string!")
                        this.token = reader.nextString()
                    }
                    "data_handler_url" -> {
                        if (token != JsonToken.STRING) throw IllegalArgumentException("Configured data handler URL must be a string!")
                        this.dataHandlerUrl = reader.nextString()
                    }
                    "paste_upload_url" -> {
                        if (token != JsonToken.STRING) throw IllegalArgumentException("Configured paste upload URL must be a string!")
                        this.pasteUploadUrl = reader.nextString()
                    }
                    else -> reader.skipValue()
                }
            }

            reader.endObject()
        }

        LOGGER.info("Read config in ${System.currentTimeMillis() - startTime}ms!")
    }

    fun watch(file: File) {
        watcher = ConfigFileWatcher(file)
        watcher.start()
    }
}

private class ConfigFileWatcher(
    private val file: File
) : Thread("$NAME Config Watcher Thread") {
    private val stopped = AtomicBoolean(false)

    override fun run() {
        val fs = FileSystems.getDefault()
        fs.newWatchService().use { service ->
            val filePath = file.absolutePath.replace("\\", "/")
            val path = Paths.get(filePath.substring(0, filePath.lastIndexOf("/")))
            path.register(service, StandardWatchEventKinds.ENTRY_MODIFY)
            while (!stopped.get()) {
                val key = service.poll(50, TimeUnit.MILLISECONDS)
                if (key == null) {
                    yield()
                    continue
                }

                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    val path = event.context() as Path
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        if (path.fileName.toString() != file.name) continue
                        LOGGER.info("Config file modified! Re-reading...")
                        runBlocking {
                            Config.read(file)
                        }
                    } else if (kind == StandardWatchEventKinds.OVERFLOW) {
                        yield()
                        continue
                    } else {
                        throw IllegalArgumentException("Kind is somehow not ENTRY_MODIFY or OVERFLOW... That's weird.")
                    }

                    if (!key.reset()) break
                }

                yield()
            }
        }
    }
}
