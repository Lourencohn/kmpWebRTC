# Status atual

> Snapshot do estado da construção. Atualizar sempre que fechar um milestone.

---

## Resumo

| Milestone | Status | PR / commit | Notas |
|---|---|---|---|
| M0 — Esqueleto e tooling | ✅ concluído | init scaffold | 5 superfícies sobem, 5 jobs CI verdes |
| M1 — Componentes do design system | ✅ concluído | m1 design system | Pill, Btn, ProductCard, VideoTile, RemotePointer, TabBar, Garments, DesignSystemPreview |
| M2 — Sessões (Home do vendedor) | ✅ concluído | m2 sessions | SellerHome + IncomingCall + SessionPrep + SessionsViewModel + SampleSessions |
| M3 — Catálogo + convite (link da sessão) | ✅ concluído | m3 catalog & invite | CatalogPicker + Invite + Voyager nav + REST + SQLDelight + webBuyer arrival |
| M4 — Web buyer mínimo | ✅ concluído | m4 web buyer + share + qr | Share intent nativo + QR + erros finos + persistência + 33 testes |
| M5 — Sinalização + handshake WebRTC | ✅ concluído | m5 webrtc | WS room + SignalingClient (Kotlin/TS) + WebRTC nativo (webrtc-kmp + browser) + LiveCall + CocoaPods |
| M6 — Áudio P2P | ✅ concluído | `45d455f` m6 | Mute na UI dos dois lados via DC `Mute` + áudio remoto anexado (`onTrack` no app, `<audio autoplay>` no web) + banner refinado |
| M7 — Co-presença básica | ✅ concluído | `7f1f6bc` m7 | DC `Scroll` (lossy ~30Hz, `sample(33)`) + `PointAt` (ordered) + catálogo grid em LiveCallScreen + productCard.ts no web + halo no cliente |
| Infra — Signaling em produção | ✅ concluído | (sem commit ainda) | Fly.io `gru` 256MB sempre-on, HTTPS automático, Dockerfile multi-stage, deploy a partir da raiz |
| Infra — webBuyer no Cloudflare Pages | ✅ concluído | `578c97f` | `https://trovatacast-buyer.pages.dev` no ar via GH Actions + Wrangler; Fly `PUBLIC_BUYER_URL` setado; cleartext Android e ATS iOS limpos |
| M8 — Carrinho ao vivo | ✅ concluído | (sem commit ainda) | DC `Navigate` + `CartUpdate` + `BuyerProductDetail` (sheet) + carrinho dock no cliente + gaveta no vendedor + `CartRepository` (SQLDelight) + toast |
| M9 — Encerrar com pedido pronto | 🟡 fase 2 parcial | (sem commit ainda) | Fase 2: `OrderRepository` (SQLDelight) + "Pedidos fechados hoje" na Home + `POST /order` (mem) + buyer envia + PDF via `window.print()`. Push fica pra M11 |
| M10 → M12 | ⏳ pendente | — | ver `docs/06-roadmap.md` |
| Integração Catálogo Link — Fase 1 (catálogos reais) | ✅ concluído | (sem commit ainda) | `CatalogLinksApi` + `CatalogLinkPickerScreen` sobre `GET /empresa/{slug}/catalogos-links`; picker de produtos aposentado; `Company.slug` |
| Integração Catálogo Link — Fase 2 (convite) | ✅ concluído | (sem commit ainda) | `POST /session` validando contra o Laravel real, Bearer do vendedor repassado, convite apontando para o `sfa_front` |
| Integração Catálogo Link — Fase 4a (vitrine real no app) | ✅ concluído | (sem commit ainda) | `VitrineApi` sobre a rota pública `/vitrine`; a `LiveCall` mostra os produtos do catálogo link, com preço e ordem do próprio catálogo, paginados |
| Infra — ICE configurável (pré-TURN) | ✅ concluído | (sem commit ainda) | `iceServers` viajam em `POST /session` e `GET /session/{token}`; app e buyer consomem; TURN entra por variável de ambiente |
| Limpeza — pedido paralelo removido | ✅ concluído | (sem commit ainda) | `POST /order`, `OrderStore` e os DTOs de submissão saíram; buyer perdeu os módulos órfãos da vitrine antiga |
| Integração Catálogo Link — Fase 4b (grade real) | ✅ concluído | (sem commit ainda) | `VitrineApi.grade()` sobre `/produtos/{id}/grades`; detalhe do produto na chamada mostra cores e tamanhos reais com saldo, no lugar dos dados fabricados |
| Integração Catálogo Link — Fase 0 (contrato) | ✅ concluído | (sem commit ainda) | `SessionCreateRequest` por identidade do catálogo link, `CatalogRoute`+`ViewState`, `Scroll` ancorado, `CartInvalidated`/`OrderPlaced`, URL de convite no `sfa_front`. Ver `prompt.md` e `docs/10-integracao-catalogo-link.md` |
| Release de demonstração (app enxugado para televenda) | ✅ concluído | (sem commit ainda) | Três abas (Sessões, Catálogos, Clientes), catálogo local e painéis sem fonte de dados removidos, telas do protótipo aposentadas |
| Carrinhos abertos (item 1 de `docs/12-proximas-telas.md`) | ✅ concluído | (sem commit ainda) | Quarta aba sobre `GET empresa/{slug}/carrinhos`, com "Chamar ao vivo" reaproveitando o caminho de convite |

---

## O que foi entregue no M0

### Estrutura
```
trovatacast/
├── protocol/                ← SessionEvent + Codec compartilhado
├── composeApp/              ← KMP Android + iOS targets
├── iosApp/                  ← Bootstrap Xcode (XcodeGen)
├── webBuyer/                ← Vite + TypeScript
├── signalingServer/         ← Ktor (porta 8080)
├── gradle/libs.versions.toml← Catálogo central de versões
├── .github/
│   ├── workflows/ci.yml     ← 5 jobs em paralelo
│   ├── dependabot.yml
│   ├── CODEOWNERS
│   ├── pull_request_template.md
│   └── ISSUE_TEMPLATE/
├── CONTRIBUTING.md
├── bootstrap.sh             ← setup auto-suficiente (baixa gradle se preciso)
└── README.md
```

### Tokens do design system (em código)
- Cores: `app.trovata.cast.theme.LightColors` (Kotlin) + `webBuyer/src/styles/tokens.css` (CSS).
- Tipografia: `app.trovata.cast.theme.DefaultType` (Geist fallback → sistema).
- Raios e espaçamentos: `app.trovata.cast.theme.DefaultRadii` / `DefaultSpacing`.
- Acesso via `TrovataTokens.{colors,type,radii,spacing}` em qualquer composable.

### Aceitação validada
- `./gradlew :protocol:jvmTest` → 2 testes verdes.
- `./gradlew :signalingServer:test` → 1 teste verde, `GET /health` retorna `200 ok`.
- `(cd webBuyer && npm run build)` → bundle gerado em `dist/`.
- iOS: `iosApp.xcodeproj` gerado por XcodeGen, Compose UI bridgeada via `MainViewControllerKt.MainViewController()`.

### Dívidas conhecidas (não-bloqueantes)
- **Fontes Geist**: slots prontos, TTFs não versionados (licença). Ver `composeApp/src/commonMain/composeResources/files/fonts/README.md`.
- **Pendências para CI**: jobs já configuradas mas não rodaram em PR ainda (sem remote configurado).
- **Coturn/TURN server**: não setup; vai entrar antes do M5 (sinalização + handshake).
- **Persistência server-side**: Postgres + Exposed + Flyway declarados em libs.versions.toml mas não wired (entra em M3).

---

## O que foi entregue no M1

### Componentes (commonMain)
- `Pill` — tones neutral/brand/jade/live (pulsa)/ghost/dark, ícone 12dp opcional.
- `Btn` — kinds primary/jade/soft/ghost/surface/dark/danger × sm/md/lg, ícone opcional.
- `IconBtn` — toolbar circular, kinds soft/dark/danger/jade/brand/line + estado active.
- `Avatar` — iniciais sobre fundo OKLCH por hue (consistência por usuário).
- `TrovataCard` — superfície + borda + sh1.
- `SectionLabel` — small caps Ink4 + ação opcional à direita.
- `ScreenHeader` — title + subtitle + eyebrow + trailing.
- `Garment` — 9 silhuetas (shirt, polo, tee, dress, jacket, pants, shoe, sweater, skirt) renderizadas como `ImageVector` parametrizado por tint, mantendo acentos com alpha.
- `ProductCard` — sm/md/lg com `highlight`, `pointed`, `inCart`, tag (Novo / Top venda / Pré-venda), bullets de cor + `+N`.
- `ProductRow` — variante linha (sm/md/lg) para carrinho/histórico.
- `VideoTile` — gradiente radial OKLCH simulando rosto + ombros + chip de nome com dot (verde/cinza); aspecto 1:1.34 e modo `mini` 1:1.
- `RemotePointer` — seta filled em OKLCH + label colorido, posicionável via `offset`.
- `TabBar` — Sessões / Catálogo / Clientes / Insights, light e dark.

### Ícones
- `app.trovata.cast.ui.icons.TrovataIcons` — 47 ícones (mesmo set do `prototype/ui.jsx`) como `ImageVector` stroke-only (1.6dp default, round caps/joins).

### Catálogo de amostra
- `app.trovata.cast.data.sample.SampleCatalog` — Coleção Outono · Atelier Norte com 9 SKUs (espelha `prototype/catalog.jsx`).
- `FashionPalette` — 8 tintas (sand, sage, walnut, slate-blue, terracota, graphite, mustard, moss).
- `ProductSwatchPalette` — 4 cores de bullet padrão.

### Cor
- `app.trovata.cast.ui.color.oklch(l%, c, h)` — conversor OKLCH → linear sRGB → sRGB cross-platform, alimenta Avatar, VideoTile, RemotePointer e borda de "apontando".
- `HueRoles` — vendedor=30, cliente=210, neutro=220.

### Preview
- `app.trovata.cast.ui.screens.preview.DesignSystemScreen` — storybook lazy column com todas as variantes; `App.kt` agora renderiza essa tela em iOS + Android.

### Aceitação validada
- `./gradlew :composeApp:compileDebugKotlinAndroid` ✅
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` ✅
- `./gradlew :composeApp:assembleDebug` ✅
- Comparação visual com `prototype/index.html` — match em proporções, tipografia, hierarquia de cor.

---

## O que foi entregue no M2

### Sample data (commonMain/data/sample/)
- `SampleSessions` — 8 clientes (Diego, Renata, Paulo, Marcia, Luciana, Eduardo, Helena, João) com `SessionClient(hue)`.
- `LiveWaitingSession` — chamada em curso (Diego, sala aberta há 1m 12s).
- `UpcomingSession` — 3 hoje (`Próximas`) + 3 nesta semana (`Esta semana`), com tags `Reposição` / `Primeira sessão` / `Top venda`.
- `HistorySession` — 2 fechadas hoje (`Fechado` / `EmRevisao`).
- `SessionPrepData` + `SessionChecklistItem` — checklist (catálogo, áudio, conexão, tabela) com toggle.

### ViewModel (commonMain/feature/sessions/)
- `SessionsViewModel` — 3 `StateFlow`s (`home`, `incoming`, `prep`) com `MutableStateFlow.update {}`.
- Ações: `selectTab`, `acceptIncoming`, `declineIncoming`, `rescheduleIncoming`, `toggleChecklistItem`.

### Telas (commonMain/ui/screens/sessions/)
1. **`SellerHomeScreen`** — header com wordmark mark + eyebrow "Atelier Norte · Outono 26", "Iniciar nova sessão" (primary lg), `Agora` (card live com Atender Jade), `Próximas`, `Esta semana`, `Histórico`, FAB flutuante "Convidar cliente" e `TabBar` fixa.
2. **`IncomingCallScreen`** — fundo dark, avatar pulsante, info do cliente, `Recusar` (Danger lg) + `Atender` (Jade lg) lado a lado, `Reagendar` (Dark md). Mostra banner de status após cada ação.
3. **`SessionPrepScreen`** — header de voltar, card de cliente com horário, checklist clicável (toggle de pronto/falta), sugestões TrovataCast, sticky bar com "Chamar agora" (Jade quando tudo pronto, Soft+disabled caso contrário).

### App shell
- `App.kt` — `DemoSwitcher` flutuante no topo com 4 abas (`Home / Atender / Preparar / Design`) até M3 trazer navegação real.

### Aceitação validada
- `./gradlew :composeApp:assembleDebug` ✅
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` ✅
- 4 telas alternáveis em runtime via switcher; estado preservado entre trocas (ViewModel em `remember`).

