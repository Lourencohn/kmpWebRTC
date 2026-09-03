# 08 — Modelo de dados da API SFA e cache local

> Como o TrovataCast consome a API SFA/ERP real e como esse dado é espelhado no SQLite local (SQLDelight) para substituir os mocks de `data/sample/`.

---

## 1. Visão geral

O app opera em **modo online**: busca dados de catálogo, preços e pessoas da API SFA/ERP e mantém um **cache local SQLite** que espelha fielmente a API. As telas leem do cache (reativo, via `StateFlow`/`Flow`), e um serviço de sincronização atualiza o cache de forma incremental.

| Item | Valor |
|---|---|
| Base URL | `https://api-int.trovata.app.br` |
| Multi-tenant | path `/empresas/{empresa_id}/...` — MVP usa `empresa_id = 97` |
| Auth (v2) | Keycloak JWT (`Authorization: Bearer <token>`) |
| Keycloak | `https://login.trovata.app.br` · realm `Base` · client `front-client` |
| v1 vs v2 | `/empresas/...` é público; `/v2/empresas/...` exige JWT. Mesmos payloads. |
| Ambiente de staging | `https://api-int-staging.trovata.app.br` **não publica a v1**: `/empresas/{id}/{recurso}` responde 404 e só `/v2/...` responde. O app usa a v2 nos dois ambientes. |
| Empresa do ambiente | **BUBA** (puericultura/brinquedos) — o schema é **agnóstico de categoria** |

Credenciais e tokens de exemplo ficam em `.env` (não versionar segredos reais).

### Autenticação

```
POST {keycloak}/realms/Base/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
grant_type=password&client_id=front-client&username=...&password=...
→ { access_token, refresh_token, expires_in (~300s), ... }
```

O token expira em ~5 min; o cliente HTTP deve renovar (via `refresh_token`) antes/entre páginas de uma sincronização longa.

---

## 2. Envelope comum e sincronização incremental

**Todo endpoint de lista** retorna o mesmo envelope:

```json
{
  "data": [ { "id": 222, "...": "..." } ],
  "deleted_ids": [ 14, 15 ],
  "pagination": { "current_page": 1, "last_page": 17, "per_page": 10, "from": 1, "to": 10, "total": 168 }
}
```

- `data` pode ser `null` (= página vazia; ainda processar `deleted_ids`).
- Query params: `page`, `per_page` (máx 100), `updated_at` (`YYYY-MM-DD HH:MM:SS`).
- **Modelo de sync delta**: guardar o maior `updated_at` visto como cursor; na próxima rodada enviar `updated_at=<cursor>` para buscar só o que mudou, e remover localmente os `deleted_ids`.

Campos comuns por entidade: `id` (PK numérica), `id_erp` (chave de negócio, string), às vezes `id_old`, e `created_at`/`updated_at`/`deleted_at` (ISO-8601, `deleted_at` = soft delete).

---

## 3. Entidades do MVP

Escopo MVP: **catálogo, preços e pessoas**. Geografia (países/estados/cidades), classificações fiscais, configurações de sistema e mapeamentos de usuário ficam de fora por ora.

### 3.1 Catálogo

O catálogo é normalizado em três níveis. **Não existe endpoint `produtos` (singular) — retorna 404.** O mestre é `produtos-pre`.

#### `produtos-pre` — produto base / mestre descritivo (~2126)
Onde vivem nome, código de barras e os **eixos da grade**.

| Campo | Tipo | Notas |
|---|---|---|
| `id` / `id_erp` | int / str | PK / chave ERP |
| `descricao` | str | **nome do produto** |
| `descricao_2`, `descricao_3`, `apelido` | str | descrições auxiliares |
| `abreviatura_unidade` | str | ex. `UN` |
| `codigo_barras` | str | EAN |
| `ncm`, `situacao` | str | fiscal / `A`=ativo |
| `preco_base` / `preco_custo` / `preco_final` | str\|null | **geralmente null** — preço real vem de `itens-tabelas-precos-pre` |
| `lista_multiplo_venda` | str | múltiplo de venda ex. `"6"` |
| `descricao_tipo_complemento_1/2/3` | str | **nomes dos eixos da grade**: ex. `COR`, `TAMANHO` |
| `categoria_id`, `colecao_id`, `marca_id`, `grupo_produto_id`, `genero_id`, `linha_id`, `nicho_id`, `familia_comercial_id` | int\|null | FKs de taxonomia |
| `descricao_categoria/_colecao/_marca/_grupo_produto/...` | str\|null | **labels denormalizados** (exibir offline sem join) |

