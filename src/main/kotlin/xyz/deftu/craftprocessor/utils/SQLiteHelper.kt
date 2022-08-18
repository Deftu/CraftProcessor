package xyz.deftu.craftprocessor.utils

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

object SQLiteHelper {
    fun openConnection(file: File): Connection {
        if (!file.exists()) file.createNewFile()
        return DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
            ?: throw IllegalStateException("Failed to open connection to SQLite database at ${file.absolutePath}")
    }

    fun update(connection: Connection, tableName: String, values: Array<Pair<String, Any?>>) {
        val columns = values.joinToString(", ") { it.first }
        val values = values.map {
            it.second
        }.joinToString(", ")
        val query = "INSERT OR REPLACE INTO $tableName ($columns) VALUES ($values)"
        connection.createStatement().execute(query)
    }

    fun select(connection: Connection, tableName: String, columns: Array<String>, where: String? = null): Array<Map<String, Any?>> {
        val query = "SELECT ${columns.joinToString(", ")} FROM $tableName" +
                if (where.isNullOrBlank()) "" else "WHERE $where"
        val resultSet = connection.createStatement().executeQuery(query)
        val result = mutableListOf<Map<String, Any?>>()
        while (resultSet.next()) {
            val row = mutableMapOf<String, Any?>()
            for (column in columns) {
                row[column] = resultSet.getObject(column)
            }
            result.add(row)
        }
        return result.toTypedArray()
    }

    fun createTable(connection: Connection, tableName: String, columns: Array<String>) {
        val statement = connection.createStatement()
        statement.execute("CREATE TABLE IF NOT EXISTS $tableName (${columns.joinToString(",")})")
    }

    fun deleteRow(connection: Connection, tableName: String, where: String, whereArgs: Array<String>) {
        val statement = connection.createStatement()
        statement.execute("DELETE FROM $tableName WHERE $where", whereArgs)
    }
}