---

## O que foi entregue no M3

### Protocol (`protocol/`)
- `SessionCreateRequest`, `SessionCreateResponse`, `SessionInfo`, `ErrorResponse` — DTOs serializáveis para criação/consulta de sessão.
- Testes de round-trip JSON (`SessionDtoTest`).

### Server (`signalingServer/`)
- `SessionStore` — store em memória com `ConcurrentHashMap`, TTL 24h, tokens alfanuméricos de 9 chars (sem ambiguidade visual).
- `POST /session` — cria sala, devolve `{ sessionId, token, url, expiresAtMs }`. URL aponta para `PUBLIC_BUYER_URL` (default `http://localhost:5173`).
- `GET /session/{token}` — devolve `SessionInfo` ou 404 com `ErrorResponse`.
- `SessionRoutesTest` — 4 cenários (criação, fetch, 404, payload vazio).

### Persistência local (`composeApp/`)
- Plugin `app.cash.sqldelight` ligado ao módulo.
- Schema `Sessions.sq` com `SessionEntity` + `SelectedProductEntity` (FK ON DELETE CASCADE).
- `DatabaseDriverFactory` (expect) + actuals Android (`AndroidSqliteDriver`) e iOS (`NativeSqliteDriver`).
- `SessionsRepository` com `observeAll(): Flow<List<StoredSessionRecord>>`, `persistCreated(...)`, `selectedSkus(id)`.

### HTTP client + remoto
- `HttpClientFactory` (Ktor 3) com `ContentNegotiation`, `Logging`, `HttpTimeout` e `Accept: application/json`.
- Engines: OkHttp no Android, Darwin no iOS.
- `ServerConfig.baseUrl` por plataforma (`10.0.2.2:8080` Android, `localhost:8080` iOS).
- `SessionsApi` com `SessionsApiResult<Ok|Fail>` mapeando 2xx vs corpo de erro.

### Catálogo + convite (UI)
- `CatalogPickerScreenModel` (Voyager `ScreenModel`) — produtos do `SampleCatalog`, seleção via `Set<String>`, filtros (`Todos`/`Novos`/`TopVenda`/`PréVenda`), submissão assíncrona (`isSubmitting`, `error`, `createdSession`).
- `CatalogPickerScreen` — `LazyVerticalGrid` 2 colunas, chips de filtro, bottom bar "Gerar link" com contador de SKUs, banner de erro top-center.
- `InviteScreen` — hero de sucesso, card com link mono e botões `Copiar link` + `Abrir teste`, CTAs `WhatsApp` / `Outro app`, próximos passos numerados, sticky "Concluir".
- Quando `createdSession` aparece no state, `LaunchedEffect` empurra `InviteScreen(record)` no `Navigator`.

### Navegação
- Voyager 1.1.0-beta03: `Navigator(SellerHomeRoute) { SlideTransition(it) }`.
- `navigation/Routes.kt`: `SellerHomeRoute`, `IncomingCallRoute`, `SessionPrepRoute(clientName)`. Cada Route consome `AppContainerHolder.current.sessionsViewModel` e empurra Telas dependentes.
- `DemoSwitcher` removido do `App.kt`.

### AppContainer (DI manual)
- `AppContainer(DatabaseDriverFactory)` cria DB, HTTP, `SessionsRepository`, `SessionsApi`, `SessionsViewModel` compartilhado.
- `AppContainerHolder` inicializado em `MainActivity` (Android) e `MainViewController` (iOS).
- Koin propositalmente adiado para milestone com mais grafo (provavelmente M5+).

### Web buyer (`webBuyer/`)
- `main.ts` lê `?t=<token>`, faz `GET ${VITE_SIGNALING_BASE ?? http://localhost:8080}/session/{token}`.
- `views/arrival.ts` renderiza 4 estados: landing (sem token), loading, arrival (com sessão), error (token inválido / 404).
- CTA "Entrar com áudio" pede `getUserMedia({audio:true})` (sem pipe WebRTC ainda — entra em M5).
- CSS estende `tokens.css` com cards, spinner, layout responsivo.

### Aceitação validada
- `./gradlew :protocol:jvmTest` ✅ (2 + 2 testes)
- `./gradlew :signalingServer:test` ✅ (1 + 4 testes)
- `./gradlew :composeApp:assembleDebug` ✅
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` ✅
- `(cd webBuyer && npm run build)` ✅ (`tsc --noEmit && vite build`)

### Dívidas conhecidas
- **Compartilhamento real do link** ainda é placeholder — botões WhatsApp / share intent precisam de plataforma (Android `ACTION_SEND`, iOS `UIActivityViewController`). Entra em M4 ou paralelo.
- **WebRTC handshake**: não conectado ainda. webBuyer pede mic mas não negocia DC. Próximo no M5.
- **TURN/STUN**: nada configurado. Idem M5.
- **Persistência de DraftSelection**: hoje a seleção vive só no `ScreenModel`. Persistir rascunhos é não-objetivo do M3; entra em M4 se a UX exigir.
- **Koin DI**: continua adiado.
- **Coil**: declarado, não usado ainda (sem imagens reais de produto).

---

## O que foi entregue no M4

### Share intent nativo (`composeApp/`)
- `ShareController` (expect class) + actuals Android (`Intent.ACTION_SEND` via `Intent.createChooser`) e iOS (`UIActivityViewController` apresentado no top view controller).
- `ActivityProvider` (Android-only) — `WeakReference<Activity>` populado em `onCreate`/`onResume`/`onDestroy` do `MainActivity`.
- `AppContainer.shareController` injeta em qualquer `Screen` que precise.
- iOS resolve `keyWindow → rootViewController → presentedViewController` recursivo para encontrar o topo.

### QR code (`composeApp/`)
- Lib `io.github.alexzhirkevich:qrose:1.0.1` (Compose Multiplatform).
- `QrCard(data, size)` em `ui/components` — `rememberQrCodePainter` com bolas circulares + pixels round-corner 0.5.

### InviteScreen rev2
- Seção nova "QR para o cliente" com `QrCard(record.url)` ao lado da explicação.
- Botões `WhatsApp` / `Outro app` agora chamam `share.share(text, "Catálogo TrovataCast")`.
- Botão `Enviar link` (substituiu "Abrir teste") compartilha só o URL.
- Botão `Copiar link` usa `LocalClipboardManager` real (multiplataforma).

### CatalogPickerScreenModel testável
- Refatorado: agora aceita `createSession: CreateSessionFn` e `persistSession: PersistSessionFn` (typealiases de função suspend), em vez de receber `SessionsApi` + `SessionsRepository` concretos. Permite fakes sem extrair interfaces nem usar mocks.
- Construído com `container.sessionsApi::createSession` e `container.sessionsRepository::persistCreated` na produção.

### Web buyer rev2
- `api/sessions.ts` — `fetchSession(baseUrl, token, deps)` com `SessionFetchError` discriminada por `kind` (`network`/`not_found`/`expired`/`server`/`malformed`); injeção de `fetchImpl` e `now` para testes.
- `storage/lastSession.ts` — `saveLastSession`/`loadLastSession`/`clearLastSession` com `localStorage` + fallback seguro quando indisponível.
- `views/arrival.ts` — assinaturas com `ArrivalActions` injetáveis; `renderError` mostra título por `kind` e label de retry contextual; `renderLanding` exibe botão "Voltar para a última sessão" se `lastToken`.
- `main.ts` — leitura do token, retomada do último, rerun após erro, atualização de URL via `history.replaceState`, request de mic apenas para validar permissão (stoppa o stream).
- Novos estilos: `arrival-cta--ghost` para o botão de retomar.

### Testes
- **Kotlin commonTest** (`:composeApp:testDebugUnitTest`): `CatalogPickerScreenModelTest` — 8 testes cobrindo `toggle`, `setFilter`, `skuCount`, `clearError`, `generateLink` (vazio, happy path, falha do servidor) e `consumeCreatedSession`. Usa `StandardTestDispatcher` + `Dispatchers.setMain`.
- **TypeScript vitest** (`npm test`): 25 testes em 3 arquivos.
  - `api/sessions`: 8 testes (happy path, trailing slash, 404, 500, network fail, JSON quebrado, schema errado, expirado).
  - `views/arrival`: 11 testes (todos os 4 estados, ação de retry, callback de reopen-last, ação de mic happy/fail, escape de HTML).
  - `storage/lastSession`: 6 testes (roundtrip, vazio, JSON corrompido, schema inválido, clear, storage null).
- **CI**: workflows atualizados — `web-buyer` roda `npm test` antes de `npm run build`; `android` roda `:composeApp:testDebugUnitTest` antes de `assembleDebug`.

### Aceitação validada
- `./gradlew :protocol:jvmTest` ✅ (4 testes)
- `./gradlew :signalingServer:test` ✅ (5 testes)
- `./gradlew :composeApp:testDebugUnitTest` ✅ (8 testes)
- `./gradlew :composeApp:assembleDebug` ✅
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` ✅
- `(cd webBuyer && npm run typecheck && npm test && npm run build)` ✅ (25 testes)

### Dívidas conhecidas
- **iPad popoverPresentationController** para `UIActivityViewController`: não setado — abre como sheet só no iPhone. iPad faria crash sem source view. Fix antes de qualquer release iPad.
- **Compartilhamento durante navegação**: `Intent.createChooser` no Android exige Activity viva (capturada via `ActivityProvider`); se a Activity for destruída e o singleton segurar uma stale ref, o share vira no-op. Aceitável para M4.
- **Persistência de mídia**: o webBuyer hoje só faz `getUserMedia` para checar permissão e libera os tracks. O stream real entra em M5.
- **Reabrir sessão expirada**: se o backend já removeu a sessão (TTL 24h), o usuário cai em `not_found` e o lastSession é limpo. Não há mensagem específica para "passou da validade no servidor" vs "validade local". Aceitável.

---

## O que foi entregue no M5

### Protocolo
- `protocol/Signaling.kt` — sealed class `SignalingMessage` com 10 variantes (`Hello`, `RoomState`, `PeerJoined`, `PeerLeft`, `Offer`, `Answer`, `IceCandidate`, `Bye`, `PresencePing`, `ProtocolError`).
- `SignalingJson` polimórfico via `classDiscriminator = "type"`.
- 6 testes round-trip (`SignalingDtoTest`).

### Servidor
- `signalingServer/Room.kt` — `RoomPeer`, `Room` (max 2 peers, sem roles duplicadas, mutex-guarded) + `RoomManager` global.
- `signalingServer/SignalingWebSocket.kt` — `/ws/session/{token}` valida token via `SessionStore`, requer `Hello` como primeira mensagem, broadcasta `RoomState`/`PeerJoined`/`PeerLeft`, relaya `Offer`/`Answer`/`IceCandidate`/`PresencePing`/`Bye` (com roteamento opcional `to`).
- WebSockets plugin instalado com ping/timeout (20s/30s).
- 7 testes (`SignalingWebSocketTest`): token desconhecido, hello+roomState, dois peers + relay + bye, sala cheia, role duplicada, peerLeft, payload completo.

### App (composeApp/)
- **CocoaPods** ligado: `kotlin-cocoapods` plugin + `cocoapods { pod("WebRTC-SDK", "125.6422.07") { linkOnly = true } }`. `iosApp/Podfile` consome o `composeApp` Podspec gerado + WebRTC.
- **webrtc-kmp** (`com.shepeliev:webrtc-kmp:0.125.10`) adicionado em commonMain — funciona em Android (AAR `io.github.webrtc-sdk`) e iOS (WebRTC.framework via Pods).
- **HttpClient** atualizado: `install(WebSockets)` no Ktor client.
- `data/signaling/SignalingClient.kt` — wrapper KMP da WebSocket com `StateFlow<SignalingState>`, `SharedFlow<SignalingMessage>`, listener UNDISPATCHED para deterministic startup. 7 testes (`SignalingClientTest`) com `FakeTransport`, `UnconfinedTestDispatcher` + `runCurrent`.
- `data/signaling/KtorSignalingTransport.kt` — implementação real via `webSocketSession`.
- `feature/call/PeerSession.kt` — orquestra `webrtc-kmp` PeerConnection: cria offer (buyer-side), responde answer (seller-side), troca ICE, abre DC `presence`, dispara `PresencePing` via signaling a cada 5s, transiciona `PeerSessionState` (Idle/Negotiating/Connected/Failed/Closed) via `onIceConnectionStateChange`.
- `feature/call/LiveCallScreenModel.kt` + `ui/screens/call/LiveCallScreen.kt` — tela dark com status pill, avatar com borda colorida, debug card (sinalização + peer), botão `Encerrar`. Navegável de `InviteScreen` via novo botão "Iniciar chamada" (Jade).

