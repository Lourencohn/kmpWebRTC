package app.trovata.cast.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.theme.TrovataTokens

enum class BtnKind { Primary, Jade, Soft, Ghost, Surface, Dark, Danger }
enum class BtnSize { Sm, Md, Lg }

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Primary,
    size: BtnSize = BtnSize.Md,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = TrovataTokens.colors
    val dims = when (size) {
        BtnSize.Sm -> BtnDims(height = 32.dp, paddingH = 12.dp, fontSize = 13.sp, radius = 8.dp, iconSize = 16.dp)
        BtnSize.Md -> BtnDims(height = 44.dp, paddingH = 16.dp, fontSize = 15.sp, radius = 12.dp, iconSize = 18.dp)
        BtnSize.Lg -> BtnDims(height = 52.dp, paddingH = 20.dp, fontSize = 16.sp, radius = 14.dp, iconSize = 19.dp)
    }
    val palette = when (kind) {
        BtnKind.Primary -> BtnPalette(colors.brand, Color.White, colors.brand2)
        BtnKind.Jade -> BtnPalette(colors.jade, Color.White, colors.jade2)
        BtnKind.Soft -> BtnPalette(colors.brandTint, colors.brand2, Color.Transparent)
        BtnKind.Ghost -> BtnPalette(Color.Transparent, colors.ink, colors.line)
        BtnKind.Surface -> BtnPalette(colors.surface, colors.ink, colors.line, elevated = true)
        BtnKind.Dark -> BtnPalette(colors.ink, Color.White, Color.Transparent)
        BtnKind.Danger -> BtnPalette(colors.live, Color.White, Color.Transparent)
    }
    val shape: Shape = RoundedCornerShape(dims.radius)

    val base = modifier
        .defaultMinSize(minHeight = dims.height)
        .height(dims.height)
    val withShadow = if (palette.elevated) base.shadow(elevation = 1.dp, shape = shape, clip = false) else base
    val withBg = withShadow.background(if (enabled) palette.background else palette.background.copy(alpha = 0.6f), shape)
    val withBorder = withBg.border(BorderStroke(1.dp, palette.border), shape)
    val clickableModifier = if (enabled) withBorder.clickable(onClick = onClick) else withBorder

    Row(
        modifier = clickableModifier.padding(horizontal = dims.paddingH),
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.content,
                modifier = Modifier.size(dims.iconSize),
            )
        }
        Text(
            text = text,
            color = palette.content,
            fontSize = dims.fontSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.005).sp,
        )
    }
}

private data class BtnDims(
    val height: Dp,
    val paddingH: Dp,
    val fontSize: TextUnit,
    val radius: Dp,
    val iconSize: Dp,
)

private data class BtnPalette(
    val background: Color,
    val content: Color,
    val border: Color,
    val elevated: Boolean = false,
)

enum class IconBtnKind { Soft, Dark, Danger, Jade, Brand, Line }

@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    kind: IconBtnKind = IconBtnKind.Soft,
    active: Boolean = false,
    contentDescription: String? = null,
) {
    val colors = TrovataTokens.colors
    val palette = when (kind) {
        IconBtnKind.Soft -> IconBtnPalette(Color.White.copy(alpha = 0.85f), colors.ink, Color.Black.copy(alpha = 0.06f))
        IconBtnKind.Dark -> IconBtnPalette(colors.ink.copy(alpha = 0.74f), Color.White, Color.White.copy(alpha = 0.10f))
        IconBtnKind.Danger -> IconBtnPalette(colors.live, Color.White, Color.Transparent)
        IconBtnKind.Jade -> IconBtnPalette(colors.jade, Color.White, Color.Transparent)
        IconBtnKind.Brand -> IconBtnPalette(colors.brand, Color.White, Color.Transparent)
        IconBtnKind.Line -> IconBtnPalette(colors.surface, colors.ink, colors.line)
    }
    val bg = if (active) colors.ink else palette.background
    val content = if (active) Color.White else palette.content
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 1.dp, shape = CircleShape, clip = false)
            .background(bg, CircleShape)
            .border(1.dp, palette.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

private data class IconBtnPalette(
    val background: Color,
    val content: Color,
    val border: Color,
)
