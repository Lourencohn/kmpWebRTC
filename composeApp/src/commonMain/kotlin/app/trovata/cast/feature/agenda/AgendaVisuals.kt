package app.trovata.cast.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.format.durationLabel
import app.trovata.cast.ui.format.formatBrl
import app.trovata.cast.ui.format.hourMinuteAt
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import kotlinx.datetime.TimeZone

@Composable
internal fun AgendaStatus.accent(): Color {
    val colors = TrovataTokens.colors
    return when (this) {
        AgendaStatus.Upcoming -> colors.brand
        AgendaStatus.Now -> colors.jade
        AgendaStatus.Pending -> colors.warn
        AgendaStatus.Done -> colors.jade
        AgendaStatus.Missed -> colors.ink4
        AgendaStatus.Cancelled -> colors.ink5
    }
}

@Composable
internal fun StatusPill(status: AgendaStatus, appointment: Appointment) {
    val order = appointment.linkedOrder
    when (status) {
        AgendaStatus.Upcoming -> Unit
        AgendaStatus.Now -> Pill(text = status.label, tone = PillTone.Jade, icon = TrovataIcons.video)
        AgendaStatus.Pending -> Pill(text = status.label, tone = PillTone.Warn, icon = TrovataIcons.alert)
        AgendaStatus.Done -> Pill(
            text = order?.let { "Pedido ${formatBrl(it.totalCents)}" } ?: appointment.outcome?.label() ?: status.label,
            tone = PillTone.Jade,
            icon = TrovataIcons.check,
        )
        AgendaStatus.Missed -> Pill(text = status.label, tone = PillTone.Neutral)
        AgendaStatus.Cancelled -> Pill(text = status.label, tone = PillTone.Ghost)
    }
}

@Composable
internal fun AppointmentCard(
    appointment: Appointment,
    nowMs: Long,
    timeZone: TimeZone,
    openCart: OpenCart?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val status = appointment.agendaStatus(nowMs)
    val shape = RoundedCornerShape(14.dp)
    val cancelled = status == AgendaStatus.Cancelled
    val secondary = appointment.objective ?: appointment.clientShop ?: appointment.clientEmail
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(colors.surface, shape)
            .border(1.dp, if (status == AgendaStatus.Now) colors.jade.copy(alpha = 0.45f) else colors.line, shape)
            .clickable(onClickLabel = "Abrir compromisso", role = Role.Button, onClick = onOpen)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(status.accent(), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)),
        )
        Column(
            modifier = Modifier.width(48.dp).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = hourMinuteAt(appointment.startsAtMs, timeZone),
                style = TrovataTokens.type.mono.copy(
                    fontSize = 14.sp,
                    color = if (cancelled) colors.ink4 else colors.ink,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(text = durationLabel(appointment.durationMinutes), color = colors.ink4, fontSize = 11.sp)
        }
        Column(
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (appointment.isPriority && !cancelled) {
                    Icon(
                        imageVector = TrovataIcons.star,
                        contentDescription = "Prioritário",
                        tint = colors.warn,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = appointment.clientName,
                    color = if (cancelled) colors.ink4 else colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.01).em,
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            secondary?.let {
                Text(text = it, color = colors.ink3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val showCart = openCart != null && appointment.isOpen
            if (status != AgendaStatus.Upcoming || showCart) {
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusPill(status = status, appointment = appointment)
                    if (showCart) {
                        Pill(
                            text = openCart?.totalCents?.let { "${formatBrl(it)} no carrinho" } ?: "Carrinho aberto",
                            tone = PillTone.Brand,
                            icon = TrovataIcons.cart,
                        )
                    }
                }
            }
            startLabel(appointment, nowMs)?.let {
                Text(text = it, color = if (status == AgendaStatus.Pending) colors.warn else colors.ink4, fontSize = 11.sp)
            }
        }
        Icon(
            imageVector = TrovataIcons.chev,
            contentDescription = null,
            tint = colors.ink5,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun AgendaTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(
                text = label.uppercase(),
                color = colors.ink4,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.08.em,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            textStyle = TextStyle(color = colors.ink, fontSize = 14.sp, lineHeight = 19.sp),
            cursorBrush = SolidColor(colors.brand),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .background(colors.surface, shape)
                .border(1.dp, colors.line, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(text = placeholder, color = colors.ink5, fontSize = 14.sp, lineHeight = 19.sp)
                    inner()
                }
            },
        )
    }
}

@Composable
internal fun CheckRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    icon: ImageVector? = null,
) {
    val colors = TrovataTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val boxShape = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(if (checked) colors.jade else colors.surface, boxShape)
                .border(1.5.dp, if (checked) colors.jade else colors.ink5, boxShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = TrovataIcons.check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                icon?.let {
                    Icon(imageVector = it, contentDescription = null, tint = colors.warn, modifier = Modifier.size(14.dp))
                }
                Text(text = label, color = colors.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            }
            detail?.let { Text(text = it, color = colors.ink3, fontSize = 12.sp, lineHeight = 16.sp) }
        }
    }
}

@Composable
internal fun ReadinessRow(check: ReadinessCheck) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(if (check.ok) colors.jadeTint else colors.warn.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (check.ok) TrovataIcons.check else TrovataIcons.alert,
                contentDescription = if (check.ok) "Pronto" else "Atenção",
                tint = if (check.ok) colors.jade2 else colors.warn,
                modifier = Modifier.size(13.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = check.label, color = colors.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            if (!check.ok) Text(text = check.hint, color = colors.ink3, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
internal fun AgendaBanner(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TrovataTokens.colors.ink, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = TrovataIcons.alert,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "OK",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
internal fun AgendaDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TrovataTokens.colors.line))
}
