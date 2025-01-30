package network.shrimpia.essentials.models

data class MisskeyStreamingChannel(
    val channel: String,
    val id: String,
    val params: Map<String, Any>?
)
