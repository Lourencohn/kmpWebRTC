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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.feature.catalog.CatalogLinkFilter
import app.trovata.cast.feature.catalog.CatalogLinkPickerScreenModel
import app.trovata.cast.feature.catalog.CatalogLinkPickerUiState
import app.trovata.cast.feature.catalog.ClientDraft
import app.trovata.cast.feature.catalog.SellerCatalogLink
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.screens.invite.InviteScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

data class CatalogLinkPickerScreen(
    val clientName: String? = null,
    val clientShop: String? = null,
    val scheduledFor: String? = null,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = koinInject<AuthRepository>()
        val initial = remember(clientName, clientShop, scheduledFor) {
            ClientDraft(clientName, clientShop, scheduledFor)
        }
        val screenModel = koinScreenModel<CatalogLinkPickerScreenModel> { parametersOf(initial) }
        val state by screenModel.state.collectAsState()
        val user by authRepository.user.collectAsState()
        val company by authRepository.activeCompany.collectAsState()

        LaunchedEffect(state.createdSession?.sessionId) {
            val record = state.createdSession ?: return@LaunchedEffect
            screenModel.consumeCreatedSession()
            navigator.push(InviteScreen(record))
        }

        CatalogLinkPickerBody(
            state = state,
            onBack = { navigator.pop() },
            onSelect = screenModel::select,
            onFilter = screenModel::setFilter,
            onSearchChange = screenModel::setSearch,
            onSearchSubmit = screenModel::submitSearch,
            onRetry = screenModel::refresh,
            onGenerate = {
                screenModel.generateLink(
                    sellerId = (user?.id ?: company?.id)?.toString() ?: "seller",
                    sellerName = user?.name?.takeIf { it.isNotBlank() } ?: company?.name ?: "Vendedor",
                )
            },
            onDismissError = screenModel::clearError,
        )
    }
}

@Composable
private fun CatalogLinkPickerBody(
    state: CatalogLinkPickerUiState,
    onBack: () -> Unit,
    onSelect: (SellerCatalogLink) -> Unit,
    onFilter: (CatalogLinkFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onRetry: () -> Unit,
    onGenerate: () -> Unit,
    onDismissError: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val visible = state.visibleLinks

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
                        text = state.client.name?.let { "Para ${it.split(' ').first()}" }
                            ?: "Escolher catálogo",
                        color = colors.ink4,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = listOfNotNull(
                            state.companyName.takeIf { it.isNotBlank() },
                            state.companySlug.takeIf { it.isNotBlank() },
                        ).joinToString(" · ").ifBlank { "Nova sessão" },
                        color = colors.ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Pill(
                    text = "${visible.size}",
                    tone = if (state.selectedUuid != null) PillTone.Brand else PillTone.Neutral,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = state.search,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.ink, fontSize = 13.sp),
                    cursorBrush = SolidColor(colors.brand),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.surface, RoundedCornerShape(999.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (state.search.isEmpty()) {
                            Text(
                                text = "Buscar catálogo por nome ou cliente",
                                color = colors.ink4,
                                fontSize = 13.sp,
                            )
                        }
                        inner()
                    },
                )
                Btn(text = "Buscar", onClick = onSearchSubmit, kind = BtnKind.Surface, size = BtnSize.Sm)
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(CatalogLinkFilter.entries.toList()) { filter ->
                    LinkFilterChip(
                        label = filter.label,
                        active = state.filter == filter,
                        onClick = { onFilter(filter) },
                    )
                }
            }

            when {
                state.isLoading -> CenteredMessage("Buscando seus catálogos...")
                visible.isEmpty() && state.error == null -> EmptyLinks(
                    unavailableCount = state.unavailableCount,
                    filter = state.filter,
                    onRetry = onRetry,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visible, key = { it.uuid }) { link ->
                        CatalogLinkRow(
                            link = link,
                            selected = link.uuid == state.selectedUuid,
                            onClick = { onSelect(link) },
                        )
                    }
                    if (state.hasMore) {
                        item {
                            Text(
                                text = "Mostrando os ${state.links.size} mais recentes de ${state.total}. Use a busca para achar outro.",
                                color = colors.ink4,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }

        BottomGenerateLinkBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selected = state.selected,
            submitting = state.isSubmitting,
            enabled = state.canGenerate,
            onGenerate = onGenerate,
        )

        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp),
            ) {
                LinkErrorBanner(text = error, onDismiss = onDismissError)
            }
        }
    }
}

@Composable
private fun CatalogLinkRow(link: SellerCatalogLink, selected: Boolean, onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) colors.brand else colors.line
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = link.nome,
                color = if (link.disponivel) colors.ink else colors.ink4,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            when {
                link.expirado -> Pill(text = "Expirado", tone = PillTone.Neutral)
                !link.ativo -> Pill(text = "Inativo", tone = PillTone.Neutral)
                selected -> Pill(text = "Selecionado", tone = PillTone.Brand)
                else -> Unit
            }
        }
        link.clienteNome?.takeIf { it != link.nome }?.let { cliente ->
            Text(text = cliente, color = colors.ink3, fontSize = 12.sp)
        }
        Text(
            text = listOfNotNull(
                link.validadeLabel?.let { "Vale até $it" },
                "${link.totalCarrinhos} carrinhos",
                "${link.totalVisualizacoes} visitas",
            ).joinToString(" · "),
            color = colors.ink4,
            fontSize = 11.5.sp,
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = colors.ink3, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmptyLinks(unavailableCount: Int, filter: CatalogLinkFilter, onRetry: () -> Unit) {
    val colors = TrovataTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (filter == CatalogLinkFilter.Disponiveis && unavailableCount > 0) {
                "Nenhum catálogo disponível agora"
            } else {
                "Você ainda não tem catálogos link"
            },
            color = colors.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (filter == CatalogLinkFilter.Disponiveis && unavailableCount > 0) {
                "$unavailableCount catálogo(s) estão expirados ou inativos. Veja em \"Todos\" ou renove a validade no Catálogo Link."
            } else {
                "Crie um catálogo link no sistema e ele aparece aqui."
            },
            color = colors.ink3,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Btn(text = "Atualizar", onClick = onRetry, kind = BtnKind.Surface, size = BtnSize.Sm)
    }
}

@Composable
private fun LinkFilterChip(label: String, active: Boolean, onClick: () -> Unit) {
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
private fun BottomGenerateLinkBar(
    modifier: Modifier = Modifier,
    selected: SellerCatalogLink?,
    submitting: Boolean,
    enabled: Boolean,
    onGenerate: () -> Unit,
) {
    val colors = TrovataTokens.colors
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
                    text = selected?.nome ?: "Escolha um catálogo",
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
                kind = if (enabled) BtnKind.Primary else BtnKind.Soft,
                size = BtnSize.Md,
                icon = TrovataIcons.share,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun LinkErrorBanner(text: String, onDismiss: () -> Unit) {
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
