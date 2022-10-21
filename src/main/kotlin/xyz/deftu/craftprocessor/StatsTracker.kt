package xyz.deftu.craftprocessor

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.WriteApiBlocking
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.logging.log4j.LogManager
import xyz.deftu.craftprocessor.config.LocalConfig
import xyz.deftu.craftprocessor.events.ItemProcessedEvent
import java.time.Instant

object StatsTracker {
    private lateinit var client: InfluxDBClient
    private val logger = LogManager.getLogger("StatsTracker")

    fun initialize(client: ShardManager) {
        val config = LocalConfig.INSTANCE.statsTracker ?: run {
            logger.warn("Stats tracker config was not found, thus the stats tracker is disabled.")
            return
        }
        this.client = InfluxDBClientFactory.create(
            config.url ?: throw IllegalStateException("Stats tracker config is present, but missing \"url\" field!"),
            config.token?.toCharArray() ?: throw IllegalStateException("Stats tracker config is present, but missing \"token\" field!"),
            config.org ?: throw IllegalStateException("Stats tracker config is present, but missing \"org\" field!"),
            config.bucket ?: throw IllegalStateException("Stats tracker config is present, but missing \"bucket\" field!"),
        )
        client.addEventListener(this)
    }

    @SubscribeEvent
    fun onItemProcessed(event: ItemProcessedEvent) {
        println("Event: (${event.version} - ${event.file})")
        val api = client.writeApiBlocking
        handleItemsProcessedPerVersion(event, api)
        handleFilesProcessed(event, api)
    }

    private fun handleItemsProcessedPerVersion(event: ItemProcessedEvent, api: WriteApiBlocking) {
        val point = Point.measurement("items_processed_per_version")
            .addField("version", event.version)
            .time(Instant.now(), WritePrecision.MS)
        api.writePoint(point)
    }

    private fun handleFilesProcessed(event: ItemProcessedEvent, api: WriteApiBlocking) {
        val point = Point.measurement("files_processed")
            .addField("file", event.file)
            .addField("version", event.version)
            .time(Instant.now(), WritePrecision.MS)
        api.writePoint(point)
    }
}
