package xyz.deftu.craftprocessor.config

import net.dv8tion.jda.api.sharding.ShardManager
import xyz.deftu.craftprocessor.utils.SQLiteHelper
import java.io.File
import java.sql.Connection

object GuildConfig {
    private lateinit var databaseConnection: Connection

    fun initialize(client: ShardManager) {
        databaseConnection = SQLiteHelper.openConnection(File("guild_config.db"))
    }
}
