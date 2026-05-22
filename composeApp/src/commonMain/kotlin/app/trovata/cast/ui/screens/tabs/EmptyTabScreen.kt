package app.trovata.cast.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.ScreenHeader
import app.trovata.cast.ui.components.TrovataCard

@Composable
fun EmptyTabScreen(
    eyebrow: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    bullets: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                ScreenHeader(
                    title = title,
                    subtitle = subtitle,
                    eyebrow = {
                        Text(
                            text = eyebrow.uppercase(),
                            color = colors.ink4,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.08.em,
                        )
                    },
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    TrovataCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(colors.surface2, RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = colors.ink3,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Pill(text = "Em breve", tone = PillTone.Brand)
                            Text(
                                text = "Esta aba está em construção",
                                color = colors.ink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Estamos preparando esta área. Quando ficar pronta, você terá acesso aos recursos listados abaixo.",
                                color = colors.ink3,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    bullets.forEach { line ->
                        TrovataCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = line,
                                color = colors.ink2,
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