### Web buyer
- `signaling/messages.ts` — tipos discriminados espelhando o protocolo Kotlin.
- `signaling/client.ts` — `SignalingClient` com fila pré-open, status discriminado (`idle`/`connecting`/`connected`/`failed`/`closed`), 8 testes (`client.test.ts`) com `FakeSocket`.
- `webrtc/peer.ts` — `PeerSession` com `RTCPeerConnection`, STUN, presence loop via DC, ICE restart, perfect-negotiation lite. 10 testes (`peer.test.ts`) com `FakePeerConnection` + `FakeDataChannel`.
- `main.ts` — após pedir mic, instancia SignalingClient + PeerSession, mostra banner colorido fixo no topo (`Conectando…` / `Conectado` / `Conexão caiu`), prende áudio remoto em `<audio autoplay playsinline>`, encerra tudo no `beforeunload`.
- CSS: `.connection-banner` com estados (`ink` default, `jade` connected, `live` failed, `warn` negotiating, `ink-3` closed).

### CI
- Job `web-buyer`: já incluído `npm test`.
- Job `android`: já roda `:composeApp:testDebugUnitTest` antes de `assembleDebug`.
- Job `ios-framework`: adiciona setup-ruby@v1 (3.3) + `gem install cocoapods` antes do `linkDebugFrameworkIosSimulatorArm64`. Cocoapods plugin executa `pod install` automaticamente para o synthetic project.

### Aceitação validada
- `./gradlew :protocol:jvmTest` ✅ (6 testes)
- `./gradlew :signalingServer:test` ✅ (12 testes)
- `./gradlew :composeApp:testDebugUnitTest` ✅ (15 testes)
- `./gradlew :composeApp:assembleDebug` ✅ (libjingle_peerconnection_so.so empacotado)
- `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` ✅ (WebRTC.framework via Pods)
- `(cd webBuyer && npm run typecheck && npm test && npm run build)` ✅ (43 testes)

### Como testar manualmente
1. Subir o server: `./gradlew :signalingServer:run` (porta 8080).
2. Subir o webBuyer: `cd webBuyer && npm run dev` (porta 5173).
3. Abrir o app Android no emulador e gerar um link (Home → Iniciar nova sessão → Catalog → Gerar link).
4. Tocar **Iniciar chamada** na InviteScreen. App entra em LiveCall como Seller (`SignalingState.Connecting → Connected`).
5. Abrir o link gerado no Chrome do celular ou desktop. Clicar **Entrar com áudio** → banner azul → cria offer.
6. Banner verde em ambos os lados quando ICE conecta. App muda `PeerSessionState` para `Connected`. DC `presence` aberto (presence ping de 5s via signaling).

### Dívidas conhecidas
- **STUN-only**: sem TURN server, NAT simétrico vai falhar. Entra no M10 (qualidade/reconnect).
- **`MediaDevices.getUserMedia` no Android**: webrtc-kmp pede `RECORD_AUDIO` em runtime — o `MainActivity` ainda não solicita explicitamente; usuário aceita no prompt do sistema. Adicionar `ActivityResultContracts.RequestPermission` no fluxo do LiveCall antes de produção.
- **iPad WebRTC**: não testado.
- **Estado `Negotiating` ↔ `Connected` flapping** durante `ICE Disconnected`: tratado pra mostrar "Conectando…" no banner, mas precisaria ICE restart automático (M10).
- **`getStats()` periódico**: declarado no PeerConnection mas não está sendo coletado — adicionar para modo economia (M10).
- **Mute/câmera**: vendedor não tem controle de microfone na UI ainda — entra no M6.
- **Áudio remoto no iOS**: `MediaStream` da `webrtc-kmp` é exposto via `PeerSession.remoteAudio` mas a UI não conecta a um `UIView` ainda; entra no M6.
- **`PeerSession` em tests**: testes unitários cobrem `SignalingClient` e os webBuyer's peer flows; o app-side `PeerSession` depende de `webrtc-kmp` que tem inicialização nativa, então não é facilmente unit-testable. Validação é manual + via testes do webBuyer (mesmo protocolo).

---

## O que foi entregue no M6

### App (`composeApp/`)
- `PeerSession.setLocalMuted(muted)` — desabilita os tracks de áudio locais (`MediaStreamTrack.enabled = false`) e propaga `DataChannelMessage.Mute` via DC `presence`.
- `PeerSession.onTrack` agora preenche `remoteAudio: StateFlow<MediaStream?>` — UI conecta ao `MediaStream` real no Android e iOS.
- `LiveCallScreen` ganhou botão de mic toggle + indicador visual quando o outro lado está mudo.

### Web buyer (`webBuyer/`)
- Botão de mute na LiveCall view + indicador remoto via DC.
- `<audio id="remote-audio" autoplay playsinline>` anexado dinamicamente em `attachRemoteAudio(stream)` no `main.ts`.
- Banner de status redesenhado: estados `connecting` / `connected` / `failed` / `closed` com cores próprias (`ink`, `jade`, `live`, `ink-3`).

### Aceitação validada
- Chamada de voz real funcionando entre app vendedor (Android e iOS) e cliente web no Chrome.
- Mute em um lado aparece imediatamente no outro.

---

## O que foi entregue no M7

### Protocolo (`protocol/`)
- `protocol/DataChannelMessage.kt` — sealed class com 3 variantes:
  - `Mute(muted, from)`
  - `Scroll(productId, offset, ts, from)`
  - `PointAt(productId, ts, from, durationMs)`
- `encode()` + `decodeDataChannel()` via Kotlinx Serialization polimórfica.
- `DataChannelMessageTest` — round-trip + tipos inválidos.

### App (`composeApp/`)
- `PeerSession.publishScroll(productId, offset)` — emite em `MutableStateFlow` throttled via `sample(33)` (≈30Hz) → manda DC binário.
- `PeerSession.publishPointAt(productId, durationMs)` — DC ordered, sem throttle.
- `remoteScroll: SharedFlow<Scroll>` e `remotePointAt: SharedFlow<PointAt>` emitidos para a UI.
- `LiveCallScreenModel` expõe ações + `pointedProductId` (com auto-clear pelo `durationMs`).
- `LiveCallScreen` agora renderiza grade do catálogo (`ProductCard` md) com:
  - Detecção de produto em foco via `LazyListState.firstVisibleItemIndex` + `firstVisibleItemScrollOffset` → publica scroll.
  - Botão "Apontar": ativa modo de seleção; tap em produto dispara `publishPointAt` + halo local de 3s.
  - Halo (`pointed = true` no `ProductCard`) quando o lado remoto aponta um produto.

### Web buyer (`webBuyer/`)
- `protocol/dataChannel.ts` — encode/decode equivalente ao Kotlin (Mute/Scroll/PointAt) + tipo discriminado por `type`.
- `data/catalog.ts` — catálogo cliente espelhando `SampleCatalog` Kotlin (9 SKUs, cores, tags).
- `ui/productCard.ts` — `renderProductCard(product, { pointed, inCart })` em HTML puro com os mesmos tokens do design system.
- `views/arrival.ts` (`renderLiveCall`) — grade scroll-snap dos produtos + `scrollToProduct(productId, offset)` (com `behavior: 'smooth'` quando offset pequeno) + halo `--pointed` aplicado.
- `main.ts.buildDataChannelHandler(view)` — roteia `mute` / `scroll` / `pointAt` para a `LiveCallView`; `pointAt` agenda `setTimeout` para limpar o halo após `durationMs ?? 3000`.
- `styles/app.css` — variáveis e classes para `.live-call`, `.live-call__grid`, `.product-card.is-pointed`, `.live-call__status` etc.

### Aceitação validada
- Vendedor rola o catálogo no app → cliente rola junto no Chrome (≈30Hz, sem jank visível).
- Vendedor toca "Apontar" + produto → cliente vê halo laranja por 3s, vendedor vê mesmo halo localmente.
- Mute, áudio e co-presença coexistem em uma única conexão WebRTC.

### Dívidas conhecidas
- **`getStats()` periódico** continua não coletado.
- **`Navigate` + `CartUpdate`** (M8) ainda não existem — `BuyerProductDetail` é placeholder.
- **TURN**: ainda STUN-only. Falhas silenciosas esperadas em NAT simétrico (operadoras móveis).
- **ICE restart automático**: ainda não implementado (M10).

---

## O que foi entregue na Infra (signaling em produção)

### Deploy
- `Dockerfile` na raiz: multi-stage (`gradle:8.11.1-jdk17` build → `eclipse-temurin:17-jre-alpine` runtime), imagem final ~72MB. `sed` remove `composeApp` do `settings.gradle.kts` antes do `gradle :signalingServer:buildFatJar` para não puxar o Android SDK.
- `fly.toml` na raiz: app `trovatacast-signaling`, região `gru`, `shared-cpu-1x` 256MB, `auto_stop_machines = "off"`, health check em `/health`.
- `.dockerignore` enxuga o contexto (exclui `iosApp`, `webBuyer`, `prototype`, `docs`, builds locais).

### URLs
- **Signaling**: `https://trovatacast-signaling.fly.dev` (HTTPS automático). WebSocket: `wss://trovatacast-signaling.fly.dev/ws/session/{token}`.
- `ServerConfig.baseUrl` (Android + iOS) e `VITE_SIGNALING_BASE` (web) apontam para a URL acima.

### Custo
- ~US$ 2/mês com 1 máquina shared-cpu-1x (rodar `fly scale count 1` se o Fly subiu 2 por padrão de HA).
- Fly.io não tem feature de "spend limit"; recomendado cartão virtual com teto se preocupação for crítica.

### Validação em produção (2026-05-22)
- iPhone vendedor em **3G** + buyer no Chrome desktop → `POST /session` 201, `wss://` 101 em ambos os lados, P2P estabelecido. Mac do vendedor não precisou estar rodando o signaling.
- Reconnect rudimentar funcionou: WS dropou (`NSURLErrorNetworkConnectionLost`) sob 3G ruim, app criou nova sessão automaticamente.

### Dívidas conhecidas
- **`PUBLIC_BUYER_URL`** não setado no Fly.io — `POST /session` ainda devolve URLs com `http://localhost:5173`. Setar quando webBuyer for hospedado.
- **Webbuyer ainda local** — depende de `npm run dev` no Mac do vendedor. Próximo passo natural pra eliminar o Mac (ver seção abaixo).
- **TURN não wired** — Cloudflare Realtime TURN tem 1TB/mês grátis; entrar antes de qualquer beta em rede móvel real.
- **Restart com perda de sessões in-memory** — `SessionStore` + `RoomManager` vivem só na RAM. Persistência Postgres entra em M9 (Order).
- **iOS `AURemoteIO StartIO failed (561145187)`** observado quando o WS reconecta após queda de rede — pipeline de áudio não retoma. Sintoma: a primeira chamada conecta; depois de um drop+retry, o áudio não inicia. Suspeitas: `AVAudioSession` não liberado entre tentativas ou categoria incorreta no `webrtc-kmp` iOS. Investigar antes do M10 (reconexão + qualidade) — combina com ICE restart.
- **Reconnect resiliente** — o app só refaz `POST /session` quando o WS quebra; precisaria reaproveitar token + sessão existente. Entra em M10.

---

## Infra — webBuyer no Cloudflare Pages

### Deploy
- Projeto Cloudflare Pages `trovatacast-buyer` (Direct Upload), bootstrap manual do primeiro `webBuyer/dist`.
- A partir de `578c97f`, cada push na `main` que toca `webBuyer/**` dispara [.github/workflows/web-buyer-deploy.yml](../.github/workflows/web-buyer-deploy.yml): `npm ci`, `npm run typecheck`, `npm test`, `npm run build`, `wrangler pages deploy dist --project-name=trovatacast-buyer --branch=main` via `cloudflare/wrangler-action@v3`.
- `VITE_SIGNALING_BASE` vem de `vars.VITE_SIGNALING_BASE` (com fallback hardcoded `https://trovatacast-signaling.fly.dev`).
- Secrets `CF_API_TOKEN` + `CF_ACCOUNT_ID` no GitHub.

