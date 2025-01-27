package network.shrimpia.essentials.modules

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import net.kyori.adventure.text.Component
import network.shrimpia.essentials.ShrimpiaEssentials
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
                            player.sendMessage(Component.text("ベッドあるいはリスポーンアンカーが設定されていません"))
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
                            player.sendMessage(Component.text("既にメインワールドにいます"))
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
                            player.sendMessage(Component.text("既にサブワールドにいます"))
                            return@executes 1
                        }
                        setLastMainWorldLocation(player, player.location)
                        val world = Bukkit.getWorld("sub") ?: throw IllegalStateException("World 'sub' is not found")
                        teleportAsync(player, world.spawnLocation)
                        1
                    }
            ).build()

        commands.registrar().register(gotoCommand)
        commands.registrar().register(spawnCommand)
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