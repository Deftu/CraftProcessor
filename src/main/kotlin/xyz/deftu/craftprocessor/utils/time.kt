package xyz.deftu.craftprocessor.utils

/**
 * Converts milliseconds into a formatted
 * string displaying the time in weeks, days,
 * hours, minutes and seconds.
 */
fun Long.toFormattedTime(): String {
    val weeks = this / (1000 * 60 * 60 * 24 * 7)
    val days = this / (1000 * 60 * 60 * 24) % 7
    val hours = this / (1000 * 60 * 60) % 24
    val minutes = this / (1000 * 60) % 60
    val seconds = this / 1000 % 60

    var time = ""
    if (weeks > 0) time += "$weeks week" + (if (weeks > 1) "s" else "") + (if (days > 0) ", " else "")
    if (days > 0) time += "$days day" + (if (days > 1) "s" else "") + (if (hours > 0) ", " else "")
    if (hours > 0) time += "$hours hour" + (if (hours > 1) "s" else "") + (if (minutes > 0) ", " else "")
    if (minutes > 0) time += "$minutes minute" + (if (minutes > 1) "s" else "") + (if (seconds > 0) ", " else "")
    if (seconds > 0) time += "$seconds second" + if (seconds > 1) "s" else ""
    return time
}