### URLs
- **Buyer (prod)**: `https://trovatacast-buyer.pages.dev`
- **Preview por deploy**: `https://<sha-curta>.trovatacast-buyer.pages.dev` (impresso no log do workflow).

### Custo
- Cloudflare Pages plano grátis: 500 builds/mês + bandwidth ilimitado. Sem cartão.

### Limpezas que entraram junto
- Proxy `http://127.0.0.1:8080` removido de [webBuyer/vite.config.ts](../webBuyer/vite.config.ts).
- `cleartextTrafficPermitted` removido (arquivo `network_security_config.xml` excluído + atributo `android:networkSecurityConfig` removido do manifest) — Android HTTPS-only.
- `NSAllowsArbitraryLoads` removido de [iosApp/iosApp/Info.plist](../iosApp/iosApp/Info.plist); mantém apenas `NSAllowsLocalNetworking`.

### Fly.io
- `fly secrets set PUBLIC_BUYER_URL=https://trovatacast-buyer.pages.dev --app trovatacast-signaling` aplicado em 2026-05-22; rolling update de 1 máquina concluído com health check OK.
- `POST /session` agora devolve `url: "https://trovatacast-buyer.pages.dev/?t=<token>"`.

### Validação em produção (2026-05-22)
- Workflow `26318176020` verde em 32s.
- `curl -X POST https://trovatacast-signaling.fly.dev/session …` → token + URL apontando pra Pages.
- `curl -I https://trovatacast-buyer.pages.dev/?t=<token>` → `HTTP/2 200` servido pelo Cloudflare.

### Dívidas conhecidas
- `vars.VITE_SIGNALING_BASE` não setado no GitHub — o workflow está usando o fallback hardcoded. Se um dia o signaling sair do `trovatacast-signaling.fly.dev`, precisa criar a variável de repo (`Settings → Secrets and variables → Actions → Variables`).
- Sem `Preview` deploys para PRs (só `main`). Para habilitar, mudar `branch=main` por `branch=${{ github.ref_name }}` e remover o filtro de `branches: [main]` no trigger.

---

## O que foi entregue no M8

### Protocolo (`protocol/`)
- `DataChannelMessage` ganhou duas variantes:
  - `Navigate(productId, ts, from)` — cliente abre detalhe → vendedor segue.
  - `CartUpdate(productId, size, units, ts, from)` — modelo absoluto por linha (`units=0` remove).
- Round-trips cobertos em `DataChannelMessageTest` (kotlin) e `dataChannel.test.ts` (vitest).

### App (`composeApp/`)
- `PeerSession.publishNavigate(productId)` + `publishCartUpdate(productId, size, units)`; observáveis `remoteNavigate: SharedFlow<Navigate>` e `remoteCartUpdate: SharedFlow<CartUpdate>` ligados ao mesmo DC `presence`.
- SQLDelight: nova tabela `CartLineEntity (sessionId, sku, size, units, updatedAtMs)` com FK em `SessionEntity`, índice em `sessionId`, e queries `upsertCartLine` (via `INSERT OR REPLACE` — dialect 3.18), `deleteCartLine`, `selectCartLinesBySession`, `clearCart`.
- `CartRepository` em `data/local/` com `observe(sessionId): Flow<List<CartLine>>`, `apply(...)`, `snapshot(...)`, `clear(...)`. Injetado em `AppContainer.cartRepository`.
- `LiveCallScreen` agora recebe também `sessionId` + `clientName`; `LiveCallScreenModel`:
  - escuta `peer.remoteNavigate` → abre `ProductDetailSheet` no vendedor (read-only, com pill "Cliente está vendo");
  - escuta `peer.remoteCartUpdate` → calcula delta usando snapshot anterior, persiste no `CartRepository`, emite `CartToast` (auto-dismiss 3.5s) com mensagem do tipo `"Diego adicionou 12un de AN-104 (M)"`;
  - `state.cart` reflete `cartRepository.observe(sessionId)` direto da DB.
- `LiveCallScreen` ganhou:
  - **Botão de carrinho** na action bar com badge contando unidades (tom Jade quando > 0).
  - **Gaveta de carrinho** (`CartDrawer`) — modal bottom sheet com `ProductRow` por linha, `Tam X` + subtotal por linha, barra "Total" formatado em R$ + contagem.
  - **`ProductDetailSheet`** — modal bottom sheet renderiza `ProductRow` Lg + chips de tamanho mostrando unidades já no pedido.
  - **`CartToastView`** — banner Jade flutuante no topo.

### Web buyer (`webBuyer/`)
- `cart/store.ts` — `CartStore` (Map keyed por `productId/size`, modelo absoluto), `subscribe(listener)`, `snapshot(): { lines, totalUnits, totalCents }`, helper `formatBrl()` e `unitPriceCentsFor()`.
- `ui/productDetail.ts` — `mountProductDetail(host, product, lines, actions)` retorna `ProductDetailView` com `setCurrentUnits(size, units)` e `destroy()`. Chips de tamanho mostram unidades atuais; stepper de qty respeita MOQ; botão muda label entre "Adicionar Nun", "Atualizar para Nun", "Reduzir para Nun".
- `ui/cartSheet.ts` — `mountCartSheet(host, snapshot, actions)` renderiza `<ul class="cart-lines">` com botão "Remover" por linha + `Subtotal` no rodapé. Estado vazio mostra hint amigável.
- `views/arrival.ts.renderLiveCall`:
  - `ProductCard` agora é clicável (chama `onOpenProduct(productId)`).
  - **Cart dock** sticky entre o catálogo e o action bar — mostra `Nun · R$ X,XX · Ver pedido →`, escondido quando carrinho vazio.
  - `LiveCallView` ganhou `setCart(snapshot)` + `host()` (retorna o container `.sheet-host` para montar overlays).
- `main.ts`:
  - Cria `CartStore` por sessão; `subscribe` atualiza dock + sheet de carrinho.
  - Tap em produto → abre `ProductDetailSheet` localmente + envia `navigate`.
  - "Adicionar" no detalhe → `cart.apply(...)` local + envia `cartUpdate` (absoluto).
  - "Remover" no cart sheet → `cart.apply(... units=0)` + envia `cartUpdate(units=0)`.
  - `buildDataChannelHandler` ganhou casos `navigate` (abre detalhe) e `cartUpdate` (aplica no store local + atualiza chip do detalhe se aberto).
- CSS — `.cart-dock`, `.sheet-overlay`, `.sheet`, `.product-detail-{preview,sizes,size,stepper,step,qty,add}`, `.cart-{lines,line,total,empty}`. Sheets sobem com `transform: translateY` + scrim semi-opaco; tap fora dispensa.

### Aceitação validada
- `./gradlew :protocol:jvmTest` ✅ (9 testes; 3 novos de `Navigate`/`CartUpdate`)
- `./gradlew :signalingServer:test` ✅ (12 testes; sem mudança)
- `./gradlew :composeApp:testDebugUnitTest` ✅ (15 testes)
- `./gradlew :composeApp:assembleDebug` ✅
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` ✅
- `(cd webBuyer && npm run typecheck && npm test && npm run build)` ✅ (54 testes; 11 em `dataChannel`)

### Como testar manualmente
1. Subir backend: `./gradlew :signalingServer:run` (porta 8080).
2. Subir webBuyer: `cd webBuyer && npm run dev` (porta 5173).
3. App Android/iOS → Home → Iniciar nova sessão → Catalog → Gerar link → **Iniciar chamada**.
4. Abrir o link gerado em outra aba/celular → **Entrar com áudio**.
5. ICE conecta → estado Jade nos dois lados.
6. **Cliente**: tocar num `ProductCard` → sheet de detalhe abre; **vendedor**: sheet de detalhe abre também (Navigate).
7. **Cliente**: escolhe tamanho, ajusta qty respeitando MOQ, toca "Adicionar" → cart dock aparece com `Nun · R$X,XX`.
8. **Vendedor**: toast Jade flutua no topo (`"Diego adicionou 12un de Blusa Tricot Canelado (M)"`), badge do carrinho na action bar ganha contagem.
9. **Vendedor**: toca botão de carrinho → `CartDrawer` lista o pedido com subtotal por linha + total geral.
10. **Cliente**: toca dock → `mountCartSheet` lista o pedido, "Remover" emite `CartUpdate(units=0)` → vendedor recebe toast de remoção.

### Dívidas conhecidas
- **Cores não modeladas** — `BuyerProductDetail` só pede tamanho (espelhando o protótipo). Se `Product.colorCount > 1` precisar diferenciar SKUs por cor, adicionar `color` no DC `CartUpdate` + chave composta `(productId, size, color)` na DB.
- **Vendedor não edita carrinho** — sheet do detalhe é read-only no vendedor; nenhuma chamada a `peer.publishCartUpdate` parte do lado dele. M9 (encerrar com pedido pronto) precisa decidir se o vendedor pode ajustar antes de confirmar.
- **Cart não restaura no buyer** — se a aba do navegador reload, o `CartStore` reseta (memória). Persistir em `localStorage` por token entraria em M9 ou M11 (catálogo assíncrono).
- **Snapshot do estado na reconexão** — quando WS dropa e reconecta, o vendedor relê DB, mas o buyer perde o cart. Sincronizar via DC após reconnect entra em M10.
- **Toast empilhado** — alterações sucessivas escondem o toast anterior. Aceitável; uma fila pequena ficaria melhor em M9.
- **`SelectedProductEntity` vs `CartLineEntity`** — agora coexistem (seleção inicial × pedido). Em M9 o `Order` deve ser materializado a partir do cart, e a seleção inicial pode virar histórico do "que foi mostrado".

---

## O que foi entregue no M9 — fase 1 (finalização da venda)

> Acordo com o PO: fase 1 confirma o pedido P2P + mostra resumo dos dois lados. Persistência server-side, PDF e push entram na fase 2.

### Protocolo (`protocol/`)
- `DataChannelMessage.OrderConfirm(orderId, ts, from, lines, totalCents)` — snapshot transacional do pedido enviado pelo vendedor.
- `OrderLine(productId, size, units, unitPriceCents)` (top-level `@Serializable`) com `subtotalCents` derivado.
- Round-trip Kotlin + TS, incluindo caso de lista vazia e payload com linha inválida.

### App (`composeApp/`)
- `PeerSession.publishOrderConfirm(message)` + `remoteOrderConfirm: SharedFlow<OrderConfirm>` ligado ao mesmo DC `presence`. Retorna `false` se DC não está open (vendedor evita estado inconsistente).
- `LiveCallUiState.summary: OrderSummaryUi?` + reducer fecha gaveta de carrinho e sheet de detalhe quando summary aparece.
- `LiveCallScreenModel.confirmOrder()`:
  - Bloqueia se `summary != null` ou `cart.isEmpty()`.
  - Constrói `OrderLine` lendo `priceCentsFor` do `SampleCatalog` (parser BRL local — `"R$ 89,90"` → `8990L`).
  - Gera `orderId` no formato `ORD-{base36(tsLow6)}-{4ASCII}`.
  - Publica DC + grava estado local (`confirmedByMe = true`).
- `LiveCallScreenModel` também escuta `remoteOrderConfirm` (defensivo) — `confirmedByMe = false` no caso de simetria.
- `LiveCallScreen`:
  - `CartDrawer` ganhou CTA `Btn Jade Lg` **"Confirmar pedido · R$ X,XX"** abaixo do total quando há itens.
  - `OrderSummaryOverlay` cobre a tela inteira em `colors.bg` quando `state.summary != null`: pill jade ("Você confirmou" / "Cliente confirmou"), título "Pedido pronto", lista de `ProductRow` com subtotal, barra inferior com total + botão "Fechar e encerrar" (chama `onHangup` que pop'a a tela).

### Web buyer (`webBuyer/`)
- `ui/orderSummary.ts` — `mountOrderSummary(host, payload, sellerName, { onClose })` cria overlay full-screen em z-index 60. Badge verde com check, título "Pedido pronto", `${sellerName} confirmou o seu pedido.`, `code` com `orderId`, lista de linhas com subtotal, footer com total grande + botão "Fechar" (chama `cleanup` + `showLanding`).
- `main.ts`:
  - `SheetController` ganhou campo `summary: OrderSummaryView | null`.
  - `showSummary(payload)` destrói detail + cart sheets, monta summary fresh, e `onClose` faz `cleanup('order_closed')` + `showLanding()`.
  - `buildDataChannelHandler` ganhou case `orderConfirm` → `showSummary(...)`.
  - `cleanup` também destroi `sheets.summary`.
- CSS — `.order-summary*` (rise animation, header com badge OKLCH-like, lista de linhas em cards, footer com total grande mono + botão Jade pill-shape). Respeita safe-area-inset top e bottom.

### Aceitação validada
- `./gradlew :protocol:jvmTest` ✅ (11 testes; 2 novos de `OrderConfirm`)
- `./gradlew :composeApp:testDebugUnitTest` ✅ (15 testes)
- `./gradlew :composeApp:assembleDebug` ✅
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` ✅
- `(cd webBuyer && npm run typecheck && npm test && npm run build)` ✅ (57 testes; 14 em `dataChannel`)

