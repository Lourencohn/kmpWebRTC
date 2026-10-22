package app.trovata.cast.feature.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.OrderLine
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.format.formatBrl
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
internal fun OrderSummaryOverlay(
    summary: OrderSummaryUi,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    Column(
        modifier = modifier
            .background(colors.bg)
            .padding(top = 56.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Pill(
                text = if (summary.confirmedByMe) "Você confirmou" else "Cliente confirmou",
                tone = PillTone.Jade,
                icon = TrovataIcons.check,
            )
            Text(
                text = "Pedido pronto",
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).em,
            )
            Text(
                text = "ID ${summary.orderId} · ${summary.lines.sumOf { it.units }} un · ${summary.lines.size} ${if (summary.lines.size == 1) "linha" else "linhas"}",
                color = colors.ink3,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(summary.lines, key = { it.productId + "/" + it.size }) { line ->
                OrderSummaryRow(line = line)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface2)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total do pedido",
                        color = colors.ink3,
                        fontSize = 11.sp,
                        letterSpacing = 0.06.em,
                    )
                    Text(
                        text = formatBrl(summary.totalCents),
                        color = colors.ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Btn(
                text = "Fechar e encerrar",
                onClick = onClose,
                kind = BtnKind.Jade,
                size = BtnSize.Lg,
                icon = TrovataIcons.hangup,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OrderSummaryRow(line: OrderLine) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${line.productId} · Tam ${line.size} · ${line.units}un",
            color = colors.ink2,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatBrl(line.subtotalCents),
            color = colors.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
