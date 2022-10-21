package xyz.deftu.craftprocessor.events

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.Event

class ItemProcessedEvent(
    client: JDA,
    val version: String,
    val file: String
) : Event(client)
