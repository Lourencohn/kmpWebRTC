package app.trovata.cast.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IceConfigTest {

    private fun env(vars: Map<String, String>): (String) -> String? = { vars[it] }

    @Test
    fun caiNoStunPublicoQuandoNadaEstaConfigurado() {
        val servers = IceConfig.fromEnv(env(emptyMap()))

        val único = servers.single()
        assertEquals(2, único.urls.size)
        assertTrue(único.urls.all { it.startsWith("stun:") })
        assertNull(único.username)
    }

    @Test
    fun montaTurnComCredencial() {
        val servers = IceConfig.fromEnv(
            env(
                mapOf(
                    "ICE_TURN_URLS" to "turn:turn.example:3478?transport=udp, turns:turn.example:5349",
                    "ICE_TURN_USERNAME" to "trovata",
                    "ICE_TURN_CREDENTIAL" to "segredo",
                ),
            ),
        )

        assertEquals(2, servers.size)
        val turn = servers.last()
        assertEquals(listOf("turn:turn.example:3478?transport=udp", "turns:turn.example:5349"), turn.urls)
        assertEquals("trovata", turn.username)
        assertEquals("segredo", turn.credential)
    }

    @Test
    fun substituiOStunPadraoQuandoInformado() {
        val servers = IceConfig.fromEnv(env(mapOf("ICE_STUN_URLS" to "stun:stun.trovata.app.br:3478")))

        assertEquals(listOf("stun:stun.trovata.app.br:3478"), servers.single().urls)
    }

    @Test
    fun ignoraEntradasVaziasEEspacos() {
        val servers = IceConfig.fromEnv(
            env(mapOf("ICE_STUN_URLS" to " , ", "ICE_TURN_URLS" to " turn:a:3478 , ,")),
        )

        assertEquals(2, servers.size)
        assertEquals(listOf("turn:a:3478"), servers.last().urls)
        assertNull(servers.last().username)
    }
}
