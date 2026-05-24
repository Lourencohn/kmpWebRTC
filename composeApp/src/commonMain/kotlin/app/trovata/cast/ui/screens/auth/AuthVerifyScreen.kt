package app.trovata.cast.ui.screens.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.SampleAuth
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun AuthVerifyScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {},
    onChangeNumber: () -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 56.dp)) {
            AuthTopBar(onBack = onBack)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
                Text(
                    text = "PASSO 2 DE 2",
                    color = colors.brand2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.em,
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Código no ",
                        color = colors.ink,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.03).em,
                        lineHeight = 33.sp,
                    )
                    Text(
                        text = "WhatsApp",
                        color = colors.jade,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.03).em,
                        lineHeight = 33.sp,
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.ink3)) {
                            append("Enviamos um código para ")
                        }
                        withStyle(
                            SpanStyle(
                                color = colors.ink2,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = TrovataTokens.type.mono.fontFamily,
                            ),
                        ) {
                            append(SampleAuth.phoneVisible)
                        }
                        withStyle(SpanStyle(color = colors.ink3)) { append(".") }
                    },
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            CodeBoxes()

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.ink3)) { append("Não recebeu? ") }
                    withStyle(SpanStyle(color = colors.ink4)) { append("Reenviar em ") }
                    withStyle(
                        SpanStyle(
                            color = colors.ink2,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = TrovataTokens.type.mono.fontFamily,
                        ),
                    ) {
                        append(SampleAuth.resendCountdown)
                    }
                },
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Btn(
                    text = "Continuar",
                    onClick = onContinue,
                    kind = BtnKind.Primary,
                    size = BtnSize.Lg,
                    icon = TrovataIcons.arrowRight,
                    modifier = Modifier.fillMaxWidth(),
                )
                Btn(
                    text = "Mudar número",
                    onClick = onChangeNumber,
                    kind = BtnKind.Ghost,
                    size = BtnSize.Md,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }

            Box(modifier = Modifier.weight(1f))

            PrivacyCallout(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp))
        }
    }
}

@Composable
private fun CodeBoxes() {
    val colors = TrovataTokens.colors
    val transition = rememberInfiniteTransition(label = "cursor-otp")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "cursor-otp-alpha",
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SampleAuth.codeDigits.forEachIndexed { index, digit ->
            val filled = digit.isNotEmpty()
            val active = index == SampleAuth.activeDigitIndex
            val borderColor = when {
                active -> colors.brand
                filled -> colors.lineStrong
                else -> colors.line
            }
            val shape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shadow(
                        elevation = if (active) 4.dp else if (filled) 1.dp else 0.dp,
                        shape = shape,
                        clip = false,
                    )
                    .background(
                        if (filled) colors.surface else colors.surface2,
                        shape,
                    )
                    .border(2.dp, borderColor, shape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = digit,
                    color = if (filled) colors.ink else colors.ink5,
                    style = TrovataTokens.type.mono.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).em,
                    ),
                )
                if (active) {
                    Box(
                        modifier = Modifier
                            .alpha(cursorAlpha)
                            .size(width = 2.dp, height = 28.dp)
                            .background(colors.brand),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyCallout(modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(colors.surface2, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.lock,
                contentDescription = null,
                tint = colors.ink3,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = "O código é privado. Nunca peça para outra pessoa.",
            color = colors.ink3,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
        )
    }
}
