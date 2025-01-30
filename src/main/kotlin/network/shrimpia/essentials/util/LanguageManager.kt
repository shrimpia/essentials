package network.shrimpia.essentials.util

import okhttp3.OkHttpClient
import okhttp3.Request
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object LanguageManager {
    private const val VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
    private const val LANG_FILE_PATH = "plugins/MyPlugin/lang/ja_jp.json"
    private val client = OkHttpClient()
    private var translations: JSONObject? = null

    fun initialize(plugin: JavaPlugin) {
        try {
            downloadLanguageFile(plugin)
            loadLanguageFile(plugin)
        } catch (e: Exception) {
            plugin.logger.severe("言語ファイルの初期化に失敗しました: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun downloadLanguageFile(plugin: JavaPlugin) {
        // 1. 対応するバージョンのマニフェストを取得
        val currentVersion = Bukkit.getServer().minecraftVersion
        val manifestJson = getJsonFromUrl(VERSION_MANIFEST)
        val manifest = JSONValue.parse(manifestJson) as JSONObject
        val current = (manifest["versions"] as List<JSONObject>)
            .first { it["id"] == currentVersion }
        val latestVersionUrl = current["url"] as String

        // 2. アセット情報を取得
        val assetsJson = getJsonFromUrl(latestVersionUrl)
        val assets = JSONValue.parse(assetsJson) as JSONObject
        val assetIndexUrl = (assets["assetIndex"] as JSONObject)["url"] as String

        // 3. 言語ファイルのハッシュを取得
        val assetIndexJson = getJsonFromUrl(assetIndexUrl)
        val assetIndex = JSONValue.parse(assetIndexJson) as JSONObject
        val hash = ((assetIndex["objects"] as JSONObject)["minecraft/lang/ja_jp.json"] as JSONObject)["hash"] as String

        // 4. 言語ファイルのURL
        val langUrl = "https://resources.download.minecraft.net/${hash.substring(0, 2)}/$hash"

        // 5. ダウンロード & 保存
        saveFileFromUrl(langUrl, LANG_FILE_PATH)
        plugin.logger.info("ja_jp.json をダウンロードしました!")
    }

    private fun getJsonFromUrl(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP request failed: ${response.code}")
            return response.body?.string() ?: throw Exception("Empty response body")
        }
    }

    private fun saveFileFromUrl(fileUrl: String, savePath: String) {
        val request = Request.Builder().url(fileUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP request failed: ${response.code}")

            val file = File(savePath)
            file.parentFile.mkdirs()
            file.writeBytes(response.body?.bytes() ?: throw Exception("Empty response body"))
        }
    }

    private fun loadLanguageFile(plugin: JavaPlugin) {
        val path = Paths.get(LANG_FILE_PATH)
        if (Files.exists(path)) {
            val content = Files.readString(path)
            translations = JSONValue.parse(content) as JSONObject
            plugin.logger.info("ja_jp.json の読み込み完了！")
        } else {
            throw Exception("ja_jp.json が見つかりません")
        }
    }

    fun translate(key: String): String {
        return translations?.getOrDefault(key, key) as String
    }
}
