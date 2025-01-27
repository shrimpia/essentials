package network.shrimpia.essentials.modules

import io.papermc.paper.registry.keys.SoundEventKeys
import net.kyori.adventure.sound.Sound
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

    @Suppress("UnstableApiUsage")
    @EventHandler
    fun onPlayerJoin(ev: PlayerJoinEvent) {
        // Note: セットから削除できたということは、新規プレイヤーであるということ
        val isNewbie = newbieSet.remove(ev.player.uniqueId)
        ev.player.playSound(
            Sound.sound(if (isNewbie) SoundEventKeys.ENTITY_ITEM_PICKUP else SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1.0f, 1.0f)
        )
        ev.player.showTitle(
            Title.title(
                "<#B93E43>Shrimpia Minecraft".asMiniMessage(),
                if (isNewbie) {
                    "ようこそ、<aqua>${ev.player.name}</aqua>さん！".asMiniMessage()
                } else {
                    "おかえりなさい、<aqua>${ev.player.name}</aqua>さん！".asMiniMessage()
                },
                Title.Times.times(Duration.ofSeconds(1L), Duration.ofSeconds(5L), Duration.ofSeconds(1L))
            )
        )
    }
}