package network.shrimpia.essentials.modules

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ChatMentionNotifierModule: ModuleBase(), Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncChatEvent) {
        val onlinePlayer = event.player.server.onlinePlayers
        val message: String = PlainTextComponentSerializer.plainText().serialize(event.message())
        val mentionedPlayers = onlinePlayer.filter { message.contains(it.name + " ") }
        mentionedPlayers.forEach {
            it.playSound(it.location, "minecraft:block.note_block.bit", SoundCategory.PLAYERS, 1f, 2f)
        }
    }
}