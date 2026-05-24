package app.trovata.cast.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.AuthBrand
import app.trovata.cast.data.sample.SampleAuth
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Wordmark
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun AuthSetupScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onFinish: () -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(24.dp).background(Color.Transparent), contentAlignment = Alignment.Center) {
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
                    Text(text = "3 / 3", color = colors.ink4, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "QUASE LÁ, CAMILA",
                        color = colors.jade2,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.12.em,
                    )
                    Text(
                        text = "Qual marca você\nrepresenta?",
                        color = colors.ink,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.03).em,
                        lineHeight = 31.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = "Você pode representar mais de uma. A escolhida agora será a padrão.",
                        color = colors.ink3,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SampleAuth.brands.forEach { brand ->
                        BrandCard(brand = brand)
                    }
                    OtherBrandCard()
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "TABELA PADRÃO",
                        color = colors.ink4,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.em,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    TierSelector()
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.bg.copy(alpha = 0.94f))
                .border(width = 1.dp, color = colors.line, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) {
            Btn(
                text = "Entrar no TrovataCast",
                onClick = onFinish,
                kind = BtnKind.Primary,
                size = BtnSize.Lg,
                icon = TrovataIcons.arrowRight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BrandCard(brand: AuthBrand) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (brand.selected) colors.brand else colors.line
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (brand.selected) 4.dp else 1.dp, shape, clip = false)
            .background(colors.surface, shape)
            .border(2.dp, borderColor, shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = Brush.linearGradient(brand.colors),
                    shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = brand.name.first().toString(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).em,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = brand.name,
                color = colors.ink,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).em,
            )
            Text(
                text = brand.tag,
                color = colors.ink3,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${brand.skuCount} SKUs",
                    color = colors.ink4,
                    style = TrovataTokens.type.mono.copy(fontSize = 10.5.sp),
                )
                Box(modifier = Modifier.size(2.dp).background(colors.ink5, RoundedCornerShape(50)))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    brand.colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(c, CircleShape)
                                .border(1.5.dp, colors.surface, CircleShape),
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (brand.selected) colors.brand else colors.surface,
                    shape = CircleShape,
                )
                .border(
                    width = if (brand.selected) 0.dp else 1.5.dp,
                    color = if (brand.selected) Color.Transparent else colors.lineStrong,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (brand.selected) {
                Icon(
                    imageVector = TrovataIcons.check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun OtherBrandCard() {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent, shape)
            .border(1.5.dp, colors.lineStrong, shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(colors.surface2, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.plus,
                contentDescription = null,
                tint = colors.ink3,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Outra marca", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Cadastre sua própria — leva 3 minutos",
                color = colors.ink3,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = TrovataIcons.chev,
            contentDescription = null,
            tint = colors.ink4,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TierSelector() {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SampleAuth.PriceTier.entries.forEach { tier ->
            val active = tier == SampleAuth.selectedTier
            Column(
                modifier = Modifier
                    .weight(1f)
                    .shadow(if (active) 0.dp else 1.dp, RoundedCornerShape(12.dp), clip = false)
                    .background(if (active) colors.ink else colors.surface, RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (active) colors.ink else colors.line,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = tier.label,
                    color = if (active) Color.White else colors.ink2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = tier.detail,
                    color = if (active) Color.White.copy(alpha = 0.6f) else colors.ink4,
                    style = TrovataTokens.type.mono.copy(fontSize = 10.sp),
                )
            }
        }
    }
}
