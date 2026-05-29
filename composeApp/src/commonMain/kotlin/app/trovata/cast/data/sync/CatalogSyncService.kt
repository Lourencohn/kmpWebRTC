package app.trovata.cast.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.sfa.SfaApi
import app.trovata.cast.data.remote.sfa.SfaApiResult
import app.trovata.cast.data.remote.sfa.SfaParse
import app.trovata.cast.data.remote.sfa.dto.AssetDto
import app.trovata.cast.data.remote.sfa.dto.ClientDto
import app.trovata.cast.data.remote.sfa.dto.CommercialProductDto
import app.trovata.cast.data.remote.sfa.dto.ComplementoDto
import app.trovata.cast.data.remote.sfa.dto.GradePadraoDto
import app.trovata.cast.data.remote.sfa.dto.PrazoDto
import app.trovata.cast.data.remote.sfa.dto.PriceTableDto
import app.trovata.cast.data.remote.sfa.dto.ProductDto
import app.trovata.cast.data.remote.sfa.dto.ProductPriceDto
import app.trovata.cast.data.remote.sfa.dto.SaleTypeDto
import app.trovata.cast.data.remote.sfa.dto.SellerClientDto
import app.trovata.cast.data.remote.sfa.dto.SellerDto
import app.trovata.cast.data.remote.sfa.dto.TaxonomyDto
import app.trovata.cast.db.TrovataDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

data class SyncStatus(
    val entity: String,
    val cursorUpdatedAt: String?,
    val lastSyncAtMs: Long?,
    val lastError: String?,
    val inFlight: Boolean,
)

data class SyncEntityResult(
    val entity: String,
    val processed: Int,
    val success: Boolean,
    val error: String?,
    val skipped: Int = 0,
)

data class SyncReport(val results: List<SyncEntityResult>) {
    val success: Boolean get() = results.all { it.success }
}

