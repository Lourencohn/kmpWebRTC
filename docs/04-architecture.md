# Arquitetura — estrutura de módulos

> Onde mora o quê. Para Claude Code, este é o mapa de "onde colocar o próximo arquivo".

---

## 1. Estrutura do repositório

```
trovatacast/
├── CLAUDE.md                          # ponto de entrada para Claude Code
├── README.md                          # README público do repo
├── docs/                              # toda a documentação
│   ├── 00-concept.md
│   ├── 01-product-flows.md
│   ├── 02-design-system.md
│   ├── 03-stack-kmp-webrtc.md
│   ├── 04-architecture.md             # este arquivo
│   ├── 05-screens-reference.md
│   └── 06-roadmap.md
│
├── prototype/                         # protótipo HTML — referência viva
│   ├── index.html
│   ├── styles.css
│   ├── app.jsx
│   ├── catalog.jsx
│   ├── frames-buyer.jsx
│   ├── frames-seller.jsx
│   ├── ui.jsx
│   └── ... starters (design-canvas/ios-frame/android-frame)
│
├── composeApp/                        # ★ app KMP (vendedor) — Compose Multiplatform
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/app/trovata/cast/
│       │   ├── App.kt                  # root composable
│       │   ├── di/                     # módulos Koin
│       │   ├── theme/                  # cores, tipografia, shapes
│       │   ├── ui/
│       │   │   ├── components/        # Pill, Btn, Avatar, VideoTile, ProductCard...
│       │   │   ├── icons/             # Icons object (paths SVG)
│       │   │   ├── nav/               # rotas
│       │   │   └── screens/
│       │   │       ├── sessions/
│       │   │       ├── catalog/
│       │   │       ├── prep/
│       │   │       ├── live/          # F5 + F6 — coração do produto
│       │   │       ├── summary/
│       │   │       ├── pipeline/
│       │   │       └── settings/
│       │   ├── feature/                # camada de feature (state, viewmodel)
│       │   │   ├── sessions/
│       │   │   ├── catalog/
│       │   │   ├── live/
│       │   │   ├── cart/
│       │   │   └── pipeline/
│       │   ├── domain/                 # entidades de domínio + casos de uso
│       │   │   ├── model/             # Product, Order, Session, Client, Price
│       │   │   ├── usecase/
│       │   │   └── repository/        # interfaces
│       │   ├── data/                   # implementações de repositório
│       │   │   ├── local/             # SQLDelight queries
│       │   │   ├── remote/            # Ktor client
│       │   │   └── sync/              # offline-first sync
│       │   ├── webrtc/                 # ★ camada WebRTC
│       │   │   ├── PeerConnection.kt   # expect class
│       │   │   ├── SessionMedia.kt
│       │   │   ├── SessionDataChannel.kt
│       │   │   ├── SignalingClient.kt
│       │   │   └── protocol/           # SessionEvent, codecs
│       │   └── util/
│       │
│       ├── androidMain/kotlin/         # actual impls Android
│       │   └── webrtc/                 # PeerConnection.android.kt
│       ├── iosMain/kotlin/             # actual impls iOS
│       │   └── webrtc/                 # PeerConnection.ios.kt
│       └── commonTest/kotlin/
│
├── iosApp/                            # bootstrap iOS (Xcode project)
│   ├── iosApp/
│   │   └── iOSApp.swift
│   ├── Podfile                        # WebRTC.framework via CocoaPods
│   └── README.md                      # passo-a-passo build iOS
│
├── webBuyer/                          # ★ app web do cliente (TypeScript)
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   ├── src/
│   │   ├── main.ts
│   │   ├── webrtc/                   # peer + DC
│   │   ├── state/
│   │   ├── ui/
│   │   │   ├── components/
│   │   │   └── views/                # arrival, live, detail, confirm, async
│   │   └── styles/
│   └── public/
│
├── signalingServer/                   # ★ Ktor server
│   ├── build.gradle.kts
│   ├── src/main/kotlin/app/trovata/cast/server/
│   │   ├── Main.kt
│   │   ├── plugins/                  # auth, cors, monitoring
│   │   ├── routing/
│   │   │   ├── sessions.kt           # REST: criar sessão, etc
│   │   │   ├── signaling.kt          # WebSocket
│   │   │   ├── orders.kt
│   │   │   └── webhook.kt
│   │   ├── domain/                   # entidades
│   │   ├── persistence/              # SQLDelight ou Exposed
│   │   └── di/
│   └── src/main/resources/
│       ├── application.conf
│       └── db/migration/             # Flyway
│
├── protocol/                          # ★ módulo compartilhado de protocolo DC
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/app/trovata/cast/protocol/
│       ├── SessionEvent.kt           # tipos sealed
│       ├── Route.kt
│       └── Codec.kt                  # JSON serializer
│
├── gradle/
│   └── libs.versions.toml             # versões centralizadas
├── settings.gradle.kts
├── build.gradle.kts
└── .gitignore
```

