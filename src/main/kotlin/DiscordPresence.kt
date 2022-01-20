import club.minnced.discord.rpc.DiscordRPC
import club.minnced.discord.rpc.DiscordRichPresence

class DiscordPresence {
    val client = DiscordRPC.INSTANCE

    init {
        client.initialize("622783718783844356")

        val presence = DiscordRichPresence()
        presence.startTimestamp = System.currentTimeMillis() / 1000 // epoch second
        presence.state = "Main page"
        presence.details = "ReadMeAManga"

        client.Discord_UpdatePresence(presence)
    }

    fun updatePresence(top: String, bottom: String, startTimestamp: Long = 0L, endTimestamp: Long = 0L) {
        val presence = DiscordRichPresence()

        presence.details = top
        presence.state = bottom
        presence.startTimestamp = startTimestamp
        presence.endTimestamp = endTimestamp

        client.Discord_UpdatePresence(presence)
    }

    fun clearPresence() {
        client.Discord_ClearPresence()
    }
}

private fun DiscordRPC.initialize(applicationID: String) = this.Discord_Initialize(applicationID, null, false, null)