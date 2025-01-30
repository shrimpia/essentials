package network.shrimpia.essentials.modules

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import network.shrimpia.essentials.util.AdventureExtension.asMiniMessage
import network.shrimpia.essentials.util.HomeUtility
import org.bukkit.entity.Player

class HomeModule : ModuleBase() {
    override fun onEnable() {

    }

    @Suppress("UnstableApiUsage")
    override fun onRegisterCommand(commands: ReloadableRegistrarEvent<Commands>) {
        val registrar = commands.registrar()
        registrar.register(
            Commands.literal("sethome")
                .requires { it.sender is Player }
                .executes {
                    val player = it.source.sender as Player
                    if (player.location.world.name != "world") {
                        player.sendMessage("<red>ホームは、メインワールドでのみ設定できます！".asMiniMessage())
                        return@executes 1
                    }
                    if (HomeUtility.setHome(player, "default")) {
                        player.sendMessage("<green>ホームを設定しました！".asMiniMessage())
                    } else {
                        player.sendMessage("<red>ホームの数が上限に達しています".asMiniMessage())
                    }
                    1
                }
                .build()
        )
    }
}