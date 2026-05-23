package app.trovata.cast.theme

import androidx.compose.ui.graphics.Color

data class TrovataColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val ink4: Color,
    val ink5: Color,
    val brand: Color,
    val brand2: Color,
    val brandTint: Color,
    val brandRing: Color,
    val jade: Color,
    val jade2: Color,
    val jadeTint: Color,
    val live: Color,
    val warn: Color,
)

val LightColors: TrovataColors = TrovataColors(
    bg = Color(0xFFFAFAF6),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF4F4EE),
    surface3 = Color(0xFFECECE4),
    line = Color(0xFFE5E4DC),
    lineStrong = Color(0xFFD6D5CB),
    ink = Color(0xFF222222),
    ink2 = Color(0xFF333A42),
    ink3 = Color(0xFF5A6470),
    ink4 = Color(0xFF8A93A0),
    ink5 = Color(0xFFB6BCC4),
    brand = Color(0xFF0057B8),
    brand2 = Color(0xFF003F87),
    brandTint = Color(0xFFE6EFF9),
    brandRing = Color(0x380057B8),
    jade = Color(0xFF1B4332),
    jade2 = Color(0xFF0F2D20),
    jadeTint = Color(0xFFE2ECE6),
    live = Color(0xFFD62828),
    warn = Color(0xFFC7711D),
)
