package network.shrimpia.essentials.modules

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import network.shrimpia.essentials.util.AdventureExtension.asMiniMessage
import org.bukkit.entity.Player

class HatModule: ModuleBase() {
    @Suppress("UnstableApiUsage")
    override fun onRegisterCommand(commands: ReloadableRegistrarEvent<Commands>) {
        val registrar = commands.registrar()
        registrar.register(
            Commands.literal("hat")
                .requires { it.sender is Player }
                .executes {
                    val player = it.source.sender as Player
                    val item = player.inventory.itemInMainHand
                    player.inventory.setItemInMainHand(player.inventory.helmet)
                    player.inventory.helmet = item
                    player.sendMessage("<green>帽子を被りました！".asMiniMessage())
                    1
                }
                .build()
        )
    }
}