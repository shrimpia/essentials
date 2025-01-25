package network.shrimpia.essentials.modules

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import org.jetbrains.annotations.ApiStatus

abstract class ModuleBase {
    /**
     * モジュールが有効化されたときに行う処理を、オーバーライドして記述します。
     */
    open fun onEnable() {}

    /**
     * モジュールが無効化されたときに行う処理を、オーバーライドして記述します。
     */
    open fun onDisable() {}

    /**
     * コンフィグがリロードされたときに行う処理を、オーバーライドして記述します。
     */
    open fun onReloadConfig() {}

    /**
     * コマンドが登録されたときに行う処理を、オーバーライドして記述します。
     */
    @Suppress("UnstableApiUsage")
    open fun onRegisterCommand(commands: ReloadableRegistrarEvent<Commands>) {}
}