### Como testar manualmente
1. Subir backend + abrir webBuyer (ou usar produção em `trovatacast-buyer.pages.dev` após deploy).
2. App vendedor: gerar link → iniciar chamada.
3. Cliente: entrar → adicionar 1-3 produtos via `ProductDetail` + stepper.
4. Vendedor: tocar no badge do carrinho → `CartDrawer` mostra linhas + total. Tocar **"Confirmar pedido · R$ X,XX"** (Jade).
5. **Vendedor**: `OrderSummaryOverlay` cobre a tela com pill "Você confirmou" + lista + total + botão "Fechar e encerrar".
6. **Cliente**: tela se transforma em `OrderSummaryOverlay` (badge verde + lista + total + botão "Fechar").
7. Qualquer lado: tocar "Fechar" → desliga WebRTC, vendedor volta pra Invite, cliente volta pra landing.

### Dívidas conhecidas (entram na fase 2)
- **Persistência server-side** — `OrderConfirm` ainda não bate em `POST /order`. `SessionStore` no Ktor não conhece `Order`. Próximo passo: endpoint + tabela em memória (mantendo padrão do `SessionStore` + Postgres com Exposed/Flyway esperando).
- **PDF no cliente** — `OrderSummary` web não baixa nada. Caminho mais leve: stylesheet `@media print` + botão "Imprimir / Salvar PDF" usando `window.print()`. Sem deps extras.
- **Persistência local no vendedor** — `OrderSummaryUi` vive só na memória do `screenModel`. Se o app fechar antes do tap em "Fechar e encerrar", o pedido some. Tabela `OrderEntity` + `OrderLineEntity` em SQLDelight resolve.
- **Push do pedido** — vendedor não recebe notificação posterior. Depende de persistência server-side primeiro.
- **Métricas da sessão** — "tempo em foco por SKU", contagem de `PointAt`, etc., ainda não são coletadas. Precisa de `SessionEventLog` em commonMain (M12 colocará isso a serviço do Pipeline).
- **Edição pós-confirmação** — uma vez `summary` setado, não dá pra voltar atrás na fase 1. Aceitável; M11 (catálogo assíncrono) pode reabrir.
- **Sem replay no buyer reconectado** — se o cliente perder a conexão depois do `OrderConfirm`, ele cai em landing sem ver o resumo. Snapshot via DC após reconnect (M10) cobre isso.

---

## O que foi entregue no M9 — fase 2 (persistência + recibo)

> Acordo com o PO: fase 2 cobre persistência local (vendedor) + server (em memória) + PDF no cliente. Push posterior do pedido fica pra M11/M19.

### Protocolo (`protocol/`)
- `protocol/Order.kt` — `OrderSubmissionRequest`, `OrderSubmissionResponse`, `OrderRecord`, `enum class OrderSource { Buyer, Seller }`. Reusa `OrderLine` do `DataChannelMessage.kt`.
- `OrderDtoTest` — 3 testes de round-trip (`OrderSubmissionRequest`, `OrderSubmissionResponse`, `OrderRecord`).

### Persistência local (`composeApp/`)
- `commonMain/sqldelight/.../Orders.sq` — `OrderEntity(orderId, sessionId, sessionToken, clientName, clientShop, sellerName, totalCents, confirmedByMe, createdAtMs)` + `OrderLineEntity(orderId, productId, size, units, unitPriceCents)` com FK em `OrderEntity`. Índices em `createdAtMs` + `sessionId`. Queries `insertOrder`, `insertOrderLine`, `selectOrderById`, `selectOrdersBetween`, `selectAllOrders`, `selectOrderLines`, `deleteOrder`.
- `data/local/OrderRepository.kt` — `persist(...)` em uma transação (insert + linhas), `observeBetween(from, until)` retorna `Flow<List<StoredOrder>>` materializando linhas em memória, `observeAll()`, `get(orderId)`.
- `AppContainer.orderRepository` injeta no `LiveCallScreenModel` e no `SessionsViewModel`.
- `LiveCallScreenModel.persistOrder(...)` — idempotente (re-checa via `get(orderId)` antes de inserir); dispara tanto no `confirmOrder()` quanto no fluxo `remoteOrderConfirm` (defensivo).

### Home do vendedor — "Pedidos fechados hoje"
- `SessionsViewModel(orderRepository, timeZone, now)` — calcula `todayWindow()` via `kotlinx.datetime` (LocalDate atStartOfDayIn) e exposes `SellerHomeUiState.closedToday: List<StoredOrder>` populado por `orderRepository.observeBetween(...)`. CoroutineScope próprio (`SupervisorJob() + Dispatchers.Default`) com `dispose()`.
- `SellerHomeScreen.ClosedTodaySection(orders)` — `TrovataCard` com:
  - Pill Jade no header: `${orders.size} · R$ X,XX`.
  - 1 linha por pedido: avatar quadrado Jade + check, nome do cliente · shop, `un · SKUs · HH:mm`, `orderId` em mono small, total em mono à direita.
  - Footer com `Total do dia · N un` + soma BRL.
- Helpers locais: `formatBrl(cents)` (sem dependências externas, BRL com separadores) e `formatTime(ms)` via `Instant.toLocalDateTime`.
- Renderiza apenas se `closedToday.isNotEmpty()`, entre "Esta semana" e "Histórico".

### Server (`signalingServer/`)
- `server/OrderStore.kt` — `ConcurrentHashMap<orderId, OrderRecord>` + `byToken: ConcurrentHashMap<token, MutableList<orderId>>`. `submit(req)` é idempotente (retorna existing se mesmo `orderId`). `isValid(req)` valida `lines.isNotEmpty()`, `units ∈ (0, MAX]`, `MAX_LINES`, preços ≥ 0.
- `server/OrderRoutes.kt`:
  - `POST /order` — valida payload, exige `SessionStore.get(token) != null`, devolve 202 com `OrderSubmissionResponse`.
  - `GET /order/{orderId}` — devolve `OrderRecord` ou 404.
  - `GET /session/{token}/orders` — lista `OrderRecord` da sessão.
- `Main.module()` registra `orderRoutes(orders, store)`.
- `OrderRoutesTest` — 5 testes: submit+fetch happy path, idempotência (segundo POST com payload diferente preserva original), 404 com token desconhecido, 400 com lines vazias, listagem por token.

### Web buyer (`webBuyer/`)
- `api/orders.ts` — `submitOrder(baseUrl, req, deps)` com `SubmitOrderResult` discriminado (`ok | network | session_unknown | invalid_payload | server`), injeção de `fetchImpl` para testes.
- `main.ts.showSummary(payload)` agora dispara `void submitOrder(SERVER_BASE, ...)` em fire-and-forget após montar o overlay; falhas vão pro `console.warn` em DEV.
- `ui/orderSummary.ts`:
  - Header ganhou `dateLabel` formatado via `Intl.DateTimeFormat('pt-BR', ...)`.
  - Footer ganhou `<button data-action="print">Salvar PDF</button>` (chama `window.print()`).
  - Mount adiciona `document.body.classList.add('is-printing-ready')`; destroy remove.
- `styles/app.css` — bloco `@media print`:
  - `body.is-printing-ready > *:not(.order-summary)` → `display: none`.
  - Overlay rebaixa para `position: static`, cor preta, sem badge nem animação.
  - Esconde `#remote-audio`, `.connection-banner`, `.order-summary-actions`.
  - `@page { margin: 16mm }`.
- `api/__tests__/orders.test.ts` — 8 testes (ok, trailing slash, 404, 400, 500, network, json malformado, schema mismatch).

### Aceitação validada
- `./gradlew :protocol:jvmTest` ✅ (24 testes; 3 novos em `OrderDtoTest`)
- `./gradlew :signalingServer:test` ✅ (17 testes; 5 novos em `OrderRoutesTest`)
- `./gradlew :composeApp:testDebugUnitTest` ✅ (15 testes)
- `./gradlew :composeApp:assembleDebug` ✅
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` ✅
- `(cd webBuyer && npm run typecheck && npm test && npm run build)` ✅ (65 testes; 8 novos em `orders.test.ts`)

### Como testar manualmente
1. Subir backend + webBuyer (ou usar produção).
2. App vendedor: gerar link → iniciar chamada.
3. Cliente: entrar → adicionar produtos → vendedor toca "Confirmar pedido".
4. `OrderSummaryOverlay` aparece nos dois lados; cliente vê botão "Salvar PDF" (toca → diálogo de print do browser, com layout `@media print` limpo).
5. Vendedor toca "Fechar e encerrar" → volta pra `InviteScreen`.
6. Voltar para a Home do vendedor — seção **"Pedidos fechados hoje"** aparece com o pedido recém-fechado (1 · R$ X,XX no pill Jade).
7. Verificar server: `curl https://trovatacast-signaling.fly.dev/session/<token>/orders` (em local: `http://localhost:8080/...`) devolve o `OrderRecord` enviado pelo buyer.

### Dívidas conhecidas
- **Postgres** — `OrderStore` continua em memória; `application.yaml` + `flyway` + `exposed` declarados mas não wired. Migrar quando alguém precisar persistência além do restart do Fly (M11 ou M19 quando push entrar).
- **Push do pedido** — não envia notificação ao vendedor pós-encerramento. Depende de canal externo (FCM + APNs) ou de o app ficar conectado ao WS após hangup. Fica pra M19.
- **`SummaryScreen` do vendedor** — métricas da sessão (tempo em foco por SKU, contagem de `PointAt`) continuam não coletadas. Ainda precisa `SessionEventLog` em commonMain. M12 cobre.
- **Reexibir pedido fechado** — `ClosedTodaySection` mostra a lista mas tap não abre detalhe ainda. Aceitável para a fase 2; entra junto do `SummaryScreen` em M12.
- **Buyer offline** — `submitOrder` é fire-and-forget; se buyer estiver offline no momento do `OrderConfirm`, o `POST /order` falha silenciosamente (warn no console DEV). Vendedor segue com persistência local válida — não há perda relevante, mas o server fica sem cópia. Fila com retry entraria com persistência server-side real.
- **Janela "hoje"** — `SessionsViewModel.todayWindow()` é calculada uma vez na construção. Não atualiza automaticamente quando o relógio cruza meia-noite; o usuário verá pedidos do dia anterior até reabrir a Home. Recalcular via flow timer entraria com observabilidade real (M12).

---

## Integração Catálogo Link — Fase 0 (contrato)

O TrovataCast deixa de ter catálogo próprio: a sessão ao vivo passa a ser uma camada sobre um catálogo link que já existe no SFA. Esta fase mexe só no contrato — as telas e o carrinho real vêm nas fases seguintes.

### O que mudou no `protocol/`

| Antes | Agora |
|---|---|
| `SessionCreateRequest(productSkus, products, collectionLabel)` | `SessionCreateRequest(empresaSlug, catalogoUuid, sellerId, sellerName, catalogoNome?, carrinhoId?, clientName?, clientEmail?)` |
| `SessionInfo` com snapshot de produtos | `SessionInfo` com identidade do catálogo link |
| `Route.Catalog/Product` | `CatalogRoute` (`inicio`, `menu`, `todos`, `secao(tabela, tabelaId)`, `carrinho`, `favoritos`) + `ViewState(route, query, focus)` |
| `Scroll(productId, offset)` | `Scroll(ScrollAnchor(page, produtoPreId, itemOffsetRatio, viewportRatio))` |
| `PointAt(productId)` | `PointAt(target, xRatio, yRatio)` com âncora DOM (`LiveAnchor`) |
| `CartUpdate(productId, size, units)` | `CartInvalidated(carrinhoId, reason, hint?)` — sinal de invalidação, nunca estado |
| `OrderConfirm(lines, totalCents)` | `OrderPlaced(carrinhoId, pedidoId?)` — total é sempre o que o Laravel disse |
| `SessionEvent` + `Codec.kt` (contrato paralelo, nunca usado) | removidos |