> **Gap: não há campo de imagem.** O catálogo visual usa placeholder/tint por ora. Backlog: confirmar endpoint de mídia ou convenção de CDN.

#### `produtos-comerciais` — variante vendável (~1510)
Regras comerciais e de quantidade (MOQ).

| Campo | Tipo | Notas |
|---|---|---|
| `id` / `id_erp` | int / str | |
| `produto_pre_id` | int | FK → `produtos-pre` |
| `nivel` | int | nível na hierarquia (ex. 3) |
| `venda_liberada` | `YES`/`NO` | disponível para venda |
| `qtde_minima_venda` | num\|null | **MOQ** |
| `multiplo_venda`, `lista_multiplo_venda` | num/str | múltiplo |
| `qtde_minima_item_venda`, `qtde_maxima_venda`, `valor_minimo_venda` | num\|null | limites |
| `permite_sortir`, `qtde_min/max_sortir...` | | sortimento de grade |
| `perc_desconto`, `data_validade_desconto` | num/str | desconto |
| FKs `categoria_id/colecao_id/marca_id/tipo_produto_id/especie_id` | int\|null | em geral null (vivem no pré) |

#### Taxonomia (lookups) — formato `{ id, id_erp, descricao, ... }`
`categorias`, `colecoes`, `marcas`, `grupos-produtos`, `generos`, `linhas`, `familias-comerciais`, `nichos`, `especies`, `tipos-produtos`. Alguns trazem `situacao` e `perc_desconto_*`.

#### Grade (eixos de variação)
`complementos-1`, `complementos-2`, `complementos-3` são os **valores** dos eixos (ex. cor/tamanho):

```json
{ "id": 1, "id_erp": "U", "descricao": "UNICO", "tipo_complemento_id_erp": "1", "atributo_generico_1..6": null, "aux": null }
```

`grades-padroes`: `{ id, id_erp, sequencia, complemento_2_id }` — grades padrão.

### 3.2 Preços

#### `tabelas-precos` — tabela de preços
`{ id, id_erp, descricao ("TABELA CHEIA = BUBA"), situacao, perc_desconto, perc_desconto_parceria, perc_desconto_gerencial }`

#### `itens-tabelas-precos-pre` — **preço por produto × tabela**
Onde está o **preço efetivo**.

| Campo | Tipo | Notas |
|---|---|---|
| `id` | int | |
| `tabela_preco_id` / `tabela_preco_id_erp` | int / str | FK tabela |
| `produto_pre_id` / `produto_id_erp` | int / str | FK produto |
| `preco` | str | preço como **string** ex. `"79"`, `"189,90"` |
| `prazo_medio`, `grade`, `agrupamento` | str | |
| `lista_grade` | array | **detalhe de preço por grade** (ver abaixo) |

`lista_grade` (matriz da grade):
```json
[{
  "complemento_1": "U", "complemento_1_id": 1, "complemento_1_descricao": "UNICO",
  "complemento_2": [{ "complemento_2_id": 1, "complemento_2_id_erp": "U", "complemento_2_descricao": "U" }],
  "complemento_3": "U", "complemento_3_id": 1, "complemento_3_descricao": "UNICO"
}]
```

#### `prazos` — condições de pagamento
`{ id, id_erp, descricao ("A VISTA ANTECIPADO"), parcelas, tipo ("V"), prazo_medio, dias_parcela_1..12, situacao, perc_desconto }`

#### `tipos-vendas` — tipos de venda
`{ id, id_erp, descricao ("VENDA"), gera_comissao, gera_contas_receber, parcela_minima, baixa_estoque, origem_pedido ("TROVATA"), local_estoque_id, ...flags YES/NO }`

### 3.3 Pessoas

#### `clientes`
| Campo | Tipo | Notas |
|---|---|---|
| `id` / `id_erp` | int / str | |
| `nome_fantasia`, `razao_social` | str | |
| `cpf_cnpj`, `rg_ie` | str | |
| `endereco`, `bairro`, `cep` | str | |
| `telefone`, `celular`, `e_mail`, `contato` | str | |
| `vendedor_id_erp` | str | FK vendedor |
| `cidade_id_erp` / `cidade_id` | str / int | |
| `tipo_pessoa_id_erp`, `situacao` | str | `A`=ativo |
| **`tabela_preco_id` / `tabela_preco_id_erp`** | int | **define qual tabela de preço usar para este cliente** |
| `prazo_id`, `perc_desconto`, `regiao_id` | | condição comercial |

