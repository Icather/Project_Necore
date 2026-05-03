package icather.pages.dev.api.plugin

data class ProtocolPluginJson(
    val providerId: String,
    val baseUrl: String,
    val authHeaderFormat: String,
    val messageFormat: String
)
