# Espelho e desenho: o vendedor vê a mesma vitrine e desenha nela

> Registro das decisões e da evidência por trás das Frentes A (vitrine embutida) e B (desenho compartilhado) descritas em `prompt-espelho-e-desenho.md`. Levantado e construído em 07/09/2026.

---

## 1. O que mudou, em uma passada

Durante a chamada, o corpo da tela do vendedor deixou de ser a grade Compose (`CatalogPanel`, painel de produto, modo apontar) e passou a ser uma WebView que abre a vitrine do Catálogo Link com `?live=<token>&embed=seller`. A barra superior de status, a barra de ações (mudo, desenhar, apagar, carrinho, encerrar), a gaveta do carrinho e o resumo do pedido continuam nativos.

O app virou um relay: tudo que a página embutida manda por `window.TrovataLive.postMessage` vai para o DataChannel como está, e tudo que chega do cliente pelo DataChannel é entregue à página por `window.__trovataLiveReceive`. O estado da conexão vai por `window.__trovataLiveStatus`.

O desenho é capturado e renderizado **dentro da página**, nos dois lados, por `LiveDrawLayer.vue`. O app só liga e desliga o modo (`window.__trovataLiveSetDrawing`) e manda apagar (`window.__trovataLiveClearDrawing`). Isso deixa o contrato simétrico: o dia em que o cliente puder desenhar, nada muda no protocolo.

---

## 2. As cinco perguntas do handoff, com a evidência

### 2.1 A página embutida exige login do vendedor?

Não. A vitrine é pública por uuid e o login por e-mail (`POST catalogos-links/{slug}/{uuid}/login`) só é exigido para mexer no carrinho e nos favoritos. `LoginCarrinhoRequest` no `sfa_back` pede apenas `e_mail`; `cpf_cnpj` e `telefone` são opcionais, e a checagem de `cliente_novo = 'NO'` em `GetCatalogoCarrinhoPorEmailUseCase` só se aplica quando vem CPF/CNPJ.

No modo embutido, `use-live-session.ts` já faz esse login sozinho: `startFor` chama `fetchLiveSessionInfo` no signaling e depois `ensureCartLogin(liveToken, info?.clientEmail)`, que grava o `SessionLoginManager` com o e-mail do cliente da sessão. Logo, a página do vendedor entra logada **como o cliente**, no mesmo carrinho em situação `D` que o app abre por `carrinhoApi.abrirCarrinho` com o mesmo e-mail.

Quando a sessão não tem `clientEmail`, a página não loga e o modal de produto abre o `LoginPublicoDialog` ao tentar carregar a grade. O app já trata esse caso com a mensagem "Esse catálogo link não tem e-mail de cliente". Foi exatamente o que apareceu no teste em Chrome headless com um token vencido (sem `clientEmail`): o diálogo "Fazer Login" abriu no lugar do modal.

Nenhum ajuste de login foi necessário no `sfa_front`. Os ajustes lá foram outros (seção 3).

### 2.2 A WebView substitui a grade Compose ou convive com ela?

Substitui. Argumento econômico do `docs/12`, item 4.1: a grade e o painel de produto eram uma segunda implementação da regra de preço e grade do Catálogo Link, e cada mudança lá tinha que ser reescrita em Compose. Manter as duas em convivência preservaria exatamente a dívida que a Frente A existe para eliminar.

O que saiu do `LiveCallScreen` e do `LiveCallScreenModel`: `products`, `loadVitrine`, `focusedGrade`, `openProductDetail`, `addFocusedProductToCart`, `publishScroll`, `publishPointAt`, o modo apontar e a `ProductDetailScreen` na chamada. `VitrineApi` ficou no módulo de rede porque `docs/12` prevê telas fora da chamada que a usam.

**A gaveta do carrinho fica.** Motivo: "Marcar pronto para envio" chama `PATCH empresa/{slug}/catalogos-links/{linkId}/carrinhos/{id}/finalizar`, rota privada com o Bearer do vendedor, que a página pública não tem. A gaveta lê `itens-para-rota-publica` do mesmo `carrinhoId` que a página usa, então as duas visões não brigam: quando o vendedor adiciona pela página, a própria página emite `cartInvalidated` pela ponte, o app o repassa ao cliente e recarrega a gaveta (`handleOwnCartChange`). Quando o cliente adiciona, o `cartInvalidated` chega pelo DataChannel, o app recarrega a gaveta e entrega a mensagem à página, que invalida as queries.

