package app.trovata.cast.ui.screens.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.CatalogClient
import app.trovata.cast.feature.clients.ClientsUiState
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun ClientsScreen(
    state: ClientsUiState,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onInviteClient: (CatalogClient) -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TabHeader(
                    eyebrow = "Carteira · ${state.total} clientes",
                    title = "Clientes",
                    subtitle = "Base sincronizada",
                    onOpenAccount = onOpenAccount,
                    secondaryIcon = TrovataIcons.filter,
                )
            }

            item { SearchBox(value = state.query, onValueChange = onQueryChange) }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = if (state.query.isBlank()) "Todos os clientes" else "Resultados",
                        action = {
                            Text(
                                text = "${state.results.size}",
                                color = colors.ink4,
                                style = TrovataTokens.type.mono.copy(fontSize = 11.sp),
                            )
                        },
                    )
                    when {
                        state.isLoading -> EmptyState(text = "Carregando clientes...")
                        state.results.isEmpty() -> EmptyState(text = "Nenhum cliente encontrado")
                        else -> ClientList(clients = state.results, onInvite = onInviteClient)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
        ) {
            Btn(
                text = "Cliente",
                onClick = {},
                kind = BtnKind.Dark,
                size = BtnSize.Md,
                icon = TrovataIcons.plus,
                modifier = Modifier.shadow(elevation = 12.dp, shape = RoundedCornerShape(999.dp), clip = false),
            )
        }
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.surface2, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = TrovataIcons.search,
            contentDescription = null,
            tint = colors.ink3,
            modifier = Modifier.size(17.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Buscar cliente, cidade, CNPJ...",
                    color = colors.ink4,
                    fontSize = 13.sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.ink, fontSize = 13.sp),
                cursorBrush = SolidColor(colors.brand),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, color = colors.ink3, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ClientList(clients: List<CatalogClient>, onInvite: (CatalogClient) -> Unit) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
        Column {
            clients.forEachIndexed { index, client ->
                ClientRow(client = client, onInvite = onInvite)
                if (index < clients.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

@Composable
private fun ClientRow(client: CatalogClient, onInvite: (CatalogClient) -> Unit) {
    val colors = TrovataTokens.colors
    val secondary = client.legalName?.takeIf { it != client.name } ?: client.document ?: client.contact
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = client.name, hue = (client.id % 360).toDouble(), size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = client.name,
                    color = colors.ink,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.01).em,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!client.active) {
                    Pill(text = "Inativo", tone = PillTone.Neutral)
                }
            }
            secondary?.let {
                Text(
                    text = it,
                    color = colors.ink3,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            client.phone?.let { phone ->
                Text(
                    text = phone,
                    color = colors.ink4,
                    style = TrovataTokens.type.mono.copy(fontSize = 10.5.sp),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Btn(
            text = "Convidar",
            onClick = { onInvite(client) },
            kind = BtnKind.Soft,
            size = BtnSize.Sm,
            icon = TrovataIcons.video,
        )
    }
}
