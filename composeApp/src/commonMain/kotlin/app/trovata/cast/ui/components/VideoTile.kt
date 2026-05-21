package app.trovata.cast.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.color.oklch

@Composable
fun VideoTile(
    name: String,
    modifier: Modifier = Modifier,
    width: Dp = 76.dp,
    hue: Double = 220.0,
    label: String? = null,
    showLabel: Boolean = true,
    muted: Boolean = false,
    mini: Boolean = false,
) {
    val ratio = if (mini) 1f else 1.34f
    val height = width * ratio
    val tileShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shadow(elevation = 6.dp, shape = tileShape, clip = false)
            .clip(tileShape)
            .border(1.dp, Color.White.copy(alpha = 0.60f), tileShape),
    ) {
        Canvas(modifier = Modifier.size(width = width, height = height)) {
            val w = size.width
            val h = size.height
            val bgBrush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to oklch(85.0, 0.10, hue),
                    0.55f to oklch(55.0, 0.13, hue),
                    1f to oklch(28.0, 0.10, hue),
                ),
                center = Offset(w * 0.5f, h * 0.40f),
                radius = maxOf(w, h),
            )
            drawRect(brush = bgBrush, topLeft = Offset.Zero, size = Size(w, h))

            val shoulderColor = oklch(38.0, 0.05, hue + 20.0)
            drawOval(
                color = shoulderColor,
                topLeft = Offset(-w * 0.10f, h * 0.40f),
                size = Size(w * 1.20f, h * 0.60f),
            )

            val faceR = w * 0.23f
            val faceCenter = Offset(w * 0.5f, h * 0.55f)
            val faceBrush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to oklch(78.0, 0.06, hue),
                    1f to oklch(48.0, 0.10, hue),
                ),
                center = Offset(faceCenter.x - faceR * 0.30f, faceCenter.y - faceR * 0.40f),
                radius = faceR * 1.4f,
            )
            drawCircle(brush = faceBrush, radius = faceR, center = faceCenter)
        }

        if (showLabel) {
            VideoTileLabel(
                text = label ?: name,
                muted = muted,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun VideoTileLabel(text: String, muted: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    if (muted) Color(0xFFB6BCC4) else Color(0xFF3DDB8A),
                    CircleShape,
                ),
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.005).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