#### `vendedores`
`{ id, id_erp, nome_fantasia, razao_social, email, telefone, situacao (A/I), perc_comissao_*, endereco, observacao }`

#### `vendedores-clientes` — M2M vendedor ↔ cliente (~58713)
`{ id, vendedor_id_erp, cliente_id_erp, vendedor_id, cliente_id }`. **Tabela grande** — paginar e não carregar tudo em memória.

---

## 4. Relacionamentos

```
produtos-pre ──< produtos-comerciais        (produto_pre_id; MOQ / venda_liberada)
produtos-pre ──< itens-tabelas-precos-pre    (produto_pre_id; preco + lista_grade)
                      │
tabelas-precos ───────┘ (tabela_preco_id)

produtos-pre ──> categorias / colecoes / marcas / grupos-produtos / generos / linhas / ...
lista_grade  ──> complementos-1 / -2 / -3    (eixos cor/tamanho)

clientes ──> vendedores            (vendedor_id_erp)
clientes ──> tabelas-precos        (tabela_preco_id → preço do catálogo p/ este cliente)
vendedores ──< vendedores-clientes >── clientes   (M2M)
```

Fluxo de preço no catálogo: **cliente da sessão → `tabela_preco_id` → `itens-tabelas-precos-pre` (por `produto_pre_id`) → `preco`/`lista_grade`**.

---

## 5. Cache local (SQLDelight)

Espelho fiel em `composeApp/src/commonMain/sqldelight/app/trovata/cast/db/`:

| Arquivo | Tabelas |
|---|---|
| `Catalog.sq` | `ProductEntity` (produtos-pre), `CommercialProductEntity` (produtos-comerciais) |
| `Pricing.sq` | `PriceTableEntity`, `ProductPriceEntity` (itens-precos), `PrazoEntity`, `SaleTypeEntity` |
| `Taxonomy.sq` | `Categoria/Colecao/Marca/GrupoProduto/Genero/Linha/FamiliaComercial/Nicho/Especie/TipoProduto`Entity, `Complemento1/2/3Entity`, `GradePadraoEntity` |
| `People.sq` | `ClientEntity`, `SellerEntity`, `SellerClientEntity` |
| `SyncState.sq` | `SyncStateEntity` (cursor de sync por entidade) |

Convenções: PK = `id` numérico da API; `idErp TEXT` indexado; dinheiro em **cents `INTEGER`** (parseado na ingestão); timestamps guardados como `updatedAt TEXT` (ISO bruto = cursor) **e** `updatedAtMs INTEGER`. FKs cross-entity são **só índice** (sem constraint), pois páginas chegam fora de ordem.

`lista_grade` é guardado como **blob JSON** (`listaGradeJson TEXT`) e parseado lazy no repositório — é projeção 1:1 do preço, lida inteira, nunca consultada cross-produto.

---

## 6. Pontos de atenção (gaps e armadilhas)

- **Sem imagem de produto** na API → placeholder/tint na UI; backlog: mídia/CDN.
- **Erro transitório do Postgres**: `ERROR: cached plan must not change result type (SQLSTATE 0A000)` aparece intermitentemente (HTTP 5xx). **Exige retry** com backoff; resolve em 1–3 tentativas. Não fazer retry em 4xx.
- **Preços como string** com vírgula decimal (`"189,90"`) e às vezes ponto de milhar (`"1.189,90"`) → parser dedicado para cents.
- **`data` null** = página vazia; ainda processar `deleted_ids`.
- **Soft delete duplo**: `deleted_ids` no envelope **e** `deleted_at` setado em linhas de `data`.
- **Tabelas grandes** (`vendedores-clientes` ~58k) → paginar com `per_page` alto, commit por página, retomar por `lastPage`. Timeout do HTTP precisa ser maior que os 10s default.
- **Token expira** (~5 min) no meio de um sync longo → renovar; 401 deve falhar-soft (cursor intacto).
- **Cursor por `max(updated_at)`** observado, não pelo relógio local (evita skip por skew).