**Desempenho**: não foi medido no aparelho. O Moto G22 estava bloqueado por PIN e o token do vendedor gravado no app tinha vencido há quatro dias, então nem chamada real nem `dumpsys meminfo` durante a chamada foram possíveis nesta sessão. Fica como primeiro item do roteiro da seção 5.

### 2.3 Qual âncora o desenho usa?

A do `pointAt`, estendida: **cada traço é ancorado ao elemento onde começou**, e todos os pontos viajam como proporção do retângulo desse elemento (`x = (px - rect.left) / rect.width`, idem em `y`). Os valores podem sair de `[0, 1]`: um círculo em volta de uma peça passa por fora da caixa do card e continua fiel.

Resolução da âncora no início do traço (`src/live/dom-anchors.ts`, `anchorAtPoint`), nesta ordem:

1. Ponto sobre o modal de produto aberto (`[role="dialog"]`): âncora `produto:<id>:modal`, retângulo do diálogo. O id vem do foco local ou remoto (`focus.ts`), porque o `DialogContent` do kit de UI não repassa atributos `data-*` e não vale mexer em `src/components/ui/`.
2. Ponto sobre um card (`[data-produto-pre-id]`): âncora `produto:<id>`, retângulo do card.
3. Fora de tudo: card visível mais próximo do ponto.
4. Página sem produto na tela (carrinho, menu): `viewport`, proporção da janela. É o único caso de coordenada normalizada pela janela, e é deliberadamente o último recurso.

Ao renderizar (`resolveAnchorRect`), o lado receptor procura o elemento **agora** e reprojeta a cada quadro, então rolagem, redimensionamento e animação do modal não deslocam o traço. Se o elemento não existe (outra rota, modal fechado, modal de outro produto), o traço não é desenhado e é descartado.

**Dois lados em posições de rolagem diferentes** não é problema, porque o traço segue o elemento, não a janela. O que o seller passa a publicar, e antes não publicava no modo embutido, é `scroll` com o card centralizado (`centeredProduct`, throttle de 200 ms, só quando o produto muda), para o cliente acompanhar a rolagem como já acompanhava a grade Compose.

Evidência do teste headless: traço iniciado em (20%, 20%) do card `produto:4932` na página A pintou na página B a caixa `x 67..198, y 362..469` para um retângulo esperado `x 71..195, y 366..466` (a diferença é a largura do halo branco). Sobre o modal, a âncora saiu `produto:4932:modal` e os pixels caíram dentro do retângulo do diálogo da página B.

### 2.4 O traço é efêmero ou fica?

Fica **enquanto o elemento ancorado existir na página** e até alguém apagar. Não há tempo de vida: o vendedor circula um detalhe e fala sobre ele por minutos, e um sumiço em três segundos, como no `pointAt`, atrapalharia. Rolar para longe e voltar mantém o traço, porque a `SecaoPage` renderiza a página inteira sem virtualização. Navegar de rota ou fechar o modal remove o elemento e, com ele, o traço.

Apagar é a mensagem `drawClear`, com `strokeId` opcional (sem id apaga tudo). O botão nativo "Apagar" chama `window.__trovataLiveClearDrawing()`, que limpa a página do vendedor e manda o `drawClear` pela ponte para o cliente. Um único caminho.

### 2.5 Taxa de amostragem e lote

- Captura por `pointermove`, descartando pontos a menos de 2 px do anterior.
- Lote a cada 50 ms (`FLUSH_INTERVAL_MS`), o que dá no máximo 20 mensagens por segundo por traço, contra os 30 Hz do `scroll` (`sample(33)`). No teste, um traço de 10 movimentos virou 7 mensagens: `start` com 1 ponto, 5 `move` com 2 pontos e `end` vazio.
- Toda mensagem carrega `strokeId` e `target`, então cada uma se basta: perder um `start` não impede o receptor de montar o traço a partir do primeiro `move`.
- O canal é confiável e ordenado nos dois lados: `createDataChannel('presence', { ordered: true })` em `peer.ts` e `createDataChannel("presence", id = 1)` no `PeerSession`, sem `maxRetransmits`. O `end` explícito existe para fechar o traço local e por simetria, não porque haja perda.

