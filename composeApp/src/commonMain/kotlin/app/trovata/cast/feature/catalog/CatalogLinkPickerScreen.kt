package app.trovata.cast.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.AppointmentsRepository
import app.trovata.cast.feature.invite.InviteRoute
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

data class CatalogLinkPickerRoute(
    val clientName: String? = null,
    val clientShop: String? = null,
    val scheduledFor: String? = null,
    val clientDocument: String? = null,
    val clientEmail: String? = null,
    val appointmentId: String? = null,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val appointments = koinInject<AppointmentsRepository>()
        val initial = remember(clientName, clientShop, scheduledFor, clientDocument, clientEmail) {
            ClientDraft(clientName, clientShop, scheduledFor, clientDocument, clientEmail)
        }
        val screenModel = koinScreenModel<CatalogLinkPickerScreenModel> { parametersOf(initial) }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(state.createdSession?.sessionId) {
            val record = state.createdSession ?: return@LaunchedEffect
            screenModel.consumeCreatedSession()
            appointmentId?.let { appointments.linkSession(it, record.sessionId, record.createdAtMs) }
            navigator.push(InviteRoute(record))
        }

        CatalogLinksBody(
            state = state,
            header = { PickerHeader(state = state, onBack = { navigator.pop() }) },
            bottomBar = { barModifier ->
                GenerateLinkBar(
                    state = state,
                    onGenerate = screenModel::generateLink,
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
}

@Composable
private fun PickerHeader(state: CatalogLinkPickerUiState, onBack: () -> Unit) {
    val colors = TrovataTokens.colors
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
                text = state.client.name?.let { "Para ${it.split(' ').first()}" } ?: "Escolher catálogo",
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
            text = "${state.visibleLinks.size}",
            tone = if (state.selectedUuid != null) PillTone.Brand else PillTone.Neutral,
        )
    }
}
