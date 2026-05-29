package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.icons.TrovataIcons

enum class SellerTab(val id: String, val label: String, val icon: ImageVector) {
    Painel("painel", "Painel", TrovataIcons.chart),
    Sessoes("sessoes", "Sessões", TrovataIcons.video),
    Catalogo("catalogo", "Catálogo", TrovataIcons.grid),
    Clientes("clientes", "Clientes", TrovataIcons.users),
    Insights("insights", "Insights", TrovataIcons.trend),
}

@Composable
fun TabBar(
    active: SellerTab,
    onSelect: (SellerTab) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    val colors = TrovataTokens.colors
    val background = if (dark) colors.ink.copy(alpha = 0.92f) else colors.surface.copy(alpha = 0.94f)
    val border = if (dark) Color.White.copy(alpha = 0.08f) else colors.line

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .drawBehind {
                drawLine(
                    color = border,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SellerTab.entries.forEach { tab ->
            val on = tab == active
            val color = when {
                on && dark -> Color.White
                on -> colors.brand
                dark -> Color.White.copy(alpha = 0.45f)
                else -> colors.ink4
            }
            Column(
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = color,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = tab.label,
                    color = color,
                    fontSize = 10.5.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}