---

## 3. O que mudou em cada repositório

### `kmpWebRTC`

- `protocol/`: `DataChannelMessage.Draw` (`strokeId`, `target`, `phase`, `points`, `color?`), `DataChannelMessage.DrawClear` (`strokeId?`), `DrawPhase`, `DrawPoint`, `LiveAnchor.viewport()`, `LiveAnchor.productModal()`, `buildSellerEmbedUrl()`. Testes em `DrawMessageTest` e `LiveUrlTest`.
- `feature/call/LiveWebBridge.kt`: fila de scripts para a página, shim que enfileira chamadas até a página definir `__trovataLiveReceive` e companhia, status lembrado para reinjeção após recarga.
- `platform/LiveCatalogWebView.kt`: `expect` com `actual` Android (`WebView`, `addJavascriptInterface`, console da página no Kermit) e iOS (`WKWebView`, `WKScriptMessageHandler`, user script que cria `window.TrovataLive` sobre `webkit.messageHandlers`). **O `actual` iOS não foi compilado**: não há Mac neste ambiente.
- `PeerSession`: `incoming` (todas as mensagens decodificadas), `publish(message)`, `publishDraw`, `publishDrawClear`.
- `LiveCallScreenModel` e `LiveCallScreen`: reescritos sobre a WebView. `CallSpec.inviteUrl` e `SfaConfig.catalogWebBaseUrlOverride` (só para apontar o app a um `sfa_front` local).
- `composeApp/src/debug/AndroidManifest.xml`: cleartext liberado só no debug, para o `adb reverse`.
- Órfãos a remover: `ui/screens/catalog/ProductDetailScreen.kt` e `feature/call/VitrineUi.kt`. Só a chamada usava os dois.

### `sfa_front` (branch `feature/catalogo-link-televenda-ao-vivo`)

- `src/live/protocol.ts`: `draw`, `drawClear`, `productModalAnchor`, `VIEWPORT_ANCHOR`, parser defensivo.
- `src/live/drawing.ts`: store dos traços e do modo de desenho. `src/live/dom-anchors.ts`: resolução de âncora e produto centralizado.
- `src/live/bridge.ts`: `__trovataLiveSetDrawing` e `__trovataLiveClearDrawing`.
- `src/components/features/live/LiveDrawLayer.vue`: canvas fixo com `z-[60]`, `pointer-events: auto` explícito (o Radix põe `pointer-events: none` no `body` com modal aberto) e `stopPropagation` no `pointerdown` (senão o `DismissableLayer` do Radix fecha o modal ao desenhar sobre ele).
- `src/composables/use-live-session.ts`: modo embutido pegajoso (a query `embed` some nas navegações internas), `embed` preservado em `routeLocationOf`, publicação de `scroll` pelo seller, publicação de `navigate` com `focus` quando o modal abre ou fecha localmente (a página nunca fazia isso), eco de scroll remoto suprimido por 800 ms.
- `src/views/public/SecaoPage.vue`: `setLiveLocalFocus` ao abrir e fechar o modal.

---

## 4. O que foi verificado e como

**Kotlin**: `./gradlew :protocol:jvmTest :composeApp:testDebugUnitTest :composeApp:assembleDebug` verdes, 35 testes no `protocol` e 95 no `composeApp`. `installDebug` no Moto G22 concluído.

**TypeScript**: `npx vue-tsc -b` e `npx eslint` limpos nos arquivos alterados. O `sfa_front` não tem `npm test` nem vitest, ao contrário do que o handoff supunha.

