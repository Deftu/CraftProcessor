package xyz.deftu.craftprocessor.events

import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.event.Event

class ItemProcessEvent(
    override val kord: Kord,
    @KordPreview override val customContext: Any?,
    override val shard: Int
) : Event {
}
