package app.trovata.cast.ui.screens.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.color.oklch
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Wordmark

@Composable
fun AuthWelcomeScreen(
    modifier: Modifier = Modifier,
    onEnterWithWhatsapp: () -> Unit = {},
    onExistingAccount: () -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.brand.copy(alpha = 0.08f), Color.Transparent),
                        radius = 480f,
                        center = Offset(x = 0f, y = 200f),
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxSize().padding(top = 56.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Wordmark(height = 22.dp)
                    Text(
                        text = "TrovataCast",
                        color = colors.ink3,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.04.em,
                    )
                }
                Text(
                    text = "v1.2.0",
                    color = colors.ink4,
                    style = TrovataTokens.type.mono.copy(fontSize = 11.sp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
            ) {
                Text(
                    text = "PARA REPRESENTANTES B2B",
                    color = colors.ink3,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.18.em,
                )

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "A venda",
                        color = colors.ink,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.035).em,
                        lineHeight = 40.sp,
                    )
                    Text(
                        text = "acontece",
                        color = colors.ink,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.035).em,
                        lineHeight = 40.sp,
                    )
                    Box {
                        Text(
                            text = "juntos.",
                            color = colors.brand,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.035).em,
                            lineHeight = 40.sp,
                        )
                        ScribbleUnderline(
                            color = colors.jade,
                            modifier = Modifier
                                .padding(top = 36.dp)
                                .size(width = 130.dp, height = 14.dp),
                        )
                    }
                }

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.ink2)) {
                            append("Você e seu cliente vendo o mesmo catálogo, ao vivo, em telas diferentes.\n")
                        }
                        withStyle(SpanStyle(color = colors.ink3)) {
                            append("Ele não precisa instalar nada.")
                        }
                    },
                    fontSize = 14.5.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 22.dp),
                )

                Spacer(modifier = Modifier.height(36.dp))
                CoPresenceVisual()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Vendedor", color = colors.ink4, fontSize = 10.5.sp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "App", color = colors.ink5, fontSize = 10.5.sp)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(text = "↔", color = colors.ink5, fontSize = 11.sp)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(text = "Navegador", color = colors.ink5, fontSize = 10.5.sp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Cliente", color = colors.ink4, fontSize = 10.5.sp)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Btn(
                    text = "Entrar pelo WhatsApp",
                    onClick = onEnterWithWhatsapp,
                    kind = BtnKind.Primary,
                    size = BtnSize.Lg,
                    modifier = Modifier.fillMaxWidth(),
                )
                Btn(
                    text = "Já tenho conta — entrar",
                    onClick = onExistingAccount,
                    kind = BtnKind.Ghost,
                    size = BtnSize.Md,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.ink4)) { append("Ao continuar você aceita os ") }
                        withStyle(SpanStyle(color = colors.ink2, fontWeight = FontWeight.SemiBold)) { append("termos") }
                        withStyle(SpanStyle(color = colors.ink4)) { append(" e a ") }
                        withStyle(SpanStyle(color = colors.ink2, fontWeight = FontWeight.SemiBold)) {
                            append("política de privacidade")
                        }
                        withStyle(SpanStyle(color = colors.ink4)) { append(".") }
                    },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CoPresenceVisual() {
    val colors = TrovataTokens.colors
    val transition = rememberInfiniteTransition(label = "copresence")
    val pulse1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2600), repeatMode = RepeatMode.Restart),
        label = "pulse1",
    )
    val pulse2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2600), repeatMode = RepeatMode.Restart, initialStartOffset = androidx.compose.animation.core.StartOffset(600)),
        label = "pulse2",
    )

    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FaceTile(hue = 28.0, brand = colors.brand, pulse = pulse1)
        Box(modifier = Modifier.weight(1f).height(2.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f.dp.toPx(), 4f.dp.toPx()))
                drawLine(
                    color = Color(0xFFB6BCC4),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2f,
                    pathEffect = pathEffect,
                    cap = StrokeCap.Round,
                )
            }
            Box(
                modifier = Modifier
                    .background(colors.bg)
                    .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "AO VIVO",
                    color = colors.ink3,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.14.em,
                )
            }
        }
        FaceTile(hue = 280.0, brand = colors.jade, pulse = pulse2)
    }
}

@Composable
private fun FaceTile(hue: Double, brand: Color, pulse: Float, size: Dp = 60.dp) {
    val colors = TrovataTokens.colors
    Box(contentAlignment = Alignment.Center) {
        val ringScale = 1f + pulse * 1.6f
        val ringAlpha = (1f - pulse) * 0.5f
        Box(
            modifier = Modifier
                .size(size + 12.dp)
                .border(2.dp, brand.copy(alpha = ringAlpha), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            oklch(85.0, 0.10, hue),
                            oklch(55.0, 0.13, hue),
                            oklch(28.0, 0.10, hue),
                        ),
                    ),
                    shape = CircleShape,
                )
                .shadow(4.dp, CircleShape, clip = false)
                .border(2.dp, colors.surface, CircleShape),
        )
    }
}

@Composable
private fun ScribbleUnderline(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(2f, h * 0.6f)
            quadraticTo(w * 0.25f, h * -0.1f, w * 0.5f, h * 0.4f)
            quadraticTo(w * 0.75f, h * 0.9f, w - 2f, h * 0.3f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
