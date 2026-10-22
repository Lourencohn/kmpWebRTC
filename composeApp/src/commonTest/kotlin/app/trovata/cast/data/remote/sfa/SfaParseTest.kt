package app.trovata.cast.data.remote.sfa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SfaParseTest {

    @Test
    fun parseTimestampToMs_acceptsIsoAndLaravelFormat() {
        val iso = SfaParse.parseTimestampToMs("2026-08-30T19:41:02Z")
        val laravel = SfaParse.parseTimestampToMs("2026-08-30 16:41:02")

        assertNotNull(iso)
        assertNotNull(laravel)
        assertEquals(iso, laravel)
        assertNull(SfaParse.parseTimestampToMs(null))
        assertNull(SfaParse.parseTimestampToMs("30/08/2026"))
    }

    @Test
    fun parseIsoToMs_parsesAndOrders() {
        val earlier = SfaParse.parseIsoToMs("2025-10-31T21:12:12Z")
        val later = SfaParse.parseIsoToMs("2026-04-29T15:12:19Z")
        assertTrue(earlier != null && later != null && later > earlier)
        assertNull(SfaParse.parseIsoToMs(null))
        assertNull(SfaParse.parseIsoToMs("not-a-date"))
    }
}
