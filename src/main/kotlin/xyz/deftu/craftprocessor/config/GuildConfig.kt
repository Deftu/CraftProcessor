package xyz.deftu.craftprocessor.config

data class GuildConfig(
    val id: String,

    var channelWhitelist: Boolean = false,
    var whitelistedChannels: MutableList<Long> = mutableListOf(),

    var channelBlacklist: Boolean = false,
    var blacklistedChannels: MutableList<Long> = mutableListOf(),

    var roleWhitelist: Boolean = false,
    var whitelistedRoles: MutableList<Long> = mutableListOf(),

    var roleBlacklist: Boolean = false,
    var blacklistedRoles: MutableList<Long> = mutableListOf()
) {
    fun isChannelWhitelisted(channelId: Long) =
        channelWhitelist && whitelistedChannels.contains(channelId)
    fun isChannelBlacklisted(channelId: Long) =
        channelBlacklist && blacklistedChannels.contains(channelId)
    fun isRoleWhitelisted(roleId: Long) =
        roleWhitelist && whitelistedRoles.contains(roleId)
    fun isRoleBlacklisted(roleId: Long) =
        roleBlacklist && blacklistedRoles.contains(roleId)
}
