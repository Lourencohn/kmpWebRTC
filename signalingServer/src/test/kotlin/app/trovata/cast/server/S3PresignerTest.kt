package app.trovata.cast.server

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class S3PresignerTest {

    private val awsExampleAccessKey = "AKIAIOSFODNN7EXAMPLE"
    private val awsExampleSecretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"

    @Test
    fun matchesTheAwsDocumentationPresignedGetExample() {
        val query = SigV4.presignedQuery(
            method = "GET",
            host = "examplebucket.s3.amazonaws.com",
            canonicalUri = "/test.txt",
            region = "us-east-1",
            accessKey = awsExampleAccessKey,
            secretKey = awsExampleSecretKey,
            at = Instant.parse("2013-05-24T00:00:00Z"),
            expiresInSeconds = 86_400,
        )
        assertEquals(
            "X-Amz-Algorithm=AWS4-HMAC-SHA256" +
                "&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F20130524%2Fus-east-1%2Fs3%2Faws4_request" +
                "&X-Amz-Date=20130524T000000Z" +
                "&X-Amz-Expires=86400" +
                "&X-Amz-SignedHeaders=host" +
                "&X-Amz-Signature=aeeed9bbccd4d02ee5c0109b86d86835f995330da4c265957d157751f604d404",
            query,
        )
    }

    @Test
    fun buildsPathStyleUrlWithBucketAndPortInTheSignedHost() {
        val presigner = S3Presigner(
            endpoint = "http://localhost:9000/",
            bucket = "trovatacast-presentations",
            region = "auto",
            accessKey = "local",
            secretKey = "local-secret",
            now = { Instant.parse("2026-09-27T12:00:00Z") },
        )
        val url = presigner.presignPut("presentations/abc/audio.m4a", 900)
        assertTrue(url.startsWith("http://localhost:9000/trovatacast-presentations/presentations/abc/audio.m4a?"))
        assertTrue(url.contains("X-Amz-Credential=local%2F20260927%2Fauto%2Fs3%2Faws4_request"))
        assertTrue(url.contains("X-Amz-Expires=900"))
    }

    @Test
    fun signatureDependsOnTheHttpMethod() {
        val presigner = S3Presigner(
            endpoint = "https://account.r2.cloudflarestorage.com",
            bucket = "bucket",
            region = "auto",
            accessKey = "key",
            secretKey = "secret",
            now = { Instant.parse("2026-09-27T12:00:00Z") },
        )
        val put = presigner.presignPut("presentations/abc/timeline.json", 900)
        val get = presigner.presignGet("presentations/abc/timeline.json", 900)
        assertTrue(put.substringAfter("X-Amz-Signature=") != get.substringAfter("X-Amz-Signature="))
    }

    @Test
    fun encodesReservedCharactersLikeAws() {
        assertEquals("a%2Fb%20c~d_e.f-g%C3%A7", SigV4.uriEncode("a/b c~d_e.f-gç"))
    }
}
