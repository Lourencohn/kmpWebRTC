# Contexto para iniciar: TrovataCast como camada ao vivo do Catálogo Link

> Documento de handoff. Leia inteiro antes de escrever qualquer linha de código.
> Ele descreve **três bases de código reais**, o que cada uma já faz hoje, e qual é a integração a construir.

---

## 1. A missão, em uma frase

**O TrovataCast deixa de ser um produto com catálogo próprio e passa a ser a camada de co-presença ao vivo do Catálogo Link.**

O vendedor abre o app, vê **os catálogos link que já são dele** (vindos do backend real), convida um cliente, e os dois entram numa sessão ao vivo **em cima do catálogo link que já existe** — mesma vitrine, mesmo carrinho, mesmas regras de negócio, mesmo pedido. O que o TrovataCast adiciona é **áudio, ponteiro compartilhado, scroll sincronizado e navegação guiada** sobre a vitrine que o cliente já conhece.

O frontend próprio do TrovataCast (o `webBuyer` em TS vanilla, com catálogo e carrinho mockados) **é substituído** pelo frontend do Catálogo Link, que já está pronto, testado e em produção.

---

## 2. As três bases de código

| Caminho | O que é | Papel na integração |
|---|---|---|
| `/home/lourenco/Documentos/sfa_back` | API Laravel 12 multi-tenant (o "SFA") | **Fonte da verdade.** Catálogo, preço, desconto, grade, estoque, carrinho, pedido. Não muda de dono. |
| `/home/lourenco/Documentos/sfa_front` | SPA Vue 3 do SFA, incluindo a vitrine pública do Catálogo Link | **Vira a superfície do cliente.** Ganha um módulo "ao vivo" por cima. |
| `/home/lourenco/Documentos/kmpWebRTC` | TrovataCast: app KMP do vendedor + sinalização Ktor + webBuyer | **Fornece a co-presença.** App do vendedor + sinalização já em deploy. O `webBuyer` é aposentado. |

---

## 3. O que existe hoje — levantado lendo o código, não suposto

### 3.1 `sfa_back` (Laravel)

- Laravel 12 / PHP 8.2, PostgreSQL **multi-tenant por schema** (`SetTenantSchema`, `ValidateEmpresa`, `SchemaService`).
- Autenticação interna via **Keycloak** (`auth:keycloak`, `CheckUserPermissions`, `CheckRolesMiddleware`). Realm `Base`.
- Vitrine pública em `routes/api/vitrine.php`, sob middleware `[ValidateEmpresa, SetTenantSchema, ValidateUUIDMiddleware]`, no padrão:
  `catalogos-links/{empresa_slug}/{catalogo_uuid}/...` → `vitrine`, `categoria`, `lista-destaque`, `prazos`, `produtos/{id}/grades`, `produtos/{id}/regras-descontos`, `vendedor`, `clientes-liberados`, `visualizacao`, além das rotas de `share/...` para preview de crawler.
- Carrinho é **cidadão de primeira classe no servidor** (`routes/api/carrinhos.php`): `CatalogoCarrinho`, `CatalogoCarrinhoItem`, com `store`, `itens` (`updateOrCreateMultiple`), `itens/delete`, `trocar`, `prazo`, `finalizar`, `definir-digitando`, `gerarPedido`, PDF via dompdf, relatório, e login público por e-mail (`POST catalogos-links/{slug}/{uuid}/login`).
- Toda a regra pesada mora aqui: tabela de preço, tipo de venda, prazos e prazo médio, descontos em cascata (`AplicarDescontos`), grade/agrupamento, saldo de estoque, `perc_desconto_101..109` com limites por catálogo link.
- Rastreamento passivo já existe: `CatalogoLinkVisualizacao`, `CatalogoLinkProdutoVisualizacao`, `CatalogoLinkItemVisualizacao`, `relatorio-visualizacao`.
- **Não existe nenhum canal de tempo real.** `BROADCAST_CONNECTION=log`, sem Reverb/Pusher/Soketi. Uma varredura por `webrtc|signaling|websocket` em `app/`, `routes/` e `config/` não retorna nada.
- `CatalogoLink` tem `uuid` público, `vendedor_id`, `usuario_id`, `data_validade`, `situacao` — ou seja, **o catálogo já pertence a um vendedor** e já tem prazo de validade.

### 3.2 `sfa_front` (Vue 3)

