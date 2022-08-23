package xyz.deftu.craftprocessor.config

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.ActionRow
import xyz.deftu.craftprocessor.CraftProcessor
import xyz.deftu.craftprocessor.utils.SQLiteHelper
import xyz.deftu.craftprocessor.utils.toButton
import xyz.deftu.craftprocessor.utils.toReadableString
import java.io.File
import java.sql.Connection
import java.time.OffsetDateTime

internal class UserConfigManager {
    private val configs = mutableMapOf<String, UserConfig>()
    private lateinit var connection: Connection

    fun initialize(file: File) {
        // Open our connection to the database file.
        connection = SQLiteHelper.openConnection(file)
        // Create the table if it doesn't exist.
        SQLiteHelper.createTable(
            connection = connection,
            tableName = "configs",
            columns = arrayOf(
                "id TEXT PRIMARY KEY",
                "toggle INTEGER",
            )
        )

        loadUsers()
    }

    fun close() {
        connection.close()
    }

    fun getUser(id: String, createIfNotExists: Boolean = false) =
        configs[id] ?: if (createIfNotExists) {
            val user = UserConfig(id)
            configs[id] = user
            user
        } else {
            null
        }

    fun saveUser(config: UserConfig) {
        configs[config.id] = config
        SQLiteHelper.update(
            connection = connection,
            tableName = "configs",
            values = arrayOf(
                "id" to config.id,
                "toggle" to config.toggle
            )
        )
    }

    fun createMessage(config: UserConfig, user: User): Message {
        val embed = CraftProcessor.createEmbed()
            .setTitle("User Config (${user.asTag})")
            .setTimestamp(OffsetDateTime.now())
            .setFooter(user.asTag, user.avatarUrl)

        embed.descriptionBuilder.apply {
            append("**").append("Toggled: ").append("**").append(config.toggle.toReadableString())
        }

        val actionRows = mutableListOf<ActionRow>()
        run {
            actionRows.add(ActionRow.of(
                config.toggle.toButton("personal_config - toggle", "Toggle")
            ))
        }

        return MessageBuilder()
            .setEmbeds(embed.build())
            .setActionRows(actionRows)
            .build()
    }

    private fun loadUsers() {
        // Load all the user configs from the database.
        val configs = SQLiteHelper.select(
            connection = connection,
            tableName = "configs",
            columns = arrayOf(
                "id",
                "toggle"
            )
        )

        // Loop through all the user configs.
        for (userConfig in configs) {
            // Create a new user config.
            val config = UserConfig(
                id = userConfig["id"]?.toString() ?: continue,
                toggle = userConfig["toggle"] == 1
            )
            // Add it to the array.
            this.configs[config.id] = config
        }
    }

    private fun createUser(id: String): UserConfig {
        val config = UserConfig(id)
        configs[id] = config
        saveUser(config)
        return config
    }
}
