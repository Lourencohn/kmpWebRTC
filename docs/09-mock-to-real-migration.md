# 09 — Migração de dados mockados → dados reais da API

> Guia operacional para continuar substituindo os dados de `data/sample/*` pelos dados reais já sincronizados no SQLite local. **Embasado em investigação ao vivo do app rodando no emulador** (`adb`, empresa **2507**, usuário `atendimento@trovata.com.br`), em 2026-05-27.
>
> Pré-requisitos já entregues: autenticação real (Keycloak), seleção de empresa, e sincronização incremental (`CatalogSyncService`). Ver [03-stack](03-stack-kmp-webrtc.md), [04-architecture](04-architecture.md) e [08-api-data-model](08-api-data-model.md).

---

## 1. Estado atual (verificado no device)

Login real e sync **funcionam**. Após logar e escolher a empresa **2507**, o `syncEssentials()` rodou e populou o banco local (`/data/data/app.trovata.cast/databases/trovatacast.db`):

| Tabela | Linhas | Observação |
|---|---:|---|
| `ProductEntity` (produtos-pre) | **979** | preço final e marca preenchidos |
| `ProductPriceEntity` (itens-preços) | **972** | tabela `1` = "TABELA PADRAO" |
| `PrazoEntity` | (a confirmar) | ✅ bug de desserialização corrigido (§5.1) — re-sincronizar p/ popular |
| `ClientEntity` | **24.444** | base real de clientes |
| `CategoriaEntity` | 35 | |
| `MarcaEntity` | 41 | ex. "3 CORAÇÕES", "RB AMORE" |
| `PriceTableEntity` | 1 | só "TABELA PADRAO" (id 1) |
| `Complemento1/2/3Entity` | 1 / 1 / 2 | grade trivial ("ÚNICO") |
| `CommercialProductEntity` | **0** | ⚠️ vazio p/ esta empresa → MOQ indisponível |
| `ColecaoEntity` | **0** | ⚠️ sem coleção p/ esta empresa |
| `PrazoEntity` | **0** | ❌ **sync falhou** (ver §5.1) |
| `SellerEntity` / `SellerClientEntity` | 0 / 0 | não estão no `syncEssentials` |

> **Conclusão:** os dados reais já estão no device. O problema das telas é **de leitura**: quase todas as telas ainda leem de `data/sample/*` diretamente, não dos repositórios. A empresa 2507 é uma distribuidora de alimentos — os produtos reais são "3 CORAÇÕES Frisco", "Cocada Cremosa" etc. (o catálogo mostrado nas telas, "Atelier Norte / Verão 26 / bolsas", é **100% mock**).

Identidade real disponível em `AuthRepository`: `user` = `{ id: 1, name: "TROVATA", email: "atendimento@trovata.com.br" }`; empresa ativa = `2507`. As telas ainda mostram "Camila Tavares / Atelier Norte" porque leem `SampleAccount`.

---

## 2. O que já está real vs. mock

