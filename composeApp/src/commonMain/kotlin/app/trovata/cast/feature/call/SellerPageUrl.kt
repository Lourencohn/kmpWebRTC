package app.trovata.cast.feature.call

import app.trovata.cast.protocol.buildSellerEmbedUrl

fun sellerPageUrl(inviteUrl: String, catalogWebBaseUrlOverride: String? = null): String {
    val rebased = catalogWebBaseUrlOverride
        ?.trimEnd('/')
        ?.let { base -> base + pathAndQueryOf(inviteUrl) }
        ?: inviteUrl
    return buildSellerEmbedUrl(rebased)
}

private fun pathAndQueryOf(url: String): String {
    val afterScheme = url.substringAfter("://", url)
    val slash = afterScheme.indexOf('/')
    return if (slash < 0) "" else afterScheme.substring(slash)
}
