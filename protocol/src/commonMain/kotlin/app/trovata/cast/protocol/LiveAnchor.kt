package app.trovata.cast.protocol

private const val ANCHOR_SEPARATOR = ":"

object LiveAnchor {
    fun product(produtoPreId: Long, complemento1Id: Long? = null): String =
        if (complemento1Id == null) {
            "produto$ANCHOR_SEPARATOR$produtoPreId"
        } else {
            "produto$ANCHOR_SEPARATOR$produtoPreId${ANCHOR_SEPARATOR}cor$ANCHOR_SEPARATOR$complemento1Id"
        }

    fun cartItem(itemId: Long): String = "carrinho${ANCHOR_SEPARATOR}item$ANCHOR_SEPARATOR$itemId"

    fun action(name: String): String = "acao$ANCHOR_SEPARATOR$name"

    fun produtoPreIdOf(target: String): Long? {
        val parts = target.split(ANCHOR_SEPARATOR)
        if (parts.size < 2 || parts[0] != "produto") return null
        return parts[1].toLongOrNull()
    }
}