| Camada | Status |
|---|---|
| Autenticação (login, refresh, logout) | ✅ real |
| Seleção de empresa | ✅ real |
| Sync incremental (catálogo, preços, clientes, taxonomia) | ✅ real |
| `CatalogRepository` / `ClientsRepository` (leitura) | ✅ prontos, com fallback p/ mock |
| Identidade global (usuário + empresa ativa) | ✅ `AuthRepository.activeCompany` persistida; alimenta Conta + header do Catálogo |
| **Prep / picker de catálogo** (`CatalogPickerScreen`) | ✅ ligada (`catalogRepository.uiProducts()`) |
| **Catálogo (tab)** (`CatalogScreen` + `CatalogScreenModel`) | ✅ ligada — 979 produtos reais, grade lazy, KPIs derivados (SKUs/marcas/categorias/com preço); `ProductDetailRoute` resolve via repo |
| **Conta** (`AccountScreen` + `AccountScreenModel`) | ✅ identidade real (nome/email do usuário, empresa ativa); performance/tier ainda mock (sem fonte) |
| **Clientes** (`ClientsScreen` + `ClientsScreenModel`) | ✅ busca local paginada (`searchClients`/`firstClients`, LIMIT 60); LTV/sparkline/segmentos/"sugestões TC" removidos (sem fonte) |
| **LiveCall** (`LiveCallScreen` + `LiveCallScreenModel`) | ✅ catálogo da sessão via `CatalogRepository.snapshotForRefs` (produtos selecionados) ou `snapshot`; nome/preço/total resolvidos por `priceCentsByRef`; sem `SampleCatalog` |
| **Sessões** (`SellerHomeScreen` + `SessionsViewModel`) | ✅ sessões recentes (`SessionsRepository.observeAll`) + pedidos fechados hoje (`OrderRepository`) + header empresa/usuário; agenda/incoming/prep mockados removidos do fluxo (estados vazios honestos) |
| **Insights** (`InsightsScreen` + `InsightsScreenModel`) | ✅ derivado de `OrderEntity`: faturamento do mês, delta MoM, ticket, itens, pedidos, sessões, top produtos + sparkline diário; funil/foco/"insight da semana" removidos (sem fonte); estado vazio |
| **Conta** (`AccountScreen` + `AccountScreenModel`) | ✅ performance derivada de pedidos do mês + contagem real de clientes; tier/"meses na rede"/swatches de marca/linhas fake removidos |
| **ProductDetail** | ✅ "Veja também" recebe produtos reais (`uiPage`/`state.products`); `PerformanceCard` (engajamento fake) removido. ⚠️ estoque-por-tamanho e swatches de cor ainda gerados localmente (sem fonte na API — ver §10) |
| Onboarding `AuthSetupScreen`/`AuthVerifyScreen` | ⚠️ órfãos (fora do grafo de navegação; login real é Keycloak via `AuthWelcome→AuthLogin→CompanySelection`). Mantidos como dev-only, não apresentados |

---

## 3. Receita para ligar uma tela (padrão a seguir)

O padrão já aplicado em [CatalogPickerScreenModel.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/feature/catalog/CatalogPickerScreenModel.kt) e [CatalogPickerScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/prep/CatalogPickerScreen.kt):

1. **Repositório expõe modelo limpo + fallback gated.** Ex.: `CatalogRepository.uiProducts()` retorna `SampleCatalog.products` quando o cache está vazio (pré-sync) e dados reais caso contrário. Nunca remover o mock — ele vira fallback.
2. **ScreenModel (Voyager) lê do repositório**, não do `Sample*`. Injete o repositório via `AppContainerHolder.current`. Use `StateFlow` + `observe...()` (reativo) ou `snapshot()` em `init`.
3. **A tela (Screen) constrói o ScreenModel** com `rememberScreenModel { ... }` e passa `state` + handlers ao Composable de corpo (que permanece "burro").
4. **Mantenha os nomes de campo do modelo de UI** (`ref`, `name`, `price`, `moq`, `sizes`, `colorCount`) para minimizar mudança visual — `CatalogProduct.toUiProduct()` já faz essa ponte.

Repositórios disponíveis (em [data/local/](../composeApp/src/commonMain/kotlin/app/trovata/cast/data/local/)):
- `CatalogRepository` → `observeCatalog(priceTableId)`, `uiProducts(priceTableId)`, `snapshot(priceTableId)`, `isEmpty()`. Modelo: `CatalogProduct(id, ref, name, priceCents, moq, sizes, colorCount, colecao, marca, categoria)`.
- `ClientsRepository` → `observeClients()`, `forSeller(erp)`, `byId(id)`, `isEmpty()`. Modelo: `CatalogClient(id, ref, name, legalName, document, city, phone, email, contact, priceTableId, sellerErp, active)`.
- `AuthRepository` → `user: StateFlow<AuthUser?>`, `companies`, `empresaId()`.

> **Preço:** passe um `priceTableId` para usar `ProductPriceEntity`. Para a empresa 2507 só existe a tabela `1`. Quando não há tabela/preço, `CatalogRepository` cai em `ProductEntity.precoFinalCents` (que **está preenchido** — ex. `14400` = R$ 144,00). Em sessão, o ideal é usar `ClientEntity.tabelaPrecoId` do cliente da sessão.

---

## 4. Inventário por tela (o que falta)

