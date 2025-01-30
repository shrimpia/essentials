package network.shrimpia.essentials.modules

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import network.shrimpia.essentials.ShrimpiaEssentials
import network.shrimpia.essentials.models.MisskeyStreamingChannel
import network.shrimpia.essentials.services.MisskeyStreaming
import network.shrimpia.essentials.util.AdventureExtension.asMiniMessage
import network.shrimpia.essentials.util.LanguageManager
import network.shrimpia.essentials.util.MisskeyApi
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.json.simple.JSONObject
import java.util.UUID

class MisskeyChatSyncModule : ModuleBase(), Listener {
    private var misskeyToken: String = ""
    private var misskeyChannelId: String = ""
    private var streaming: MisskeyStreaming? = null
    private var streamingChannelId = ""

    private val plugin by lazy { ShrimpiaEssentials.get() }

    override fun onEnable() {
        onReloadConfig()
    }

    override fun onDisable() {
        disconnect()
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
            initializeStreaming()
            plugin.logger.info("MisskeyChatSyncModule: Misskeyサーバーに接続しました: ${meta["name"]} ${meta["version"]}")
        } catch (e: Exception) {
            plugin.logger.warning("MisskeyChatSyncModule: Misskeyサーバーへの接続に失敗しました。")
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncChatEvent) {
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        val formatted = "**${event.player.name}:** $message"
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
            plugin.logger.warning("MisskeyChatSyncModule: サーバーにメッセージを投稿できませんでした: ${e.message}")
        }
    }

    private fun initializeStreaming() {
        disconnect()

        streamingChannelId = UUID.randomUUID().toString()
        streaming = MisskeyApi.connectStreaming(misskeyToken)
        streaming?.subscribeChannel(MisskeyStreamingChannel(
            "channel",
            streamingChannelId,
            mapOf(
                "channelId" to misskeyChannelId
            )
        ))
        streaming?.onChannelMessage = ev@{ id, body ->
            if (id != streamingChannelId || body["type"] != "note") return@ev
            val note = body["body"] as JSONObject
            val user = note["user"] as JSONObject
            plugin.logger.info(user["isBot"].toString())
            plugin.logger.info(user["isBot"]?.javaClass?.simpleName ?: "null")
            plugin.logger.info(if (user["isBot"] == true) "true" else "false")
            if (user["isBot"] == true) return@ev
            if (note["text"] !is String || note["text"] == "") return@ev
            val formatted = MiniMessage.miniMessage().deserialize(
                "<gray><username>: <white><message>",
                Placeholder.unparsed("username", "@${user["username"]}"),
                Placeholder.unparsed("message", note["text"] as String)
            )
            plugin.server.broadcast(formatted)
        }
    }

    private fun disconnect() {
        if (streaming == null) return
        plugin.logger.info("MisskeyChatSyncModule: ストリーミングを切断しています…")
        streaming?.disconnect()
        streaming = null
    }
}