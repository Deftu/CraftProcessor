package xyz.deftu.craftprocessor.util

import dev.kord.gateway.retry.Retry
import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {  }

class ConstantRetry(
    val maxDelay: Long = 30_000 // 30 seconds
) : Retry {
    private val attempts = AtomicLong(0L)
    override val hasNext = true

    override fun reset() {
        attempts.set(0)
    }

    override suspend fun retry() {
        // get and increment the number of attempts
        val attempt = attempts.getAndIncrement()
        // calculate the delay
        val delay = (attempt * 1000).coerceAtMost(maxDelay)
        // log the delay
        logger.warn {
            if (attempt == 0L) "Retrying in $delay ms" else "Retrying for the ${attempts.get().withOrdinalIndicator()} time in $delay ms"
        }

        // delay
        delay(delay)
    }
}
