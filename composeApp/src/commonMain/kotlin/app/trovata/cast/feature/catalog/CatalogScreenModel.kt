package app.trovata.cast.feature.catalog

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.Company
import app.trovata.cast.data.local.CatalogProduct
import app.trovata.cast.data.local.CatalogRepository
import app.trovata.cast.data.local.toUiProduct
import app.trovata.cast.data.sample.Product
import app.trovata.cast.data.sample.ProductTag
import app.trovata.cast.data.sample.SampleCatalog
import app.trovata.cast.data.sync.CatalogSyncCoordinator
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogStatItem(val label: String, val value: String)

data class CatalogTabUiState(
    val products: List<Product>,
    val headerEyebrow: String,
    val headerSubtitle: String,
    val heroEyebrow: String,
    val heroTitle: String,
    val heroSubtitle: String?,
    val heroDescription: String,
    val stats: List<CatalogStatItem>,
    val brandChips: List<String>,
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Long = 0,
    val syncing: Boolean = false,
)

class CatalogScreenModel(
    private val catalogRepository: CatalogRepository,
    authRepository: AuthRepository,
    syncCoordinator: CatalogSyncCoordinator,
) : ScreenModel {

    private val pageSize = 24
    private val _page = MutableStateFlow(1)
    private val _state = MutableStateFlow(sampleState(authRepository.activeCompany.value, false))
    val state: StateFlow<CatalogTabUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            combine(
                _page,
                catalogRepository.observeCount(),
                catalogRepository.observeAssetCount(),
                authRepository.activeCompany,
                syncCoordinator.isSyncing,
            ) { page, count, _, company, syncing ->
                State(page, count, company, syncing)
            }.collectLatest { s ->
                _state.value = buildState(s.page, s.count, s.company, s.syncing)
            }
        }
    }

    private data class State(val page: Int, val count: Long, val company: Company?, val syncing: Boolean)

    fun nextPage() = _page.update { (it + 1).coerceAtMost(_state.value.totalPages) }
    fun prevPage() = _page.update { (it - 1).coerceAtLeast(1) }

    private suspend fun buildState(page: Int, count: Long, company: Company?, syncing: Boolean): CatalogTabUiState {
        if (count == 0L) return sampleState(company, syncing)
        val totalPages = ((count + pageSize - 1) / pageSize).toInt().coerceAtLeast(1)
        val safePage = page.coerceIn(1, totalPages)
        val products = catalogRepository.page(pageSize, (safePage - 1) * pageSize)
        val stats = catalogRepository.stats()
        return realState(products, stats, count, safePage, totalPages, company, syncing)
    }

    private fun realState(
        products: List<CatalogProduct>,
        stats: app.trovata.cast.data.local.CatalogStats,
        total: Long,
        page: Int,
        totalPages: Int,
        company: Company?,
        syncing: Boolean,
    ): CatalogTabUiState {
        val collection = products.firstNotNullOfOrNull { it.colecao }
        val companyName = company?.name ?: "Catálogo"
        val summary = "$total SKUs" + if (stats.brands > 0) " · ${stats.brands} marcas" else ""
        return CatalogTabUiState(
            products = products.map { it.toUiProduct() },
            headerEyebrow = companyName,
            headerSubtitle = if (syncing) "$summary · sincronizando…" else summary,
            heroEyebrow = if (collection != null) "Coleção em destaque" else "Catálogo ativo",
            heroTitle = collection ?: companyName,
            heroSubtitle = collection?.let { companyName },
            heroDescription = "$summary.\n${stats.categories} categorias · ${stats.priced} com preço.",
            stats = listOf(
                CatalogStatItem("SKUs", total.toString()),
                CatalogStatItem("Marcas", stats.brands.toString()),
                CatalogStatItem("Categorias", stats.categories.toString()),
                CatalogStatItem("Com preço", stats.priced.toString()),
            ),
            brandChips = products.mapNotNull { it.marca }.distinct().take(5),
            page = page,
            totalPages = totalPages,
            total = total,
            syncing = syncing,
        )
    }

    private fun sampleState(company: Company?, syncing: Boolean): CatalogTabUiState {
        val products = SampleCatalog.products
        val brand = company?.name ?: "Atelier Norte"
        return CatalogTabUiState(
            products = products,
            headerEyebrow = "$brand · Verão 26",
            headerSubtitle = if (syncing) "Sincronizando catálogo…" else "${products.size} SKUs · 3 estreias · 1 pré-venda",
            heroEyebrow = "Coleção em destaque",
            heroTitle = "Verão",
            heroSubtitle = "vinte e seis",
            heroDescription = "${products.size} SKUs · 3 estreias · Chenson, LEE, Dumond.\nBolsas femininas atacado.",
            stats = listOf(
                CatalogStatItem("SKUs", products.size.toString()),
                CatalogStatItem("Novos", products.count { it.tag == ProductTag.Novo }.toString()),
                CatalogStatItem("Pré-venda", products.count { it.tag == ProductTag.PreVenda }.toString()),
                CatalogStatItem("Top venda", products.count { it.tag == ProductTag.TopVenda }.toString()),
            ),
            brandChips = listOf("Verão 26", "Chenson", "LEE", "Dumond"),
            page = 1,
            totalPages = 1,
            total = products.size.toLong(),
            syncing = syncing,
        )
    }
}
