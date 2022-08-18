package xyz.deftu.craftprocessor.config

import net.dv8tion.jda.api.sharding.ShardManager
import xyz.deftu.craftprocessor.utils.SQLiteHelper
import java.io.File
import java.sql.Connection

// Eah user has their own config.
// The config is stored in a sqlite database.
// The config has the following values:
// - is_disabled (boolean)

object UserConfig {
    // Our connection to the database.
    private lateinit var databaseConnection: Connection
    // A map of all the user config data.
    private val userConfigs = mutableMapOf<String, UserConfigData>()

    fun initialize(client: ShardManager) {
        // Open our connection to the database file.
        databaseConnection = SQLiteHelper.openConnection(File("user_config.db"))
        // Create the table if it doesn't exist.
        SQLiteHelper.createTable(
            databaseConnection,
            "user_config",
            arrayOf(
                "user_id INTEGER PRIMARY KEY",
                "is_disabled INTEGER"
            )
        )
        // Load all the user config data.
        loadUserConfigs()
    }

    fun getUserConfig(userId: String): UserConfigData? {
        return userConfigs[userId]
    }

    fun createUserConfig(userId: String): UserConfigData {
        // Create a new user config data.
        val userConfig = UserConfigData(userId)
        // Add it to the map.
        userConfigs[userId] = userConfig
        // Save it to the database.
        saveUserConfig(userConfig)
        // Return the user config data.
        return userConfig
    }

    fun saveUserConfig(userConfig: UserConfigData) {
        // Save the user config data to the database.
        SQLiteHelper.update(
            connection = databaseConnection,
            tableName = "user_config",
            values = arrayOf(
                "user_id" to userConfig.userId,
                "is_disabled" to userConfig.isDisabled
            )
        )
    }

fun loadUserConfigs() {
        // Load all the user config data from the database.
        val userConfigs = SQLiteHelper.select(
            connection = databaseConnection,
            tableName = "user_config",
            columns = arrayOf(
                "user_id",
                "is_disabled"
            )
        )
        // Loop through all the user config data.
        for (userConfig in userConfigs) {
            // Create a new user config data.
            val newUserConfig = UserConfigData(
                userId = userConfig["user_id"] as String,
                isDisabled = userConfig["is_disabled"] as Boolean
            )
            // Add it to the map.
            this.userConfigs[newUserConfig.userId] = newUserConfig
        }
    }
}

data class UserConfigData(
    val userId: String,
    val isDisabled: Boolean = false
)
