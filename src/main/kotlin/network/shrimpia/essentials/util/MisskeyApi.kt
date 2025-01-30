package network.shrimpia.essentials.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.simple.JSONObject
import org.json.simple.JSONValue

object MisskeyApi {
    // TODO: ハードコードされたURLを設定可能に
    private const val URL = "https://mk.shrimpia.network"
    private val cli = OkHttpClient()

    fun createNoteOnChannel(text: String, channelId: String, token: String): JSONObject {
        return call("notes/create", mapOf(
            "text" to text,
            "channelId" to channelId
        ), token)
    }

    fun getMeta(token: String): JSONObject {
        return call("meta", mapOf(), token)
    }

    fun call(endpoint: String, body: Map<String, Any>, token: String): JSONObject {
        val bodyWithToken = mapOf("i" to token) + body
        val bodyData = JSONValue.toJSONString(bodyWithToken)

        val req = Request.Builder()
            .url("$URL/api/$endpoint")
            .method("POST", bodyData.toRequestBody("application/json".toMediaType()))
            .build()

        cli.newCall(req).execute().use { res ->
            return JSONValue.parse(res.body?.string()) as JSONObject
        }
    }
}