### 4.1 Catálogo — tab principal ✅ (ligado)
- **Arquivos:** [CatalogScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/catalog/CatalogScreen.kt), [ProductDetailScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/catalog/ProductDetailScreen.kt), componentes [ProductCard.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/components/ProductCard.kt)/[ProductRow.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/components/ProductRow.kt), e [Routes.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/navigation/Routes.kt) (`ProductDetailRoute`).
- **Mock:** lê `SampleCatalog.products` direto (sem ScreenModel).
- **Fonte real:** `CatalogRepository` (979 produtos prontos). **Maior ganho, dados 100% disponíveis.**
- **Falta:** criar `CatalogScreenModel` (observar `observeCatalog`), trocar `SampleCatalog.products` → state; imagens reais via recurso `arquivos` (ver §8 — a suposição inicial de "API não tem imagem" estava errada); header "ATELIER NORTE · VERÃO 26" → nome da empresa + coleção (coleção vazia p/ 2507 → ocultar). KPIs "8 SKUs / 3 estreias / 1 pré-venda" são mock (derivar de contagem real ou ocultar). `ProductDetailRoute` deve buscar via repo, não `SampleCatalog`.

### 4.2 Clientes ✅ (ligado)
- **Arquivo:** [ClientsScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/clients/ClientsScreen.kt).
- **Mock:** `SampleClients` (Diego/Renata/Paulo, LTV, sparkline, segmentos Recentes/Top/Atenção/Novos, "prontos para abordar").
- **Fonte real:** `ClientsRepository` (24.444 clientes prontos: nome, razão social, doc, cidade, contato).
- **Falta:** `ClientsScreenModel` observando `observeClients()`. **Sem fonte na API:** LTV, sparkline de atividade, segmentação, "ao vivo agora", "sugestões TC". Decidir: ocultar/calcular localmente (de `OrderEntity`) ou manter como mock claramente rotulado. Considerar paginação/busca local (são 24k linhas).

### 4.3 Conta ✅ (ligado)
- **Arquivos:** [AccountScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/account/AccountScreen.kt), [AccountChip.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/components/AccountChip.kt).
- **Mock:** `SampleAccount` ("Camila Tavares", "camila@ateliernorte.com.br", "Atelier Norte", Top 3%) + `SampleAuth`.
- **Fonte real:** `AuthRepository.user` (nome/email reais) + `Company` selecionada (marca/empresa). Iniciais do avatar ("CT") → derivar do `user.name`.
- **Falta:** ligar perfil + marca representada à empresa ativa. **Sem fonte API:** "14 meses na rede", "Top 3%", performance (vendas/sessões/conversão) — mock ou derivar de orders.

### 4.4 Sessões (home) ❌
- **Arquivos:** [SessionsViewModel.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/feature/sessions/SessionsViewModel.kt) → `SellerHomeScreen`.
- **Mock:** `SampleSessions` (agenda de hoje, próximas, histórico, "aguardando você").
- **Natureza:** sessões de co-presença são **domínio do próprio app** (não existem na API SFA). O que dá para tornar real: o **cliente** de cada sessão (via `ClientsRepository`), e o header "Atelier Norte · Outono 26" (empresa + coleção). A agenda em si precisa de um modelo de sessões persistido localmente (`SessionEntity` já existe, hoje vazio) — escopo de produto, não de API.

### 4.5 Insights ❌
- **Arquivo:** [InsightsScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/insights/InsightsScreen.kt).
- **Mock:** `SampleInsights` (vendas R$142k, conversão, ticket, tempo em foco).
- **Sem fonte na API SFA.** Só fará sentido quando houver pedidos reais (`OrderEntity`) e logs de sessão. Manter mock por ora, claramente rotulado.

### 4.6 LiveCall (sessão ao vivo) ❌
- **Arquivos:** [LiveCallScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/call/LiveCallScreen.kt), [LiveCallScreenModel.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/feature/call/LiveCallScreenModel.kt).
- **Mock:** `SampleCatalog` (resolve nome/preço por `ref`).
- **Fonte real:** `CatalogRepository` — durante a sessão, resolver produto/preço pela tabela do cliente (`ClientEntity.tabelaPrecoId`).

### 4.7 DesignSystemScreen (preview) — **deixar mock** (tela de desenvolvimento).

