package app.trovata.cast.feature.call

import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CartItemLine
import app.trovata.cast.data.remote.sfa.CartSession

data class LiveCartSnapshot(
    val session: CartSession,
    val items: List<CartItemLine>,
    val completedCarrinhoId: Long?,
)

suspend fun loadLiveCart(
    carrinhoId: Long,
    summary: suspend (Long) -> ApiResult<CartSession>,
    open: suspend () -> ApiResult<CartSession>,
    items: suspend (Long) -> ApiResult<List<CartItemLine>>,
): ApiResult<LiveCartSnapshot> {
    val current = when (val result = summary(carrinhoId)) {
        is ApiResult.Fail -> return result
        is ApiResult.Ok -> result.value
    }
    val editable = if (current.status == "D") current else {
        when (val result = open()) {
            is ApiResult.Fail -> return result
            is ApiResult.Ok -> result.value
        }
    }
    if (editable.status != "D") {
        return ApiResult.Fail("cart_not_editable", "Não foi possível abrir um carrinho para continuar a compra", 0)
    }
    return when (val result = items(editable.id)) {
        is ApiResult.Fail -> result
        is ApiResult.Ok -> ApiResult.Ok(
            LiveCartSnapshot(
                session = editable,
                items = result.value,
                completedCarrinhoId = current.id.takeIf { current.status in setOf("F", "PE", "E") },
            ),
        )
    }
}
