package network.shrimpia.essentials.modules

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import network.shrimpia.essentials.ShrimpiaEssentials
import network.shrimpia.essentials.util.LanguageManager
import network.shrimpia.essentials.util.MisskeyApi
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class MisskeyChatSyncModule : ModuleBase(), Listener {
    private var misskeyToken: String = ""
    private var misskeyChannelId: String = ""

    private val plugin by lazy { ShrimpiaEssentials.get() }

    override fun onEnable() {
        onReloadConfig()
    }

    override fun onReloadConfig() {
        val config = plugin.config
        misskeyToken = config.getString("misskey_token") ?: ""
        misskeyChannelId = config.getString("misskey_channel_id") ?: ""
        if (misskeyToken.isEmpty() || misskeyChannelId.isEmpty()) {
            plugin.logger.warning("MisskeyChatSyncModule: misskey_token or misskey_channel_id is not set at config.yml")
            return
        }

        try {
            val meta = MisskeyApi.getMeta(misskeyToken)
            plugin.logger.info("MisskeyChatSyncModule: Connected to Misskey server: ${meta["name"]} ${meta["version"]}")
        } catch (e: Exception) {
            plugin.logger.warning("MisskeyChatSyncModule: Failed to connect to Misskey server")
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncChatEvent) {
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        val formatted = "**${event.player.name}**\n$message"
        postMessage(formatted)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        postMessage("**${event.player.name}** がゲームに参加しました")
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        postMessage("**${event.player.name}** がゲームから退出しました")
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerAdvancementDone(event: PlayerAdvancementDoneEvent) {
        val advancement = event.advancement.display ?: return
        val title = (advancement.title() as TranslatableComponent).key()
        val message = "\$[tada 🎉] **${event.player.name}** が進捗 \$[sparkle **${LanguageManager.translate(title)}**] を達成しました"
        postMessage(message)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val deathMessage = event.deathMessage() as? TranslatableComponent

        val message = if (deathMessage == null) {
            "\uD83D\uDC80 **${event.player.name}** が死亡しました"
        } else {
            "\uD83D\uDC80 ${PlainTextComponentSerializer.plainText().serialize(deathMessage)}"
        }
        postMessage(message)
    }

    private fun postMessage(message: String) {
        try {
            MisskeyApi.createNoteOnChannel(message, misskeyChannelId, misskeyToken)
        } catch (e: Exception) {
            plugin.logger.warning("MisskeyChatSyncModule: Failed to post message to Misskey server")
        }
    }
}