class CatalogSyncService(
    private val sfaApi: SfaApi,
    private val db: TrovataDatabase,
    private val json: Json = HttpClientFactory.sfaJson,
) {
    private val catalog = db.catalogQueries
    private val pricing = db.pricingQueries
    private val taxonomy = db.taxonomyQueries
    private val people = db.peopleQueries
    private val assets = db.assetsQueries
    private val sync = db.syncStateQueries

    fun observeSyncState(): Flow<List<SyncStatus>> =
        sync.selectAllSyncState().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map {
                SyncStatus(it.entity, it.cursorUpdatedAt, it.lastSyncAtMs, it.lastError, it.inFlight == 1L)
            }
        }

    suspend fun syncAll(): SyncReport {
        val results = ORDER.map { it() }
        return SyncReport(results)
    }

    suspend fun syncEssentials(): SyncReport {
        val essentials: List<suspend () -> SyncEntityResult> = listOf(
            ::syncCategorias, ::syncColecoes, ::syncMarcas,
            ::syncProdutosPre, ::syncArquivos, ::syncClientes,
            ::syncTabelasPrecos, ::syncComplementos1, ::syncComplementos2, ::syncComplementos3,
            ::syncItensPrecos, ::syncProdutosComerciais, ::syncPrazos,
        )
        return SyncReport(essentials.map { it() })
    }

    private val ORDER: List<suspend () -> SyncEntityResult> = listOf(
        ::syncCategorias, ::syncColecoes, ::syncMarcas, ::syncGruposProdutos, ::syncGeneros,
        ::syncLinhas, ::syncFamiliasComerciais, ::syncNichos, ::syncEspecies, ::syncTiposProdutos,
        ::syncComplementos1, ::syncComplementos2, ::syncComplementos3, ::syncGradesPadroes,
        ::syncTabelasPrecos, ::syncProdutosPre, ::syncProdutosComerciais, ::syncItensPrecos,
        ::syncPrazos, ::syncArquivos, ::syncTiposVendas, ::syncVendedores, ::syncClientes, ::syncVendedoresClientes,
    )

    suspend fun syncCategorias() = syncResource<TaxonomyDto>("categorias", 100, { it.updatedAt }, taxonomy::deleteCategoriasByIds) { r ->
        r.forEach { taxonomy.upsertCategoria(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncColecoes() = syncResource<TaxonomyDto>("colecoes", 100, { it.updatedAt }, taxonomy::deleteColecoesByIds) { r ->
        r.forEach { taxonomy.upsertColecao(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncMarcas() = syncResource<TaxonomyDto>("marcas", 100, { it.updatedAt }, taxonomy::deleteMarcasByIds) { r ->
        r.forEach { taxonomy.upsertMarca(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncGruposProdutos() = syncResource<TaxonomyDto>("grupos-produtos", 100, { it.updatedAt }, taxonomy::deleteGruposProdutosByIds) { r ->
        r.forEach { taxonomy.upsertGrupoProduto(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncGeneros() = syncResource<TaxonomyDto>("generos", 100, { it.updatedAt }, taxonomy::deleteGenerosByIds) { r ->
        r.forEach { taxonomy.upsertGenero(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncLinhas() = syncResource<TaxonomyDto>("linhas", 100, { it.updatedAt }, taxonomy::deleteLinhasByIds) { r ->
        r.forEach { taxonomy.upsertLinha(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncFamiliasComerciais() = syncResource<TaxonomyDto>("familias-comerciais", 100, { it.updatedAt }, taxonomy::deleteFamiliasComerciaisByIds) { r ->
        r.forEach { taxonomy.upsertFamiliaComercial(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncNichos() = syncResource<TaxonomyDto>("nichos", 100, { it.updatedAt }, taxonomy::deleteNichosByIds) { r ->
        r.forEach { taxonomy.upsertNicho(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncEspecies() = syncResource<TaxonomyDto>("especies", 100, { it.updatedAt }, taxonomy::deleteEspeciesByIds) { r ->
        r.forEach { taxonomy.upsertEspecie(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncTiposProdutos() = syncResource<TaxonomyDto>("tipos-produtos", 100, { it.updatedAt }, taxonomy::deleteTiposProdutosByIds) { r ->
        r.forEach { taxonomy.upsertTipoProduto(it.id, it.idErp, it.descricao, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }

    suspend fun syncComplementos1() = syncResource<ComplementoDto>("complementos-1", 1000, { it.updatedAt }, taxonomy::deleteComplementos1ByIds) { r ->
        r.forEach { taxonomy.upsertComplemento1(it.id, it.idErp, it.descricao, it.tipoComplementoIdErp, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncComplementos2() = syncResource<ComplementoDto>("complementos-2", 1000, { it.updatedAt }, taxonomy::deleteComplementos2ByIds) { r ->
        r.forEach { taxonomy.upsertComplemento2(it.id, it.idErp, it.descricao, it.tipoComplementoIdErp, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncComplementos3() = syncResource<ComplementoDto>("complementos-3", 1000, { it.updatedAt }, taxonomy::deleteComplementos3ByIds) { r ->
        r.forEach { taxonomy.upsertComplemento3(it.id, it.idErp, it.descricao, it.tipoComplementoIdErp, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }
    suspend fun syncGradesPadroes() = syncResource<GradePadraoDto>("grades-padroes", 200, { it.updatedAt }, taxonomy::deleteGradesPadroesByIds) { r ->
        r.forEach { taxonomy.upsertGradePadrao(it.id, it.idErp, it.sequencia, it.complemento2Id, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt)) }
    }

    suspend fun syncTabelasPrecos() = syncResource<PriceTableDto>("tabelas-precos", 100, { it.updatedAt }, pricing::deletePriceTablesByIds) { r ->
        r.forEach {
            pricing.upsertPriceTable(
                it.id, it.idErp, it.descricao, it.situacao,
                it.percDesconto?.toDoubleOrNull(), it.percParceria?.toDoubleOrNull(), it.percGerencial?.toDoubleOrNull(),
                it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt),
            )
        }
    }

    suspend fun syncProdutosPre() = syncResource<ProductDto>("produtos-pre", 1000, { it.updatedAt }, catalog::deleteProductsByIds) { r ->
        r.forEach {
            catalog.upsertProduct(
                it.id, it.idErp, it.descricao, it.descricao2, it.apelido, it.unidade, it.codigoBarras, it.ncm, it.situacao,
                SfaParse.parseCents(it.precoBase), SfaParse.parseCents(it.precoFinal), SfaParse.parseLong(it.listaMultiploVenda),
                it.tipoComplemento1, it.tipoComplemento2, it.tipoComplemento3,
                it.categoriaId, it.colecaoId, it.marcaId, it.grupoProdutoId, it.generoId, it.linhaId,
                it.descricaoCategoria, it.descricaoColecao, it.descricaoMarca, it.descricaoGrupoProduto,
                it.sequenciaCatalogoLink?.toString(), it.createdAt, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt), it.deletedAt,
            )
        }
    }

    suspend fun syncProdutosComerciais() = syncResource<CommercialProductDto>("produtos-comerciais", 200, { it.updatedAt }, catalog::deleteCommercialByIds) { r ->
        r.forEach {
            catalog.upsertCommercial(
                it.id, it.idErp, it.produtoPreId, it.nivel, SfaParse.parseBool(it.vendaLiberada),
                SfaParse.parseLong(it.listaMultiploVenda), SfaParse.parseLong(it.multiploVenda),
                SfaParse.parseLong(it.qtdeMinimaVenda), SfaParse.parseLong(it.qtdeMinimaItemVenda), SfaParse.parseLong(it.qtdeMaximaVenda),
                SfaParse.parseCents(it.valorMinimoVenda), SfaParse.parseBool(it.permiteSortir),
                it.percDesconto?.toDoubleOrNull(), it.dataValidadeDesconto, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt),
            )
        }
    }

    suspend fun syncItensPrecos() = syncResource<ProductPriceDto>("itens-tabelas-precos-pre", 1000, { it.updatedAt }, pricing::deleteProductPricesByIds) { r ->
        r.forEach {
            val gradeJson = it.listaGrade?.let { g -> json.encodeToString(g) }
            pricing.upsertProductPrice(
                it.id, it.tabelaPrecoId, it.tabelaPrecoIdErp, it.produtoPreId, it.produtoIdErp,
                SfaParse.parseCents(it.preco), it.prazoMedio, it.agrupamento, gradeJson,
                it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt),
            )
        }
    }

    suspend fun syncPrazos() = syncResource<PrazoDto>("prazos", 100, { it.updatedAt }, pricing::deletePrazosByIds) { r ->
        r.forEach {
            pricing.upsertPrazo(
                it.id, it.idErp, it.descricao, it.parcelas, it.tipo, it.prazoMedio, it.diasParcelasCsv(),
                it.situacao, it.percDesconto?.toDoubleOrNull(), it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt),
            )
        }
    }

    suspend fun syncArquivos() = syncResource<AssetDto>("arquivos", 1000, { it.updatedAt }, assets::deleteAssetsByIds) { r ->
        r.forEach {
            assets.upsertAsset(
                it.id, it.idErp, it.nome, it.caminhoOriginal, it.caminhoMedia, it.caminhoDetail, it.caminhoThumb,
                it.tipo, it.tipoMime, it.sequencia, it.situacao, it.complemento1IdErp, it.produtoIdErp,
                it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt), it.deletedAt,
            )
        }
    }

    suspend fun syncTiposVendas() = syncResource<SaleTypeDto>("tipos-vendas", 100, { it.updatedAt }, pricing::deleteSaleTypesByIds) { r ->
        r.forEach {
            pricing.upsertSaleType(
                it.id, it.idErp, it.descricao, SfaParse.parseBool(it.geraComissao), SfaParse.parseBool(it.geraContasReceber),
                SfaParse.parseLong(it.parcelaMinima), SfaParse.parseBool(it.baixaEstoque), it.origemPedido,
                it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt),
            )
        }
    }

    suspend fun syncVendedores() = syncResource<SellerDto>("vendedores", 200, { it.updatedAt }, people::deleteSellersByIds) { r ->
        r.forEach {
            people.upsertSeller(it.id, it.idErp, it.nomeFantasia, it.razaoSocial, it.email, it.telefone, it.situacao, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt))
        }
    }

    suspend fun syncClientes() = syncResource<ClientDto>("clientes", 1000, { it.updatedAt }, people::deleteClientsByIds) { r ->
        r.forEach {
            people.upsertClient(
                it.id, it.idErp, it.vendedorIdErp, it.cidadeIdErp, it.situacao, it.nomeFantasia, it.razaoSocial, it.cpfCnpj,
                it.endereco, it.bairro, it.cep, it.telefone, it.celular, it.email, it.contato,
                it.tabelaPrecoId, it.prazoId, it.percDesconto?.toDoubleOrNull(), it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt), it.deletedAt,
            )
        }
    }

    suspend fun syncVendedoresClientes() = syncResource<SellerClientDto>("vendedores-clientes", 1000, { it.updatedAt }, people::deleteSellerClientsByIds) { r ->
        r.forEach {
            people.upsertSellerClient(it.id, it.vendedorIdErp ?: "", it.clienteIdErp ?: "", it.vendedorId, it.clienteId, it.updatedAt, SfaParse.parseIsoToMs(it.updatedAt))
        }
    }

    private suspend inline fun <reified T> syncResource(
        resource: String,
        perPage: Int,
        crossinline updatedAtOf: (T) -> String?,
        crossinline deleteByIds: (List<Long>) -> Unit,
        crossinline upsertAll: (List<T>) -> Unit,
    ): SyncEntityResult = withContext(Dispatchers.Default) {
        val state = sync.selectSyncState(resource).executeAsOneOrNull()
        val cursor = state?.cursorUpdatedAt
        sync.upsertSyncState(resource, cursor, state?.lastSyncAtMs, 0, 1, null)
        var page = 1
        var maxUpdated: String? = cursor
        var processed = 0
        var skipped = 0
        while (true) {
            val result = sfaApi.fetchPageRaw(resource, page, perPage, cursor)
            when (result) {
                is SfaApiResult.Fail -> {
                    sync.upsertSyncState(resource, cursor, state?.lastSyncAtMs, page.toLong(), 0, "${result.code}: ${result.message}")
                    return@withContext SyncEntityResult(resource, processed, false, result.message, skipped)
                }
                is SfaApiResult.Ok -> {
                    val env = result.value
                    val rows = ArrayList<T>(env.data?.size ?: 0)
                    env.data?.forEach { element ->
                        val parsed = runCatching { json.decodeFromJsonElement<T>(element) }.getOrNull()
                        if (parsed != null) rows.add(parsed) else skipped++
                    }
                    val deleted = env.deletedIds.orEmpty()
                    db.transaction {
                        if (rows.isNotEmpty()) upsertAll(rows)
                        if (deleted.isNotEmpty()) deleted.chunked(900).forEach { deleteByIds(it) }
                    }
                    processed += rows.size
                    rows.forEach { row ->
                        val u = updatedAtOf(row)
                        if (u != null && (maxUpdated == null || u > maxUpdated!!)) maxUpdated = u
                    }
                    val pag = env.pagination
                    if (pag == null || pag.currentPage >= pag.lastPage) break
                    page++
                }
            }
        }
        val now = Clock.System.now().toEpochMilliseconds()
        sync.upsertSyncState(resource, maxUpdated, now, 0, 0, null)
        SyncEntityResult(resource, processed, true, null, skipped)
    }
}
