package network.shrimpia.essentials

import network.shrimpia.essentials.modules.CommandTestModule
import network.shrimpia.essentials.modules.ModuleBase
import network.shrimpia.essentials.modules.ShrimpiaAuthModule
import org.bukkit.plugin.java.JavaPlugin

class ShrimpiaEssentials : JavaPlugin() {
    private val modules = mutableListOf<ModuleBase>()

    override fun onEnable() {
        instance = this
        registerModuleAll(
            CommandTestModule(),
            ShrimpiaAuthModule(),
        )
    }

    override fun onDisable() {
        instance = null
        modules.forEach { it.onDisable() }
    }

    fun registerModuleAll(vararg modules: ModuleBase) {
        modules.forEach { registerModule(it) }
    }

    fun registerModule(module: ModuleBase) {
        modules.add(module)
        logger.info("Registering module: ${module.javaClass.simpleName}")
        module.onEnable()
    }

    fun unregisterModule(module: ModuleBase) {
        modules.remove(module)
        logger.info("Unregistering module: ${module.javaClass.simpleName}")
        module.onDisable()
    }

    companion object {
        private var instance: ShrimpiaEssentials? = null

        fun get(): ShrimpiaEssentials {
            return getOrNull() ?: throw IllegalStateException("Plugin instance is not initialized yet")
        }

        fun getOrNull(): ShrimpiaEssentials? {
            return instance
        }
    }
}
