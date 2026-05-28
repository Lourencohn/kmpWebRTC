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
| **Tela "Prep / picker de catálogo"** (`CatalogPickerScreen`) | ✅ **única tela já ligada** (via `loadProducts = { catalogRepository.uiProducts() }`) |
| Telas Sessões, Catálogo (tab), Clientes, Insights, Conta, LiveCall | ❌ ainda leem `data/sample/*` |

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

### 4.1 Catálogo — tab principal ❌
- **Arquivos:** [CatalogScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/catalog/CatalogScreen.kt), [ProductDetailScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/catalog/ProductDetailScreen.kt), componentes [ProductCard.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/components/ProductCard.kt)/[ProductRow.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/components/ProductRow.kt), e [Routes.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/navigation/Routes.kt) (`ProductDetailRoute`).
- **Mock:** lê `SampleCatalog.products` direto (sem ScreenModel).
- **Fonte real:** `CatalogRepository` (979 produtos prontos). **Maior ganho, dados 100% disponíveis.**
- **Falta:** criar `CatalogScreenModel` (observar `observeCatalog`), trocar `SampleCatalog.products` → state; resolver imagem (API **não tem imagem** → manter placeholder/`FashionPalette`); header "ATELIER NORTE · VERÃO 26" → nome da empresa + coleção (coleção vazia p/ 2507 → ocultar). KPIs "8 SKUs / 3 estreias / 1 pré-venda" são mock (derivar de contagem real ou ocultar). `ProductDetailRoute` deve buscar via repo, não `SampleCatalog`.

### 4.2 Clientes ❌
- **Arquivo:** [ClientsScreen.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/clients/ClientsScreen.kt).
- **Mock:** `SampleClients` (Diego/Renata/Paulo, LTV, sparkline, segmentos Recentes/Top/Atenção/Novos, "prontos para abordar").
- **Fonte real:** `ClientsRepository` (24.444 clientes prontos: nome, razão social, doc, cidade, contato).
- **Falta:** `ClientsScreenModel` observando `observeClients()`. **Sem fonte na API:** LTV, sparkline de atividade, segmentação, "ao vivo agora", "sugestões TC". Decidir: ocultar/calcular localmente (de `OrderEntity`) ou manter como mock claramente rotulado. Considerar paginação/busca local (são 24k linhas).

### 4.3 Conta ❌
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

### 5.1 ❌ Bug: sync de `prazos` falha na desserialização
`SyncStateEntity` registrou para `prazos`:
```
network_error: Unexpected JSON token at offset ...: Unexpected symbol '.' in numeric literal
... "prazo_medio":17.5 ...
```
**Causa:** [PricingDto.kt](../composeApp/src/commonMain/kotlin/app/trovata/cast/data/remote/sfa/dto/PricingDto.kt) declara `PrazoDto.prazoMedio: Long?`, mas a API retorna **decimal** (`17.5`). Um único registro inválido derruba a **página inteira** → `PrazoEntity` fica com 0 linhas.
**Correção:** mudar `prazoMedio` para `Double?` (e ajustar `PrazoEntity.prazoMedio` para `REAL`, ou arredondar na ingestão). Revisar outros campos numéricos que possam vir decimais (`parcela_minima`, `perc_*` já são `String?`, ok).

### 5.2 ⚠️ Robustez: um registro ruim derruba a página toda
O decode é por página (`SfaListEnvelope<T>`). Vale tornar o mapeamento resiliente por-registro (decodificar como `JsonElement` e mapear item a item dentro de try/catch), de modo que um campo inesperado não zere a entidade inteira.

### 5.3 ⚠️ `produtos-comerciais` e `colecoes` vazios para a empresa 2507
- `CommercialProductEntity = 0` → **MOQ indisponível**; `CatalogRepository` cai em `listaMultiploVenda`/`1`. Confirmar se outras empresas têm comerciais; senão, MOQ via `produtos-pre.lista_multiplo_venda`.
- `ColecaoEntity = 0` → não há rótulo de coleção; o header não deve assumir coleção.

### 5.4 Header e identidade ainda mock
"ATELIER NORTE · VERÃO 26" e "Camila Tavares" aparecem em quase todas as telas. Centralizar um provedor de identidade (empresa ativa + usuário) e consumir nos headers.

---

## 6. Ordem recomendada (próximos diálogos)

1. **Fix `prazos` (§5.1)** — 1 linha de DTO; destrava o sync completo.
2. **Catálogo (tab)** — maior ganho, dados 100% prontos (979 produtos). Criar `CatalogScreenModel` + `ProductDetailRoute` real.
3. **Conta** — ligar `AuthRepository.user` + empresa ativa (remove "Camila Tavares").
4. **Clientes** — `ClientsScreenModel` com busca/paginação local (24k linhas); decidir o destino dos campos sem fonte (LTV/segmentos).
5. **Header/identidade** global (empresa + coleção).
6. **LiveCall** — preço pela tabela do cliente.
7. **Sessões / Insights** — dependem de domínio próprio (sessões/pedidos) — tratar quando houver `OrderEntity`/`SessionEntity` reais.

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
