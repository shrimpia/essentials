package network.shrimpia.essentials.modules

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import network.shrimpia.essentials.ShrimpiaEssentials
import org.bukkit.entity.Player

class CommandTestModule : ModuleBase() {
    @Suppress("UnstableApiUsage")
    override fun onEnable() {
        val plugin = ShrimpiaEssentials.get()
        val manager = plugin.lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS, { event ->
            val commands = event.registrar()
            commands.register(
                Commands.literal("test").then(
                    Commands.literal("playsound").then(
                        Commands.argument("sound", ArgumentTypes.resource(RegistryKey.SOUND_EVENT))
                            .executes { ctx ->
                                val sound = ctx.getArgument("sound", String::class.java)
                                val player = ctx.source.sender as? Player ?: return@executes 0
                                player.playSound(player.location, sound, 1f, 1f)

                                1
                            }
                        )
                    )
                .build(),
                "テスト用のコマンドです"
            )
        })
    }
}