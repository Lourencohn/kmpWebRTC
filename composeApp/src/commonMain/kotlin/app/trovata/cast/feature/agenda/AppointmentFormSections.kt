package app.trovata.cast.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.CatalogClient
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.SelectableChip
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.components.avatarHueFor
import app.trovata.cast.ui.format.durationLabel
import app.trovata.cast.ui.format.hourMinute
import app.trovata.cast.ui.format.monthShort
import app.trovata.cast.ui.format.weekdayShort
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import kotlinx.datetime.LocalDate

private const val SlotsPerRow = 4

@Composable
internal fun ClientSection(
    state: AppointmentFormUiState,
    onQuery: (String) -> Unit,
    onPick: (CatalogClient) -> Unit,
    onClear: () -> Unit,
) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = "Cliente")
        val client = state.client
        if (client != null) {
            TrovataCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(name = client.name, hue = avatarHueFor(client.name), size = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = client.name, color = colors.ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                            val detail = listOfNotNull(client.shop, client.email ?: client.phone).joinToString(" · ")
                            if (detail.isNotEmpty()) {
                                Text(text = detail, color = colors.ink3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (!state.clientLocked) {
                            Text(
                                text = "Trocar",
                                color = colors.brand,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable(onClick = onClear).padding(6.dp),
                            )
                        }
                    }
                    if (state.clientWithoutEmail) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = TrovataIcons.alert, contentDescription = null, tint = colors.warn, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Sem e-mail no cadastro. A chamada não vai abrir o carrinho dele.",
                                color = colors.warn,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
            return@Column
        }
        AgendaTextField(
            label = "",
            value = state.clientQuery,
            onValueChange = onQuery,
            placeholder = "Buscar cliente, cidade, CNPJ...",
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                if (state.clientResults.isEmpty()) {
                    Text(
                        text = if (state.clientQuery.isBlank()) "Nenhum cliente sincronizado ainda" else "Nenhum cliente encontrado",
                        color = colors.ink3,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                }
                state.clientResults.forEachIndexed { index, result ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(result) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Avatar(name = result.name, hue = (result.id % 360).toDouble(), size = 32.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = result.name, color = colors.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            (result.legalName?.takeIf { it != result.name } ?: result.document)?.let {
                                Text(text = it, color = colors.ink4, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (index < state.clientResults.lastIndex) AgendaDivider()
                }
            }
        }
    }
}

@Composable
internal fun DaySection(state: AppointmentFormUiState, onSelect: (LocalDate) -> Unit, onToggleMonth: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = "Dia", modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.dayOptions, key = { it.toEpochDays() }) { day ->
                DayOption(
                    day = day,
                    today = state.today,
                    selected = day == state.date,
                    load = state.loadOf(day),
                    onClick = { onSelect(day) },
                )
            }
            item {
                SelectableChip(
                    label = if (state.showMonthPicker) "Fechar" else "Outro dia",
                    active = state.showMonthPicker,
                    onClick = onToggleMonth,
                )
            }
        }
        if (state.showMonthPicker) {
            Spacer(modifier = Modifier.height(12.dp))
            MonthGrid(
                days = monthGridOf(state.date),
                month = state.date,
                selected = state.date,
                today = state.today,
                loadOf = { day -> AgendaDayLoad(total = state.loadOf(day), pending = 0, done = 0, priority = false) },
                onSelect = onSelect,
                isEnabled = { it >= state.today },
            )
        }
    }
}

@Composable
private fun DayOption(day: LocalDate, today: LocalDate, selected: Boolean, load: Int, onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(12.dp)
    val title = when (day.toEpochDays() - today.toEpochDays()) {
        0 -> "Hoje"
        1 -> "Amanhã"
        else -> weekdayShort(day.dayOfWeek).replaceFirstChar { it.uppercase() }
    }
    Column(
        modifier = Modifier
            .background(if (selected) colors.ink else colors.surface, shape)
            .border(1.dp, if (selected) colors.ink else colors.line, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = title, color = if (selected) Color.White else colors.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = "${day.dayOfMonth} ${monthShort(day.month)}",
            color = if (selected) Color.White.copy(alpha = 0.75f) else colors.ink4,
            fontSize = 11.sp,
        )
        Text(
            text = if (load == 0) "livre" else plural(load, "chamada", "chamadas"),
            color = if (selected) Color.White.copy(alpha = 0.75f) else if (load == 0) colors.ink5 else colors.brand,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun TimeSection(state: AppointmentFormUiState, onSelect: (Int) -> Unit, onNudge: (Boolean) -> Unit) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Horário",
            action = {
                state.minuteOfDay?.let { minute ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconBtn(icon = TrovataIcons.back, onClick = { onNudge(false) }, kind = IconBtnKind.Line, size = 28.dp, contentDescription = "15 minutos antes")
                        Text(
                            text = hourMinute(minute / 60, minute % 60),
                            style = TrovataTokens.type.mono.copy(fontSize = 13.sp, color = colors.ink, fontWeight = FontWeight.SemiBold),
                        )
                        IconBtn(icon = TrovataIcons.chev, onClick = { onNudge(true) }, kind = IconBtnKind.Line, size = 28.dp, contentDescription = "15 minutos depois")
                    }
                }
            },
        )
        if (state.slots.isEmpty()) {
            Text(text = "Não sobrou horário hoje. Escolha outro dia.", color = colors.ink3, fontSize = 13.sp)
            return@Column
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.slots.chunked(SlotsPerRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { slot ->
                        SlotChip(
                            slot = slot,
                            selected = slot.minuteOfDay == state.minuteOfDay,
                            onClick = { onSelect(slot.minuteOfDay) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(SlotsPerRow - row.size) { Box(modifier = Modifier.weight(1f)) }
                }
            }
        }
        Text(
            text = "Horários riscados já têm chamada marcada.",
            color = colors.ink4,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )
    }
}

@Composable
private fun SlotChip(slot: TimeSlot, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(10.dp)
    val busy = slot.state == SlotState.Busy
    val background = when {
        selected -> colors.brand
        busy -> colors.surface2
        else -> colors.surface
    }
    Box(
        modifier = modifier
            .background(background, shape)
            .border(1.dp, if (selected) colors.brand else colors.line, shape)
            .then(if (busy && !selected) Modifier else Modifier.clickable(role = Role.RadioButton, onClick = onClick))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = hourMinute(slot.minuteOfDay / 60, slot.minuteOfDay % 60),
            color = when {
                selected -> Color.White
                busy -> colors.ink5
                else -> colors.ink
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = if (busy && !selected) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
internal fun DurationSection(selected: Int, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = "Duração", modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DurationOptions) { minutes ->
                SelectableChip(label = durationLabel(minutes), active = minutes == selected, onClick = { onSelect(minutes) })
            }
        }
    }
}

@Composable
internal fun ObjectiveSection(objective: String, onObjective: (String) -> Unit, onSuggestion: (String) -> Unit) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = "Objetivo da conversa", modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ObjectiveSuggestions) { suggestion ->
                SelectableChip(label = suggestion, active = suggestion == objective, onClick = { onSuggestion(suggestion) })
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            AgendaTextField(
                label = "",
                value = objective,
                onValueChange = onObjective,
                placeholder = "Ou escreva com suas palavras",
            )
        }
        Text(
            text = "Ele aparece no topo do compromisso para você não perder o foco na chamada.",
            color = colors.ink4,
            fontSize = 11.5.sp,
            letterSpacing = (-0.005).em,
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 6.dp),
        )
    }
}