**Ponte e desenho, de página a página**: dois Chromes headless (412x915) abrindo a vitrine real de staging (`buba/f8bc1dd8...`) servida pelo Vite local com proxy para `api-staging`, cada um com um `window.TrovataLive` falso, e um relay em Node fazendo o papel do app (o que uma página manda por `postMessage` entra na outra por `__trovataLiveReceive`). O login público foi feito com o e-mail do cliente da sessão gravada no app. Resultado:

| Passo | Resultado |
|---|---|
| Página detecta `embed=seller` com a ponte presente | canvas montado, barra de chamada da página oculta, `__trovataLiveSetDrawing` definido |
| Seller navega para `todos` pelo router | `navigate` pela ponte, outra página segue e mostra os mesmos 50 cards |
| Traço sobre um card | 7 mensagens `draw`, âncora `produto:4932`, pixels na outra página dentro do retângulo do mesmo card |
| Apagar | `drawClear` pela ponte, canvas da outra página zerado |
| Abrir modal de produto | `navigate` com `focus`, modal abre nos dois lados |
| Traço sobre o modal | âncora `produto:4932:modal`, pixels dentro do retângulo do diálogo da outra página, modal não fecha |
| Fechar modal | `navigate` sem `focus`, modal fecha do outro lado |
| Rolar | `scroll` com o card centralizado, outra página rola até ele |

**Não verificado**: a chamada real com o app no aparelho. O Moto G22 estava com tela bloqueada (`dumpsys trust`: `deviceLocked=1`) e os tokens Keycloak em `trovatacast.auth.xml` haviam vencido, então não deu para criar sessão nem abrir a tela da chamada. O lado Android da WebView está coberto só por compilação e pelos testes do `LiveWebBridge`.

**Correção de 18/09/2026 sobre o ambiente**: em 07/09 este documento afirmava que o bundle de `staging.trovata.app.br` não tinha `src/live`. A busca tinha sido feita só no `index-*.js`, e a vitrine é rota lazy: o código ao vivo vai para o chunk `PublicLayout-*.js`. Aquela verificação não provava ausência. Desde 18/09, com o PR #12 mesclado na `staging` e publicado, o chunk `PublicLayout` contém `TrovataLive`, `__trovataLiveSetDrawing`, `quantityDraft` e `drawClear`, e o app aponta direto para staging, sem Vite local.

---

## 5. Roteiro para fechar a verificação no aparelho

1. Desbloquear o Moto G22 e entrar no app com o vendedor (o token vencido derruba para a tela de login).
2. No `sfa_front`, na branch de televenda, subir o Vite com proxy para a API de staging (a API não responde CORS para `localhost`). O arquivo abaixo não é versionado; crie-o na raiz como `vite.live-test.config.ts`:

   ```ts
   import { mergeConfig } from 'vite'
   import base from './vite.config'

   export default mergeConfig(base, {
     server: { proxy: { '/api': { target: 'https://api-staging.trovata.app.br', changeOrigin: true } } },
   })
   ```

   E rode: `VITE_API_BASE_URL=http://127.0.0.1:5173/api npx vite --config vite.live-test.config.ts --port 5173 --host 127.0.0.1`.
3. `~/Android/Sdk/platform-tools/adb reverse tcp:5173 tcp:5173`. O APK debug instalado nesta sessão já aponta para `http://localhost:5173` (`SfaConfig.catalogWebBaseUrlOverride`); o código versionado está com `null`.
4. Criar a sessão pela aba Carrinhos abertos ou Catálogos, com um cliente que tenha e-mail, e tocar em "Atender".
5. Abrir o link do convite num navegador do computador, apontando o host para `http://127.0.0.1:5173`, e entrar na chamada.
6. Conferir, nesta ordem: a vitrine carrega na WebView; `adb logcat -s LiveCatalogWebView PeerSession` mostra `[page]` sem erro e `session live`; rolar no app move o cliente; abrir um produto no app abre no cliente; "Desenhar" e um traço no app aparecem no cliente; "Apagar" limpa; adicionar pela página atualiza a gaveta nativa e o cliente.
7. Medir: `adb shell dumpsys meminfo app.trovata.cast` antes e durante a chamada, e `adb shell dumpsys gfxinfo app.trovata.cast` depois de rolar a vitrine. Registrar os números aqui.

---

