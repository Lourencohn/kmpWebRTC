package app.trovata.cast.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WhatsAppTest {

    @Test
    fun localNumbersGainTheBrazilianCountryCode() {
        assertEquals("5511988887777", whatsAppNumberOf("(11) 98888-7777"))
        assertEquals("551133334444", whatsAppNumberOf("11 3333-4444"))
        assertEquals("5511988887777", whatsAppNumberOf("011 98888-7777"))
    }

    @Test
    fun numbersWithCountryCodeAreKept() {
        assertEquals("5511988887777", whatsAppNumberOf("+55 (11) 98888-7777"))
        assertEquals("5555999998888", whatsAppNumberOf("0055 55 99999-8888"))
        assertEquals("555599998888", whatsAppNumberOf("55 9999-8888"))
    }

    @Test
    fun numbersWithoutAreaCodeOrEmptyAreRejected() {
        assertNull(whatsAppNumberOf("98888-7777"))
        assertNull(whatsAppNumberOf(""))
        assertNull(whatsAppNumberOf(null))
        assertNull(whatsAppNumberOf("sem telefone"))
    }

    @Test
    fun placeholderNumbersFromTheRegistryAreRejected() {
        assertNull(whatsAppNumberOf("(11) 00000-0000"))
        assertNull(whatsAppNumberOf("11000000000"))
        assertNull(whatsAppNumberOf("(11) 99999-9999"))
        assertNull(whatsAppNumberOf("(11) 90000-0000"))
        assertNull(whatsAppNumberOf("(10) 98888-7777"))
        assertNull(whatsAppNumberOf("(11) 88888-7777"))
        assertNull(whatsAppNumberOf("(11) 1333-4444"))
    }

    @Test
    fun urlCarriesTheEncodedMessage() {
        assertEquals(
            "https://wa.me/5511988887777?text=Oi%2C%20Marina%21%20Pedido%20pronto%3A%20R%24%2010",
            whatsAppUrl("5511988887777", "Oi, Marina! Pedido pronto: R$ 10"),
        )
    }

    @Test
    fun numberIsShownTheWaySellersWriteIt() {
        assertEquals("(11) 98888-7777", formatWhatsAppNumber("5511988887777"))
        assertEquals("(11) 3333-4444", formatWhatsAppNumber("551133334444"))
    }
}
