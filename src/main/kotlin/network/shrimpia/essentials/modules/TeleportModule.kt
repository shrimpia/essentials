package network.shrimpia.essentials.modules

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import net.kyori.adventure.text.Component
import network.shrimpia.essentials.ShrimpiaEssentials
import network.shrimpia.essentials.util.AdventureExtension.asMiniMessage
import network.shrimpia.essentials.util.HomeUtility
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataType

class TeleportModule : ModuleBase(), Listener {
    private val plugin = ShrimpiaEssentials.get()
    private val movingPlayerIdSet = mutableSetOf<String>()

    override fun onEnable() {
        Bukkit.getServer().pluginManager.registerEvents(this, plugin)
    }

    @Suppress("UnstableApiUsage")
    override fun onRegisterCommand(commands: ReloadableRegistrarEvent<Commands>) {
        val reg = commands.registrar()

        // リスポーン地点にテレポートする
        val spawnCommand =
            Commands.literal("spawn")
                .requires { it.sender is Player }
                .executes {
                    val player = it.source.sender as Player
                    teleportAsync(player, player.world.spawnLocation, 5 * 20)
                    1
                }
                .build()

        val gotoCommand = Commands.literal("goto")
            .then(spawnCommand)
            .then(
                // ベッドにテレポートする
                Commands.literal("bed")
                    .requires { it.sender is Player }
                    .executes {
                        val player = it.source.sender as Player
                        val bedLocation = player.respawnLocation
                        if (bedLocation == null) {
                            player.sendMessage("<red>ベッドあるいはリスポーンアンカーが設定されていません".asMiniMessage())
                            return@executes 1
                        }
                        teleportAsync(player, bedLocation, 5 * 20)
                        1
                    },
            )
            .then(
                // メインワールドにテレポートする
                Commands.literal("main")
                    .requires { it.sender is Player }
                    .executes {
                        val player = it.source.sender as Player
                        if (player.world.name == "world") {
                            player.sendMessage("<red>既にメインワールドにいます！".asMiniMessage())
                            return@executes 1
                        }
                        val location = getSavedLastMainWorldLocation(player)

                        teleportAsync(player, location, 5 * 20)
                        1
                    },
            )
            .then(
                // サブワールドにテレポートする
                Commands.literal("sub")
                    .requires { it.sender is Player }
                    .executes {
                        val player = it.source.sender as Player
                        if (player.world.name.startsWith("sub")) {
                            player.sendMessage("<red>既にサブワールドにいます！".asMiniMessage())
                            return@executes 1
                        }
                        setLastMainWorldLocation(player, player.location)
                        val world = Bukkit.getWorld("sub") ?: throw IllegalStateException("World 'sub' is not found")
                        teleportAsync(player, world.spawnLocation)
                        1
                    }
            ).then(
                // プレイヤーのホームにテレポートする
                Commands.literal("home")
                    .requires { it.sender is Player }
                    .executes {
                        val player = it.source.sender as Player
                        val home = HomeUtility.getHome(player, "default")
                        if (home == null) {
                            player.sendMessage("<red>ホームが設定されていません！<yellow>/home <red>で、現在地をホームに設定し、いつでもここにテレポートできます。".asMiniMessage())
                            return@executes 1
                        }
                        teleportAsync(player, home, 5 * 20)
                        1
                    }
            ).build()

        reg.register(gotoCommand)
        reg.register(spawnCommand)
    }

    /**
     * プレイヤーを指定した場所にテレポートします。
     */
    private fun teleportAsync(player: Player, location: Location, delayInTick: Long = 0L) {
        if (movingPlayerIdSet.contains(player.uniqueId.toString())) {
            player.sendMessage(Component.text("移動中です。しばらくお待ちください。"))
            return
        }
        if (delayInTick == 0L) {
            player.teleportAsync(location)
            return
        }
        player.sendMessage(Component.text("${delayInTick / 20}秒後に移動します…"))
        val id = player.uniqueId.toString()
        movingPlayerIdSet.add(id)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            movingPlayerIdSet.remove(id)
            player.teleportAsync(location)
        }, delayInTick)
    }

    /**
     * プレイヤーがサブワールドに移動する前にいたメインワールドの位置を取得します。
     */
    private fun getSavedLastMainWorldLocation(player: Player): Location {
        val pdc = player.persistentDataContainer
        val key = NamespacedKey(plugin, "lastMainWorldLocation")
        val location = pdc.get(key, PersistentDataType.INTEGER_ARRAY)
        val world = Bukkit.getWorld("world") ?: throw IllegalStateException("World 'world' is not found")
        return if (location != null) {
            Location(world, location[0].toDouble(), location[1].toDouble(), location[2].toDouble())
        } else {
            player.world.spawnLocation
        }
    }

    /**
     * プレイヤーがメインワールドにいた位置を保存します。
     */
    private fun setLastMainWorldLocation(player: Player, location: Location) {
        val pdc = player.persistentDataContainer
        val key = NamespacedKey(plugin, "lastMainWorldLocation")
        val value = intArrayOf(location.blockX, location.blockY, location.blockZ)
        pdc.set(key, PersistentDataType.INTEGER_ARRAY, value)
    }
}