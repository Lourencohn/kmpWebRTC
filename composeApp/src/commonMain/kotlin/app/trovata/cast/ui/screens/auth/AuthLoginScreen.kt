package app.trovata.cast.ui.screens.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Wordmark
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun AuthLoginScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {},
    onUseEmail: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 56.dp)) {
            AuthTopBar(onBack = onBack)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
                Text(
                    text = "PASSO 1 DE 2",
                    color = colors.brand2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.em,
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Entrar com ",
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
                    text = "Vamos te chamar uma vez nesse número para confirmar quem é você. Não usamos o WhatsApp para mais nada.",
                    color = colors.ink3,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text(
                    text = "SEU NÚMERO",
                    color = colors.ink4,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.08.em,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                PhoneInput()

                WhatsAppCallout(modifier = Modifier.padding(top = 14.dp))

                Btn(
                    text = "Continuar",
                    onClick = onContinue,
                    kind = BtnKind.Primary,
                    size = BtnSize.Lg,
                    icon = TrovataIcons.arrowRight,
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.line))
                    Text(
                        text = "OU",
                        color = colors.ink4,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.12.em,
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.line))
                }

                Btn(
                    text = "Entrar com email",
                    onClick = onUseEmail,
                    kind = BtnKind.Surface,
                    size = BtnSize.Md,
                    icon = TrovataIcons.msg,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            }

            Box(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Não tem conta? ", color = colors.ink4, fontSize = 11.5.sp)
                Text(
                    text = "Criar agora",
                    color = colors.brand,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onCreateAccount),
                )
                Text(text = " — leva 2 minutos.", color = colors.ink4, fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
internal fun AuthTopBar(onBack: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = TrovataIcons.back,
                contentDescription = "Voltar",
                tint = colors.ink,
                modifier = Modifier.size(22.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Wordmark(height = 18.dp)
            Text(
                text = "TrovataCast",
                color = colors.ink4,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.04.em,
            )
        }
        Box(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun PhoneInput() {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(14.dp)
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "cursor-alpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(2.dp, colors.brand, shape)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .height(54.dp)
                .border(0.dp, Color.Transparent)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrazilFlag()
            Text(
                text = "+55",
                color = colors.ink,
                style = TrovataTokens.type.mono.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            )
            Icon(
                imageVector = TrovataIcons.chevDown,
                contentDescription = null,
                tint = colors.ink4,
                modifier = Modifier.size(14.dp),
            )
        }
        Box(modifier = Modifier.width(1.dp).height(54.dp).background(colors.line))
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "(31) 99876-",
                color = colors.ink,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.04.em,
                ),
            )
            Text(
                text = "____",
                color = colors.ink5,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.04.em,
                ),
            )
            Box(
                modifier = Modifier
                    .alpha(cursorAlpha)
                    .width(2.dp)
                    .height(20.dp)
                    .background(colors.brand),
            )
        }
    }
}

@Composable
private fun BrazilFlag() {
    Box(
        modifier = Modifier
            .size(width = 22.dp, height = 16.dp)
            .background(Color(0xFF009C3B), RoundedCornerShape(3.dp))
            .border(0.dp, Color.Transparent, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = 9.dp)
                .background(Color(0xFFFFDF00)),
        )
        Box(
            modifier = Modifier
                .size(width = 6.dp, height = 6.dp)
                .background(Color(0xFF002776), RoundedCornerShape(999.dp)),
        )
    }
}

@Composable
private fun WhatsAppCallout(modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.jadeTint, shape)
            .border(1.dp, colors.jadeTint, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(colors.jade, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.msg,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = "Vamos enviar um código de 6 dígitos no WhatsApp. Em segundos você está dentro.",
            color = colors.jade2,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
        )
    }
}
