package network.shrimpia.essentials.modules

import net.kyori.adventure.text.Component
import network.shrimpia.essentials.ShrimpiaEssentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

class ShrimpiaAuthModule : ModuleBase(), Listener {
    private val plugin = ShrimpiaEssentials.get()
    private val http = OkHttpClient()
    private lateinit var apiKey: String
    private lateinit var apiUrl: String

    override fun onEnable() {
        onReloadConfig()

        // このモジュールを、Bukkit イベントリスナーとして登録
        Bukkit.getServer().pluginManager.registerEvents(this, plugin)
    }

    override fun onReloadConfig() {
        // コンフィグからAPIキーとAPIのURLを取得
        val config = plugin.config
        apiKey = config.getString("portal_api_key") ?: throw IllegalStateException("portal_api_key is not set at config.yml")
        apiUrl = config.getString("portal_api_url") ?: throw IllegalStateException("portal_api_url is not set at config.yml")
    }

    /**
     * プレイヤーの参加時に呼び出される
     */
    @EventHandler
    fun onPlayerLogin(ev: AsyncPlayerPreLoginEvent) {
        // 既にログインが弾かれている場合は処理を行わない
        if (ev.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return
        }

        // プレイヤーIDと名前から、リクエストのbodyを生成
        val root = JSONObject()
        root["playerId"] = ev.uniqueId.toString()
        root["playerName"] = ev.name
        val body = root.toJSONString().toRequestBody("application/json".toMediaType())

        // リクエストを生成
        val req = Request.Builder()
            .url("$apiUrl/minecraft/internal-auth")
            .method("POST", body)
            .header("X-Internal-Api-Key", apiKey)
            .build()

        try {
            // APIリクエストを送信
            http.newCall(req).execute().use { res ->
                // レスポンスが返ってきたあとの処理
                val resBody = res.body?.string()
                // APIが異常であれば、内部エラーとして扱う
                if (!res.isSuccessful || resBody == null) {
                    ev.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        Component.text("ポータルとの接続に失敗しました。皇帝へお問い合わせください。")
                    )
                    plugin.logger.severe("シュリンピアポータルへのリクエストに失敗しました。")
                    plugin.logger.severe("status:${res.code} body:${resBody ?: "null"}")
                    return
                }

                // レスポンスのJSONをパース
                val data = JSONParser().parse(resBody) as JSONObject
                val ok = data["ok"] as Boolean

                // 認証が通っていれば、そのままログインを許可
                if (ok) {
                    ev.allow()
                    plugin.logger.info("プレイヤー ${ev.name} は、ポータル認証に成功しました。")
                    return
                }

                // 通っていなければ、メッセージをそのまま表示する
                val message = data["error"] as String
                ev.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(message))
                plugin.logger.info("プレイヤー ${ev.name} は、ポータル認証に失敗しました。")
            }
        } catch (exception: Exception) {
            // 例外が発生した場合は、内部エラーとして扱う
            ev.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("ポータルとの接続に失敗しました。皇帝へお問い合わせください。")
            )
            plugin.logger.severe("シュリンピアポータルへのリクエスト時に例外が発生しました。")
            plugin.logger.severe("${exception.javaClass.simpleName}: ${exception.message}\n${exception.stackTrace.joinToString("\n")}")
        }
    }
}