- Vue 3 + Vite + TS, Tailwind + shadcn-vue/radix/reka, TanStack Query, Vuex, vue-router, vue-i18n, `keycloak-js`.
- Vitrine pública em `src/router/public-routes.ts`: `/catalogo-link-view/:slug/:uuid` com filhos `inicio`, `menu`, `todos`, `:type/:typeId`, `carrinho`, `favoritos`; e `/catalogo-link-resume/:slug/:uuid/carrinho/:carrinhoId`.
- `src/api/axios.ts` tem um interceptor que, ao detectar `/catalogo-link-view` ou `/catalogo-link-resume` na URL, **prefixa automaticamente** todas as chamadas com `/catalogos-links/{slug}/{uuid}/`. Qualquer código novo dentro dessas rotas herda esse comportamento.
- Identidade do comprador: e-mail, via `LoginPublicoDialog` (`src/components/features/sessao/`) + `SessionLoginManager` (localStorage por uuid) + store Vuex `login-carrinho`. O carrinho ativo é resolvido por `useFetchUltimoCarrinho`.
- Whitelabel por empresa: `CatalogoLinkCustomizado` + `applyTheme` no `PublicLayout.vue`.
- Componentes de vitrine já prontos e reutilizáveis: `components/features/card-product`, `common/product-card`, `common/item-product-layout`, `common/floating-cart-bar`, `common/infinite-scroll`, `views/public/SecaoPage.vue`, `views/public/carrinho/*`.
- **Dois caminhos de deploy convivem no repositório**: workflow de ECR + `k8s/ingress.yaml` (cluster) *e* `vercel.json` + `middleware.js` (edge, com prerender para crawlers). Precisa ficar claro qual é a verdade antes de decidir onde a rota ao vivo mora.
- Nenhuma linha de WebRTC/WebSocket hoje.

### 3.3 `kmpWebRTC` (TrovataCast)

Estrutura: `composeApp/` (app KMP do vendedor, Android + iOS), `webBuyer/` (cliente web em TS vanilla), `signalingServer/` (Ktor + WebSocket), `protocol/` (DTOs compartilhados), `iosApp/`.

**Já integrado com o SFA real:**
- `data/remote/sfa/KeycloakAuthService.kt` — login e refresh contra `https://login.trovata.app.br`, realm `Base`, client `front-client`, grant `password`.
- `data/remote/sfa/SfaApi.kt` — paginação incremental em `{baseUrl}/empresas/{empresaId}/{resource}` com cursor `updated_at` e retry em 5xx.
- `data/sync/CatalogSyncService.kt` — sincroniza taxonomia, produtos, preços, prazos, clientes, vendedores para SQLDelight local.
- `SfaConfig.kt` aponta hoje para `https://api-int.trovata.app.br` com `empresaId = 97` **fixo em código** — dívida conhecida, precisa virar seleção de empresa real (já existe `CompanySelectionScreen`).

**Tempo real que já funciona em produção:**
- `signalingServer` no **Fly.io**, região `gru`, 1 máquina 256MB sempre ligada, `https://trovatacast-signaling.fly.dev`, HTTPS automático, healthcheck em `/health`. **Salas em memória** (`RoomManager`/`Room`, máximo 2 peers, um `Seller` e um `Buyer`).
- `protocol/Signaling.kt` — `hello`, `roomState`, `peerJoined`, `peerLeft`, `offer`, `answer`, `ice`, `bye`, `presencePing`, `error`.
- `protocol/DataChannelMessage.kt` — `mute`, `scroll`, `pointAt`, `navigate`, `cartUpdate`, `orderConfirm`.
- `POST /session` cria uma sessão com snapshot de produtos e devolve `{ sessionId, token, url, expiresAtMs }`, onde `url = {PUBLIC_BUYER_URL}/?t={token}`.
- Handshake WebRTC completo nos dois lados (webrtc-kmp no app, WebRTC nativo no browser), áudio P2P, scroll a ~30Hz, ponteiro, carrinho ao vivo e confirmação de pedido — validado ponta a ponta com iPhone em 3G.

**O que é mock e vai morrer:** carrinho em SQLDelight local (`CartRepository`), pedido em memória no Ktor (`OrderStore`, `POST /order`), catálogo de exemplo (`data/sample/*`), e o `webBuyer` inteiro como superfície do cliente.

Status detalhado em `docs/07-status.md` (M0–M9 fechados). Migração de mock para real em `docs/09-mock-to-real-migration.md`. **Pauta da conversa com o time do SFA em `docs/10-integracao-catalogo-link.md`** — leia, é o levantamento de riscos e perguntas que originou este trabalho.

---

## 4. A ideia, reescrita com precisão

