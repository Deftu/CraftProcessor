package xyz.deftu.craftprocessor.utils

import java.io.File

fun File.ensureExists() =
    if (!exists() && !mkdirs()) {
        throw IllegalStateException("Could not create directory $this")
    } else this
