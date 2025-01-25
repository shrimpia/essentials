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
import org.bukkit.event.player.PlayerLoginEvent
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
    @EventHandler
    /**
     * プレイヤーの参加時に呼び出される
     */
    fun onPlayerLogin(ev: PlayerLoginEvent) {
        // プレイヤーIDと名前から、リクエストのbodyを生成
        val root = JSONObject();
        root["playerId"] = ev.player.uniqueId.toString()
        root["playerName"] = ev.player.name
        val body = root.toJSONString().toRequestBody("application/json".toMediaType())

        // リクエストを生成
        val req = Request.Builder()
            .url("$apiUrl/internal-auth")
            .method("POST", body)
            .header("X-Internal-Api-Key", apiKey)
            .build();

        // APIリクエストを送信
        http.newCall(req).execute().use { res ->
            // レスポンスが返ってきたあとの処理

            val resBody = res.body?.string()
            // APIが異常であれば、内部エラーとして扱う
            if (!res.isSuccessful || resBody == null) {
                ev.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("ポータルとの接続に失敗しました。皇帝へお問い合わせください。"))
                return
            }

            // レスポンスのJSONをパース
            val data = JSONParser().parse(resBody) as JSONObject
            val ok = data["ok"] as Boolean

            // 認証が通っていれば、そのままログインを許可
            if (ok) {
                ev.allow()
                return
            }

            // 通っていなければ、メッセージをそのまま表示する
            val message = data["message"] as String
            ev.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text(message))
        }

    }
}
