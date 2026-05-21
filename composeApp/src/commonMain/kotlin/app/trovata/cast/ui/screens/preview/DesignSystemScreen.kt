package app.trovata.cast.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.SampleCatalog
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.color.HueRoles
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Garment
import app.trovata.cast.ui.components.GarmentKind
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.ProductCard
import app.trovata.cast.ui.components.ProductCardSize
import app.trovata.cast.ui.components.ProductRow
import app.trovata.cast.ui.components.RemotePointer
import app.trovata.cast.ui.components.ScreenHeader
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.SellerTab
import app.trovata.cast.ui.components.TabBar
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.components.VideoTile
import app.trovata.cast.ui.components.Wordmark
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun DesignSystemScreen(modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    var activeTab by remember { mutableStateOf(SellerTab.Sessoes) }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Wordmark() }

            item {
                ScreenHeader(
                    title = "Design system",
                    subtitle = "Componentes da fase M1, lado a lado com o protótipo.",
                    eyebrow = { Pill(text = "M1 · v0.1", tone = PillTone.Brand, icon = TrovataIcons.sparkle) },
                    trailing = { IconBtn(icon = TrovataIcons.more, onClick = {}, kind = IconBtnKind.Line) },
                )
            }

            item { Section(title = "Pills") { PillsRow() } }

            item { Section(title = "Botões — Primários") { ButtonsPrimaryRow() } }
            item { Section(title = "Botões — Suporte") { ButtonsSupportRow() } }
            item { Section(title = "Tamanhos") { ButtonsSizesRow() } }

            item { Section(title = "Ícones de toolbar") { IconBtnsRow() } }

            item { Section(title = "Avatares") { AvatarsRow() } }

            item { Section(title = "Cartão base") { CardSample() } }

            item { Section(title = "Vestuário (silhuetas)") { GarmentsGrid() } }

            item { Section(title = "ProductCard · tamanhos") { ProductCardsSizesRow() } }
            item { Section(title = "ProductCard · estados") { ProductCardsStatesRow() } }

            item { Section(title = "ProductRow") { ProductRowsList() } }

            item { Section(title = "VideoTile") { VideoTilesRow() } }

            item { Section(title = "RemotePointer (overlay)") { PointerSample() } }

            item {
                Section(title = "Tab bar") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TabBar(active = activeTab, onSelect = { activeTab = it })
                        TabBar(active = activeTab, onSelect = { activeTab = it }, dark = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = title)
        content()
    }
}

@Composable
private fun PillsRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Pill(text = "Neutral", tone = PillTone.Neutral)
        Pill(text = "Brand", tone = PillTone.Brand, icon = TrovataIcons.sparkle)
        Pill(text = "Jade", tone = PillTone.Jade, icon = TrovataIcons.check)
        Pill(text = "Ao vivo", tone = PillTone.Live, icon = TrovataIcons.signal)
        Pill(text = "Ghost", tone = PillTone.Ghost)
        Pill(text = "Dark", tone = PillTone.Dark, icon = TrovataIcons.lock)
    }
}

@Composable
private fun ButtonsPrimaryRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Btn(text = "Atender", onClick = {}, kind = BtnKind.Primary, icon = TrovataIcons.video)
        Btn(text = "Confirmar pedido", onClick = {}, kind = BtnKind.Jade, icon = TrovataIcons.check)
        Btn(text = "Convidar", onClick = {}, kind = BtnKind.Soft, icon = TrovataIcons.send)
    }
}

@Composable
private fun ButtonsSupportRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Btn(text = "Cancelar", onClick = {}, kind = BtnKind.Ghost)
        Btn(text = "Compartilhar", onClick = {}, kind = BtnKind.Surface, icon = TrovataIcons.share)
        Btn(text = "Encerrar", onClick = {}, kind = BtnKind.Dark, icon = TrovataIcons.hangup)
        Btn(text = "Recusar", onClick = {}, kind = BtnKind.Danger, icon = TrovataIcons.hangup)
    }
}

@Composable
private fun ButtonsSizesRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Btn(text = "Pequeno", onClick = {}, size = BtnSize.Sm)
        Btn(text = "Médio", onClick = {}, size = BtnSize.Md)
        Btn(text = "Grande", onClick = {}, size = BtnSize.Lg)
    }
}