`ViewState.query` viaja filtrada por `SyncedQueryKeys` (`categoria`, `grupo_produto`, `subgrupo_produto`, `marca`, `search`, `page`, `total`, `sort`, `direction`). O token da sessão nunca entra nessa lista.

### URL de convite

```
https://{PUBLIC_CATALOG_URL}/catalogo-link-view/{empresaSlug}/{catalogoUuid}?live={token}
```

O parâmetro é `live` e **não pode** ser `token`: o interceptor em `sfa_front/src/api/axios.ts` trata `?token=` como JWT legado e troca todo o esquema de prefixo das chamadas.

### Decisões tomadas nesta fase

- **TTL da sessão ao vivo: 4h** (era 24h). Um link que dá acesso ao áudio do vendedor não vale um dia inteiro.
- **`Secao.tabela`/`tabelaId`** em vez de `type`/`typeId`: `type` colide com o discriminador JSON do protocolo. `tabela` é o nome do campo no backend (`CategoriaVitrine.tabela`) e mapeia para `params.type` do vue-router.
- **Produto é overlay, não rota**: no `sfa_front` o detalhe abre em modal (`abrirDetalhesProduto`), então `ProductFocus` acompanha a rota em vez de substituí-la.
- **`ProductFocus` composto** (`produtoPreId` + `produtoPre1Id` + `complemento1Id`): a vitrine identifica produto+cor, não só produto.

### Dívidas abertas por esta fase

- **`SfaConfig.empresaSlug` / `catalogoLinkUuid` vazios.** Sem catálogo link escolhido, `generateLink` responde "Escolha um catálogo link para convidar o cliente". Some na Fase 1, junto com o `empresaId = 97`.
- **`SessionEntity` mudou de colunas sem migration.** SQLDelight aqui só faz `CREATE TABLE IF NOT EXISTS`; bancos locais anteriores precisam de reinstalação do app.
- **`OrderStore` / `POST /order` continuam de pé.** Saem na Fase 6, quando `finalizar`/`gerarPedido` do Laravel assumirem.
- **`webBuyer` não é mais superfície do cliente.** `protocol/dataChannel.ts` foi atualizado como material de porte para o Vue; a vitrine mock cai no fixture porque `/session/{token}` não devolve mais produtos.
- **`PUBLIC_CATALOG_URL` = `https://trovata.app.br`** no `fly.toml` — precisa ser confirmado (Vercel ou cluster ainda está em aberto).

### Verificação
- `./gradlew :protocol:jvmTest :signalingServer:test :composeApp:testDebugUnitTest --rerun-tasks` ✅ (29 + 20 + 26)
- `./gradlew :composeApp:assembleDebug :signalingServer:build` ✅
- `(cd webBuyer && npm run typecheck && npm test && npm run build)` ✅ (67 testes)

---

## Integração Catálogo Link — Fases 1 e 2

**Fase 1 — o vendedor vê os catálogos que são dele.** Nenhum endpoint novo foi preciso: `GET /empresa/{slug}/catalogos-links` já existe sob `auth:keycloak` e o `ListCatalogoLinkUseCase` já filtra por `usuario_id` + roles.

- `CatalogLinksApi` consome a listagem paginada e mapeia para `SellerCatalogLink`.
- `CatalogLinkPickerScreen` substitui o antigo `CatalogPickerScreen` (que selecionava produtos soltos do catálogo de amostra). O picker de produtos foi removido.
- `Company` ganhou `slug` — é ele que vira `empresaSlug` no contrato e na URL de convite.
- Catálogo expirado ou inativo aparece sob o filtro "Todos", não é selecionável, e a tela explica o motivo em vez de falhar na criação da sessão.

**Fase 2 — o convite aponta para a vitrine real.** `POST /session` valida contra o Laravel antes de emitir o token, e o app manda o Bearer do vendedor junto.

### Duas correções de rota descobertas na integração real

**As rotas do catálogo link não estão em `api-int`.** O `api-int.trovata.app.br` é um gateway Go que serve `/empresas` e `/v2/usuarios/logado` — o que o app já usava para conta e sync. As rotas de catálogo link, vitrine e carrinho são do Laravel e vivem em **`https://api.trovata.app.br/api`**, com o prefixo `/api`. Daí o `SfaConfig.laravelApiUrl`, separado do `baseUrl`.

**Catálogo expirado devolve HTTP 500, não 400.** O `ValidateUUIDMiddleware` faz `throw new Exception('Catalogo expirado', 400)`, mas o segundo argumento de `Exception` é o *code*, não o status HTTP — e o `Handler` do projeto só mapeia 404. Com `app.debug=false` em produção o corpo ainda vem mascarado como `{"message":"Server Error"}`, então não há como distinguir "catálogo expirado" de "SFA instável".

Consequência: o validador rejeita 404 (catálogo inexistente na empresa) e trata 500 como indisponibilidade — segue sem bloquear, e loga. A defesa real contra catálogo expirado é a Fase 1, onde a listagem já traz `expirado` calculado e a tela não deixa escolher.

**Vale um PR no `sfa_back`** trocando essas duas `Exception(..., 400)` por uma `HttpException` com status próprio. É mudança de duas linhas e faz a rejeição virar contrato de verdade para qualquer cliente da vitrine.

### Verificação ponta a ponta (signaling local → Laravel de produção)

Com `SFA_API_URL=https://api.trovata.app.br/api`:

| Cenário | Resultado |
|---|---|
| uuid inexistente em `buba-teste` | Laravel devolveu 404 → signaling respondeu `422 catalogo_not_found` |
| catálogo real `0234d934…` (validade 2026-04-18, expirado) | Laravel devolveu 500 mascarado → sessão criada com log de aviso |
| payload sem `catalogoUuid` | `400 missing_catalogo`, sem sair para a rede |
| `GET /session/{token}` | devolve identidade do catálogo link, sem nenhum produto |
| URL do convite no front de produção | `https://trovata.app.br/catalogo-link-view/buba-teste/{uuid}?live={token}` → HTTP 200 |

### O que falta para o fluxo completo do vendedor

- **Catálogo link vigente no `buba-teste`.** O tenant 99 tem um único catálogo e ele expirou em 18/04/2026. Sem isso não dá para exercitar o caminho feliz da Fase 1 ponta a ponta.
- **Credencial Keycloak de um vendedor** de teste: a listagem exige `auth:keycloak`, então a Fase 1 foi verificada com testes de contrato sobre o payload real do `CatalogoLinkListResource`, não contra a API autenticada.

### Verificação
- `./gradlew :protocol:jvmTest :signalingServer:test :composeApp:testDebugUnitTest` ✅ (29 + 30 + 37 = 96)
- `./gradlew :composeApp:assembleDebug :signalingServer:build` ✅

---

## Integração Catálogo Link — Fase 4a (o vendedor vê a mesma vitrine que o cliente)

> Pré-requisito da co-presença. Enquanto as duas pontas liam catálogos diferentes, apontar um produto podia apontar para o vazio.

### O problema

A `LiveCall` lia `CatalogRepository`, ou seja, o cache do `api-int` sincronizado em SQLDelight: outra origem, outra ordenação e outro preço. O cliente, do outro lado, vê `catalogos-links/{slug}/{uuid}/vitrine`, que aplica tabela de preço do catálogo link, desconto por faixa, saldo, sequência e as restrições de tipo de venda. O protocolo já ancora tudo em `produtoPreId` (`ScrollAnchor`, `ProductFocus`, `LiveAnchor`), mas os ids vinham de uma lista que o cliente não estava vendo.

### O que mudou

- **`data/remote/sfa/VitrineApi.kt`** — `GET {laravelApiUrl}/catalogos-links/{slug}/{uuid}/vitrine` com `page`, `total`, `search`, `categoria`, `catalogo_carrinho` e `destaques`. Rota pública, sem `Authorization`. Devolve `VitrinePage(produtos, currentPage, lastPage, total)`.
- **`dto/VitrineDto.kt`** — espelha o contrato que o `sfa_front` consome (`CatalogoLinkVitrine`), incluindo `produto_pre_1_id`, `complemento_1_id`, `saldo_disponivel`, `is_carrinho`, `lista_multiplo_venda` e `arquivos`. O envelope da vitrine é **flat** (`data`, `current_page`, `per_page`, `total`, `last_page`), diferente do `{data, meta}` das rotas privadas.
- **`LenientNumbers.kt`** — `preco_final` e `preco_de` chegam ora como número (`144.9`), ora como string no padrão brasileiro (`"1.189,90"`). Dois serializers toleram as duas formas e entregam cents.
- **`LiveCallScreenModel`** — deixa de injetar `CatalogRepository` e `SessionsRepository` e passa a carregar da `VitrineApi`, com `isLoadingCatalog`, `catalogError`, `catalogPage` e `catalogLastPage` no state. `selectedSkus` sai do fluxo: não existe mais seleção de produtos.
- **`CallSpec` / `LiveCallScreen` / `InviteScreen`** — propagam `empresaSlug` e `catalogoUuid`, que já vinham em `StoredSessionRecord`.
- **`LiveCallScreen`** — estado vazio honesto (carregando, erro com "Tentar de novo", vitrine sem produtos) e paginação no rodapé quando há mais de uma página.

### Decisões

- **Nenhum cálculo nosso.** `preco_final` já vem com a regra do catálogo link aplicada; o app converte para cents e exibe. `preco_de` só é mostrado quando `teve_desconto` é verdadeiro.
- **Mapeamento para o `Product` da UI** (`feature/call/VitrineUi.kt`) em vez de reescrever os componentes. `sizes` fica vazio porque a grade vem de `/produtos/{id}/grades`, que entra junto do carrinho ao vivo.
- **Sem cache local da vitrine.** A lista é sempre da API: é o catálogo do cliente, não uma cópia nossa.

### O que ficou pendente

- **Grade e cores** (`/produtos/{produto_pre_id}/grades`, `/itens-cores`) — necessárias para o detalhe do produto e para o carrinho ao vivo (Fase 5).
- **Navegação por seção** — a vitrine tem categoria, grupo, marca e coleção; hoje o app carrega a listagem completa paginada. `CatalogRoute.Secao` já existe no protocolo esperando isso.
- **`CartRepository` / `OrderRepository` / `OrderStore`** continuam no grafo, agora claramente órfãos do modelo antigo. Saem na Fase 5.
- **`selectedSkus` e `SelectedProductEntity`** ficaram sem uso no fluxo da chamada.

### Verificação

- `./gradlew :protocol:jvmTest :signalingServer:test :composeApp:testDebugUnitTest` ✅ (29 + 30 + 44 = 103; 7 novos em `VitrineApiTest`, cobrindo caminho da URL, ausência de auth, preço numérico e string, `preco_de` sem desconto, 404 e falha de rede)
- `./gradlew :composeApp:assembleDebug` ✅
- **Não verificado contra a API real**: falta rodar com um catálogo link vigente para confirmar os nomes de campo que a query da vitrine devolve, já que o `SELECT` final é `produtos.*` e não um Resource fechado.

---

## Infra — ICE configurável (o degrau antes do TURN)

Antes, os dois lados tinham a lista de ICE fixa em código, só com STUN público (`PeerSession.kt` e `webrtc/peer.ts`). Em NAT simétrico, comum em 4G e Wi-Fi corporativo, isso simplesmente não conecta, e trocar exigia recompilar app e buyer.

Agora o servidor é a fonte da configuração:

- `protocol/`: `IceServerConfig(urls, username?, credential?)`, presente em `SessionCreateResponse` e `SessionInfo`.
- `signalingServer/`: `IceConfig.fromEnv()` lê `ICE_STUN_URLS`, `ICE_TURN_URLS` (ambas em CSV), `ICE_TURN_USERNAME` e `ICE_TURN_CREDENTIAL`. Sem nada configurado, mantém o STUN público de hoje, então o comportamento atual não muda.
- `composeApp/`: `PeerSession` recebe um `iceServersProvider` que consulta `GET /session/{token}` na hora de abrir a conexão, com fallback para o default se a chamada falhar. Buscar na hora, e não no momento do convite, importa porque credencial de TURN costuma ser temporária.
- `webBuyer/`: `startLiveSession` lê os `iceServers` da mesma rota antes de criar o `RTCPeerConnection`.