O TrovataCast e o Catálogo Link resolvem o mesmo momento comercial por caminhos diferentes. O Catálogo Link resolve **o catálogo**: vitrine, regra de preço, carrinho, pedido — tudo maduro, com anos de regra de negócio dentro. O TrovataCast resolve **a presença**: o vendedor e o cliente olhando a mesma coisa ao mesmo tempo, com voz, ponteiro e navegação compartilhada.

Hoje cada um carrega uma cópia pobre do domínio do outro. O TrovataCast tem um catálogo e um carrinho de brinquedo. O Catálogo Link tem "tempo real" simulado por polling e diálogos (`definir-digitando`, `NotifyVendedorDialog`).

A integração é a soma correta: **um catálogo link, uma vitrine, um carrinho, um pedido — e, opcionalmente, uma sessão ao vivo por cima.**

Concretamente, o cliente recebe **o mesmo link de sempre**. Se o vendedor estiver chamando, o mesmo link entra em modo ao vivo. O cliente não instala nada, não aprende nada novo, não muda de site.

### A regra arquitetural que decide todo o resto

| Trafega pelo DataChannel P2P (efêmero, nunca persistido) | Passa pelo Laravel (fonte da verdade) |
|---|---|
| Áudio (e vídeo, se houver) | Adicionar/remover item, quantidade, grade |
| Posição do ponteiro, scroll sincronizado | Preço, desconto, prazo, tabela de preço |
| Navegação guiada ("estamos vendo esta seção agora") | Estoque e reserva |
| Foco de produto, reações, estados de UI (mute, sheet aberto) | Fechamento do pedido e PDF |
| **Aviso** de que o carrinho mudou | O conteúdo do carrinho em si |

Isto é inegociável e resolve o conflito central: o TrovataCast trata carrinho como estado P2P, o Catálogo Link trata como entidade transacional pesada. **Vence o Catálogo Link.** O DataChannel nunca carrega o estado do carrinho — carrega apenas o sinal de "invalide e refaça a query".

---

## 5. Arquitetura alvo, ponta a ponta

1. **Vendedor autentica** no app KMP via Keycloak (já funciona) e escolhe a empresa ativa (precisa substituir o `empresaId = 97` fixo).
2. **App lista os catálogos link do vendedor** — vindos da API real, não de mock. O vendedor escolhe um.
3. **App cria a sessão ao vivo**: `POST /session` no signaling passa a receber `{ empresaSlug, catalogoUuid, carrinhoId?, sellerId, sellerName, clientName? }` **em vez do snapshot de produtos**. O signaling devolve um token curto.
4. **A URL do convite muda**: de `{buyer}/?t={token}` para `https://{front}/catalogo-link-view/{slug}/{uuid}?live={token}`. O vendedor compartilha por WhatsApp (o share nativo já existe no app).
5. **Cliente abre o link** no navegador. É o `sfa_front` de sempre: mesma vitrine, mesmo tema whitelabel, mesmo login por e-mail, mesmo carrinho. A presença de `?live=` **carrega em lazy chunk** o módulo de co-presença.
6. **Handshake**: os dois lados abrem WebSocket no signaling do Fly.io, trocam SDP/ICE, sobem áudio P2P e o DataChannel.
7. **Durante a sessão**: ponteiro, scroll e navegação viajam P2P. Toda alteração de carrinho é `POST`/`PATCH` no Laravel pelo lado que agiu, seguido de um `cartInvalidated` no DataChannel; o outro lado invalida a query do TanStack Query e refaz. O número no carrinho é sempre o que o Laravel disse.
8. **Fechamento**: `finalizar` / `gerarPedido` nas rotas que já existem. Nada de pedido paralelo. O PDF é o PDF do Laravel.
9. **Depois**: a sessão vira registro estruturado (o que foi apontado, o que foi olhado, o que entrou no carrinho ao vivo) — decidir se reaproveita as tabelas `CatalogoLink*Visualizacao` ou cria `sessao_*`.

---

## 6. Mudanças concretas previstas, por repositório

### `kmpWebRTC/protocol`
- `SessionCreateRequest` deixa de carregar `productSkus`/`products` e passa a carregar a **identidade do catálogo link** (`empresaSlug`, `catalogoUuid`, `carrinhoId?`).
- `Route` (hoje `Catalog(collectionId)` / `Product(sku)`) precisa espelhar as rotas reais do vue-router do catálogo link: `inicio`, `menu`, `todos`, `{type}/{typeId}`, `carrinho`, `favoritos`, além do detalhe de produto.
- `DataChannelMessage.CartUpdate` deixa de transportar `{productId, size, units}` e vira uma **notificação de invalidação** (`carrinhoId`, `ts`, `from`, motivo).
- `Scroll` precisa de âncora estável entre viewports diferentes (id do produto em vista + razão de offset), não pixel absoluto.

