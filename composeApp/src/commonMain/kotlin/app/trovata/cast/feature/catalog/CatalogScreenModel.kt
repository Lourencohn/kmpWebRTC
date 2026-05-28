package app.trovata.cast.feature.catalog

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.Company
import app.trovata.cast.data.local.CatalogProduct
import app.trovata.cast.data.local.CatalogRepository
import app.trovata.cast.data.local.toUiProduct
import app.trovata.cast.data.sample.Product
import app.trovata.cast.data.sample.ProductTag
import app.trovata.cast.data.sample.SampleCatalog
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
)

class CatalogScreenModel(
    private val catalogRepository: CatalogRepository,
    authRepository: AuthRepository,
) : ScreenModel {

    private val pageSize = 24
    private val _page = MutableStateFlow(1)
    private val _state = MutableStateFlow(sampleState(authRepository.activeCompany.value))
    val state: StateFlow<CatalogTabUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            combine(_page, catalogRepository.observeCount(), authRepository.activeCompany) { page, count, company ->
                Triple(page, count, company)
            }.collectLatest { (page, count, company) ->
                _state.value = buildState(page, count, company)
            }
        }
    }

    fun nextPage() = _page.update { (it + 1).coerceAtMost(_state.value.totalPages) }
    fun prevPage() = _page.update { (it - 1).coerceAtLeast(1) }

    private suspend fun buildState(page: Int, count: Long, company: Company?): CatalogTabUiState {
        if (count == 0L) return sampleState(company)
        val totalPages = ((count + pageSize - 1) / pageSize).toInt().coerceAtLeast(1)
        val safePage = page.coerceIn(1, totalPages)
        val products = catalogRepository.page(pageSize, (safePage - 1) * pageSize)
        val stats = catalogRepository.stats()
        return realState(products, stats, count, safePage, totalPages, company)
    }

    private fun realState(
        products: List<CatalogProduct>,
        stats: app.trovata.cast.data.local.CatalogStats,
        total: Long,
        page: Int,
        totalPages: Int,
        company: Company?,
    ): CatalogTabUiState {
        val collection = products.firstNotNullOfOrNull { it.colecao }
        val companyName = company?.name ?: "Catálogo"
        val summary = "$total SKUs" + if (stats.brands > 0) " · ${stats.brands} marcas" else ""
        return CatalogTabUiState(
            products = products.map { it.toUiProduct() },
            headerEyebrow = companyName,
            headerSubtitle = summary,
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
        )
    }

    private fun sampleState(company: Company?): CatalogTabUiState {
        val products = SampleCatalog.products
        val brand = company?.name ?: "Atelier Norte"
        return CatalogTabUiState(
            products = products,
            headerEyebrow = "$brand · Verão 26",
            headerSubtitle = "${products.size} SKUs · 3 estreias · 1 pré-venda",
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
        )
    }
}
