package xyz.deftu.craftprocessor.util

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object SQLiteHelper {
    fun openConnection(file: File): Connection {
        if (!file.exists()) file.createNewFile()
        return DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
            ?: throw IllegalStateException("Failed to open connection to SQLite database at ${file.absolutePath}")
    }

    fun update(connection: Connection, tableName: String, values: Array<Pair<String, String>>) {
        val query = StringBuilder("INSERT OR REPLACE INTO $tableName (")
        val args = mutableListOf<Any?>()

        values.forEachIndexed { index, pair ->
            query.append(pair.first)
            args.add(pair.second)

            if (index != values.size - 1) query.append(", ")
        }

        query.append(") VALUES (")

        values.forEachIndexed { index, _ ->
            query.append("?")
            if (index != values.size - 1) query.append(", ")
        }

        query.append(")")

        val statement = connection.prepareStatement(query.toString())
        args.forEachIndexed { index, any ->
            statement.setObject(index + 1, any)
        }

        statement.executeUpdate()
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

    fun select(connection: Connection, tableName: String): Array<Map<String, Any?>> {
        val query = "SELECT * FROM $tableName"
        val resultSet = connection.createStatement().executeQuery(query)
        val result = mutableListOf<Map<String, Any?>>()
        while (resultSet.next()) {
            val row = mutableMapOf<String, Any?>()
            for (column in 1..resultSet.metaData.columnCount) {
                row[resultSet.metaData.getColumnName(column)] = resultSet.getObject(column)
            }
            result.add(row)
        }
        return result.toTypedArray()
    }

    fun createTable(
        connection: Connection,
        tableName: String,
        columns: Array<String>,
        checkColumns: Boolean = true,
        dropBadColumns: Boolean = false
    ) {
        val statement = connection.createStatement()
        statement.execute("CREATE TABLE IF NOT EXISTS $tableName (${columns.joinToString(", ")})")

        val resultSet = statement.executeQuery("PRAGMA table_info($tableName)")
        val existingColumns = mutableListOf<String>()
        while (resultSet.next()) {
            existingColumns.add(resultSet.getString("name"))
        }

        // check that all the columns exist
        if (checkColumns) {
            columns.forEach { column ->
                if (!existingColumns.any { existingColumn ->
                        column.contains(existingColumn)
                    }) {
                    statement.execute("ALTER TABLE $tableName ADD COLUMN $column")
                }
            }
        }

        // Delete columns that are not in the array
        if (dropBadColumns) {
            existingColumns.forEach { existingColumn ->
                if (!columns.any { column ->
                        column.contains(existingColumn)
                    }) {
                    statement.execute("ALTER TABLE $tableName DROP COLUMN $existingColumn")
                }
            }
        }
    }

    fun deleteRow(connection: Connection, tableName: String, where: String, whereArgs: Array<String>) {
        val statement = connection.createStatement()
        statement.execute("DELETE FROM $tableName WHERE $where", whereArgs)
    }
}
