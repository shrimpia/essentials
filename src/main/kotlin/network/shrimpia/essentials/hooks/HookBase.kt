package network.shrimpia.essentials.hooks

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import org.bukkit.plugin.Plugin

abstract class HookBase {
    abstract val pluginName: String

    open fun onEnable(plugin: Plugin) {

    }

    open fun onDisable() {

    }

    /**
     * コマンドが登録されたときに行う処理を、オーバーライドして記述します。
     */
    @Suppress("UnstableApiUsage")
    open fun onRegisterCommand(commands: ReloadableRegistrarEvent<Commands>) {}
}