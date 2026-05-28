package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.sfa.dto.GradeBreakDto
import app.trovata.cast.data.sample.Product
import app.trovata.cast.data.sample.SampleCatalog
import app.trovata.cast.db.ProductEntity
import app.trovata.cast.db.TrovataDatabase
import app.trovata.cast.ui.components.GarmentKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class CatalogProduct(
    val id: Long,
    val ref: String,
    val name: String,
    val priceCents: Long?,
    val moq: Int,
    val sizes: List<String>,
    val colorCount: Int,
    val colecao: String?,
    val marca: String?,
    val categoria: String?,
)

class CatalogRepository(
    private val db: TrovataDatabase,
    private val json: Json = HttpClientFactory.sfaJson,
) {
    private val catalog = db.catalogQueries
    private val pricing = db.pricingQueries

    fun observeCatalog(priceTableId: Long? = null): Flow<List<CatalogProduct>> =
        catalog.selectAllProducts().asFlow().mapToList(Dispatchers.Default).map { rows ->
            assemble(rows, priceTableId)
        }

    suspend fun snapshot(priceTableId: Long? = null): List<CatalogProduct> = withContext(Dispatchers.Default) {
        assemble(catalog.selectAllProducts().executeAsList(), priceTableId)
    }

    suspend fun uiProducts(priceTableId: Long? = null): List<Product> = withContext(Dispatchers.Default) {
        val real = assemble(catalog.selectAllProducts().executeAsList(), priceTableId)
        if (real.isEmpty()) SampleCatalog.products else real.map { it.toUiProduct() }
    }

    suspend fun isEmpty(): Boolean = withContext(Dispatchers.Default) {
        catalog.countProducts().executeAsOne() == 0L
    }

    private fun assemble(rows: List<ProductEntity>, priceTableId: Long?): List<CatalogProduct> {
        val commercialByPre = catalog.selectAllCommercial().executeAsList()
            .filter { it.produtoPreId != null }
            .groupBy { it.produtoPreId!! }
        val priceByPre = (priceTableId?.let { pricing.selectPricesForTable(it).executeAsList() } ?: emptyList())
            .filter { it.produtoPreId != null }
            .associateBy { it.produtoPreId!! }

        return rows.map { p ->
            val commercial = commercialByPre[p.id]?.minByOrNull { it.qtdeMinimaVenda ?: Long.MAX_VALUE }
            val price = priceByPre[p.id]
            val grade = parseGrade(price?.listaGradeJson)
            CatalogProduct(
                id = p.id,
                ref = p.idErp ?: p.id.toString(),
                name = p.descricao ?: p.apelido ?: (p.idErp ?: p.id.toString()),
                priceCents = price?.precoCents ?: p.precoFinalCents ?: p.precoBaseCents,
                moq = (commercial?.qtdeMinimaVenda ?: commercial?.listaMultiploVenda ?: p.listaMultiploVenda ?: 1L).toInt().coerceAtLeast(1),
                sizes = grade.sizes.ifEmpty { listOf("Único") },
                colorCount = grade.colorCount.coerceAtLeast(1),
                colecao = p.descricaoColecao,
                marca = p.descricaoMarca,
                categoria = p.descricaoCategoria,
            )
        }
    }

    private fun parseGrade(listaGradeJson: String?): GradeInfo {
        if (listaGradeJson.isNullOrBlank()) return GradeInfo(emptyList(), 0)
        val breaks = runCatching { json.decodeFromString<List<GradeBreakDto>>(listaGradeJson) }.getOrNull()
            ?: return GradeInfo(emptyList(), 0)
        val sizes = breaks.flatMap { b -> b.complemento2.orEmpty().mapNotNull { it.complemento2Descricao } }
            .distinct()
        val colors = breaks.mapNotNull { it.complemento1Descricao }.distinct()
        return GradeInfo(sizes, colors.size)
    }

    private data class GradeInfo(val sizes: List<String>, val colorCount: Int)
}

fun CatalogProduct.toUiProduct(): Product = Product(
    ref = ref,
    name = name,
    garment = GarmentKind.Shirt,
    tintIndex = (id % 8).toInt(),
    price = priceCents?.let { centsToBrl(it) } ?: "—",
    moq = moq,
    sizes = sizes,
    colorCount = colorCount,
    tag = null,
    image = null,
)

fun centsToBrl(cents: Long): String {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val reais = abs / 100
    val centavos = (abs % 100).toString().padStart(2, '0')
    val reaisStr = reais.toString().reversed().chunked(3).joinToString(".").reversed()
    return "${if (negative) "-" else ""}R$ $reaisStr,$centavos"
}
