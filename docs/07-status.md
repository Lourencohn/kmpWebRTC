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
| M4 — Web buyer mínimo | 🚧 próximo | — | parcialmente entregue em M3 (BuyerArrival); falta CTA de áudio integrado |
| M5 → M12 | ⏳ pendente | — | ver `docs/06-roadmap.md` |

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

## Próximo milestone — M4: Web buyer mínimo

Critério de aceitação: cliente abre o link gerado num celular real e vê tela de chegada com nome real do vendedor; CTA "Entrar com áudio" pede permissão de mic.

### O que já está adiantado
- Tela de chegada renderizada (parcialmente entregue no M3).
- `GET /session/{token}` no servidor.
- Pedido de mic via `getUserMedia`.

### Falta
- Share intent real Android + iOS (sair de placeholder).
- QR code do link na `InviteScreen` (cliente lê com a câmera).
- Estados de erro mais finos no webBuyer (rede offline, token expirado vs inválido).
- Persistência de "ultima sessão aberta" no webBuyer para retomar após reload.

---

## Histórico

| Data | Marco |
|---|---|
| 2026-05-20 | M0 fechado: estrutura KMP + 4 superfícies + CI |
| 2026-05-20 | M1 fechado: design system completo (13 componentes + 47 ícones + 9 silhuetas + preview) |
| 2026-05-21 | M2 fechado: SellerHome + IncomingCall + SessionPrep + ViewModel com StateFlow |
| 2026-05-21 | M3 fechado: CatalogPicker + Invite + Voyager + REST `/session` + SQLDelight + webBuyer arrival |
