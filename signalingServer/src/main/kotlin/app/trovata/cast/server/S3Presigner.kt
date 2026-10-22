package app.trovata.cast.server

import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class S3Presigner(
    endpoint: String,
    private val bucket: String,
    private val region: String,
    private val accessKey: String,
    private val secretKey: String,
    private val now: () -> Instant = Instant::now,
) {
    private val baseUrl = endpoint.trimEnd('/')
    private val host = URI(baseUrl).let { uri -> if (uri.port == -1) uri.host else "${uri.host}:${uri.port}" }

    fun presignPut(key: String, expiresInSeconds: Long): String = presign("PUT", key, expiresInSeconds)

    fun presignGet(key: String, expiresInSeconds: Long): String = presign("GET", key, expiresInSeconds)

    private fun presign(method: String, key: String, expiresInSeconds: Long): String {
        val path = (listOf(bucket) + key.split('/')).joinToString(separator = "/", prefix = "/") { SigV4.uriEncode(it) }
        val query = SigV4.presignedQuery(
            method = method,
            host = host,
            canonicalUri = path,
            region = region,
            accessKey = accessKey,
            secretKey = secretKey,
            at = now(),
            expiresInSeconds = expiresInSeconds,
        )
        return "$baseUrl$path?$query"
    }
}

internal object SigV4 {
    private const val ALGORITHM = "AWS4-HMAC-SHA256"
    private const val SERVICE = "s3"
    private const val TERMINATOR = "aws4_request"
    private const val UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD"
    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"

    private val amzDateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    fun presignedQuery(
        method: String,
        host: String,
        canonicalUri: String,
        region: String,
        accessKey: String,
        secretKey: String,
        at: Instant,
        expiresInSeconds: Long,
    ): String {
        val amzDate = amzDateFormat.format(at)
        val dateStamp = amzDate.substring(0, 8)
        val scope = "$dateStamp/$region/$SERVICE/$TERMINATOR"
        val canonicalQuery = sortedMapOf(
            "X-Amz-Algorithm" to ALGORITHM,
            "X-Amz-Credential" to "$accessKey/$scope",
            "X-Amz-Date" to amzDate,
            "X-Amz-Expires" to expiresInSeconds.toString(),
            "X-Amz-SignedHeaders" to "host",
        ).entries.joinToString("&") { (name, value) -> "${uriEncode(name)}=${uriEncode(value)}" }

        val canonicalRequest = listOf(
            method,
            canonicalUri,
            canonicalQuery,
            "host:$host\n",
            "host",
            UNSIGNED_PAYLOAD,
        ).joinToString("\n")
        val stringToSign = listOf(ALGORITHM, amzDate, scope, sha256Hex(canonicalRequest)).joinToString("\n")
        val signingKey = listOf(dateStamp, region, SERVICE, TERMINATOR)
            .fold("AWS4$secretKey".toByteArray()) { key, part -> hmacSha256(key, part) }
        val signature = hmacSha256(signingKey, stringToSign).toHex()
        return "$canonicalQuery&X-Amz-Signature=$signature"
    }

    fun uriEncode(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val char = byte.toInt().toChar()
            if (byte >= 0 && char in UNRESERVED) {
                append(char)
            } else {
                append('%')
                append("%02X".format(byte.toInt() and 0xFF))
            }
        }
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).toHex()

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray())
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
