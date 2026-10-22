package app.trovata.cast.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
internal fun ResultSheet(
    state: AppointmentDetailUiState,
    onSelect: (CallResult) -> Unit,
    onNotes: (String) -> Unit,
    onNextStep: (String) -> Unit,
    onToggleReturn: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val draft = state.outcome
    AgendaSheet(onDismiss = onDismiss) {
        Text(
            text = "Como foi a chamada?",
            color = colors.ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).em,
        )
        state.appointment?.linkedOrder?.let {
            Text(text = "O pedido ${it.orderId} já foi fechado nessa chamada.", color = colors.jade2, fontSize = 12.5.sp)
        }
        CallResult.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { result ->
                    ResultOption(
                        result = result,
                        selected = draft.result == result,
                        onClick = { onSelect(result) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        AgendaTextField(
            label = "Anotações",
            value = draft.notes,
            onValueChange = onNotes,
            placeholder = "Dúvidas, objeções, o que foi negociado",
            singleLine = false,
        )
        AgendaTextField(
            label = "Próximo passo",
            value = draft.nextStep,
            onValueChange = onNextStep,
            placeholder = "Ex.: mandar a tabela de inverno na segunda",
        )
        CheckRow(
            label = "Agendar o retorno agora",
            detail = "Abre a agenda já com o cliente e o próximo passo preenchidos.",
            checked = draft.scheduleReturn,
            onToggle = onToggleReturn,
        )
        Btn(
            text = "Salvar resultado",
            onClick = onSave,
            kind = BtnKind.Primary,
            size = BtnSize.Lg,
            icon = TrovataIcons.check,
            enabled = draft.result != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ResultOption(result: CallResult, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(12.dp)
    val accent = when (result) {
        CallResult.OrderPlaced -> colors.jade
        CallResult.FollowUp -> colors.brand
        CallResult.NotInterested -> colors.ink3
        CallResult.NoAnswer -> colors.warn
    }
    Box(
        modifier = modifier
            .background(if (selected) accent else colors.surface, shape)
            .border(1.dp, if (selected) accent else colors.line, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = result.label,
            color = if (selected) Color.White else colors.ink,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun CancelSheet(clientName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = TrovataTokens.colors
    AgendaSheet(onDismiss = onDismiss) {
        Text(
            text = "Cancelar a chamada com $clientName?",
            color = colors.ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).em,
        )
        Text(
            text = "Ela sai da agenda, mas continua no histórico do cliente como cancelada.",
            color = colors.ink3,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Btn(
            text = "Cancelar compromisso",
            onClick = onConfirm,
            kind = BtnKind.Danger,
            size = BtnSize.Md,
            modifier = Modifier.fillMaxWidth(),
        )
        Btn(
            text = "Voltar",
            onClick = onDismiss,
            kind = BtnKind.Ghost,
            size = BtnSize.Md,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AgendaSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.bg, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.line, RoundedCornerShape(2.dp)),
            )
            content()
        }
    }
}
