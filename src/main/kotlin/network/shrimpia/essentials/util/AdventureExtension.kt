package network.shrimpia.essentials.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Adventure APIをKotlinで扱いやすくするための拡張関数を提供します。
 */
object AdventureExtension {
    fun String.toComponent() = Component.text(this)

    fun String.asMiniMessage() = MiniMessage.miniMessage().deserialize(this)
}