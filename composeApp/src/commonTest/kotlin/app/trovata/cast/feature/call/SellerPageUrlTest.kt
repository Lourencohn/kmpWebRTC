package app.trovata.cast.feature.call

import kotlin.test.Test
import kotlin.test.assertEquals

class SellerPageUrlTest {

    private val invite = "https://staging.trovata.app.br/catalogo-link-view/buba/5f6c?live=tok123"

    @Test
    fun keepsInviteHostAndAddsEmbedFlag() {
        assertEquals("$invite&embed=seller", sellerPageUrl(invite))
    }

    @Test
    fun rebasesOntoOverrideForLocalFrontEnd() {
        assertEquals(
            "http://localhost:5173/catalogo-link-view/buba/5f6c?live=tok123&embed=seller",
            sellerPageUrl(invite, catalogWebBaseUrlOverride = "http://localhost:5173/"),
        )
    }
}
