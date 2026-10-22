package app.trovata.cast.server

import app.trovata.cast.protocol.IceServerConfig
import io.ktor.client.HttpClient

private val DefaultStunUrls = listOf(
    "stun:stun.l.google.com:19302",
    "stun:stun1.l.google.com:19302",
)

fun interface IceServersProvider {
    suspend fun current(): List<IceServerConfig>
}

object IceConfig {
    fun providerFromEnv(client: HttpClient, env: (String) -> String? = System::getenv): IceServersProvider {
        val fixed = fromEnv(env)
        val keyId = env("ICE_CLOUDFLARE_TURN_KEY_ID")?.takeIf { it.isNotBlank() }
        val apiToken = env("ICE_CLOUDFLARE_TURN_API_TOKEN")?.takeIf { it.isNotBlank() }
        if (keyId == null || apiToken == null) return IceServersProvider { fixed }

        val cloudflare = CloudflareTurnCredentials(client, keyId, apiToken)
        return IceServersProvider { fixed + cloudflare.generate() }
    }

    fun fromEnv(env: (String) -> String? = System::getenv): List<IceServerConfig> {
        val stunUrls = split(env("ICE_STUN_URLS")).ifEmpty { DefaultStunUrls }
        val turnUrls = split(env("ICE_TURN_URLS"))
        val turnUsername = env("ICE_TURN_USERNAME")?.takeIf { it.isNotBlank() }
        val turnCredential = env("ICE_TURN_CREDENTIAL")?.takeIf { it.isNotBlank() }

        return buildList {
            if (stunUrls.isNotEmpty()) add(IceServerConfig(urls = stunUrls))
            if (turnUrls.isNotEmpty()) {
                add(
                    IceServerConfig(
                        urls = turnUrls,
                        username = turnUsername,
                        credential = turnCredential,
                    ),
                )
            }
        }
    }

    private fun split(raw: String?): List<String> =
        raw?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
}
