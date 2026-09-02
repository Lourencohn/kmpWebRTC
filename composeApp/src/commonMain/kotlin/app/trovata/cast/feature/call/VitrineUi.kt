package app.trovata.cast.feature.call

import app.trovata.cast.data.local.centsToBrl
import app.trovata.cast.data.remote.sfa.VitrineProduto
import app.trovata.cast.ui.components.GarmentKind
import app.trovata.cast.ui.components.Product

fun VitrineProduto.toUiProduct(): Product = Product(
    ref = ref,
    produtoPreId = produtoPreId,
    name = nome,
    garment = GarmentKind.Shirt,
    tintIndex = (produtoPreId % 8).toInt(),
    price = precoCents?.let { centsToBrl(it) } ?: "—",
    moq = multiploVenda,
    sizes = emptyList(),
    colorCount = 1,
    tag = null,
    image = null,
    imageUrl = imageUrl,
)
