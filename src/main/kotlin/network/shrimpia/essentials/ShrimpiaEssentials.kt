package network.shrimpia.essentials

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import network.shrimpia.essentials.hooks.HookBase
import network.shrimpia.essentials.modules.ModuleBase
import network.shrimpia.essentials.modules.ShrimpiaAuthModule
import network.shrimpia.essentials.modules.TeleportModule
import org.bukkit.plugin.java.JavaPlugin

class ShrimpiaEssentials : JavaPlugin() {
    private val modules = mutableListOf<ModuleBase>()

    @Suppress("UnstableApiUsage")
    override fun onEnable() {
        saveDefaultConfig()
        instance = this
        registerModuleAll(
            ShrimpiaAuthModule(),
            TeleportModule(),
        )

        // コマンド登録
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            modules.forEach {
                it.onRegisterCommand(commands)
            }
            hooks.forEach {
                it.onRegisterCommand(commands)
            }
        }
    }

    override fun onDisable() {
        instance = null
        modules.forEach { it.onDisable() }
    }

    override fun reloadConfig() {
        super.reloadConfig()
        modules.forEach { it.onReloadConfig() }
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