@Composable
private fun IconBtnsRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBtn(icon = TrovataIcons.mic, onClick = {}, kind = IconBtnKind.Soft)
        IconBtn(icon = TrovataIcons.video, onClick = {}, kind = IconBtnKind.Soft, active = true)
        IconBtn(icon = TrovataIcons.hangup, onClick = {}, kind = IconBtnKind.Danger)
        IconBtn(icon = TrovataIcons.check, onClick = {}, kind = IconBtnKind.Jade)
        IconBtn(icon = TrovataIcons.pointer, onClick = {}, kind = IconBtnKind.Brand)
        IconBtn(icon = TrovataIcons.more, onClick = {}, kind = IconBtnKind.Dark)
        IconBtn(icon = TrovataIcons.bell, onClick = {}, kind = IconBtnKind.Line)
    }
}

@Composable
private fun AvatarsRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(name = "Camila Rocha", hue = HueRoles.SELLER)
        Avatar(name = "Diego Almeida", hue = HueRoles.BUYER)
        Avatar(name = "Ateliê Norte", hue = 145.0, size = 44.dp)
        Avatar(name = "Helena Vargas", hue = 320.0, size = 52.dp)
    }
}

@Composable
private fun CardSample() {
    TrovataCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Próxima chamada",
                color = TrovataTokens.colors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Camila — Atelier Norte · daqui a 8min",
                color = TrovataTokens.colors.ink3,
                fontSize = 13.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(text = "Confirmada", tone = PillTone.Jade, icon = TrovataIcons.check)
                Pill(text = "Coleção Outono", tone = PillTone.Brand, icon = TrovataIcons.layers)
            }
        }
    }
}

@Composable
private fun GarmentsGrid() {
    val tints = listOf(
        0xFFEEEAE0 to 0xFF7C6E58,
        0xFFE6EAE5 to 0xFF5C6C5F,
        0xFFE8E4DC to 0xFF534234,
        0xFFDDE4E6 to 0xFF3A5260,
        0xFFE8DEDA to 0xFF8B4C44,
        0xFFE0DCD2 to 0xFF3D3833,
        0xFFF0E7D6 to 0xFF7A5A2C,
        0xFFD8DDD2 to 0xFF3C4530,
        0xFFEEEAE0 to 0xFF7C6E58,
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(320.dp),
    ) {
        itemsIndexed(GarmentKind.entries) { index, kind ->
            val (bg, fg) = tints[index % tints.size]
            Box(
                modifier = Modifier
                    .height(96.dp)
                    .background(androidx.compose.ui.graphics.Color(bg), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Garment(kind = kind, tint = androidx.compose.ui.graphics.Color(fg), size = 72.dp)
            }
        }
    }
}

@Composable
private fun ProductCardsSizesRow() {
    val sample = SampleCatalog.products
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.width(120.dp)) {
            ProductCard(product = sample[0], size = ProductCardSize.Sm)
        }
        Box(modifier = Modifier.width(150.dp)) {
            ProductCard(product = sample[1], size = ProductCardSize.Md)
        }
        Box(modifier = Modifier.width(190.dp)) {
            ProductCard(product = sample[2], size = ProductCardSize.Lg)
        }
    }
}

@Composable
private fun ProductCardsStatesRow() {
    val sample = SampleCatalog.products
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.width(150.dp)) {
            ProductCard(product = sample[3])
        }
        Box(modifier = Modifier.width(150.dp)) {
            ProductCard(product = sample[5], highlight = true)
        }
        Box(modifier = Modifier.width(150.dp)) {
            ProductCard(product = sample[4], pointed = true)
        }
        Box(modifier = Modifier.width(150.dp)) {
            ProductCard(product = sample[6], inCart = 12)
        }
    }
}

@Composable
private fun ProductRowsList() {
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
        Column {
            ProductRow(product = SampleCatalog.products[0], quantity = 12)
            ProductRow(product = SampleCatalog.products[2], quantity = 6)
            ProductRow(product = SampleCatalog.products[5], quantity = 3)
        }
    }
}

@Composable
private fun VideoTilesRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        VideoTile(name = "Camila", hue = HueRoles.SELLER, width = 84.dp)
        VideoTile(name = "Diego", hue = HueRoles.BUYER, width = 84.dp, muted = true)
        VideoTile(name = "Mini", hue = 145.0, width = 64.dp, mini = true)
    }
}

@Composable
private fun PointerSample() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(TrovataTokens.colors.surface2, RoundedCornerShape(14.dp)),
    ) {
        RemotePointer(name = "Camila", hue = HueRoles.SELLER, x = 28.dp, y = 22.dp)
        RemotePointer(name = "Diego", hue = HueRoles.BUYER, x = 180.dp, y = 76.dp)
    }
}

