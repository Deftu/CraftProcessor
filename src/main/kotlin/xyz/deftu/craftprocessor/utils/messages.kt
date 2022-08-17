package xyz.deftu.craftprocessor.utils

import com.google.gson.JsonObject
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.time.OffsetDateTime

fun Boolean.toReadableString(trueStr: String = "Yes", falseStr: String = "No") =
    if (this) trueStr else falseStr

fun JsonObject.toMessageEmbed(): MessageEmbed {
    val builder = EmbedBuilder()
    if (has("title")) builder.setTitle(this["title"].asString, if (has("url")) this["url"].asString else null)
    if (has("description")) builder.setDescription(this["description"].asString)
    if (has("color")) builder.setColor(this["color"].asInt)
    if (has("footer")) builder.setFooter(this["footer"].asString)
    if (has("image")) builder.setImage(this["image"].asString)
    if (has("thumbnail")) builder.setThumbnail(this["thumbnail"].asString)
    if (has("author")) builder.setAuthor(this["author"].asString)
    if (has("fields")) {
        val fields = this["fields"].asJsonArray
        for (field in fields) {
            if (!field.isJsonObject) continue
            val fieldObject = field.asJsonObject
            if (!fieldObject.has("name") || !fieldObject.has("value")) continue
            builder.addField(
                fieldObject["name"].asString,
                fieldObject["value"].asString,
                fieldObject["inline"]?.asBoolean ?: false
            )
        }
    }
    if (has("timestamp")) builder.setTimestamp(OffsetDateTime.parse(this["timestamp"].asString))
    return builder.build()
}
