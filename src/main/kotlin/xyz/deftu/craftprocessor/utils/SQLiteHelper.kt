package xyz.deftu.craftprocessor.utils

import java.io.File
import java.sql.DriverManager

object SQLiteHelper {
    fun openConnection(file: File) =
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}") ?: throw IllegalStateException("Failed to open connection to SQLite database at ${file.absolutePath}")
}