---

## 5. Pendências técnicas encontradas no device

### 5.1 ✅ Bug corrigido: sync de `prazos` falhava na desserialização
`SyncStateEntity` registrava para `prazos`:
```
network_error: Unexpected JSON token at offset ...: Unexpected symbol '.' in numeric literal
... "prazo_medio":17.5 ...
```
**Causa:** [PricingDto.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/data/remote/sfa/dto/PricingDto.kt) declarava `PrazoDto.prazoMedio: Long?`, mas a API retorna **decimal** (`17.5`). Um único registro inválido derrubava a **página inteira** → `PrazoEntity` ficava com 0 linhas.
**Correção aplicada:** `prazoMedio` agora é `Double?` no DTO e `PrazoEntity.prazoMedio` é `REAL` ([Pricing.sq](../composeApp/src/commonMain/sqldelight/app/trovata/cast/db/Pricing.sq)). Falta re-sincronizar no device para confirmar a contagem de linhas. Ainda **pendente** a robustez por-registro (§5.2): hoje um registro ruim em outra entidade ainda derruba a página.

### 5.2 ✅ Robustez: decode resiliente por-registro
`SfaApi.fetchPageRaw` agora retorna `SfaListEnvelope<JsonElement>` e `CatalogSyncService.syncResource` decodifica **cada registro** via `json.decodeFromJsonElement<T>` dentro de `runCatching` — um registro inválido é contado em `SyncEntityResult.skipped` e **pulado**, sem zerar a página/entidade. `paginação` e `deleted_ids` continuam decodificados normalmente.

### 5.3 ⚠️ `produtos-comerciais` e `colecoes` vazios para a empresa 2507
- `CommercialProductEntity = 0` → **MOQ indisponível**; `CatalogRepository` cai em `listaMultiploVenda`/`1`. Confirmar se outras empresas têm comerciais; senão, MOQ via `produtos-pre.lista_multiplo_venda`.
- `ColecaoEntity = 0` → não há rótulo de coleção; o header não deve assumir coleção.

### 5.4 Header e identidade ainda mock
"ATELIER NORTE · VERÃO 26" e "Camila Tavares" aparecem em quase todas as telas. Centralizar um provedor de identidade (empresa ativa + usuário) e consumir nos headers.

---

## 6. Ordem recomendada (próximos diálogos)

1. ✅ **Fix `prazos` (§5.1)** — DTO `Double?` + coluna `REAL`. Falta re-sincronizar no device.
2. ✅ **Catálogo (tab)** — `CatalogScreenModel` + grade lazy + `ProductDetailRoute` via repo.
3. ✅ **Conta** — `AccountScreenModel` lê `AuthRepository.user` + `activeCompany` (remove "Camila Tavares").
4. ✅ **Clientes** — `ClientsScreenModel` com busca local (`searchClients`, LIMIT 60); campos sem fonte (LTV/sparkline/segmentos/"sugestões TC") **removidos** em vez de mockados.
5. ✅ **Header/identidade** global — `AuthRepository.activeCompany` persistida e consumida pelo Catálogo e Conta. Coleção fica oculta enquanto `ColecaoEntity` estiver vazia (empresa 2507).
6. ✅ **LiveCall** — catálogo da sessão (selecionados ou snapshot) + nome/preço/total reais. Preço por `priceTableId` suportado no `LiveCallScreenModel` (hoje passado `null` → cai em `precoFinalCents`; falta propagar a tabela do cliente da sessão — ver §10).
7. ✅ **Sessões / Insights / Conta / ProductDetail** — ligados a `OrderEntity`/`SessionEntity`/`ClientsRepository` + identidade. Campos sem fonte removidos.

Pendências técnicas ainda abertas: **§5.3** (MOQ/coleção vazios p/ 2507), e os itens de §10.

> **Decisão de design (Clientes):** como `CatalogClient` não tem LTV/atividade/segmento e a API não fornece, a tela foi redesenhada para uma **lista buscável honesta** (nome, razão social/CNPJ, contato, telefone, status ativo) com ação "Convidar" → `CatalogPickerScreen`. O fallback mock pré-sync vira o estado vazio "Nenhum cliente encontrado" (sem clientes fictícios). `SampleClients.kt` permanece no repo, sem uso na tela.

