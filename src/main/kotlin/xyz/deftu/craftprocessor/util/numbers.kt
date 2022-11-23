package xyz.deftu.craftprocessor.util

fun Number.getOrdinalIndicator(): String {
    // round to the nearest int
    val num = toInt()
    // get last digit
    val lastDigit = num % 10
    // return the ordinal indicator
    return when (lastDigit) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

fun Number.withOrdinalIndicator() = "$this${getOrdinalIndicator()}"
