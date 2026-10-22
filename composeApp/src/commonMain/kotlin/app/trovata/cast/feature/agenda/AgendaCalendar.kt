package app.trovata.cast.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.SelectableChip
import app.trovata.cast.ui.format.weekdayLong
import app.trovata.cast.ui.format.weekdayShort
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import kotlinx.datetime.LocalDate

private val WeekHeaderLetters = listOf("S", "T", "Q", "Q", "S", "S", "D")

@Composable
internal fun PeriodControls(
    view: AgendaView,
    periodLabel: String,
    isShowingToday: Boolean,
    onView: (AgendaView) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val colors = TrovataTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewSwitcher(active = view, onSelect = onView, modifier = Modifier.weight(1f))
            if (!isShowingToday) SelectableChip(label = "Hoje", active = false, onClick = onToday)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBtn(
                icon = TrovataIcons.back,
                onClick = onPrevious,
                kind = IconBtnKind.Line,
                size = 34.dp,
                contentDescription = "Período anterior",
            )
            Text(
                text = periodLabel,
                color = colors.ink,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconBtn(
                icon = TrovataIcons.chev,
                onClick = onNext,
                kind = IconBtnKind.Line,
                size = 34.dp,
                contentDescription = "Próximo período",
            )
        }
    }
}

@Composable
private fun ViewSwitcher(active: AgendaView, onSelect: (AgendaView) -> Unit, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .background(colors.surface2, shape)
            .border(1.dp, colors.line, shape)
            .padding(3.dp),
    ) {
        AgendaView.entries.forEach { option ->
            val on = option == active
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (on) Modifier.shadow(1.dp, shape, clip = false) else Modifier)
                    .background(if (on) colors.surface else Color.Transparent, shape)
                    .clickable { onSelect(option) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    color = if (on) colors.ink else colors.ink3,
                    fontSize = 12.5.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun WeekStrip(
    week: List<LocalDate>,
    selected: LocalDate?,
    today: LocalDate,
    loadOf: (LocalDate) -> AgendaDayLoad,
    onSelect: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        week.forEach { day ->
            DayCell(
                day = day,
                isSelected = day == selected,
                isToday = day == today,
                isPast = day < today,
                load = loadOf(day),
                onClick = { onSelect(day) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isPast: Boolean,
    load: AgendaDayLoad,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        isSelected -> colors.ink
        isToday -> colors.brandTint
        else -> Color.Transparent
    }
    val numberColor = when {
        isSelected -> Color.White
        isToday -> colors.brand2
        isPast -> colors.ink4
        else -> colors.ink
    }
    val description = buildString {
        append("${weekdayLong(day.dayOfWeek)}, dia ${day.dayOfMonth}")
        if (isToday) append(", hoje")
        append(", ${plural(load.total, "chamada", "chamadas")}")
        if (load.pending > 0) append(", ${load.pending} pendente")
    }
    Column(
        modifier = modifier
            .background(background, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = weekdayShort(day.dayOfWeek).uppercase(),
            color = if (isSelected) Color.White.copy(alpha = 0.7f) else colors.ink4,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.06.em,
        )
        Text(text = day.dayOfMonth.toString(), color = numberColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        LoadDots(load = load, onDark = isSelected)
    }
}

@Composable
internal fun LoadDots(load: AgendaDayLoad, onDark: Boolean) {
    val colors = TrovataTokens.colors
    val dots = buildList {
        repeat(load.pending) { add(colors.warn) }
        repeat(load.done) { add(if (onDark) Color.White else colors.jade) }
        repeat((load.total - load.pending - load.done).coerceAtLeast(0)) { add(if (onDark) Color.White.copy(alpha = 0.75f) else colors.brand) }
    }.take(3)
    Row(
        modifier = Modifier.height(5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        dots.forEach { color -> Box(modifier = Modifier.size(5.dp).background(color, CircleShape)) }
    }
}

@Composable
internal fun MonthGrid(
    days: List<LocalDate>,
    month: LocalDate,
    selected: LocalDate?,
    today: LocalDate,
    loadOf: (LocalDate) -> AgendaDayLoad,
    onSelect: (LocalDate) -> Unit,
    isEnabled: (LocalDate) -> Boolean = { true },
) {
    val colors = TrovataTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WeekHeaderLetters.forEach { letter ->
                Text(
                    text = letter,
                    color = colors.ink4,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
            }
        }
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { day ->
                    MonthCell(
                        day = day,
                        inMonth = day.month == month.month,
                        isSelected = day == selected,
                        isToday = day == today,
                        enabled = isEnabled(day),
                        load = loadOf(day),
                        onClick = { onSelect(day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCell(
    day: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    enabled: Boolean,
    load: AgendaDayLoad,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(10.dp)
    val background = when {
        isSelected -> colors.ink
        isToday -> colors.brandTint
        else -> Color.Transparent
    }
    val numberColor = when {
        isSelected -> Color.White
        !enabled || !inMonth -> colors.ink5
        isToday -> colors.brand2
        else -> colors.ink
    }
    Column(
        modifier = modifier
            .height(44.dp)
            .background(background, shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(top = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = day.dayOfMonth.toString(), color = numberColor, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
        if (inMonth) LoadDots(load = load, onDark = isSelected) else Spacer(modifier = Modifier.height(5.dp))
    }
}
