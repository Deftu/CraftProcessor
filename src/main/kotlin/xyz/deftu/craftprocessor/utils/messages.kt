package xyz.deftu.craftprocessor.utils

import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

fun Boolean.toReadableString(trueStr: String = "Yes", falseStr: String = "No") =
    if (this) trueStr else falseStr

fun Boolean.toButton(id: String, label: String) =
    Button.of(if (this) ButtonStyle.SUCCESS else ButtonStyle.DANGER, id, label)

fun List<Long>.convertToMentions(map: (Long) -> IMentionable?) = mapNotNull {
    map(it)
}.joinToString(", ", transform = IMentionable::getAsMention).ifBlank {
    "None"
}
