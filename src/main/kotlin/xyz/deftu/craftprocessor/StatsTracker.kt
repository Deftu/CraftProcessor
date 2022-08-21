package xyz.deftu.craftprocessor

import io.prometheus.client.Gauge
import io.prometheus.client.exporter.HTTPServer
import org.apache.logging.log4j.LogManager
import xyz.deftu.craftprocessor.config.LocalConfig

object StatsTracker {
    private lateinit var server: HTTPServer
    private val logger = LogManager.getLogger("StatsTracker")

    private val itemsProcessed = Gauge.build()
        .name("items_processed")
        .help("The total amount of crashes/logs processed.")
        .register()
    private val itemsProcessedPerVersion = Gauge.build()
        .name("items_processed_per_version")
        .help("The total amount of crashes/logs processed per version.")
        .labelNames("version")
        .register()

    fun initialize() {
        val config = LocalConfig.INSTANCE.statsTracker ?: run {
            logger.warn("Stats tracker config was not found, thus the stats tracker is disabled.")
            return
        }

        server = HTTPServer.Builder()
            .withPort(config.port)
            .build()

        CraftProcessor.addShutdownListener {
            server.close()
        }
    }

    fun incrementItemsProcessed() {
        itemsProcessed.inc()
    }

    fun incrementItemsProcessed(version: String) {
        itemsProcessedPerVersion.labels(version).inc()
    }
}