## 5.1 O que a chamada real no Moto G22 revelou (08/09/2026)

Com a sessão `dEJk8zQfV` criada no app, o `sfa_front` servido pelo Vite local com proxy para `api-staging` e o cliente num Chrome do desktop, a chamada conectou (`session live` no logcat) e apareceram cinco defeitos que o teste headless não pegava. Todos corrigidos e verificados no aparelho.

**Diálogo "Fazer Login" abrindo sozinho na página do vendedor.** `InicioPage.vue` tem um `watchEffect` que abre o login quando a store não tem e-mail e não há login no `sessionStorage`. Na WebView a página monta antes de `ensureCartLogin` terminar (o login chegou 9 segundos depois, pelo log "Dados de login atualizados"), então o diálogo abria e ficava. Correção em `use-live-session.ts` e `PublicLayout.vue`: enquanto `cartLoginPending` estiver ativo, `openLoginDialog` não abre, e quando o login chega o diálogo fecha e a sessão do layout é reavaliada por `cartLoginAt`.

**Modal do produto com 1 px de altura na WebView.** Medido pelo devtools da WebView (`adb forward tcp:9226 localabstract:webview_devtools_remote_<pid>`): `max-height` computado em 0 px e, na sondagem direta, `100vh`, `100dvh`, `100svh` e `100lvh` valiam 0 com `innerHeight` 694. É comportamento da WebView Android quando ela é medida sem altura exata. Correção no app: `layoutParams` MATCH_PARENT na `WebView` dentro do `AndroidView`. Depois disso `100vh` mede 694 e o diálogo renderiza com 639 px, nos dois sentidos (vendedor abre, cliente abre).

**Traço deslocado na tela do aparelho e certo no desktop.** O canvas era `fixed inset-0` sem largura e altura em CSS, e um elemento substituído sem tamanho assume os atributos `width`/`height`, que são em pixels físicos. Com `devicePixelRatio` 1,75 o canvas ocupava 719 px CSS numa janela de 411, e cada traço aparecia 1,75 vez deslocado no aparelho, embora os dados enviados estivessem corretos, que é o que o desktop mostrava. Correção: `h-full w-full` no canvas. Medido depois: canvas de 411x694 px CSS.

**"Tentar de novo" no cliente não reconectava.** `join` retornava cedo sempre que existia um transporte, mesmo em `failed`. Agora, em `failed`, fecha o transporte antigo e abre outro.

**Carrinho só avisava o outro lado ao adicionar pela grade.** `notifyLiveCartChanged` era chamado num único lugar, `AdicaoProdutoDialog.vue`. Remover item, mudar quantidade na página do carrinho, adicionar por agrupamento, trocar prazo e finalizar não avisavam. O aviso foi para a camada das mutations públicas (`use-carrinho-itens.ts` e `use-carrinho.ts`), com o motivo real (`itemAdded`, `itemRemoved`, `quantityChanged`, `prazoChanged`, `finalized`) e as unidades calculadas da própria request. O composable passa a enviar esse motivo em vez de `itemAdded` fixo.

Fica registrado também que `setWebContentsDebuggingEnabled` é ligado só quando o app é debuggable, para permitir a inspeção acima.

---

## 5.2 Quantidade ao vivo no modal e traço sobre a foto (17/09/2026)

Dois pedidos do dono do app, validados com a sessão `47zVUAuG9` no Moto G22 e um Chrome no desktop.

**Quantidade sincronizada dentro do modal.** Antes, o contador do modal era estado local até "Adicionar", e o outro lado só via o resultado depois do aviso de carrinho e da nova busca. Agora cada mudança no contador publica `quantityDraft` (`produtoPreId`, `gradeKey`, `units`), e o modal aberto do outro lado para o mesmo produto aplica direto em `localQuantities`. A chave `gradeKey` é `complemento1_complemento2_complemento3`, derivada dos dados do produto, então é a mesma nos dois lados. Um snapshot por diálogo evita eco. Medido: 43 a 143 ms entre o toque num lado e o número mudar no outro, nas duas direções, contra 2,6 a 8,8 s do ciclo de carrinho, que continua existindo para o lançamento em si.

