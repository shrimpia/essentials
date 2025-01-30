package network.shrimpia.essentials.util

import com.jeff_media.morepersistentdatatypes.DataType
import network.shrimpia.essentials.ShrimpiaEssentials
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

object HomeUtility {
    private val plugin by lazy { ShrimpiaEssentials.get() }

    /**
     * プレイヤーのホームを取得します。
     */
    fun getHome(player: Player, name: String): Location? {
        val pdc = player.persistentDataContainer
        val homes = pdc.get(NamespacedKey(plugin, "homes"), DataType.asMap(DataType.STRING, DataType.LOCATION)) ?: emptyMap()
        return homes[name]
    }

    /**
     * プレイヤーのホームを設定します。
     * もしホームの数が上限を超える場合は、false を返します。
     */
    fun setHome(player: Player, name: String): Boolean {
        val pdc = player.persistentDataContainer
        val homes = pdc.get(NamespacedKey(plugin, "homes"), DataType.asMap(DataType.STRING, DataType.LOCATION)) ?: mutableMapOf()
        homes[name] = player.location
        if (homes.size > getHomeCreationLimit(player)) {
            return false
        }
        pdc.set(NamespacedKey(plugin, "homes"), DataType.asMap(DataType.STRING, DataType.LOCATION), homes)
        return true
    }

    fun getHomeCreationLimit(player: Player): Int {
        // TODO: 将来的にリミットを増やせるようにする
        return 1
    }
}