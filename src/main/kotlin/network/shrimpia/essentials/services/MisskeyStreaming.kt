package network.shrimpia.essentials.services

import network.shrimpia.essentials.ShrimpiaEssentials
import network.shrimpia.essentials.models.MisskeyStreamingChannel
import okhttp3.*
import org.bukkit.Bukkit
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

class MisskeyStreaming(url: String, token: String) {
    var onChannelMessage: ((id: String, body: JSONObject) -> Unit)? = null

    private val cli = OkHttpClient()
    private val req: Request = Request.Builder()
        .url("$url/streaming?i=$token")
        .build()
    private val logger by lazy { ShrimpiaEssentials.get().logger }
    private val subscribingChannels = mutableSetOf<MisskeyStreamingChannel>()

    private var ws: WebSocket? = null
    private var shouldReconnect = AtomicBoolean(true)
    private var reconnectAttempts = 0

    init {
        connect()
    }

    fun connect() {
        ws = cli.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                logger.info("MisskeyStreaming: Connected to Misskey server")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                logger.info("MisskeyStreaming: Connection closed: $code $reason")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                processMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket Closed: $code $reason")
                attemptReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("WebSocket Error: ${t.message}")
                attemptReconnect()
            }
        })
    }

    fun disconnect() {
        ws?.close(1000, "Disconnect")
        logger.info("MisskeyStreaming: Disconnected from Misskey server")
    }

    fun subscribeChannel(channel: MisskeyStreamingChannel) {
        subscribingChannels.add(channel)
        sendConnectChannelMessage(channel)
    }

    fun unsubscribeChannel(channel: MisskeyStreamingChannel) {
        subscribingChannels.remove(channel)
        sendDisconnectChannelMessage(channel)
    }

    private fun attemptReconnect() {
        if (!shouldReconnect.get()) return

        val delayTime = (2.0.pow(reconnectAttempts.toDouble())).toLong().coerceAtMost(30) // 最大30秒
        reconnectAttempts++
        println("Reconnecting in ${delayTime}s... (Attempt $reconnectAttempts)")

        val delayTick = delayTime * 20

        Bukkit.getScheduler().runTaskLater(ShrimpiaEssentials.get(), Runnable {
            connect()
            subscribingChannels.forEach { sendConnectChannelMessage(it) }
        }, delayTick)
    }

    private fun sendConnectChannelMessage(channel: MisskeyStreamingChannel) {
        val bodyJson = JSONValue.toJSONString(mapOf(
            "type" to "connect",
            "body" to mapOf(
                "channel" to channel.channel,
                "id" to channel.id,
                "params" to channel.params,
            )
        ))
        ws?.send(bodyJson)
    }

    private fun sendDisconnectChannelMessage(channel: MisskeyStreamingChannel) {
        val bodyJson = JSONValue.toJSONString(mapOf(
            "type" to "disconnect",
            "body" to mapOf(
                "id" to channel.id,
            )
        ))
        ws?.send(bodyJson)
    }

    private fun processMessage(text: String) {
        // TODO: イベントを処理する
        val json = JSONValue.parse(text) as JSONObject
        when (val type = json["type"] as String) {
            "channel" -> {
                val id = (json["body"] as JSONObject)["id"] as String
                val body = json["body"] as JSONObject
                onChannelMessage?.invoke(id, body)
            }
        }
    }
}