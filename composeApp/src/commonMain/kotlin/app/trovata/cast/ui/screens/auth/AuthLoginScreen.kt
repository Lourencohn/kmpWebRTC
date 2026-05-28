package app.trovata.cast.ui.screens.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.feature.auth.LoginUiState
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Wordmark
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun AuthLoginScreen(
    state: LoginUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 56.dp)) {
            AuthTopBar(onBack = onBack)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
                Text(
                    text = "ENTRAR",
                    color = colors.brand2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.em,
                )
                Text(
                    text = "Acesse sua conta",
                    color = colors.ink,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.03).em,
                    lineHeight = 33.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "Use o e-mail e a senha cadastrados na Trovata.",
                    color = colors.ink3,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                AuthField(
                    label = "E-MAIL",
                    value = state.email,
                    onValueChange = onEmailChange,
                    placeholder = "voce@empresa.com.br",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )

                AuthField(
                    label = "SENHA",
                    value = state.password,
                    onValueChange = onPasswordChange,
                    placeholder = "Sua senha",
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    isPassword = true,
                    onImeAction = onSubmit,
                    modifier = Modifier.padding(top = 16.dp),
                )

                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = Color(0xFFC0392B),
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                    )
                }

                Btn(
                    text = if (state.isSubmitting) "Entrando…" else "Entrar",
                    onClick = { if (state.canSubmit) onSubmit() },
                    kind = BtnKind.Primary,
                    size = BtnSize.Lg,
                    icon = TrovataIcons.arrowRight,
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                )
            }

            Box(modifier = Modifier.weight(1f))
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
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    onImeAction: () -> Unit = {},
) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(14.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = colors.ink4,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.em,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, shape)
                .border(1.5.dp, colors.line, shape)
                .height(54.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(text = placeholder, color = colors.ink5, fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(colors.brand),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(onDone = { onImeAction() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