### `kmpWebRTC/signalingServer`
- `POST /session` passa a validar a sessão contra o Laravel (o catálogo existe? o vendedor é dono? está dentro da validade?) em vez de aceitar qualquer payload.
- `OrderStore` e `POST /order` **saem**.
- Salas em memória só sobrevivem com uma réplica. Se um dia escalar, precisa de estado compartilhado — decisão em aberto.

### `kmpWebRTC/composeApp`
- Tela nova (ou reaproveitar `CatalogPickerScreen`) listando **catálogos link do vendedor**, não produtos soltos.
- `empresaId` fixo → seleção real de empresa.
- `CartRepository`/`OrderRepository` locais deixam de ser fonte da verdade; viram cache de leitura ou saem.
- `LiveCallScreen` passa a espelhar a navegação do catálogo link, não um grid próprio.

### `kmpWebRTC/webBuyer`
- **Aposentado como superfície do cliente.** Serve no máximo como referência de implementação (`src/webrtc/peer.ts`, `src/webrtc/live.ts`, `src/signaling/client.ts`, `src/protocol/dataChannel.ts` são o material a portar para o Vue).

### `sfa_front`
- Módulo novo de co-presença, carregado em lazy chunk apenas quando há `?live=` na URL: cliente de sinalização, `RTCPeerConnection`, DataChannel, elemento de áudio remoto, ponteiro remoto, sincronização de scroll, banner de conexão.
- Precisa conviver com TanStack Query sem brigar: eventos remotos disparam `invalidateQueries`, não escrita direta no cache.
- O `middleware.js` (prerender de crawler) precisa ignorar a rota ao vivo.
- Whitelabel continua valendo — a UI ao vivo respeita o tema da empresa.

### `sfa_back`
- Idealmente, **poucos endpoints novos**: emissão/validação do token de sessão ao vivo e, se necessário, um snapshot completo do carrinho para hidratar os dois lados de uma vez.
- Nada de regra de negócio nova. O objetivo é encostar no mínimo.

---

## 7. Decisões já tomadas

Estas vieram do product owner e **não devem ser revisitadas** sem motivo forte:

1. O backend permanece como está e continua dono de toda a lógica de negócio.
2. O frontend do Catálogo Link é a superfície do cliente. O `webBuyer` do TrovataCast é substituído.
3. A sinalização WebSocket **já está em deploy** (Fly.io) e é o ponto de partida — não se começa do zero nem se troca de tecnologia por gosto.
4. O vendedor acessa a aplicação, vê os catálogos que são dele, e compartilha com os clientes. O fluxo de link de catálogo com suas regras de negócio continua existindo exatamente como hoje.
5. O que o TrovataCast entrega é navegação e comunicação em tempo real sobre o catálogo link.

---

## 8. Decisões ainda em aberto

Não invente resposta para estas. Onde travar, pergunte.

- **TURN.** Só STUN não basta: em 4G brasileiro e Wi-Fi corporativo com NAT simétrico, uma fatia relevante das chamadas só conecta via relay. Fornecedor e quem paga estão indefinidos.
- **Onde o `sfa_front` é servido de verdade** — Vercel ou cluster. Os dois caminhos estão no repositório e isso decide onde a rota ao vivo mora e como o `middleware.js` entra.
- **Client Keycloak para app nativo** (Authorization Code + PKCE). Hoje o app usa grant `password` com o client `front-client`, o que não é o padrão correto para mobile.
- **Onde o token de sessão é emitido** — Laravel ou signaling — e como o contexto multi-tenant (`empresa_id`/schema) chega a um serviço fora do Laravel.
- **Concorrência no carrinho.** Vendedor e cliente mexendo no mesmo `carrinho_id` simultaneamente é o caso normal aqui, não a exceção. Existe versionamento ou é last-write-wins?
- **Latência p50/p95** das rotas de carrinho em produção. Acima de ~300ms dentro de uma chamada, o cliente percebe travamento.
- **Modelo de dados da sessão** — reaproveitar `CatalogoLink*Visualizacao` ou criar `sessao_*`, e como aplicar migration em ambiente multi-schema.
- **Gravação da chamada.** Se for requisito, P2P puro não serve mais (entra SFU) e entra LGPD com consentimento explícito.