---

## 2. Princípios de camadas

```
┌─────────────────────────────────────────────────┐
│ ui/screens/<feature>/<Screen>.kt                │  ← Composable, sem lógica
├─────────────────────────────────────────────────┤
│ feature/<feature>/<Feature>ViewModel.kt         │  ← StateFlow<UiState>
├─────────────────────────────────────────────────┤
│ domain/usecase/<UseCase>.kt                     │  ← regra de negócio pura
├─────────────────────────────────────────────────┤
│ domain/repository/<Repo>.kt (interface)         │
│ data/<RepoImpl>.kt                              │  ← orquestra local + remoto
├─────────────────────────────────────────────────┤
│ data/local + data/remote + webrtc/              │  ← fontes
└─────────────────────────────────────────────────┘
```

**Regras**:
- UI nunca chama repository direto.
- Repository nunca importa Compose.
- Domain é Kotlin puro (sem coroutines fora de `suspend`).
- WebRTC fica isolado em `webrtc/` com `expect/actual` para PeerConnection.

---

## 3. Camada WebRTC — interface única

```kotlin
// commonMain
expect class PeerConnection(config: PeerConfig) {
    val state: StateFlow<PeerState>
    val tracks: StateFlow<List<MediaTrack>>

    suspend fun createOffer(): SessionDescription
    suspend fun createAnswer(): SessionDescription
    suspend fun setLocalDescription(sdp: SessionDescription)
    suspend fun setRemoteDescription(sdp: SessionDescription)
    suspend fun addIceCandidate(candidate: IceCandidate)

    fun openDataChannel(label: String, reliable: Boolean = true): DataChannel
    fun addAudioTrack(track: AudioTrack)
    fun addVideoTrack(track: VideoTrack)
    fun close()
}

interface DataChannel {
    val incoming: Flow<ByteArray>
    suspend fun send(payload: ByteArray)
    fun close()
}
```

Implementações em `androidMain/webrtc/PeerConnection.android.kt` e `iosMain/webrtc/PeerConnection.ios.kt`. Cliente web tem sua própria implementação em `webBuyer/src/webrtc/`.

---

## 4. Protocolo do Data Channel

> Spec versionada em `protocol/SessionEvent.kt`. Cliente web tem cópia em `webBuyer/src/webrtc/events.ts` (mantido em sync à mão — pequeno o suficiente).

