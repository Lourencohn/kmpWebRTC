package app.trovata.cast.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val Geist: FontFamily = FontFamily.Default
val GeistMono: FontFamily = FontFamily.Monospace

data class TrovataType(
    val title: TextStyle,
    val body: TextStyle,
    val mono: TextStyle,
)

val DefaultType: TrovataType = TrovataType(
    title = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.025).em,
    ),
    body = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    ),
    mono = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.04.em,
    ),
)
