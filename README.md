# TrovataCast

Aplicativo móvel de venda B2B com **co-presença em tempo real** — vendedor e cliente vendo o mesmo catálogo, ao mesmo tempo, em telas diferentes.

> **Stack**: Kotlin Multiplatform (Compose Multiplatform) + WebRTC P2P + Ktor.
> **Categoria-alvo do MVP**: moda atacado.
> **Status**: Milestone 0 — esqueleto montado; 4 superfícies abrem com a wordmark.

---

## ⚡ Primeira vez? Comece aqui

```bash
./bootstrap.sh        # (opcional) instala wrappers e dependências de cada superfície
```

Para entender o produto antes do código, leia o `CLAUDE.md` e abra `prototype/index.html`.

---

## 📂 Estrutura

```
trovatacast/
├── CLAUDE.md                ← Ponto de entrada para Claude Code
├── docs/                    ← Documentação do produto (0 a 6)
├── prototype/               ← Protótipo HTML (referência visual viva)
│
├── protocol/                ← Módulo KMP compartilhado: SessionEvent + Codec
├── composeApp/              ← App KMP do vendedor (Android + iOS)
│   ├── src/commonMain/      ← Compose UI + theme + components
│   ├── src/androidMain/     ← MainActivity, manifest, recursos Android
│   └── src/iosMain/         ← MainViewController exposto pra Swift
│
├── iosApp/                  ← Bootstrap Xcode (XcodeGen)
├── webBuyer/                ← Cliente web (Vite + TypeScript)
├── signalingServer/         ← Ktor + WebSocket (sinalização e pedidos)
│
├── gradle/libs.versions.toml← Catálogo central de versões
└── .github/workflows/ci.yml ← Build Android + iOS + web + server
```

---

## 🚀 Rodar cada superfície

### Pré-requisitos (uma vez)

```bash
# JDK 17 (instale via brew/sdkman/asdf)
java -version

# Gradle 8.11+ (só pra gerar o wrapper na primeira vez)
brew install gradle
gradle wrapper --gradle-version=8.11.1

# Para iOS
brew install xcodegen
xcode-select --install

# Para webBuyer
brew install node
```

### Android (vendedor)

```bash
./gradlew :composeApp:installDebug
# ou abra no Android Studio: arquivo > abrir > seleciona a pasta raiz
```

### iOS (vendedor)

```bash
cd iosApp
xcodegen generate
open iosApp.xcodeproj
# selecione um simulador iOS e cmd+R
```

Passos detalhados em `iosApp/README.md`.

### webBuyer (cliente)

```bash
cd webBuyer
npm install
npm run dev
# http://localhost:5173
```

### signalingServer (Ktor)

```bash
./gradlew :signalingServer:run
# http://localhost:8080/health → "ok"
```

---

## ✅ Critério de aceitação do Milestone 0

- [x] Estrutura KMP per `docs/04-architecture.md`
- [x] `gradle/libs.versions.toml` com todas as libs do `docs/03`
- [x] Theme com tokens do `docs/02` (cores, raios, tipografia)
- [x] Fontes Geist + Geist Mono — slot pronto, instruções em `composeApp/.../fonts/README.md`
- [x] Android: wordmark TrovataCast renderiza
- [x] iOS: mesma wordmark (via `iosApp/`)
- [x] Ktor: `GET /health → "ok"`
- [x] Web buyer: `index.html` com wordmark
- [x] CI: Android + iOS framework + web + server em PR

---

## 🧭 Próximos passos

Roadmap em `docs/06-roadmap.md`. Próximo: **Milestone 1 — Componentes do design system** (Pill, Btn, ProductCard, VideoTile, RemotePointer reproduzindo o protótipo).

---

## 🎯 Conceito em uma frase

> O TrovataCast não é um catálogo com vídeo. É o registro vivo da relação comercial entre um vendedor e seus clientes, construído sessão a sessão, sem nenhum esforço manual de documentação.

(Trecho de `docs/00-concept.md`.)
