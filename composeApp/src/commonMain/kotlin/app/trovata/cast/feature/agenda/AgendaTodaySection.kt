package app.trovata.cast.feature.agenda

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.Appointment
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.format.hourMinuteAt
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens

private const val TodayPreviewLimit = 4

@Composable
fun AgendaTodaySection(
    state: AgendaUiState,
    onOpenAgenda: () -> Unit,
    onOpenAppointment: (Appointment) -> Unit,
    onNewAppointment: () -> Unit,
) {
    if (!state.hasOwner || state.isLoading) return
    val colors = TrovataTokens.colors
    val today = state.todayAppointments
    val pending = state.summary.pending
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Hoje na agenda",
            action = {
                Text(
                    text = "Ver agenda",
                    color = colors.brand,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onOpenAgenda).padding(4.dp),
                )
            },
        )
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                if (today.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Nenhuma chamada marcada para hoje.",
                            color = colors.ink3,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Btn(text = "Agendar", onClick = onNewAppointment, kind = BtnKind.Soft, size = BtnSize.Sm, icon = TrovataIcons.calendar)
                    }
                }
                today.take(TodayPreviewLimit).forEachIndexed { index, appointment ->
                    TodayRow(appointment = appointment, state = state, onClick = { onOpenAppointment(appointment) })
                    if (index < minOf(today.size, TodayPreviewLimit) - 1) AgendaDivider()
                }
                if (today.size > TodayPreviewLimit) {
                    AgendaDivider()
                    Text(
                        text = "Mais ${today.size - TodayPreviewLimit} na agenda",
                        color = colors.brand,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAgenda).padding(14.dp),
                    )
                }
                if (pending > 0) {
                    AgendaDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAgenda).padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(imageVector = TrovataIcons.alert, contentDescription = null, tint = colors.warn, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${plural(pending, "chamada", "chamadas")} sem resultado registrado",
                            color = colors.warn,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayRow(appointment: Appointment, state: AgendaUiState, onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val status = appointment.agendaStatus(state.nowMs)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = hourMinuteAt(appointment.startsAtMs, state.timeZone),
            style = TrovataTokens.type.mono.copy(fontSize = 13.5.sp, color = status.accent(), fontWeight = FontWeight.SemiBold),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appointment.clientName,
                color = colors.ink,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = startLabel(appointment, state.nowMs) ?: appointment.objective
            detail?.let { Text(text = it, color = colors.ink4, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        StatusPill(status = status, appointment = appointment)
    }
}
