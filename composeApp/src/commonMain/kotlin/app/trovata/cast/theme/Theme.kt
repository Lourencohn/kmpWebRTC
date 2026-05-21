package app.trovata.cast.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColors = staticCompositionLocalOf { LightColors }
private val LocalType = staticCompositionLocalOf { DefaultType }
private val LocalRadii = staticCompositionLocalOf { DefaultRadii }
private val LocalSpacing = staticCompositionLocalOf { DefaultSpacing }

object TrovataTokens {
    val colors: TrovataColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: TrovataType
        @Composable @ReadOnlyComposable get() = LocalType.current
    val radii: TrovataRadii
        @Composable @ReadOnlyComposable get() = LocalRadii.current
    val spacing: TrovataSpacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}

@Composable
fun TrovataTheme(content: @Composable () -> Unit) {
    val colors = LightColors
    val materialScheme = lightColorScheme(
        primary = colors.brand,
        onPrimary = colors.surface,
        secondary = colors.jade,
        onSecondary = colors.surface,
        background = colors.bg,
        onBackground = colors.ink,
        surface = colors.surface,
        onSurface = colors.ink,
        error = colors.live,
    )
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalType provides DefaultType,
        LocalRadii provides DefaultRadii,
        LocalSpacing provides DefaultSpacing,
    ) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}