Sempre seguir a receita do §3 (repositório → ScreenModel → fallback gated) e nunca apagar os `Sample*` (viram fallback pré-sync).

---

## 7. Como reinvestigar o banco local (para próximos diálogos)

```bash
PKG=app.trovata.cast
# device tem que estar logado/sincronizado
adb exec-out run-as $PKG cat databases/trovatacast.db > /tmp/trovatacast.db
# inspeção (não há sqlite3 no emulador; usar python local)
python3 - <<'PY'
import sqlite3; c=sqlite3.connect('/tmp/trovatacast.db').cursor()
for t in [r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")]:
    print(t, c.execute(f"SELECT count(*) FROM {t}").fetchone()[0])
for r in c.execute("SELECT entity,lastError FROM SyncStateEntity"): print(r)
PY
# empresa/usuário selecionados:
adb exec-out run-as $PKG cat shared_prefs/trovatacast.auth.xml
```

Disparar sync manualmente (após login) é automático via `AppContainer.startCatalogSync()`; para forçar tudo (inclusive `vendedores-clientes`), chamar `catalogSyncService.syncAll()`.

---

## 8. Imagens reais de produto (assets / `arquivos`) ✅

Mapeado a partir do app `trovata-offline` (que já busca as imagens) e implementado no TrovataCast.

- **Endpoint:** `GET {base}/empresas/{empresa}/arquivos?page&per_page=100` (recurso **`arquivos`**). Não aparece na coleção Postman `API.json` (incompleta), mas o offline usa exatamente esse caminho.
- **Payload (`AssetDto`):** `id`, `id_erp`, `produto_id_erp`, `complemento_1_id_erp`, `sequencia`, `caminho_thumb`, `caminho_detail`, `caminho_media`, `caminho_original`, `situacao`, `updated_at`, `deleted_at`. Os `caminho_*` já são **URLs completas/públicas** (sem header de auth — passadas direto ao image loader).
- **Vínculo:** asset → produto por `produto_id_erp = ProductEntity.idErp` (ordenar por `sequencia`).
- **Implementação:**
  - DTO [AssetDto.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/data/remote/sfa/dto/AssetDto.kt); tabela [Assets.sq](../composeApp/src/commonMain/sqldelight/app/trovata/cast/db/Assets.sq) (`AssetEntity` + `assetsQueries`).
  - Sync `syncArquivos()` em [CatalogSyncService.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/data/sync/CatalogSyncService.kt) (no `syncEssentials` e no `ORDER`).
  - `CatalogRepository`: `assemble()` resolve uma thumb por produto (`selectThumbsForProducts`, primeiro por `sequencia`) → `CatalogProduct.imageUrl`; `gallery(idErp)` retorna a lista para o detalhe.
  - UI: **Coil 3** (`coil-compose` + `coil-network-ktor3`), `ImageLoader` singleton em [App.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/App.kt). `ProductCard` e `ProductDetailScreen` usam `AsyncImage(imageUrl)`; fallback: imagem local (sample) → `Garment`.
- **Ordem de fallback de URL:** card = `thumb ?: detail ?: media`; galeria do detalhe = `detail ?: media ?: original ?: thumb`.
- **Pendente:** confirmar no device que `arquivos` popula (e que a empresa logada tem assets); variações por cor via `complemento_1_id_erp` ainda não são usadas (mostramos 1 imagem por asset/sequência).

## 9. Paginação do catálogo (páginas numeradas) ✅

Antes ambas as telas carregavam os 979 produtos numa lista única ("scroll infinito"). Agora paginam por **páginas numeradas** (Anterior / "Página X de Y" / Próxima):