O `docs/10-integracao-catalogo-link.md` deste repositório detalha cada um destes pontos em formato de pauta.

---

## 9. Ordem sugerida de ataque

Não é obrigatória, mas respeita as dependências reais.

1. **Fase 0 — Alinhar o contrato.** Redesenhar `SessionCreateRequest`, `Route` e `CartUpdate` no `protocol/`. É barato e desbloqueia os dois lados.
2. **Fase 1 — Vendedor vê catálogos reais.** App KMP lista catálogos link do vendedor com empresa selecionável. Sem tempo real ainda.
3. **Fase 2 — Convite aponta para o `sfa_front`.** `POST /session` novo, URL nova, share por WhatsApp funcionando. O cliente abre a vitrine real — ainda sem co-presença.
4. **Fase 3 — Sinalização e áudio no Vue.** Portar `signaling/client.ts` + `webrtc/peer.ts` para um módulo Vue lazy. Meta: os dois lados se ouvem dentro da vitrine real.
5. **Fase 4 — Co-presença.** Ponteiro, scroll, navegação guiada. É aqui que o produto aparece.
6. **Fase 5 — Carrinho ao vivo pelo backend.** Mutação no Laravel + invalidação P2P nos dois lados. Aposentar `CartRepository`/`OrderStore`.
7. **Fase 6 — Fechamento e histórico.** `gerarPedido` real, PDF real, registro da sessão.

---

## 10. Princípios que não mudam

1. **Co-presença é o produto.** Tudo que reforça "estamos vendo a mesma coisa, juntos" tem prioridade sobre feature genérica de catálogo.
2. **O cliente nunca instala nada.** Quebrar isso quebra o produto.
3. **O catálogo tem que parecer o catálogo de sempre.** A camada ao vivo se soma à vitrine existente; não a substitui nem a reinventa.
4. **P2P para o efêmero, Laravel para o que vale dinheiro.**
5. **Pedido nasce na chamada.** Se um fluxo exige formulário depois, está errado.
6. **Mobile-real.** O vendedor está andando na feira com 4G ruim; o cliente está no depósito.
7. **Português brasileiro coloquial-profissional** na UI: "Atender", "Convidar", "Apontando", "Pedido pronto".
8. **Sem comentários no código.** Nomes descritivos e código autoexplicativo.

---

## 11. Mapa rápido de onde olhar

| Pergunta | Onde |
|---|---|
| Como a vitrine pública funciona hoje? | `sfa_front/src/router/public-routes.ts`, `src/views/public/`, `src/layouts/PublicLayout.vue` |
| Como o front fala com a API pública? | `sfa_front/src/api/axios.ts` (interceptor de prefixo), `src/api/public/`, `src/queries/public/` |
| Quais rotas públicas o backend expõe? | `sfa_back/routes/api/vitrine.php`, `routes/api/carrinhos.php` |
| Onde mora a regra de carrinho/preço? | `sfa_back/app/UseCases/CatalogoCarrinho*`, `app/UseCases/CatalogoLink*`, `app/Services/` |
| Como o app do vendedor autentica e sincroniza? | `kmpWebRTC/composeApp/src/commonMain/.../data/remote/sfa/`, `data/sync/CatalogSyncService.kt` |
| Qual o contrato de tempo real hoje? | `kmpWebRTC/protocol/src/commonMain/.../Signaling.kt`, `DataChannelMessage.kt`, `Session.kt` |
| Como a sinalização funciona? | `kmpWebRTC/signalingServer/src/main/.../Room.kt`, `SignalingWebSocket.kt`, `Routing.kt`, `fly.toml` |
| Referência de WebRTC no browser para portar | `kmpWebRTC/webBuyer/src/webrtc/`, `src/signaling/`, `src/protocol/dataChannel.ts` |
| Riscos e perguntas em aberto | `kmpWebRTC/docs/10-integracao-catalogo-link.md` |
| Estado da construção do TrovataCast | `kmpWebRTC/docs/07-status.md`, `docs/09-mock-to-real-migration.md` |

---

## 12. Primeira coisa a fazer no chat novo

Antes de codar, **confirme o entendimento e proponha o desenho da Fase 0** — o contrato novo de `SessionCreateRequest`, `Route` e `CartUpdate`, com o desenho da URL de convite. É a peça que trava as outras seis fases, e é barata de errar agora e cara de errar depois.

/home/lourenco/Documentos/sfa_front
/home/lourenco/Documentos/sfa_back