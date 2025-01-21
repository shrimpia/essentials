package network.shrimpia.essentials.modules

abstract class ModuleBase {
    /**
     * モジュールが有効化されたときに行う処理を、オーバーライドして記述します。
     */
    open fun onEnable() {
    }

    /**
     * モジュールが無効化されたときに行う処理を、オーバーライドして記述します。
     */
    open fun onDisable() {

    }
}