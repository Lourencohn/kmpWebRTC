package app.trovata.cast.ui.screens.prep

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.AppContainerHolder
import app.trovata.cast.feature.catalog.CatalogFilter
import app.trovata.cast.feature.catalog.CatalogPickerScreenModel
import app.trovata.cast.feature.catalog.CatalogPickerUiState
import app.trovata.cast.feature.catalog.ClientDraft
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.ProductCard
import app.trovata.cast.ui.components.ProductCardSize
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.screens.invite.InviteScreen
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class CatalogPickerScreen(
    val clientName: String? = null,
    val clientShop: String? = null,
    val scheduledFor: String? = null,
) : Screen {

    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        val initial = remember(clientName, clientShop, scheduledFor) {
            ClientDraft(clientName, clientShop, scheduledFor)
        }
        val screenModel = rememberScreenModel {
            CatalogPickerScreenModel(
                createSession = container.sessionsApi::createSession,
                persistSession = container.sessionsRepository::persistCreated,
                initialClient = initial,
                loadProducts = { container.catalogRepository.uiProducts() },
            )
        }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(state.createdSession?.sessionId) {
            val record = state.createdSession ?: return@LaunchedEffect
            screenModel.consumeCreatedSession()
            navigator.push(InviteScreen(record))
        }

        CatalogPickerBody(
            state = state,
            onBack = { navigator.pop() },
            onToggle = screenModel::toggle,
            onFilter = screenModel::setFilter,
            onGenerate = { screenModel.generateLink() },
            onDismissError = screenModel::clearError,
        )
    }
}

@Composable
private fun CatalogPickerBody(
    state: CatalogPickerUiState,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onFilter: (CatalogFilter) -> Unit,
    onGenerate: () -> Unit,
    onDismissError: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val visible = state.visibleProducts
    val selectedCount = state.selectedSkus.size

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn(icon = TrovataIcons.back, onClick = onBack, kind = IconBtnKind.Line)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Montar catálogo",
                        color = colors.ink4,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = state.client.name?.let { "Para ${it.split(' ').first()}" } ?: "Nova sessão",
                        color = colors.ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Pill(
                    text = "$selectedCount sel.",
                    tone = if (selectedCount > 0) PillTone.Brand else PillTone.Neutral,
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(CatalogFilter.entries.toList()) { filter ->
                    FilterChip(
                        label = filter.label,
                        active = state.filter == filter,
                        onClick = { onFilter(filter) },
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visible) { product ->
                    val selected = product.ref in state.selectedSkus
                    ProductCard(
                        product = product,
                        size = ProductCardSize.Sm,
                        highlight = selected,
                        inCart = if (selected) 1 else 0,
                        onClick = { onToggle(product.ref) },
                    )
                }
            }
        }

        BottomGenerateBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedCount = selectedCount,
            skuCount = state.skuCount,
            submitting = state.isSubmitting,
            onGenerate = onGenerate,
        )

        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp),
            ) {
                ErrorBanner(text = error, onDismiss = onDismissError)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(999.dp)
    val bg = if (active) colors.ink else colors.surface
    val fg = if (active) Color.White else colors.ink2
    val borderColor = if (active) colors.ink else colors.line
    Box(
        modifier = Modifier
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.005).em,
        )
    }
}

@Composable
private fun BottomGenerateBar(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    skuCount: Int,
    submitting: Boolean,
    onGenerate: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val canGenerate = selectedCount > 0 && !submitting
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.96f))
            .border(1.dp, colors.line, RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selectedCount == 0) "Escolha produtos" else "$selectedCount produtos · $skuCount SKUs",
                    color = colors.ink4,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (submitting) "Gerando link..." else "Pronto para convidar",
                    color = colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Btn(
                text = if (submitting) "Gerando..." else "Gerar link",
                onClick = onGenerate,
                kind = if (canGenerate) BtnKind.Primary else BtnKind.Soft,
                size = BtnSize.Md,
                icon = TrovataIcons.share,
                enabled = canGenerate,
            )
        }
    }
}

@Composable
private fun ErrorBanner(text: String, onDismiss: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.live, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.bell,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "OK",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Spacer(Modifier.height(0.dp))
    }
}