Para ligar TURN em produção, nada de código:

```bash
fly secrets set ICE_TURN_URLS="turn:host:3478?transport=udp,turns:host:5349" \
                ICE_TURN_USERNAME="..." ICE_TURN_CREDENTIAL="..."
```

Continua em aberto quem fornece e paga o TURN. O que saiu do caminho crítico foi a parte técnica.

### Verificação
- `./gradlew :protocol:jvmTest :signalingServer:test :composeApp:testDebugUnitTest` ✅ (29 + 35 + 44 = 108; 5 novos entre `IceConfigTest` e o cenário de `SessionRoutesTest`)
- `(cd webBuyer && npm run typecheck && npm test)` ✅ (67 testes)

---

## Limpeza — o pedido paralelo saiu do caminho

O plano de integração diz que o pedido nasce e morre no Laravel. O que ainda existia do modelo antigo virou ruído: código sem chamador, com testes verdes dando falsa sensação de cobertura.

### Removido

- **`signalingServer`**: `OrderRoutes.kt` (`POST /order`, `GET /order/{id}`, `GET /session/{token}/orders`) e `OrderStore.kt`, mais o wiring no `Main.kt`. Nenhum cliente chamava: o app nunca chamou, e o `main.ts` do buyer parou de chamar quando virou vitrine.
- **`protocol`**: `OrderSubmissionRequest`, `OrderSubmissionResponse`, `OrderRecord` e `OrderSource`. `OrderLine` fica, porque o resumo de pedido do vendedor ainda usa.
- **`webBuyer`**: `api/orders.ts`, `ui/orderSummary.ts`, `ui/cartSheet.ts`, `ui/productDetail.ts`, `ui/productCard.ts`, `cart/store.ts` e `protocol/order.ts`, todos órfãos desde a reescrita da vitrine. `views/arrival.ts` ficou só com `renderLoading`, que é o único trecho ainda montado.
- **`composeApp`**: `SessionsRepository.selectedSkus`, sem chamador desde que a `LiveCall` passou a ler a vitrine real.

### Mantido de propósito

- **`CartRepository`** continua alimentando a gaveta de carrinho do vendedor durante a chamada. Some na Fase 5, quando o conteúdo do carrinho passar a vir do Laravel; removê-lo agora deixaria a tela sem fonte.
- **`OrderRepository`** alimenta "Pedidos fechados hoje" na Home, além de Insights, Conta e Dashboard. Também sai na Fase 5 ou 6, quando esses números vierem dos pedidos reais. Tirar agora esvaziaria quatro telas sem substituto.
- **`SelectedProductEntity`** continua no schema (a query de escrita segue existindo); só a leitura órfã saiu. Remover tabela pede migration e não há ganho hoje.

### Efeito na contagem de testes

Caiu de 108 para 100 testes Kotlin e de 67 para 50 no webBuyer. A diferença é exatamente a cobertura do código removido: `OrderDtoTest`, `OrderRoutesTest`, `orders.test.ts` e a parte de `arrival.test.ts` que exercitava telas que não são mais montadas.

### Verificação
- `./gradlew :protocol:jvmTest :signalingServer:test :composeApp:testDebugUnitTest` ✅ (26 + 30 + 44 = 100)
- `./gradlew :composeApp:assembleDebug` ✅
- `(cd webBuyer && npm run typecheck && npm test)` ✅ (50 testes)

---

## Integração Catálogo Link — Fase 4b (grade real no detalhe do produto)

Até aqui, o detalhe do produto na chamada inventava tamanho e cor: `sampleSizesFor` distribuía estoques de uma lista fixa (`24, 36, 18, 6, 12`) e `sampleSwatchesFor` pintava swatches com nomes decorativos ("Verde-musgo", "Areia"). Era a dívida registrada no §10 do `09-mock-to-real-migration.md`, e ficou insustentável quando a listagem passou a ser real: produto verdadeiro com grade inventada engana mais do que mock declarado.

### O que mudou

- **`VitrineApi.grade(empresaSlug, catalogoUuid, produtoPreId, carrinhoId?)`** sobre `GET catalogos-links/{slug}/{uuid}/produtos/{id}/grades`, rota pública como a da vitrine.
- **`dto/GradeDto.kt`** espelha o `ProdutoComGradeResource`: `variacoes[].complemento_1` (a cor), `variacoes[].grades[].tamanhos[]` com `complemento_2`, `disponivel` e `adicionados_count`, mais `arquivos` por variação. Quando a resposta traz `variacao` no singular em vez da lista, o mapeamento usa ela.
- **Modelo**: `ProdutoGrade(cores: List<CorGrade>)`, com `CorGrade(complemento1Id, descricao, imageUrl, tamanhos)` e `TamanhoGrade(complemento2Id, label, disponivel, adicionados)`. São exatamente as chaves que o item de carrinho do catálogo link exige (`complemento_1_id` + quantidades por `complemento_2`), então a Fase 5 já encontra o dado no formato certo.
- **`LiveCallScreenModel`** busca a grade ao abrir o detalhe, com `isLoadingGrade` e `gradeError` no state. Se o vendedor trocar de produto antes da resposta chegar, o resultado atrasado é descartado em vez de sobrescrever o produto novo.
- **`ProductDetailScreen`** aceita `grade` opcional: quando existe, tamanhos e cores vêm dela; quando não, mantém o comportamento antigo, o que preserva a tela fora da chamada (aba Catálogo) enquanto ela não for ligada.

### Decisões

- **Preço não veio da grade.** O `calculos.preco` das variações usa o value object `Preco` do Laravel, com serialização própria; como o preço já chega pronto na listagem da vitrine, não valia acoplar a mais uma estrutura nesta fatia.
- **`BigDecimal` do PHP** chega ora como número, ora como string. O `LenientDoubleSerializer` criado para os preços resolve os dois casos, e o teste cobre `"8"` como string.

### Pendências

- **Preço por variação** (cor com preço diferente) continua não modelado.
- **Cores na aba Catálogo** seguem fabricadas: só a chamada recebe a grade real por enquanto.
- **`/itens-cores`** não foi consumido; a lista de cores sai das próprias variações da grade.

### Verificação
- `./gradlew :protocol:jvmTest :signalingServer:test :composeApp:testDebugUnitTest` ✅ (26 + 30 + 48 = 104; 4 novos cobrindo caminho da rota, mapeamento de cores/tamanhos/saldo, `variacao` singular e resposta sem dados)
- `./gradlew :composeApp:assembleDebug` ✅

---

## Integração Catálogo Link — Fase 5 (a televenda grava no carrinho do cliente)

### O problema

Até aqui o app mostrava a vitrine e a grade reais, mas o pedido continuava numa cópia local (`CartRepository` em SQLDelight). O que o vendedor lançava na chamada não existia para o Catálogo Link: o cliente não via, o backoffice não via, e nada virava pedido. A venda estava sendo feita duas vezes, em dois lugares que não se falavam.

### O que mudou

- **`clientEmail` e `catalogoLinkId` atravessam o caminho todo.** Os dois vinham do catálogo escolhido e se perdiam antes da chamada. Agora vão no `SessionCreateRequest`/`SessionInfo`, na tabela local de sessões (migração `1.sqm`, schema na versão 2) e no `CallSpec`. Sem o e-mail não há carrinho; sem o id numérico não há "pronto para envio".
- **O carrinho abre junto com a chamada.** `LiveCallScreenModel` chama `POST .../login` com o e-mail do catálogo link, retomando o carrinho "Digitando" do cliente, e resolve tabela de preço e tipo de venda em seguida. O `carrinhoId` resultante passa a alimentar a vitrine e a grade, então `is_carrinho` e `adicionados_count` chegam certos.
- **Quantidade por tamanho no detalhe do produto.** A cor selecionada troca a grade exibida, cada tamanho tem stepper no múltiplo de venda do catálogo, iniciado com o que já está no carrinho, e o botão lança tudo de uma vez em `POST .../itens-multiple`.
- **A gaveta lê o Laravel.** As linhas vêm de `carrinhos/{id}/itens-para-rota-publica`, com tamanhos, quantidades e valores calculados pelo backend.
- **O fechamento é "pronto para envio".** `PATCH empresa/{slug}/catalogos-links/{id}/carrinhos/{id}` na rota privada, com o Bearer do vendedor.
- **`CartRepository` saiu**, junto com a tabela `CartLineEntity`.

### Decisões

- **Retomar o carrinho do cliente, não criar outro.** `POST .../carrinhos` clona um carrinho existente; quem cria de fato é o `login` por e-mail. Retomar mantém vendedor e cliente no mesmo carrinho.
- **Preço lido por `numerador`/`denominador`.** O `PrecoTransformer` do Laravel serializa `valor` como decimal com ponto (`"144.900000"`), formato que o parser BR do projeto interpretaria errado. A fração é exata e não depende de locale.
- **Quantidade zero não é enviada**, igual ao `use-adicao-produto-dialog` do `sfa_front`: o payload leva o total absoluto por tamanho, não um delta.
- **`OrderRepository` fica.** O fechamento continua gerando registro local (`CAR-{carrinhoId}`) porque Home, Insights e Conta leem dele.

### Pendências

- **Remover item do carrinho** pelo app (existe `DELETE .../itens/{id}`, ainda não consumido).
- **Prazo e desconto** seguem como o carrinho já os tem; o app não os edita.
- **`carrinhos-resumo/{id}` está quebrada no `sfa_back`**: a rota aponta para `CatalogoCarrinhoController::showPublic`, método que não existe no controller. O app usa `carrinhos/{id}` (`show`), que existe.