**Relay tolerante a tipo desconhecido.** O primeiro teste falhou dos dois lados porque o APK instalado não conhecia `quantityDraft`: `decodeDataChannel` devolvia nulo e o app descartava. O app passou a repassar o JSON bruto de qualquer envelope válido (`type`, `from`, `ts`), tanto da página para o canal (`publishRaw`) quanto do canal para a página (`incomingRaw`), e só decodifica para os efeitos locais que conhece (carrinho, pedido). Evita que toda mensagem nova do front dependa de atualizar o app.

**Traço sobre o modal.** A caixa do diálogo tem proporção parecida nos dois lados, mas na WebView o conteúdo rola internamente e no desktop não. Dentro do modal o traço passou a ancorar na própria imagem sob o dedo (`imagem:<src>`, mesmo `src` nos dois lados) e, fora da imagem, na caixa do conteúdo rolável descontando `scrollTop`. Medido com a foto rolada 87 px na WebView: o traço caiu em x 0,20 a 0,62 e y 0,53 da foto no vendedor, e em x 0,20 a 0,62 e y 0,49 da mesma foto no cliente.

---

## 5.3 Staging real, login persistente e convite sem atrito (18/09/2026)

O PR #12 do `sfa_front` foi mesclado na `staging` e publicado. O app, com `catalogWebBaseUrlOverride` em `null`, abriu a vitrine de `staging.trovata.app.br` dentro da WebView com `embed=seller`, ponte presente, canvas em 411x694, `100vh` em 694 e carrinho logado, e a chamada conectou com um cliente no navegador (`session live`), sem Vite local nem `adb reverse`.

**Login persistente no app.** O refresh token comum do Keycloak vencia cerca de 15 minutos depois do access token (idle da sessão SSO), e `hasUsableSession()` exige refresh válido, então qualquer pausa maior derrubava o login; além disso, qualquer falha no refresh, inclusive falta de rede, apagava a sessão. Três mudanças: o login pede `scope=offline_access` e recua para a sessão comum se o Keycloak responder `not_allowed` ou `invalid_scope`; `refresh_expires_in` igual a zero, que é como o Keycloak descreve um token offline, vira `AuthTokens.NEVER_EXPIRES`; e só uma recusa do servidor (HTTP 4xx) encerra a sessão, falha de rede devolve `null` e mantém o login. Verificado no aparelho: depois do login, `trovatacast.auth.xml` tem `refresh_expires_at` no valor máximo e o refresh token é do tipo `Offline`. A empresa escolhida já era persistida. `AuthRepository` passou a depender da interface `AuthStorage` para a regra ter teste (`AuthSessionTest`, 7 casos).

**Convite sem o diálogo de login na frente.** Na vitrine publicada, o cliente que abre o convite via "Fazer Login" por cima da página, e medi que o diálogo bloqueia o botão "Entrar na chamada" (`body` com `pointer-events: none`, o clique no centro do botão cai no overlay). A correção, na branch `fix/catalogo-link-login-no-convite-ao-vivo` do `sfa_front`, faz o login do carrinho com o e-mail da sessão já na chegada do convite, com a mesma trava `cartLoginPending`. Verificado localmente: nenhum diálogo, carrinho logado, botão clicável e a barra mostrando o nome do vendedor.

---

## 6. Limitações conhecidas

- A página do seller segue `navigate` do cliente como o cliente segue o seller. É co-presença simétrica, e pode puxar o vendedor de rota. Se incomodar, o filtro é por `role` em `handleMessage`.
- `live` e `embed` não sobrevivem a links internos que não repassam a query. A sessão sobrevive por `activeToken` e pelo modo embutido pegajoso, mas um recarregamento da WebView depois disso volta ao modo cliente. Se virar problema, a solução é o router preservar a query nas rotas públicas.
- Traço com âncora `viewport` só coincide se as duas janelas tiverem a mesma proporção. É o último recurso, para páginas sem produto na tela.
- O cliente não tem como apagar um desenho do vendedor. Ele some quando o vendedor apaga ou muda de rota.
- Sem TURN, a chamada continua falhando em NAT simétrico, como antes.
