package app.trovata.cast.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val Geist: FontFamily = FontFamily.Default
val GeistMono: FontFamily = FontFamily.Monospace

data class TrovataType(
    val displayLg: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val subhead: TextStyle,
    val body: TextStyle,
    val bodyEm: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
    val mono: TextStyle,
    val monoBig: TextStyle,
)

val DefaultType: TrovataType = TrovataType(
    displayLg = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.03).em,
    ),
    title = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.025).em,
    ),
    heading = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.02).em,
    ),
    subhead = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.01).em,
    ),
    body = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    ),
    bodyEm = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    ),
    caption = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
    ),
    label = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.08.em,
        fontStyle = FontStyle.Normal,
    ),
    mono = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.04.em,
    ),
    monoBig = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.02).em,
    ),
)
