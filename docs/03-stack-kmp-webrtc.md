# Stack: KMP + WebRTC

> Decisões técnicas e racional. Cada escolha é defensável; quando for trocar, registre o porquê aqui.

---

## 1. Visão geral

```
┌───────────────────────────────────┐    ┌────────────────────────────┐
│  Vendedor (iOS / Android)         │    │  Cliente (Web — browser)   │
│  Kotlin Multiplatform             │    │  TypeScript / vanilla JS   │
│  Compose Multiplatform UI         │    │  WebRTC nativo do browser  │
│  WebRTC nativo (lib KMP wrapper)  │    │                            │
└───────────────┬───────────────────┘    └─────────────┬──────────────┘
                │                                      │
                │              WebRTC P2P              │
                │  ◄──────── audio + video ────────►   │
                │  ◄────── data channel (estado) ───►  │
                │                                      │
                ▼                                      ▼
        ┌────────────────────────────────────────────────────┐
        │       Servidor de sinalização (Ktor + WS)          │
        │  - rooms, tokens, ICE relay (signaling only)       │
        │  - persistência de sessão / pedido (PostgreSQL)    │
        │  - integração ERP / pagamento (out-of-band)        │
        └────────────────────────────────────────────────────┘
                                │
                                ▼
                  ┌──────────────────────────┐
                  │  STUN / TURN (coturn)    │
                  │  para NAT traversal      │
                  └──────────────────────────┘
```

**Princípio**: o servidor é leve — só faz **handshake** (sinalização) e **persistência** (pedidos finais). Áudio, vídeo e estado do catálogo nunca passam por ele.

---

## 2. Por que KMP

- O vendedor está em iPhone (a maioria dos representantes de moda no BR) e Android. Não podemos manter dois apps separados.
- A lógica de **sessão**, **catálogo**, **carrinho**, **pricing**, **sync de estado** é a mesma nos dois lados. KMP compartilha essa lógica como Kotlin puro.
- Compose Multiplatform permite compartilhar **também a UI** entre iOS e Android, sem perder fidelidade.
- Equipe pequena. Cada feature feita uma vez.

**Não compartilhamos**: o web app do cliente (escrito em TS). Mas compartilhamos o **protocolo** de DC via uma spec versionada — vide `docs/04-architecture.md`.

---

## 3. Bibliotecas — vendedor (KMP)

| Camada | Lib | Versão | Por quê |
|---|---|---|---|
| UI | `compose-multiplatform` | 1.7.x | UI compartilhada iOS+Android |
| Nav | `voyager` ou `decompose` | última | Navegação multiplatform |
| Estado | `StateFlow` / `MutableStateFlow` (built-in) | — | MVVM reativo |
| DI | `koin` | 4.x | Multiplataforma, simples |
| HTTP | `ktor-client` | 3.x | Multiplataforma |
| Serialização | `kotlinx.serialization` | última | JSON do DC + REST |
| DB local | `sqldelight` | 2.x | SQL compartilhado iOS/Android |
| Datas | `kotlinx-datetime` | última | Multiplataforma |
| Coroutines | `kotlinx-coroutines` | última | Async em tudo |
| WebRTC | `webrtc-kmp` (Shepeliev) ou wrappers manuais | última | Cobre `google-webrtc` (Android) e `WebRTC.framework` (iOS) |
| Mídia (camera) | `kmp-camera` ou expect/actual próprio | — | Câmera/microfone |
| Imagem | `coil3` (multiplatform) | 3.x | Cache de fotos do catálogo |
| Logs | `kermit` | última | Multiplataforma |
| Testes | `kotlin.test` + `turbine` + `mockative` | — | Lógica + Flow |

**WebRTC — nota importante**: a lib `webrtc-kmp` envelopa `org.webrtc:google-webrtc` no Android e `WebRTC.framework` no iOS (instalado via CocoaPods). Se a lib externa não acompanhar uma versão de WebRTC que precisamos, fallback é `expect/actual` próprio com chamadas diretas a essas libs nativas — isolamos esse risco numa interface única (`PeerConnection`).

---

## 4. Bibliotecas — cliente web

| Camada | Lib | Por quê |
|---|---|---|
| UI | Vanilla TS + lit-html ou Preact (decidir) | Bundle pequeno; abre rápido |
| WebRTC | nativo do browser | Não precisa lib |
| Estado | `nanostores` ou hand-rolled | Pequeno |
| Build | `vite` | Rápido, simples |
| CSS | CSS puro com tokens (do `02-design-system.md`) | Sem dependência |

**Por que não React Native ou Flutter**: cliente NÃO instala app. É web pura, embed-friendly.

---

## 5. Bibliotecas — servidor

| Camada | Lib |
|---|---|
| HTTP / WS | `ktor-server-netty` |
| DI | `koin` |
| DB | `postgresql` + `exposed` ou `sqldelight-jvm` |
| Migrations | `flyway` |
| Auth | JWT (`ktor-server-auth-jwt`) |
| Observabilidade | `micrometer` + Prometheus |
| Logs | `logback` |

