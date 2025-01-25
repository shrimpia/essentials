package network.shrimpia.essentials.modules

import net.kyori.adventure.title.Title
import network.shrimpia.essentials.ShrimpiaEssentials
import network.shrimpia.essentials.util.AdventureExtension.asMiniMessage
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.time.Duration
import java.util.UUID

class GreetingModule : ModuleBase(), Listener {
    private val newbieSet = mutableSetOf<UUID>()

    @EventHandler
    fun onPlayerPreLogin(ev: AsyncPlayerPreLoginEvent) {
        if (!Bukkit.getOfflinePlayer(ev.uniqueId).hasPlayedBefore()) {
            ShrimpiaEssentials.get().logger.info("New player joined: ${ev.name}")
            newbieSet.add(ev.uniqueId)
        }
    }

    @EventHandler
    fun onPlayerJoin(ev: PlayerJoinEvent) {
        // Note: セットから削除できたということは、新規プレイヤーであるということ
        val isNewbie = newbieSet.remove(ev.player.uniqueId)
        ev.player.showTitle(
            Title.title(
                "<#B93E43>Shrimpia Minecraft".asMiniMessage(),
                if (isNewbie) {
                    "ようこそ、<cyan>${ev.player.name}</cyan>さん！".asMiniMessage()
                } else {
                    "おかえりなさい、<cyan>${ev.player.name}</cyan>さん！".asMiniMessage()
                },
                Title.Times.times(Duration.ofSeconds(1L), Duration.ofSeconds(5L), Duration.ofSeconds(1L))
            )
        )
    }
}