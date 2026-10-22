package app.trovata.cast.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.sfa.SellerCatalogLink
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.CenteredMessage
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SelectableChip
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
fun CatalogLinksTab(
    screenModel: CatalogLinkPickerScreenModel,
    accountName: String,
    onOpenAccount: () -> Unit,
    onSessionCreated: (StoredSessionRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by screenModel.state.collectAsState()

    LaunchedEffect(state.createdSession?.sessionId) {
        val record = state.createdSession ?: return@LaunchedEffect
        screenModel.consumeCreatedSession()
        onSessionCreated(record)
    }

    CatalogLinksBody(
        modifier = modifier,
        state = state,
        header = {
            TabHeader(
                modifier = Modifier.padding(top = 56.dp),
                eyebrow = state.companyName.ifBlank { "Catálogo Link" },
                title = "Catálogos",
                subtitle = tabSubtitle(state),
                accountName = accountName,
                onOpenAccount = onOpenAccount,
                secondaryIcon = TrovataIcons.refresh,
                onSecondaryClick = screenModel::refresh,
            )
        },
        bottomBar = { barModifier ->
            GenerateLinkBar(
                state = state,
                onGenerate = screenModel::generateLink,
                bottomInset = 0.dp,
                modifier = barModifier,
            )
        },
        onSelect = screenModel::select,
        onFilter = screenModel::setFilter,
        onSearchChange = screenModel::setSearch,
        onSearchSubmit = screenModel::submitSearch,
        onRetry = screenModel::refresh,
        onDismissError = screenModel::clearError,
    )
}

private fun tabSubtitle(state: CatalogLinkPickerUiState): String {
    if (state.isLoading) return "Carregando seus catálogos..."
    val available = state.links.count { it.isAvailable }
    return "${state.links.size} catálogos · $available disponíveis para televenda"
}

@Composable
fun CatalogLinksBody(
    state: CatalogLinkPickerUiState,
    header: @Composable () -> Unit,
    bottomBar: @Composable (Modifier) -> Unit,
    onSelect: (SellerCatalogLink) -> Unit,
    onFilter: (CatalogLinkFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val visible = state.visibleLinks

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            header()

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
                    SelectableChip(
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
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 140.dp,
                    ),
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

        bottomBar(Modifier.align(Alignment.BottomCenter))

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
                text = link.name,
                color = if (link.isAvailable) colors.ink else colors.ink4,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            when {
                link.expired -> Pill(text = "Expirado", tone = PillTone.Neutral)
                !link.active -> Pill(text = "Inativo", tone = PillTone.Neutral)
                selected -> Pill(text = "Selecionado", tone = PillTone.Brand)
                else -> Unit
            }
        }
        Text(
            text = link.recipientLabel?.let { "Para $it" } ?: "Sem destinatário definido",
            color = if (link.hasRecipient) colors.ink3 else colors.ink4,
            fontSize = 12.sp,
        )
        Text(
            text = listOfNotNull(
                link.validityLabel?.let { "Vale até $it" },
                "${link.cartCount} carrinhos",
                "${link.viewCount} visitas",
            ).joinToString(" · "),
            color = colors.ink4,
            fontSize = 11.5.sp,
        )
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
            text = if (filter == CatalogLinkFilter.Available && unavailableCount > 0) {
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
            text = if (filter == CatalogLinkFilter.Available && unavailableCount > 0) {
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
fun CatalogBottomBar(
    modifier: Modifier = Modifier,
    bottomInset: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TrovataTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.96f))
            .border(1.dp, colors.line, RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun GenerateLinkBar(
    state: CatalogLinkPickerUiState,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 18.dp,
) {
    val colors = TrovataTokens.colors
    val selected = state.selected
    val submitting = state.isSubmitting
    val enabled = state.canGenerate
    CatalogBottomBar(modifier = modifier, bottomInset = bottomInset) {
        state.recipientWarning?.let { texto ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.warn.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = TrovataIcons.bell,
                    contentDescription = null,
                    tint = colors.warn,
                    modifier = Modifier.size(14.dp),
                )
                Text(text = texto, color = colors.ink2, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selected?.name ?: "Escolha um catálogo",
                    color = colors.ink4,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = when {
                        submitting -> "Gerando link..."
                        selected == null -> "Pronto para convidar"
                        else -> state.callRecipient?.let { "Convite para $it" } ?: "Catálogo sem destinatário"
                    },
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
