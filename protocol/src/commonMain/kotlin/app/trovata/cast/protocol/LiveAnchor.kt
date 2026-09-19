package app.trovata.cast.protocol

private const val ANCHOR_SEPARATOR = ":"
private const val VIEWPORT_ANCHOR = "viewport"
private const val IMAGE_ANCHOR_PREFIX = "imagem:"

object LiveAnchor {
    fun product(produtoPreId: Long, complemento1Id: Long? = null): String =
        if (complemento1Id == null) {
            "produto$ANCHOR_SEPARATOR$produtoPreId"
        } else {
            "produto$ANCHOR_SEPARATOR$produtoPreId${ANCHOR_SEPARATOR}cor$ANCHOR_SEPARATOR$complemento1Id"
        }

    fun productModal(produtoPreId: Long): String =
        "produto$ANCHOR_SEPARATOR$produtoPreId${ANCHOR_SEPARATOR}modal"

    fun isProductModal(target: String): Boolean {
        val parts = target.split(ANCHOR_SEPARATOR)
        return parts.size == 3 && parts[0] == "produto" && parts[2] == "modal"
    }

    fun cartItem(itemId: Long): String = "carrinho${ANCHOR_SEPARATOR}item$ANCHOR_SEPARATOR$itemId"

    fun action(name: String): String = "acao$ANCHOR_SEPARATOR$name"

    fun viewport(): String = VIEWPORT_ANCHOR

    fun image(src: String): String = "$IMAGE_ANCHOR_PREFIX$src"

    fun imageSrcOf(target: String): String? =
        if (target.startsWith(IMAGE_ANCHOR_PREFIX)) target.removePrefix(IMAGE_ANCHOR_PREFIX) else null

    fun isViewport(target: String): Boolean = target == VIEWPORT_ANCHOR

    fun produtoPreIdOf(target: String): Long? {
        val parts = target.split(ANCHOR_SEPARATOR)
        if (parts.size < 2 || parts[0] != "produto") return null
        return parts[1].toLongOrNull()
    }
}