**TURN/STUN**: rodar `coturn` em VPS separada. STUN é grátis (Google), mas redes 4G brasileiras frequentemente exigem TURN. Orçar ~$20/mês por mil sessões/dia.

---

## 6. WebRTC — detalhes que importam

### 6.1 Sinalização (não-WebRTC, mas precondição)
- WebSocket via Ktor.
- Mensagens: `offer`, `answer`, `ice-candidate`, `peer-joined`, `peer-left`.
- Tudo serializado com `kotlinx.serialization`.

### 6.2 Media constraints
```kotlin
// Áudio: prioridade. Sempre Opus, banda baixa por padrão.
audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true }
// Vídeo (vendedor): VGA por padrão, sobe se rede permitir.
video: { width: 640, height: 480, frameRate: 24 }
// Vídeo do cliente: opcional, default false. Cliente pode ligar.
```

### 6.3 Data Channel
- **Reliable + ordered** para eventos de estado (cart, navigate, presence).
- **Unreliable** (lossy) para cursor/scroll (~30Hz, último valor vence).
- Mensagens JSON serializadas com discriminador `type`.
- Spec do protocolo: `docs/04-architecture.md` § 4.

### 6.4 ICE / NAT
- STUN: `stun:stun.l.google.com:19302`.
- TURN: `turn:turn.trovata.cast:3478` (operado pela nossa infra).
- Para venda em campo com 4G ruim: usar `iceTransportPolicy: 'relay'` como fallback quando P2P falha em <5s.

### 6.5 Reconexão
- ICE restart automático em queda.
- App mantém estado local; ao reconectar, troca snapshot via DC (event `StateSnapshot`).

### 6.6 Telemetria
- `getStats()` a cada 5s → bitrate, packet loss, RTT, jitter.
- Métricas exportadas para o backend após a call (anônimas).
- Triggers no app: <100kbps → modo economia; >5% packet loss → banner "rede ruim".

---

## 7. Estratégia de modo offline

- Catálogo: cacheado em SQLDelight no vendedor. Funciona offline durante preparação de sessão.
- Sincroniza com backend quando online.
- Cliente web: cacheado em IndexedDB durante a sessão (não persiste depois — privacidade).

---

## 8. Segurança

- Tokens de sessão: JWT curto (1h). Renovado automaticamente enquanto a sessão está aberta.
- Link da sessão: `https://trovata.cast/s/<token>`. Token contém `sessionId` + assinatura.
- DC: mensagens não criptografadas em camada de app (WebRTC já criptografa por DTLS/SRTP em transporte).
- Não armazenamos áudio/vídeo no servidor. Eventos persistidos são metadados (timestamps, productIds, qty), não conteúdo de fala.
- LGPD: termo de aceite no primeiro login do cliente. Possibilidade de "esquecer" sessão completa via UI.

---

## 9. Build & CI

- **Gradle** com convention plugins.
- **GitHub Actions**:
  - `assembleDebug` + lint + testes em PR.
  - `assembleRelease` + assinar + subir TestFlight/Play em tag.
  - Web app: build + deploy em Cloudflare Pages.
  - Servidor: build Docker image + deploy em VPS (Hetzner ou similar).
- **Versionamento**: SemVer no servidor; build number incremental no app.

---

## 10. Por que **NÃO** escolhemos cada alternativa óbvia

| Em vez de | Escolhemos | Por quê |
|---|---|---|
| Flutter | KMP + Compose | Time já é Kotlin; Compose Multiplatform maduro; melhor interop com WebRTC nativo |
| React Native | KMP + Compose | Performance crítica em telas com ponteiro a 30Hz; bridge JS é gargalo |
| Native iOS + Android separados | KMP | Dobra o trabalho com pouco ganho |
| Twilio / Daily.co / Agora | WebRTC P2P | $0 por sessão vs $0.004+; controle total; alinha com premissa de privacidade |
| Socket.io | Ktor WS | Mantém stack 100% Kotlin |
| Firebase | Servidor próprio | Custos crescem com sessão; queremos controle do schema |
| Wear / Desktop | Foco mobile | Mobile é 100% do uso real (premissa do produto) |
| GraphQL | REST simples + WS | Esquema é pequeno; over-engineering |

---

## 11. Riscos técnicos conhecidos

1. **WebRTC iOS é capricho** — versão do framework e setup do CocoaPods precisam ser exatos. Travar versão.
2. **Compose Multiplatform iOS ainda evolui rápido** — manter próximo da release estável; evitar APIs experimental sem fallback.
3. **NAT traversal em redes 4G brasileiras** — TURN obrigatório, não opcional.
4. **Bateria do iPhone com WebRTC ativo** — chamadas longas (>20min) drenam. Monitorar.
5. **CocoaPods + KMP build** — vai dar trabalho de configurar. Documentar passo-a-passo no `iosApp/README.md`.
