package app.trovata.cast.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trovata.cast.theme.TrovataTokens
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.square
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

@Composable
fun QrCard(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
) {
    val colors = TrovataTokens.colors
    val painter = rememberQrCodePainter(data = data) {
        shapes {
            ball = QrBallShape.circle()
            darkPixel = QrPixelShape.roundCorners(.5f)
            frame = QrFrameShape.roundCorners(.2f)
            lightPixel = QrPixelShape.square()
        }
    }
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = "QR do link da sessão",
            modifier = Modifier.size(size),
        )
    }
}
