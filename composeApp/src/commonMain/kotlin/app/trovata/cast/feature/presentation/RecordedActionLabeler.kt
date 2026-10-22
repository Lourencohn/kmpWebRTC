package app.trovata.cast.feature.presentation

import app.trovata.cast.protocol.CatalogRoute
import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.DrawPhase
import app.trovata.cast.protocol.PresentationEvent
import app.trovata.cast.protocol.ViewState

const val StartingPointLabel = "Ponto de partida marcado"
private const val StartingPointWindowMs: Long = 800

data class RecordedAction(val label: String, val sequence: Int)

class RecordedActionLabeler {
    private var lastView: ViewState? = null
    private var labeled = 0

    fun reset() {
        lastView = null
        labeled = 0
    }

    fun label(event: PresentationEvent): String? {
        val message = event.message
        val previousView = lastView
        if (message is DataChannelMessage.Navigate) lastView = message.view
        val isFirst = labeled == 0
        labeled++
        return when {
            isFirst -> StartingPointLabel
            event.t < StartingPointWindowMs -> null
            message is DataChannelMessage.Navigate -> navigationLabel(previousView, message.view)
            else -> message.actionLabel()
        }
    }

    private fun navigationLabel(previous: ViewState?, current: ViewState): String = when {
        current.focus != null -> "Abriu um produto"
        previous?.focus != null && previous.route == current.route -> "Fechou o produto"
        else -> current.route.label()
    }
}

private fun DataChannelMessage.actionLabel(): String? = when (this) {
    is DataChannelMessage.Scroll -> "Rolou a vitrine"
    is DataChannelMessage.PointAt -> "Apontou um produto"
    is DataChannelMessage.Draw -> if (phase == DrawPhase.Start) "Desenhou na vitrine" else null
    is DataChannelMessage.DrawClear -> "Apagou o desenho"
    is DataChannelMessage.QuantityDraft -> "Mudou a quantidade"
    else -> null
}

private fun CatalogRoute.label(): String = when (this) {
    CatalogRoute.Inicio -> "Voltou ao início"
    CatalogRoute.Menu -> "Abriu o menu"
    CatalogRoute.Todos -> "Abriu todos os produtos"
    is CatalogRoute.Secao -> "Abriu uma seção"
    CatalogRoute.Carrinho -> "Abriu o carrinho"
    CatalogRoute.Favoritos -> "Abriu os favoritos"
}
