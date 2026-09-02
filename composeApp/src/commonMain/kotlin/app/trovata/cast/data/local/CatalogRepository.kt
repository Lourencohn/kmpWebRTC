package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.sfa.dto.GradeBreakDto
import app.trovata.cast.ui.components.Product
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
    val imageUrl: String?,
)

data class CatalogStats(val brands: Int, val categories: Int, val priced: Int)

class CatalogRepository(
    private val db: TrovataDatabase,
    private val json: Json = HttpClientFactory.sfaJson,
) {
    private val catalog = db.catalogQueries
    private val pricing = db.pricingQueries
    private val assets = db.assetsQueries

    fun observeCatalog(priceTableId: Long? = null): Flow<List<CatalogProduct>> =
        catalog.selectAllProducts().asFlow().mapToList(Dispatchers.Default).map { rows ->
            assemble(rows, priceTableId)
        }

    fun observeCount(): Flow<Long> =
        catalog.countProducts().asFlow().mapToOne(Dispatchers.Default)

    fun observeAssetCount(): Flow<Long> =
        assets.countAssets().asFlow().mapToOne(Dispatchers.Default)

    suspend fun count(): Long = withContext(Dispatchers.Default) {
        catalog.countProducts().executeAsOne()
    }

    suspend fun stats(): CatalogStats = withContext(Dispatchers.Default) {
        CatalogStats(
            brands = catalog.countDistinctMarcas().executeAsOne().toInt(),
            categories = catalog.countDistinctCategorias().executeAsOne().toInt(),
            priced = catalog.countPricedProducts().executeAsOne().toInt(),
        )
    }

    suspend fun snapshot(priceTableId: Long? = null): List<CatalogProduct> = withContext(Dispatchers.Default) {
        assemble(catalog.selectAllProducts().executeAsList(), priceTableId)
    }

    suspend fun snapshotForRefs(refs: List<String>, priceTableId: Long? = null): List<CatalogProduct> = withContext(Dispatchers.Default) {
        if (refs.isEmpty()) return@withContext emptyList()
        val order = refs.withIndex().associate { (index, ref) -> ref to index }
        val rows = catalog.selectAllProducts().executeAsList()
            .filter { (it.idErp ?: it.id.toString()) in order }
        assemble(rows, priceTableId).sortedBy { order[it.ref] ?: Int.MAX_VALUE }
    }

    suspend fun page(limit: Int, offset: Int, priceTableId: Long? = null): List<CatalogProduct> = withContext(Dispatchers.Default) {
        assemble(catalog.selectProductsPaged(limit.toLong(), offset.toLong()).executeAsList(), priceTableId)
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
        val imageByErp = imageMapFor(rows.mapNotNull { it.idErp })

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
                imageUrl = p.idErp?.let { imageByErp[it] },
            )
        }
    }

    private fun imageMapFor(erps: List<String>): Map<String, String> {
        if (erps.isEmpty()) return emptyMap()
        val map = HashMap<String, String>()
        assets.selectThumbsForProducts(erps).executeAsList().forEach { row ->
            val erp = row.produtoIdErp ?: return@forEach
            if (erp in map) return@forEach
            val url = row.caminhoThumb ?: row.caminhoDetail ?: row.caminhoMedia ?: return@forEach
            map[erp] = url
        }
        return map
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
    produtoPreId = id,
    name = name,
    garment = GarmentKind.Shirt,
    tintIndex = (id % 8).toInt(),
    price = priceCents?.let { centsToBrl(it) } ?: "—",
    moq = moq,
    sizes = sizes,
    colorCount = colorCount,
    tag = null,
    image = null,
    imageUrl = imageUrl,
)

fun centsToBrl(cents: Long): String {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val reais = abs / 100
    val centavos = (abs % 100).toString().padStart(2, '0')
    val reaisStr = reais.toString().reversed().chunked(3).joinToString(".").reversed()
    return "${if (negative) "-" else ""}R$ $reaisStr,$centavos"
}
