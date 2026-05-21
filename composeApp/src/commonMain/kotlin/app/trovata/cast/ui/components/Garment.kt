package app.trovata.cast.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GarmentKind { Shirt, Polo, Tee, Dress, Jacket, Pants, Shoe, Sweater, Skirt }

@Composable
fun Garment(
    kind: GarmentKind,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val vector = remember(kind, tint) { buildGarment(kind, tint) }
    Image(
        imageVector = vector,
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

private fun buildGarment(kind: GarmentKind, tint: Color): ImageVector {
    val b = ImageVector.Builder(
        name = "garment.${kind.name.lowercase()}",
        defaultWidth = 100.dp,
        defaultHeight = 100.dp,
        viewportWidth = 100f,
        viewportHeight = 100f,
    )
    when (kind) {
        GarmentKind.Shirt -> b.fill(
            "M22 28 L34 18 L40 22 Q50 30 60 22 L66 18 L78 28 L72 38 L68 36 L68 82 L32 82 L32 36 L28 38 Z",
            tint,
        )
        GarmentKind.Polo -> {
            b.fill(
                "M22 30 L36 20 L44 26 Q50 31 56 26 L64 20 L78 30 L72 40 L68 38 L68 82 L32 82 L32 38 L28 40 Z",
                tint,
            )
            b.fill(
                "M42 26 L50 34 L58 26 L56 22 L50 28 L44 22 Z",
                Color.Black.copy(alpha = 0.18f),
            )
            b.fill(
                "M49 32 L51 32 L51 42 L49 42 Z",
                Color.Black.copy(alpha = 0.20f),
            )
        }
        GarmentKind.Tee -> {
            b.fill(
                "M22 30 L36 20 L42 24 Q50 30 58 24 L64 20 L78 30 L72 40 L68 38 L68 82 L32 82 L32 38 L28 40 Z",
                tint,
            )
            b.stroke(
                "M42 22 Q50 28 58 22",
                Color.Black.copy(alpha = 0.20f),
                width = 1.5f,
            )
        }
        GarmentKind.Dress -> {
            b.fill(
                "M36 22 L44 18 Q50 22 56 18 L64 22 L66 36 L72 82 L28 82 L34 36 Z",
                tint,
            )
            b.stroke(
                "M44 18 Q50 22 56 18",
                Color.Black.copy(alpha = 0.22f),
                width = 1.5f,
            )
        }
        GarmentKind.Jacket -> {
            b.fill(
                "M22 28 L34 18 L40 22 L40 82 L60 82 L60 22 L66 18 L78 28 L72 38 L66 36 L66 82 L34 82 L34 36 L28 38 Z",
                tint,
            )
            b.stroke("M50 24 L50 82", Color.Black.copy(alpha = 0.25f), width = 1.4f)
            b.fill(circlePath(50f, 38f, 1.2f), Color.Black.copy(alpha = 0.30f))
            b.fill(circlePath(50f, 52f, 1.2f), Color.Black.copy(alpha = 0.30f))
            b.fill(circlePath(50f, 66f, 1.2f), Color.Black.copy(alpha = 0.30f))
        }
        GarmentKind.Pants -> {
            b.fill(
                "M32 18 L68 18 L70 50 L62 82 L52 82 L50 56 L48 56 L46 82 L38 82 L30 50 Z",
                tint,
            )
            b.stroke("M50 22 L50 56", Color.Black.copy(alpha = 0.18f), width = 1f)
        }
        GarmentKind.Shoe -> {
            b.fill(
                "M16 60 Q22 42 38 44 L62 50 Q78 54 84 62 L82 70 Q80 76 70 76 L20 76 Q14 76 14 70 Z",
                tint,
            )
            b.stroke("M38 44 L46 60 L58 56", Color.Black.copy(alpha = 0.25f), width = 1.5f)
            b.stroke("M16 70 L82 70", Color.Black.copy(alpha = 0.22f), width = 1f)
        }
        GarmentKind.Sweater -> {
            b.fill(
                "M22 30 L34 20 L40 24 Q50 32 60 24 L66 20 L78 30 L72 42 L68 40 L68 82 L32 82 L32 40 L28 42 Z",
                tint,
            )
            b.stroke("M32 50 L68 50", Color.Black.copy(alpha = 0.12f), width = 1f)
            b.stroke("M32 60 L68 60", Color.Black.copy(alpha = 0.12f), width = 1f)
            b.stroke("M32 70 L68 70", Color.Black.copy(alpha = 0.12f), width = 1f)
        }
        GarmentKind.Skirt -> {
            b.fill(
                "M34 24 L66 24 L72 82 L28 82 Z",
                tint,
            )
            b.stroke("M34 24 L66 24", Color.Black.copy(alpha = 0.25f), width = 1.4f)
        }
    }
    return b.build()
}

private fun circlePath(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0 Z"

private fun ImageVector.Builder.fill(path: String, color: Color): ImageVector.Builder = addPath(
    pathData = PathParser().parsePathString(path).toNodes(),
    fill = SolidColor(color),
)

private fun ImageVector.Builder.stroke(path: String, color: Color, width: Float = 1.4f): ImageVector.Builder = addPath(
    pathData = PathParser().parsePathString(path).toNodes(),
    stroke = SolidColor(color),
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
)
