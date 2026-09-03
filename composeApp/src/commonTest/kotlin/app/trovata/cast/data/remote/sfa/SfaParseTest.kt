package app.trovata.cast.data.remote.sfa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SfaParseTest {

    @Test
    fun parseCents_handlesIntegerString() {
        assertEquals(7900L, SfaParse.parseCents("79"))
    }

    @Test
    fun parseCents_handlesBrazilianDecimal() {
        assertEquals(18990L, SfaParse.parseCents("189,90"))
    }

    @Test
    fun parseCents_handlesThousandsSeparator() {
        assertEquals(118990L, SfaParse.parseCents("1.189,90"))
    }

    @Test
    fun parseCents_handlesCurrencyPrefixAndSpaces() {
        assertEquals(24900L, SfaParse.parseCents(" R$ 249,00 "))
    }

    @Test
    fun parseCents_handlesNullAndBlank() {
        assertNull(SfaParse.parseCents(null))
        assertNull(SfaParse.parseCents(""))
        assertNull(SfaParse.parseCents("   "))
    }

    @Test
    fun parseBool_mapsYesNo() {
        assertEquals(1L, SfaParse.parseBool("YES"))
        assertEquals(0L, SfaParse.parseBool("NO"))
        assertEquals(0L, SfaParse.parseBool(null))
    }

    @Test
    fun parseLong_handlesStringNumbers() {
        assertEquals(6L, SfaParse.parseLong("6"))
        assertNull(SfaParse.parseLong(""))
        assertNull(SfaParse.parseLong("abc"))
    }

    @Test
    fun parseTimestampToMs_aceitaIsoEFormatoDoLaravel() {
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
