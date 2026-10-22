package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
fun SelectableChip(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(999.dp)
    val bg = if (active) colors.ink else colors.surface
    val fg = if (active) Color.White else colors.ink2
    val borderColor = if (active) colors.ink else colors.line
    Box(
        modifier = Modifier
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.005).em,
        )
    }
}