### Verificação
- `./gradlew :composeApp:testDebugUnitTest :protocol:allTests :signalingServer:test` ✅ (64 no composeApp, 7 novos cobrindo listagem de itens, rota do resumo, carrinho vazio, mapeamento de linha e leitura de preço fracionário)
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64 :signalingServer:build` ✅

---

## Release de demonstração (02/09/2026)

O app deixou de ter tela que não se sustenta em dado real. A navegação passou de cinco para três abas:

| Aba | Fonte |
|---|---|
| Sessões | `SessionsRepository` (SQLDelight) + `OrderRepository` para os pedidos fechados no dia |
| Catálogos | `CatalogLinksApi`, os catálogos link reais do vendedor, com validade, carrinhos e visitas; selecionar e gerar o convite acontece na própria aba |
| Clientes | `ClientsRepository`, alimentado pelo `api-int` |

### O que saiu

- **Painel e Insights**: liam do `OrderRepository`, que só recebe pedido fechado dentro da chamada. Fora dessa condição eram tela vazia permanente, e o Painel era a aba inicial.
- **Aba Catálogo (catálogo local)**: navegador do catálogo sincronizado do `api-int`, com busca e filtros decorativos e um botão flutuante sem ação. Não participa da televenda: quem manda na chamada é a vitrine do catálogo link. Com ela saiu o `SampleCatalog`, as oito fotos de bolsa que apareciam sempre que o banco local estava vazio.
- **Telas órfãs do protótipo**: `IncomingCallScreen`, `SessionPrepScreen`, `AuthSetupScreen`, `AuthVerifyScreen`, `DesignSystemScreen` e `EmptyTabScreen` não eram alcançáveis por nenhuma rota. Com elas saiu todo o pacote `data/sample`.
- **Esqueletos de carregamento falsos**: Home e Clientes desenhavam linhas cinza de "conteúdo carregando" quando a lista estava vazia. Viraram texto honesto.
- **Boas-vindas**: "Entrar pelo WhatsApp" levava ao login de e-mail e senha do Keycloak, e a versão dizia v1.2.0. Agora é um botão "Entrar" e a versão real.

### O que mudou de lugar

- `Product`, `ProductTag`, `FashionPalette` e `ProductSwatchPalette` saíram de `data/sample` para `ui/components/Product.kt`: são o modelo de UI que `ProductCard`, `ProductRow` e a vitrine da chamada usam, e não têm mais nada de amostra.
- `AccountRow`, `AccountStat` e `SupportRow` foram para `feature/account/AccountModels.kt`.
- A lista de catálogos link virou `CatalogLinksBody`, um corpo único usado pela aba e pela tela empilhada que vem do convite a partir de um cliente.
- O `ProductDetailScreen` **ficou**: ele é o painel de produto aberto durante a chamada (`inCallContext = true`), e não só uma tela do catálogo. Só perdeu a entrada pela aba removida.

### Sincronização de login

`syncEssentials` baixava treze recursos do `api-int` (produtos, preços, complementos, arquivos). Com o catálogo local fora da UI, só `clientes` alimenta alguma tela, e é só isso que roda agora. `syncAll` e as funções individuais continuam no `CatalogSyncService`, então religar é trivial quando o catálogo local voltar a ter uso.

### Verificação
- `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug` ✅ (64 testes)
- APK instalado no Moto G22 apontando para staging

### Próximo passo

As telas da fase seguinte estão em `docs/12-proximas-telas.md`, com o endpoint que alimenta cada uma e o que depende do time do SFA.

---

## Carrinhos abertos: o que a leitura do `sfa_back` mudou no desenho

A tela saiu do item 1 de `docs/12-proximas-telas.md`, mas o desenho não é o que aquele documento supunha, porque a leitura do backend antes de escrever o código mudou três decisões.

**A chamada só retoma carrinho em situação `D`.** O `POST catalogos-links/{slug}/{uuid}/login`, que é o que o app já usa para abrir o carrinho dentro da chamada, cai em `GetCatalogoCarrinhoPorEmailUseCase`. Esse caso de uso procura o carrinho mais recente daquele catálogo link com o e-mail informado **e situação `D` (Digitando)**. Não achando, ele cria um carrinho novo e vazio. Ou seja: chamar ao vivo um carrinho finalizado, enviado ou cancelado não retomaria aquele carrinho, abriria outro. Por isso a tela filtra `situacao=D` por padrão e, nos demais, troca o botão por uma frase dizendo exatamente isso, em vez de oferecer uma ação que faria a coisa errada em silêncio.

**O filtro `vazio` não é enviado.** `CatalogoCarrinhoListInputData` declara `#[WithCast(BuiltinTypeCast::class, 'bool')] ?bool $vazio`, e em PHP `(bool) "false"` é `true`. Sem poder chamar a API para conferir qual valor chega de fato, mandar `vazio=false` corria o risco de trazer exatamente o oposto do pedido, ou seja, só os carrinhos vazios. O `CarrinhoListagemGeralResource` já devolve `itens`, então o corte de carrinho vazio é feito no app. Fica registrado como pergunta para o time do SFA.

**As datas do Laravel não são ISO.** `config/data.php` define `date_format` como `Y-m-d H:i:s` e `date_timezone` como `America/Sao_Paulo`, e `updated_at` do resource é um `Carbon`. Isso não passa por `Instant.parse`. `SfaParse.parseTimestampToMs` tenta ISO primeiro (que é o formato do `api-int`) e cai para o formato do Laravel assumindo o fuso de São Paulo. O teste correspondente prova que `2026-08-30T19:41:02Z` e `2026-08-30 16:41:02` chegam ao mesmo instante.

O resto do contrato foi lido em `CarrinhoListagemGeralResource`, `CatalogoLinkPorCarrinhoResource` e `ClienteListResource`: o `uuid` do catálogo link vem aninhado na linha do carrinho, que é o que permite criar a sessão sem uma segunda chamada; `valor_total` e `quantidade_total` são `BigDecimal` e chegam como string, por causa do `BigDecimalTransformer`; a paginação é `meta` com `current_page`/`last_page`/`per_page`/`total`, e o tamanho de página é o parâmetro `total`, não `per_page`.

Ordenação: o documento sugeria "parado há mais tempo" como padrão. Ficou como alternativa em um chip, e o padrão é o mesmo do `sfa_front` (`updated_at desc`), porque abrir a lista pelo carrinho mais antigo empurra para o topo o que já esfriou faz meses.

**Não validado contra a API.** Nenhuma dessas rotas foi chamada: não há credencial Keycloak de vendedor disponível aqui. Os testes são de contrato sobre o payload que os resources descrevem, do mesmo jeito que na Fase 1. O formato exato e o comportamento com dado real ainda precisam ser confrontados com staging.

---

## O sync estava 404 desde a virada para staging

A aba Clientes mostrava "Nenhum cliente sincronizado para esta empresa" na buba. A causa não era ausência de dado.

O banco do aparelho (lido com `adb run-as`, build debug) tinha as 13 entidades de sync gravadas com o mesmo erro, `http_404`, e `ClientEntity` zerada. O `SfaApi` montava `"$baseUrl/empresas/{id}/{recurso}"`, que é a v1 pública do `api-int`. Três GET sem credencial contra o `api-int-staging` isolaram o problema: `empresas/97/clientes` responde 404, `v2/empresas/97/clientes` responde 401 e `empresas` responde 200. O 401 prova que a rota existe e só falta o token; o 404 diz que aquele caminho não está publicado em staging, embora exista em produção, que é o que o `docs/08-api-data-model.md` descrevia.

O `AccountApi` nunca quebrou porque já usava `/v2/usuarios/logado` e `/v2/empresa-usuarios`. Só o sync tinha ficado na v1. A correção foi apontar o `SfaApi` para `/v2/empresas/...` e tratar ausência de token como falha explícita, já que na v2 o Bearer deixou de ser opcional.

Verificado no aparelho depois da correção: `clientes` terminou com `inFlight = 0`, sem erro e com `lastSyncAtMs` gravado, e `ClientEntity` foi de 0 para **36.798** linhas.

Esse número é a observação seguinte, e é de produto, não de bug: o primeiro login baixa a base inteira de clientes da empresa, em páginas de 1.000. No Wi-Fi levou cerca de dois minutos. Na rua, em 4G, isso é caro e o vendedor não precisa de 36 mil clientes, precisa dos dele. O `ClientEntity` já guarda `vendedorIdErp` e existe um `syncVendedoresClientes` fora do `syncEssentials`, então há caminho para escopar, mas não foi medido se a API aceita esse filtro na origem.

---

## Histórico

| Data | Marco |
|---|---|
| 2026-05-20 | M0 fechado: estrutura KMP + 4 superfícies + CI |
| 2026-05-20 | M1 fechado: design system completo (13 componentes + 47 ícones + 9 silhuetas + preview) |
| 2026-05-21 | M2 fechado: SellerHome + IncomingCall + SessionPrep + ViewModel com StateFlow |
| 2026-05-21 | M3 fechado: CatalogPicker + Invite + Voyager + REST `/session` + SQLDelight + webBuyer arrival |
| 2026-05-21 | M4 fechado: share intent nativo + QR + erros finos no webBuyer + persistência + 33 testes (8 Kotlin + 25 TS) |
| 2026-05-21 | M5 fechado: signaling WS + SignalingClient (Kotlin/TS) + WebRTC (webrtc-kmp + browser nativo) + LiveCall + CocoaPods · 76 testes totais |
| 2026-05-21 | M6 fechado: mute na UI dos dois lados + áudio remoto anexado + banner refinado |
| 2026-05-21 | M7 fechado: DC Scroll/PointAt + grade de catálogo na LiveCall + halo de produto apontado |
| 2026-05-22 | Infra: signaling deployado no Fly.io `gru`, clientes apontados para `https://trovatacast-signaling.fly.dev`, Mac não mais necessário para o servidor |
| 2026-05-22 | Validação E2E: iPhone em 3G + buyer desktop através do Fly.io estabeleceram WSS + WebRTC; ficou registrada dívida de `AURemoteIO` no iOS após reconnect (M10) e webBuyer ainda local (próximo passo de infra) |
| 2026-05-22 | Infra: webBuyer no Cloudflare Pages (`trovatacast-buyer.pages.dev`) via GH Actions + Wrangler; Fly `PUBLIC_BUYER_URL` setado; Mac do vendedor não é mais necessário para nenhum componente |
| 2026-05-23 | M8 fechado: DC `Navigate` + `CartUpdate`, `BuyerProductDetail` sheet, cart dock no cliente, gaveta + toast no vendedor, `CartRepository` (SQLDelight) · 78 testes totais (9 protocol + 15 composeApp + 12 signaling + 54 webBuyer) |
| 2026-05-23 | M9 fase 1 fechado: DC `OrderConfirm`, botão Confirmar pedido na gaveta, `OrderSummaryOverlay` (vendedor) + `mountOrderSummary` (cliente). PDF, persistência server-side e push ficam pra fase 2 · 83 testes totais (11 protocol + 15 composeApp + 12 signaling + 57 webBuyer) |
| 2026-08-22 | Integração Catálogo Link — Fase 4a fechada: `VitrineApi` sobre a rota pública da vitrine, `LiveCall` mostrando os produtos do catálogo link (preço, ordem e saldo do próprio catálogo) com paginação e estados vazios; `CatalogRepository` sai do fluxo da chamada · 103 testes Kotlin (29 protocol + 30 signaling + 44 composeApp) |
| 2026-08-22 | Infra: ICE configurável pelo servidor (`ICE_STUN_URLS`/`ICE_TURN_*`), consumido por app e buyer na abertura da conexão · 108 testes Kotlin + 67 webBuyer |
| 2026-08-22 | Limpeza: `POST /order`/`OrderStore` e os módulos órfãos do buyer removidos; `CartRepository` e `OrderRepository` ficam até a Fase 5 por ainda terem telas dependentes · 100 testes Kotlin + 50 webBuyer |
| 2026-08-22 | Integração Catálogo Link — Fase 4b fechada: grade real (cores, tamanhos e saldo) no detalhe do produto durante a chamada, no formato que o item de carrinho do catálogo link exige · 104 testes Kotlin + 50 webBuyer |
| 2026-08-06 | Integração Catálogo Link — Fases 1 e 2 fechadas: app lista catálogos link reais do vendedor (`CatalogLinksApi`, `CatalogLinkPickerScreen`, `Company.slug`), picker de produtos removido, `POST /session` validando contra o Laravel de produção e convite apontando para o `sfa_front`. Descobertos o prefixo `/api` da API Laravel e o 500 mascarado em catálogo expirado · 163 testes totais (29 protocol + 30 signaling + 37 composeApp + 67 webBuyer) |
| 2026-08-06 | Integração Catálogo Link — Fase 0 fechada: contrato redesenhado no `protocol/` (identidade do catálogo link, `CatalogRoute`+`ViewState`, `Scroll` ancorado, `CartInvalidated`, `OrderPlaced`), URL de convite apontando para o `sfa_front` com `?live=`, `SessionEvent`/`Codec` removidos · 142 testes totais (29 protocol + 20 signaling + 26 composeApp + 67 webBuyer) |
| 2026-05-23 | M9 fase 2 (parcial) fechada: `OrderRepository` + `Orders.sq`, "Pedidos fechados hoje" na Home, `POST /order` em memória + buyer envia, PDF via `window.print()` + `@media print`. `SummaryScreen` (métricas) movido pra M12 e push pra M19 · 121 testes totais (24 protocol + 17 signaling + 15 composeApp + 65 webBuyer) |
| 2026-08-30 | Integração Catálogo Link — Fase 5 fechada: `clientEmail`/`catalogoLinkId` propagados até a chamada (migração SQLDelight v2), carrinho do cliente aberto por e-mail, quantidade por tamanho no detalhe, gaveta lendo `itens-para-rota-publica` e fechamento por "pronto para envio"; `CartRepository` removido · 64 testes no composeApp |
| 2026-09-02 | Release de demonstração: app reduzido a três abas (Sessões, Catálogos, Clientes), catálogo local e painéis sem fonte removidos, `data/sample` extinto, sync de login limitado a clientes · 64 testes no composeApp |
| 2026-09-02 | Carrinhos abertos: quarta aba sobre `GET empresa/{slug}/carrinhos` com "Chamar ao vivo", filtro por situação e ordem, `OpenCartsApi`/`OpenCartsScreenModel`, parser de data do Laravel. Descoberto que o `login` só retoma carrinho em situação `D` · 82 testes no composeApp |
| 2026-09-02 | Sync destravado: `SfaApi` apontado para `/v2/empresas/...` depois de descobrir que a v1 pública não existe em `api-int-staging` (404 em todas as 13 entidades). `ClientEntity` foi de 0 para 36.798 no Moto G22 |
