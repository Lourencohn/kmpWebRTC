package app.trovata.cast.feature.agenda

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.feature.catalog.CatalogLinkPickerRoute
import app.trovata.cast.feature.invite.InviteRoute
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.CenteredMessage
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.format.formatBrl
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf

data class AppointmentDetailRoute(val appointmentId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<AppointmentDetailScreenModel> { parametersOf(appointmentId) }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(state.createdSession?.sessionId) {
            val record = state.createdSession ?: return@LaunchedEffect
            screenModel.consumeCreatedSession()
            navigator.push(InviteRoute(record))
        }

        LaunchedEffect(state.followUpRequestedFor) {
            val previousId = state.followUpRequestedFor ?: return@LaunchedEffect
            screenModel.consumeFollowUpRequest()
            navigator.push(AppointmentFormRoute(followUpOfId = previousId))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AppointmentDetailBody(
                state = state,
                actions = DetailActions(
                    onBack = { navigator.pop() },
                    onEdit = { navigator.push(AppointmentFormRoute(appointmentId = appointmentId)) },
                    onReschedule = { navigator.push(AppointmentFormRoute(appointmentId = appointmentId, reschedule = true)) },
                    onInvite = { appointment -> navigator.push(catalogPickerFor(appointment)) },
                    onCallWithCart = screenModel::callWithCart,
                    onToggleCart = screenModel::toggleCart,
                    onRetryCarts = screenModel::refreshCarts,
                    onTogglePrep = screenModel::togglePrep,
                    onOpenResult = screenModel::openResult,
                    onNewContact = { navigator.push(AppointmentFormRoute(followUpOfId = appointmentId)) },
                    onRequestCancel = screenModel::requestCancel,
                    onDismissError = screenModel::clearError,
                ),
            )

            if (state.showResultSheet) {
                ResultSheet(
                    state = state,
                    onSelect = screenModel::selectResult,
                    onNotes = screenModel::setOutcomeNotes,
                    onNextStep = screenModel::setNextStep,
                    onToggleReturn = screenModel::toggleScheduleReturn,
                    onSave = screenModel::saveResult,
                    onDismiss = screenModel::dismissResult,
                )
            }

            if (state.confirmingCancel) {
                CancelSheet(
                    clientName = state.appointment?.clientName.orEmpty(),
                    onConfirm = screenModel::confirmCancel,
                    onDismiss = screenModel::dismissCancel,
                )
            }
        }
    }
}

private fun catalogPickerFor(appointment: Appointment) = CatalogLinkPickerRoute(
    clientName = appointment.clientName,
    clientShop = appointment.clientShop,
    scheduledFor = scheduledForOf(appointment),
    clientDocument = appointment.clientDocument,
    clientEmail = appointment.clientEmail,
    appointmentId = appointment.appointmentId,
)

internal class DetailActions(
    val onBack: () -> Unit,
    val onEdit: () -> Unit,
    val onReschedule: () -> Unit,
    val onInvite: (Appointment) -> Unit,
    val onCallWithCart: (OpenCart) -> Unit,
    val onToggleCart: (OpenCart) -> Unit,
    val onRetryCarts: () -> Unit,
    val onTogglePrep: (PrepTask) -> Unit,
    val onOpenResult: () -> Unit,
    val onNewContact: () -> Unit,
    val onRequestCancel: () -> Unit,
    val onDismissError: () -> Unit,
)

@Composable
private fun AppointmentDetailBody(state: AppointmentDetailUiState, actions: DetailActions) {
    val colors = TrovataTokens.colors
    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        val appointment = state.appointment
        when {
            state.isLoading -> CenteredMessage("Carregando compromisso...")
            appointment == null -> Column {
                DetailTopBar(title = "Compromisso", subtitle = null, canEdit = false, onBack = actions.onBack, onEdit = {})
                CenteredMessage("Esse compromisso não está mais na sua agenda.")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 56.dp, bottom = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        DetailTopBar(
                            title = appointment.clientName,
                            subtitle = relativeDayLabel(localDateAt(appointment.startsAtMs, state.timeZone), localDateAt(state.nowMs, state.timeZone)),
                            canEdit = appointment.status == AppointmentStatus.Scheduled,
                            onBack = actions.onBack,
                            onEdit = actions.onEdit,
                        )
                    }
                    item { HeroCard(state = state, appointment = appointment, actions = actions) }
                    item { ObjectiveCard(appointment = appointment, onEdit = actions.onEdit) }
                    if (appointment.canCall) {
                        item { PrepCard(state = state, appointment = appointment, onToggle = actions.onTogglePrep) }
                    }
                    if (appointment.status != AppointmentStatus.Scheduled || appointment.outcomeNotes != null) {
                        item { ResultCard(appointment = appointment) }
                    }
                    item { CartsCard(state = state, appointment = appointment, actions = actions) }
                    if (state.ordersInProgress.isNotEmpty()) {
                        item { OrdersInProgressCard(carts = state.ordersInProgress, nowMs = state.nowMs) }
                    }
                    item { HistoryCard(state = state) }
                    item { ClientCard(state = state, appointment = appointment) }
                }
                DetailBottomBar(
                    state = state,
                    appointment = appointment,
                    actions = actions,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        state.error?.let { message ->
            AgendaBanner(
                text = message,
                onDismiss = actions.onDismissError,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 104.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}

@Composable
private fun DetailTopBar(title: String, subtitle: String?, canEdit: Boolean, onBack: () -> Unit, onEdit: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconBtn(icon = TrovataIcons.back, onClick = onBack, kind = IconBtnKind.Line, contentDescription = "Voltar")
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            subtitle?.let { Text(text = "Chamada · $it", color = colors.ink4, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
            Text(
                text = title,
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (canEdit) {
            IconBtn(icon = TrovataIcons.edit, onClick = onEdit, kind = IconBtnKind.Line, contentDescription = "Editar compromisso")
        } else {
            Box(modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
private fun DetailBottomBar(
    state: AppointmentDetailUiState,
    appointment: Appointment,
    actions: DetailActions,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 18.dp),
    ) {
        if (!appointment.canCall) {
            Btn(
                text = "Agendar novo contato",
                onClick = actions.onNewContact,
                kind = BtnKind.Primary,
                size = BtnSize.Md,
                icon = TrovataIcons.calendar,
                modifier = Modifier.fillMaxWidth(),
            )
            return@Box
        }
        val cart = state.callableCart
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn(
                text = "Resultado",
                onClick = actions.onOpenResult,
                kind = BtnKind.Ghost,
                size = BtnSize.Md,
                icon = TrovataIcons.check,
                modifier = Modifier.weight(1f),
            )
            Btn(
                text = when {
                    state.callingCartId != null -> "Abrindo sessão..."
                    cart != null -> "Chamar · ${cart.totalCents?.let(::formatBrl) ?: "carrinho"}"
                    else -> "Convidar"
                },
                onClick = { if (cart != null) actions.onCallWithCart(cart) else actions.onInvite(appointment) },
                kind = BtnKind.Jade,
                size = BtnSize.Md,
                icon = TrovataIcons.video,
                enabled = state.callingCartId == null,
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}
