package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.AppointmentStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class AppointmentMessagesTest {

    private val now = at(Wednesday, 10)

    @Test
    fun remindsTheTimeOfAnUpcomingCall() {
        val today = appointment("apt-1", at(Wednesday, 15), clientName = "Marina Couto")
        val tomorrow = appointment("apt-2", at(Wednesday, 9) + DayMs, clientName = "Marina Couto")
        val nextWeek = appointment("apt-3", at(Wednesday, 16, 30) + 7 * DayMs, clientName = "Marina Couto")

        assertEquals(
            "Oi, Marina! Aqui é Lourenço. Combinamos de conversar hoje às 15:00. " +
                "Na hora eu te mando o link do catálogo, é só abrir no celular.",
            appointmentReminderMessage(today, "Lourenço Pereira", now, SaoPaulo),
        )
        assertEquals(
            "Oi, Marina! Aqui é Lourenço. Combinamos de conversar amanhã às 09:00. " +
                "Na hora eu te mando o link do catálogo, é só abrir no celular.",
            appointmentReminderMessage(tomorrow, "Lourenço Pereira", now, SaoPaulo),
        )
        assertEquals(
            "Oi, Marina! Aqui é Lourenço. Combinamos de conversar no dia 28/10 às 16:30. " +
                "Na hora eu te mando o link do catálogo, é só abrir no celular.",
            appointmentReminderMessage(nextWeek, "Lourenço Pereira", now, SaoPaulo),
        )
    }

    @Test
    fun pastOrClosedCallsOnlyGreet() {
        val past = appointment("apt-1", at(Wednesday, 8), clientName = "Marina Couto")
        val done = appointment("apt-2", at(Wednesday, 15), clientName = "Marina Couto", status = AppointmentStatus.Done)

        assertEquals("Oi, Marina! Aqui é Lourenço.", appointmentReminderMessage(past, "Lourenço", now, SaoPaulo))
        assertEquals("Oi, Marina! Aqui é Lourenço.", appointmentReminderMessage(done, "Lourenço", now, SaoPaulo))
    }
}
