package app.trovata.cast.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class TrovataRadii(
    val r1: Dp = 6.dp,
    val r2: Dp = 10.dp,
    val r3: Dp = 14.dp,
    val r4: Dp = 20.dp,
    val r5: Dp = 28.dp,
) {
    val pill: Shape = RoundedCornerShape(999.dp)
    val card: Shape = RoundedCornerShape(r3)
    val buttonSm: Shape = RoundedCornerShape(r2)
    val buttonLg: Shape = RoundedCornerShape(r3)
}

data class TrovataSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
)

val DefaultRadii: TrovataRadii = TrovataRadii()
val DefaultSpacing: TrovataSpacing = TrovataSpacing()
