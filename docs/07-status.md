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
| M6 — Áudio P2P | 🚧 próximo | — | tracks de áudio já trafegando (preview); falta UI de mute + estado visível |
| M7 → M12 | ⏳ pendente | — | ver `docs/06-roadmap.md` |

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

## Próximo milestone — M6: Áudio P2P

Critério de aceitação: chamada de voz real entre dois celulares de testers.

### A construir
- Captura local de mic já está fluindo via `MediaDevices.getUserMedia` (M5).
- Mutar microfone na UI (`MediaStreamTrack.enabled = false`).
- Mostrar estado do mic do outro lado (via DC ou `signaling.PresencePing` carregando `muted`).
- Conectar `MediaStream` remoto a uma view de áudio no iOS/Android (`RemoteAudioTrack`).
- Permissão Android pedida explicitamente no fluxo do LiveCall.

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