```kotlin
@Serializable
sealed class SessionEvent {
    abstract val ts: Long          // millis since epoch
    abstract val from: String      // userId

    @Serializable @SerialName("cursor")
    data class Cursor(val x: Float, val y: Float, val productId: String?,
                      override val ts: Long, override val from: String) : SessionEvent()

    @Serializable @SerialName("scroll")
    data class Scroll(val productInView: String, val offset: Float,
                      override val ts: Long, override val from: String) : SessionEvent()

    @Serializable @SerialName("point")
    data class PointAt(val productId: String,
                       override val ts: Long, override val from: String) : SessionEvent()

    @Serializable @SerialName("nav")
    data class Navigate(val route: Route,
                        override val ts: Long, override val from: String) : SessionEvent()

    @Serializable @SerialName("cart")
    data class CartUpdate(val sku: String, val sizes: Map<String, Int>,
                          override val ts: Long, override val from: String) : SessionEvent()

    @Serializable @SerialName("reaction")
    data class Reaction(val productId: String, val kind: ReactionKind,
                        override val ts: Long, override val from: String) : SessionEvent()

    @Serializable @SerialName("offer")
    data class OfferProposed(val sku: String, val priceCents: Long,
                             val validUntilMs: Long,
                             override val ts: Long, override val from: String) : SessionEvent()

    @Serializable @SerialName("draw")
    data class DrawStroke(val productId: String, val points: List<Point>,
                          val color: String,
                          override val ts: Long, override val from: String) : SessionEvent()

    @Serializable @SerialName("presence")
    data class Presence(val state: PresenceState,
                        override val ts: Long, override val from: String) : SessionEvent()

    @Serializable @SerialName("snapshot")
    data class Snapshot(val state: SessionState,
                        override val ts: Long, override val from: String) : SessionEvent()
}

@Serializable
sealed class Route {
    @Serializable @SerialName("catalog") data class Catalog(val collectionId: String) : Route()
    @Serializable @SerialName("product") data class Product(val sku: String) : Route()
}
```

Codec: usar `Json { classDiscriminator = "type"; ignoreUnknownKeys = true }`.

---

## 5. Modelo de domínio principal

```kotlin
data class Product(
    val sku: String,           // "AN-217"
    val name: String,
    val collectionId: String,
    val moq: Int,
    val sizes: List<String>,
    val colors: List<Color>,
    val basePriceCents: Long,
    val photos: List<PhotoRef>,
    val tags: List<Tag>,       // "Novo", "Top venda", "Pré-venda"
)

data class Client(
    val id: ClientId,
    val name: String,          // "Diego Albuquerque"
    val shopName: String,      // "Trama Multimarcas"
    val city: String,
    val priceTableId: PriceTableId,
    val notes: String,
    val avatarHue: Int,        // consistente entre telas
)

data class Session(
    val id: SessionId,
    val token: String,
    val sellerId: UserId,
    val clientId: ClientId,
    val plannedCatalog: List<String>,  // SKUs
    val status: SessionStatus,
    val startedAt: Instant?,
    val endedAt: Instant?,
)

data class Order(
    val id: OrderId,
    val sessionId: SessionId,
    val lines: List<OrderLine>,
    val totalCents: Long,
    val createdAt: Instant,
)

data class OrderLine(
    val sku: String,
    val sizes: Map<String, Int>,
    val priceCents: Long,
    val discountCents: Long,
)
```

---

## 6. Persistência local (SQLDelight)

Schemas no `composeApp/src/commonMain/sqldelight/app/trovata/cast/db/`:
- `Product.sq` — catálogo cacheado.
- `Client.sq` — carteira.
- `Session.sq` — histórico local de sessões.
- `Order.sq` — pedidos pendentes / em revisão.
- `Event.sq` — eventos brutos da sessão (para construir o resumo).

---

## 7. Estado de uma sessão ao vivo

```kotlin
data class LiveSessionState(
    val sessionId: SessionId,
    val peer: Peer,
    val media: MediaState,                        // audio/video on/off
    val pointer: Map<UserId, PointerState>,       // pos por usuário
    val route: Route,                             // catalog ou product detail
    val cart: Map<String, CartLine>,              // sku → linha
    val reactions: List<Reaction>,
    val proposedOffers: List<Offer>,
    val networkQuality: NetworkQuality,
    val connection: ConnectionStatus,
)
```

Único `StateFlow<LiveSessionState>` no `LiveSessionViewModel`. UI dos dois (catalog + product detail) deriva daí.

---

## 8. Convenções extras

- **Logs**: kermit com tag por feature (`Log.tag("live")`, `Log.tag("webrtc")`).
- **Análise**: eventos disparados via `Analytics.track(event)`, implementação pluggable.
- **Feature flags**: simples `FeatureFlags.kt` com booleans (`enableDrawOnProduct`, `enableMultiClient`).
