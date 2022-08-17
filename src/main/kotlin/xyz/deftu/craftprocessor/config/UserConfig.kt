package xyz.deftu.craftprocessor.config

import net.dv8tion.jda.api.sharding.ShardManager
import xyz.deftu.craftprocessor.utils.SQLiteHelper
import java.io.File
import java.sql.Connection

object UserConfig {
    private lateinit var databaseConnection: Connection
    var userConfigs = mutableMapOf<String, UserConfigData>()
        private set

    fun initialize(client: ShardManager) {
        databaseConnection = SQLiteHelper.openConnection(File("user_config.db"))
        // Create table if it doesn't exist
        // The table will have the following columns:
        // - user_id (text, primary key)
        // - has_disabled (boolean)
        databaseConnection.createStatement()
            .executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS user_config (
                    user_id TEXT PRIMARY KEY,
                    has_disabled BOOLEAN
                )
                """.trimIndent())

        // Load all user configs
        val resultSet = databaseConnection.createStatement().executeQuery("SELECT * FROM user_config")
        while (resultSet.next()) {
            val userId = resultSet.getString("user_id")
            val hasDisabled = resultSet.getBoolean("has_disabled")
            userConfigs[userId] = UserConfigData(
                userId,
                hasDisabled
            )
        }
    }

    fun getUserConfig(userId: String): UserConfigData? {
        return userConfigs[userId]
    }

    fun setUserConfig(userId: String, hasDisabled: Boolean) {
        userConfigs[userId] = UserConfigData(
            userId,
            hasDisabled
        )
        databaseConnection.createStatement().executeUpdate(
            """
            INSERT OR REPLACE INTO user_config (
                user_id,
                has_disabled
            ) VALUES (
                '$userId',
                $hasDisabled
            )
            """.trimIndent()
        )
    }
}

data class UserConfigData(
    val userId: String,
    val hasDisabled: Boolean
)
