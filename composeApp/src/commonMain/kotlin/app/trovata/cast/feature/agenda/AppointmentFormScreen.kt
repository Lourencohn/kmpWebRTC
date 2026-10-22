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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.AppointmentPriority
import app.trovata.cast.data.local.CatalogClient
import app.trovata.cast.data.remote.sfa.OpenCart
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
import kotlinx.datetime.LocalDate
import org.koin.core.parameter.parametersOf

data class AppointmentFormRoute(
    val appointmentId: String? = null,
    val followUpOfId: String? = null,
    val reschedule: Boolean = false,
    val clienteId: Long? = null,
    val clientName: String? = null,
    val clientShop: String? = null,
    val clientDocument: String? = null,
    val clientEmail: String? = null,
    val clientPhone: String? = null,
    val carrinhoId: Long? = null,
    val objective: String? = null,
    val epochDay: Int? = null,
    val minuteOfDay: Int? = null,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val spec = remember { toSpec() }
        val screenModel = koinScreenModel<AppointmentFormScreenModel> { parametersOf(spec) }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(state.savedAppointmentId) {
            if (state.savedAppointmentId != null) navigator.pop()
        }

        AppointmentFormBody(
            state = state,
            onBack = { navigator.pop() },
            onClientQuery = screenModel::setClientQuery,
            onPickClient = screenModel::pickClient,
            onClearClient = screenModel::clearClient,
            onSelectDate = screenModel::selectDate,
            onToggleMonth = screenModel::toggleMonthPicker,
            onSelectSlot = screenModel::selectSlot,
            onNudge = screenModel::nudgeTime,
            onDuration = screenModel::selectDuration,
            onObjective = screenModel::setObjective,
            onObjectiveSuggestion = screenModel::applyObjective,
            onNotes = screenModel::setNotes,
            onTogglePriority = screenModel::togglePriority,
            onSave = screenModel::save,
        )
    }

    private fun toSpec(): AppointmentFormSpec = AppointmentFormSpec(
        appointmentId = appointmentId,
        followUpOfId = followUpOfId,
        reschedule = reschedule,
        client = clientName?.let {
            AppointmentClient(
                clienteId = clienteId,
                name = it,
                shop = clientShop,
                document = clientDocument,
                email = clientEmail,
                phone = clientPhone,
            )
        },
        carrinhoId = carrinhoId,
        objective = objective,
        epochDay = epochDay,
        minuteOfDay = minuteOfDay,
    )

    companion object {
        fun forClient(client: CatalogClient): AppointmentFormRoute {
            val chosen = client.toAppointmentClient()
            return AppointmentFormRoute(
                clienteId = chosen.clienteId,
                clientName = chosen.name,
                clientShop = chosen.shop,
                clientDocument = chosen.document,
                clientEmail = chosen.email,
                clientPhone = chosen.phone,
            )
        }

        fun forCart(cart: OpenCart): AppointmentFormRoute = AppointmentFormRoute(
            clienteId = cart.clienteId,
            clientName = cart.title,
            clientEmail = cart.clientEmail,
            carrinhoId = cart.carrinhoId,
            objective = "Retomar carrinho parado",
        )

        fun forDay(date: LocalDate?, minuteOfDay: Int?): AppointmentFormRoute =
            AppointmentFormRoute(epochDay = date?.toEpochDays(), minuteOfDay = minuteOfDay)
    }
}

@Composable
private fun AppointmentFormBody(
    state: AppointmentFormUiState,
    onBack: () -> Unit,
    onClientQuery: (String) -> Unit,
    onPickClient: (CatalogClient) -> Unit,
    onClearClient: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleMonth: () -> Unit,
    onSelectSlot: (Int) -> Unit,
    onNudge: (Boolean) -> Unit,
    onDuration: (Int) -> Unit,
    onObjective: (String) -> Unit,
    onObjectiveSuggestion: (String) -> Unit,
    onNotes: (String) -> Unit,
    onTogglePriority: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = TrovataTokens.colors
    Box(modifier = Modifier.fillMaxSize().background(colors.bg).imePadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { FormTopBar(title = state.mode.title, subtitle = state.followUpOf?.let { "Retorno de ${it.clientName}" }, onBack = onBack) }
            item {
                ClientSection(
                    state = state,
                    onQuery = onClientQuery,
                    onPick = onPickClient,
                    onClear = onClearClient,
                )
            }
            item {
                DaySection(
                    state = state,
                    onSelect = onSelectDate,
                    onToggleMonth = onToggleMonth,
                )
            }
            item { TimeSection(state = state, onSelect = onSelectSlot, onNudge = onNudge) }
            item { DurationSection(selected = state.durationMinutes, onSelect = onDuration) }
            item {
                ObjectiveSection(
                    objective = state.objective,
                    onObjective = onObjective,
                    onSuggestion = onObjectiveSuggestion,
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AgendaTextField(
                        label = "Preparação",
                        value = state.notes,
                        onValueChange = onNotes,
                        placeholder = "O que levar para a conversa: condições, dúvidas da última vez, peças que ele pediu",
                        singleLine = false,
                    )
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CheckRow(
                        label = "Cliente prioritário",
                        detail = "Fica destacado na agenda com uma estrela.",
                        checked = state.priority == AppointmentPriority.High,
                        onToggle = onTogglePriority,
                    )
                }
            }
        }

        SaveBar(
            state = state,
            onSave = onSave,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun FormTopBar(title: String, subtitle: String?, onBack: () -> Unit) {
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
            subtitle?.let {
                Text(
                    text = it,
                    color = colors.ink4,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(text = title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(modifier = Modifier.size(38.dp))
    }
}

@Composable
private fun SaveBar(state: AppointmentFormUiState, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val message = state.problem ?: state.conflictPreview
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        message?.let {
            Text(text = it, color = colors.warn, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp)
        }
        Btn(
            text = if (state.isSaving) "Salvando..." else state.mode.saveLabel,
            onClick = onSave,
            kind = BtnKind.Primary,
            size = BtnSize.Lg,
            icon = TrovataIcons.calendar,
            enabled = !state.isSaving && !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
