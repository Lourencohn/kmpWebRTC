package app.trovata.cast.feature.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.remote.sfa.SellerCatalogLink
import app.trovata.cast.feature.catalog.CatalogBottomBar
import app.trovata.cast.feature.catalog.CatalogLinkPickerScreenModel
import app.trovata.cast.feature.catalog.CatalogLinksBody
import app.trovata.cast.feature.catalog.ClientDraft
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf

object PresentationCatalogPickerRoute : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<CatalogLinkPickerScreenModel> { parametersOf(ClientDraft()) }
        val state by screenModel.state.collectAsState()

        CatalogLinksBody(
            state = state,
            header = { RecordPickerHeader(onBack = { navigator.pop() }) },
            bottomBar = { barModifier ->
                RecordWithCatalogBar(
                    selected = state.selected,
                    modifier = barModifier,
                    onRecord = { link ->
                        navigator.replace(
                            PresentationRecorderRoute(
                                catalogoUuid = link.uuid,
                                catalogoNome = link.name,
                            ),
                        )
                    },
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
}

@Composable
private fun RecordPickerHeader(onBack: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(icon = TrovataIcons.back, onClick = onBack, kind = IconBtnKind.Line, contentDescription = "Voltar")
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "GRAVAR APRESENTAÇÃO",
                color = colors.live,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
            Text(
                text = "Qual catálogo você vai mostrar?",
                color = colors.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.01).em,
            )
        }
    }
}

@Composable
private fun RecordWithCatalogBar(
    selected: SellerCatalogLink?,
    onRecord: (SellerCatalogLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val recordable = selected?.takeIf { it.isAvailable }
    CatalogBottomBar(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selected?.name ?: "Nenhum catálogo escolhido",
                    color = colors.ink4,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        selected == null -> "Toque em um catálogo da lista"
                        recordable == null -> "Esse catálogo está expirado ou inativo"
                        else -> "Na próxima tela você grava"
                    },
                    color = colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Btn(
                text = "Gravar",
                onClick = { recordable?.let(onRecord) },
                kind = if (recordable != null) BtnKind.Danger else BtnKind.Soft,
                size = BtnSize.Md,
                icon = TrovataIcons.mic,
                enabled = recordable != null,
            )
        }
    }
}
