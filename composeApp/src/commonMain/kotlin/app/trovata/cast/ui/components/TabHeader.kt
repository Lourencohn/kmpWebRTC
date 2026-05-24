package app.trovata.cast.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.theme.TrovataTokens

@Composable
fun TabHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryIcon: ImageVector? = null,
    onSecondaryClick: () -> Unit = {},
) {
    val colors = TrovataTokens.colors
    ScreenHeader(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        eyebrow = {
            Text(
                text = eyebrow.uppercase(),
                color = colors.ink4,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
        },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (secondaryIcon != null) {
                    IconBtn(icon = secondaryIcon, onClick = onSecondaryClick, kind = IconBtnKind.Line)
                }
                AccountChip(onClick = onOpenAccount)
            }
        },
    )
}