- **Aba Catálogo** ([CatalogScreenModel.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/feature/catalog/CatalogScreenModel.kt)): `pageSize = 24`, carrega via `CatalogRepository.page(limit, offset)`; reage à contagem via `observeCount()` (Flow) → repopula ao terminar o sync. KPIs do header vêm de contagens reais (`stats()`: marcas/categorias/com preço) em vez de varrer todos os produtos. "Em destaque" só na página 1.
- **Montar catálogo** ([CatalogPickerScreenModel.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/feature/catalog/CatalogPickerScreenModel.kt)): `pageSize = 30`, `countProducts()` + `uiPage(limit, offset)`. A seleção persiste entre páginas guardando `selectedProducts: Map<ref, Product>`, então o contador de SKUs soma corretamente itens escolhidos em páginas diferentes.
- Queries: `selectProductsPaged(LIMIT, OFFSET)` + `countDistinctMarcas/Categorias` + `countPricedProducts` em [Catalog.sq](../composeApp/src/commonMain/sqldelight/app/trovata/cast/db/Catalog.sq).
- **Nota:** filtros (Novos/Top/Pré-venda) operam só sobre a página atual; com dados reais (sem `tag`) apenas "Todos" tem efeito — comportamento pré-existente.

## 10 — Rodada "telas restantes → dados reais" ✅

Concluída a migração das telas que ainda liam `data/sample/*` para dados reais (build Android + `compileKotlinMetadata` + testes unitários verdes; 25 testes passam). Resumo do que mudou:

- **API resiliente (§5.2)** — `fetchPageRaw` + decode por-registro; um registro ruim é pulado (`SyncEntityResult.skipped`), não zera a página.
- **LiveCall** — `LiveCallScreenModel` injeta `CatalogRepository` + `SessionsRepository`; mostra os produtos selecionados da sessão (`snapshotForRefs`) ou o catálogo (`snapshot`); `CartLineUi`/total/summary usam `priceCentsByRef` reais. Header usa `collectionLabel` da sessão.
- **Sessões** — `SellerHomeScreen` reescrita: header (empresa+usuário), "Pedidos fechados hoje" (`OrderRepository`), "Sessões recentes" (`SessionsRepository.observeAll`), estado vazio honesto. `SessionsViewModel` combina sessões+pedidos+identidade. Agenda/`nowWaiting`/`IncomingCall`/`SessionPrep` mockados saíram do fluxo (telas demo permanecem inertes).
- **Insights** — novo `InsightsScreenModel` deriva tudo de `OrderEntity` (faturamento do mês, delta MoM, ticket, itens, pedidos, sessões, top-5 produtos + sparkline diário). Funil/foco-conversão/"insight da semana"/seletor de período removidos (sem fonte).
- **Conta** — `AccountScreenModel` recebe `OrderRepository`+`ClientsRepository`: performance (Vendas/Pedidos/Itens) do mês + contagem real de clientes; removidos tier, "meses na rede", swatches de marca e linhas fake.
- **ProductDetail** — "Veja também" recebe produtos reais; `PerformanceCard` (engajamento fake) removido.
- **Identidade global** — `AccountChip` (avatar em todo header de aba) e os metadados da sessão (`sellerName`/`collectionLabel` no `CatalogPickerScreen`) agora vêm de `AuthRepository.user`/`activeCompany`, não de `SampleAccount`/`SampleCatalog`.

`Sample*` que **permanecem** (legítimos, não apresentados como dados reais): fallback pré-sync (`CatalogRepository`, `CatalogScreenModel`, `CatalogPickerScreenModel`), `SampleAccount.support`/gradiente (chrome/estilo), `SampleSessions.incoming/prep` (telas demo inertes), `DesignSystemScreen` (preview), `AuthSetup/AuthVerify` (órfãos).

### Pendências desta rodada (sem fonte na API hoje)
- **Preço por tabela do cliente na LiveCall** — `LiveCallScreenModel` aceita `priceTableId`, mas `StoredSessionRecord` não guarda o `tabelaPrecoId` do cliente; hoje passa `null` (cai em `precoFinalCents`). Para usar a tabela do cliente, persistir `clientId`/`tabelaPrecoId` na sessão (`SessionEntity`) ao criar.
- **Estoque por tamanho / swatches de cor (ProductDetail)** — ainda gerados localmente (`sampleSizesFor`/`sampleSwatchesFor`); a API expõe `locais-estoques` (não sincronizado) e grade via `itens-tabelas-precos-pre`. Wiring de estoque real é trabalho à parte.
- **§5.3** — `produtos-comerciais`/`colecoes` vazios p/ empresa 2507 (MOQ/coleção indisponíveis).
