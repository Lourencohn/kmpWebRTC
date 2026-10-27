package app.trovata.cast.feature.aftercall

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.CallInterest
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.feature.agenda.AppointmentFormRoute
import app.trovata.cast.feature.agenda.interestDetailOf
import app.trovata.cast.platform.ShareController
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.CenteredMessage
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.format.formatBrl
import app.trovata.cast.ui.format.formatWhatsAppNumber
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

data class AfterCallRoute(val sessionId: String, val carrinhoId: Long? = null) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current
        val share = koinInject<ShareController>()
        val screenModel = koinScreenModel<AfterCallScreenModel> { parametersOf(AfterCallSpec(sessionId, carrinhoId)) }
        val state by screenModel.state.collectAsState()

        AfterCallBody(
            state = state,
            onMessage = {
                val link = screenModel.whatsAppLink()
                if (link != null) uriHandler.openUri(link) else share.share(screenModel.messageForClient())
            },
            onFollowUp = { navigator.push(screenModel.followUpDraft().toRoute()) },
            onDone = { navigator.pop() },
        )
    }
}

private fun FollowUpDraft.toRoute(): AppointmentFormRoute = AppointmentFormRoute(
    followUpOfId = followUpOfId,
    clienteId = clienteId,
    clientName = clientName,
    clientShop = clientShop,
    clientDocument = clientDocument,
    clientEmail = clientEmail,
    clientPhone = clientPhone,
    objective = objective,
    notes = notes,
)

@Composable
private fun AfterCallBody(
    state: AfterCallUiState,
    onMessage: () -> Unit,
    onFollowUp: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = TrovataTokens.colors
    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        if (state.isLoading) {
            CenteredMessage("Organizando o que aconteceu na chamada...")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 56.dp, bottom = 190.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item { AfterCallHeader(state) }
                state.order?.let { order -> item { OrderCard(order) } }
                item { LeftOutLabel(state) }
                if (state.leftOut.isEmpty()) {
                    item { LeftOutEmpty(state) }
                } else {
                    items(state.leftOut, key = { it.produtoPreId }) { interest -> LeftOutRow(interest) }
                }
            }
        }
        AfterCallBottomBar(
            state = state,
            onMessage = onMessage,
            onFollowUp = onFollowUp,
            onDone = onDone,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AfterCallHeader(state: AfterCallUiState) {
    val colors = TrovataTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(text = "Chamada encerrada", tone = PillTone.Neutral, icon = TrovataIcons.check)
        Text(
            text = state.clientName ?: "Cliente",
            color = colors.ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).em,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        state.session?.catalogLabel?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, color = colors.ink3, fontSize = 13.sp)
        }
    }
}

@Composable
private fun OrderCard(order: StoredOrder) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.jadeTint, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = TrovataIcons.check, contentDescription = null, tint = colors.jade2, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pedido ${order.orderId} pronto",
                color = colors.jade2,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${order.lines.sumOf { it.units }} un · ${formatBrl(order.totalCents)}",
                color = colors.jade2,
                fontSize = 12.5.sp,
            )
        }
    }
}

@Composable
private fun LeftOutLabel(state: AfterCallUiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Ficou de fora do pedido",
            action = if (state.leftOut.isNotEmpty()) ({ Pill(text = state.leftOut.size.toString(), tone = PillTone.Brand) }) else null,
        )
        Text(
            text = leftOutCaption(state),
            color = TrovataTokens.colors.ink3,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

private fun leftOutCaption(state: AfterCallUiState): String {
    val inCart = state.inCartCount
    val cartPart = when (inCart) {
        0 -> "Nenhum produto entrou no carrinho."
        1 -> "1 produto entrou no carrinho."
        else -> "$inCart produtos entraram no carrinho."
    }
    return if (state.leftOut.isEmpty()) cartPart else "Produtos abertos na chamada que não entraram no carrinho. $cartPart"
}

@Composable
private fun LeftOutEmpty(state: AfterCallUiState) {
    val text = if (state.interests.isEmpty()) {
        "O cliente não abriu nenhum produto nesta chamada."
    } else {
        "Tudo o que vocês abriram entrou no carrinho."
    }
    TrovataCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(text = text, color = TrovataTokens.colors.ink3, fontSize = 13.sp)
    }
}

@Composable
private fun LeftOutRow(interest: CallInterest) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductThumbnail(imageUrl = interest.imageUrl)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = interest.label,
                    color = if (interest.isNamed) colors.ink else colors.ink3,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(interest.ref?.takeIf { interest.isNamed }?.let { "Ref. $it" }, interestDetailOf(interest))
                        .joinToString(" · "),
                    color = colors.ink3,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ProductThumbnail(imageUrl: String?) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface2),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(imageVector = TrovataIcons.swatch, contentDescription = null, tint = colors.ink3, modifier = Modifier.size(18.dp))
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AfterCallBottomBar(
    state: AfterCallUiState,
    onMessage: () -> Unit,
    onFollowUp: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Btn(
            text = "Agendar retorno",
            onClick = onFollowUp,
            kind = BtnKind.Primary,
            size = BtnSize.Md,
            icon = TrovataIcons.calendar,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn(
                text = if (state.order != null) "Enviar resumo" else "Mandar mensagem",
                onClick = onMessage,
                kind = BtnKind.Jade,
                size = BtnSize.Md,
                icon = TrovataIcons.msg,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1.3f),
            )
            Btn(
                text = "Concluir",
                onClick = onDone,
                kind = BtnKind.Surface,
                size = BtnSize.Md,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = state.whatsAppNumber?.let { "A mensagem abre no WhatsApp de ${formatWhatsAppNumber(it)}" }
                ?: "Sem celular válido do cliente. A mensagem abre no compartilhamento.",
            color = colors.ink4,
            fontSize = 11.5.sp,
        )
    }
}
