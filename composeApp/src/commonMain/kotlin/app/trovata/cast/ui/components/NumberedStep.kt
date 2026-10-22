package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
fun NumberedStep(number: Int, title: String, detail: String) {
    val colors = TrovataTokens.colors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(colors.ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "$number", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(text = detail, color = colors.ink3, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
