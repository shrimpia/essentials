package network.shrimpia.essentials

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import network.shrimpia.essentials.hooks.HookBase
import network.shrimpia.essentials.modules.*
import network.shrimpia.essentials.util.AdventureExtension.asMiniMessage
import network.shrimpia.essentials.util.LanguageManager
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class ShrimpiaEssentials : JavaPlugin() {
    private val modules = mutableListOf<ModuleBase>()
    private val hooks = mutableListOf<HookBase>()

    @Suppress("UnstableApiUsage")
    override fun onEnable() {
        saveDefaultConfig()
        instance = this

        registerKernelCommands()
        LanguageManager.initialize(this)

        // フック登録
        registerHookAll(
        )

        // モジュール登録
        registerModuleAll(
            ShrimpiaAuthModule(),
            TeleportModule(),
            GreetingModule(),
            HomeModule(),
            HatModule(),
            ChatMentionNotifierModule(),
            MisskeyChatSyncModule(),
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
        hooks.forEach { it.onDisable() }
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
        if (module is Listener) {
            server.pluginManager.registerEvents(module, this)
        }
    }

    fun registerHookAll(vararg hooks: HookBase) {
        hooks.forEach { registerHook(it) }
    }

    fun registerHook(hook: HookBase) {
        val plugin = server.pluginManager.getPlugin(hook.pluginName) ?: return

        logger.info("プラグイン ${hook.pluginName} が見つかりました。フックを ${hook.javaClass.simpleName} として登録します。")
        hook.onEnable(plugin)
        hooks.add(hook)
    }

    fun unregisterHook(hook: HookBase) {
        hooks.remove(hook)
        hook.onDisable()
    }

    @Suppress("UnstableApiUsage")
    private fun registerKernelCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            commands.registrar().register(
                Commands.literal("shrimpia")
                    .then(
                        Commands.literal("reload")
                            .requires { it.sender.hasPermission("shrimpiaEssentials.adminCommands") }
                            .executes {
                                reloadConfig()
                                it.source.sender.sendMessage("<green>Config reloaded!".asMiniMessage())
                                1
                            }
                    )
                    .build()
            )
        }
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
