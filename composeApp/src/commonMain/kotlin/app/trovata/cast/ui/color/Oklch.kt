package app.trovata.cast.ui.color

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

fun oklch(lPercent: Double, c: Double, hueDeg: Double, alpha: Float = 1f): Color {
    val l = lPercent / 100.0
    val hRad = hueDeg * PI / 180.0
    val a = c * cos(hRad)
    val b = c * sin(hRad)

    val lPrime = l + 0.3963377774 * a + 0.2158037573 * b
    val mPrime = l - 0.1055613458 * a - 0.0638541728 * b
    val sPrime = l - 0.0894841775 * a - 1.2914855480 * b

    val lc = lPrime * lPrime * lPrime
    val mc = mPrime * mPrime * mPrime
    val sc = sPrime * sPrime * sPrime

    val rLin = 4.0767416621 * lc - 3.3077115913 * mc + 0.2309699292 * sc
    val gLin = -1.2684380046 * lc + 2.6097574011 * mc - 0.3413193965 * sc
    val bLin = -0.0041960863 * lc - 0.7034186147 * mc + 1.7076147010 * sc

    return Color(
        red = linearToSrgb(rLin).coerceIn(0.0, 1.0).toFloat(),
        green = linearToSrgb(gLin).coerceIn(0.0, 1.0).toFloat(),
        blue = linearToSrgb(bLin).coerceIn(0.0, 1.0).toFloat(),
        alpha = alpha,
    )
}

private fun linearToSrgb(c: Double): Double =
    if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1.0 / 2.4) - 0.055

object HueRoles {
    const val SELLER: Double = 30.0
    const val BUYER: Double = 210.0
    const val NEUTRAL: Double = 220.0
}
