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
# https://localhost:5173  (cert self-signed — aceite o aviso)
```

O Vite faz proxy de `/session` e `/ws` para o `signalingServer` em `127.0.0.1:8080`, então o buyer não precisa saber a porta 8080.

### signalingServer (Ktor)

```bash
./gradlew :signalingServer:run
# http://localhost:8080/health → "ok"
```

---

## 🔌 Rodar os dois servidores juntos (testar entre dispositivos na mesma LAN)

Para o cenário real (iPhone vendedor + outro celular como cliente abrindo o link), os dois servidores precisam ficar de pé e acessíveis pela rede local.

### 1. Descobrir o IP local do Mac

```bash
ipconfig getifaddr en0
# ex.: 192.168.1.101
```

> Use esse IP nos passos abaixo no lugar de `192.168.1.101`.

### 2. Configurar o webBuyer para usar esse IP

```bash
echo "VITE_SIGNALING_BASE=https://192.168.1.101:5173" > webBuyer/.env.local
```

### 3. Configurar o app KMP (iOS/Android) para apontar para o Mac

Em `composeApp/src/iosMain/kotlin/app/trovata/cast/platform/PlatformIos.kt` (e o equivalente Android), o `baseUrl` deve ser `http://192.168.1.101:8080`. O `iosApp/iosApp/Info.plist` já tem `NSAppTransportSecurity` + `NSLocalNetworkUsageDescription` configurados para permitir HTTP local.

### 4. Limpar instâncias antigas (sempre antes de subir)

Se já tentou rodar antes, daemons do Gradle ou Vite podem estar ocupando as portas. Limpe primeiro:

```bash
kill -9 %1 %2 %3 %4 %5 2>/dev/null
lsof -ti :8080 | xargs kill -9 2>/dev/null
lsof -ti :5173 | xargs kill -9 2>/dev/null
lsof -ti :5174 | xargs kill -9 2>/dev/null
sleep 3

lsof -ti :8080 :5173 :5174 2>/dev/null && echo "AINDA TEM ALGO RODANDO" || echo "tudo limpo"
```

Só prossiga quando aparecer `tudo limpo`. Se sobrar processo:

```bash
ps aux | grep -iE "gradle|java.*signaling|vite" | grep -v grep
```

Pegue o PID na 2ª coluna e mate com `kill -9 <PID>`.

### 5. Subir tudo em background com logs

Da raiz do projeto:

```bash
PUBLIC_BUYER_URL=https://192.168.1.101:5173 \
  nohup ./gradlew :signalingServer:run \
  < /dev/null > /tmp/trovata-signaling.log 2>&1 &
disown

nohup bash -lc "cd webBuyer && npm run dev" \
  < /dev/null > /tmp/trovata-vite.log 2>&1 &
disown
```

> Os `< /dev/null` e `disown` são essenciais no zsh — sem eles os processos travam em `suspended (tty input)` quando Gradle ou npm tentam ler do terminal.

Acompanhar logs:

```bash
tail -f /tmp/trovata-signaling.log
tail -f /tmp/trovata-vite.log
```

### 6. Verificar que estão de pé

Espere uns 12s (Gradle demora pra subir o Ktor), depois:

```bash
sleep 12
curl -sf http://192.168.1.101:8080/health && echo " ← signaling OK"
grep -E "Local:|Network:" /tmp/trovata-vite.log
```

Você deve ver `ok ← signaling OK` + `Network: https://192.168.1.101:5173/`. Se o Vite cair na 5174, alguma instância antiga ficou na 5173 — volta pro passo 4.

### 7. Parar os servidores

```bash
lsof -ti :8080 | xargs kill -9 2>/dev/null
lsof -ti :5173 | xargs kill -9 2>/dev/null
lsof -ti :5174 | xargs kill -9 2>/dev/null
```

> No `lsof` do macOS, **uma porta por comando** (a sintaxe `lsof -ti :8080 :5173` não funciona). Limpe a 5174 também caso uma instância antiga do Vite tenha caído pra essa porta.

### Notas

- O webBuyer **precisa** ser HTTPS para o navegador liberar `getUserMedia` num device diferente do Mac (só `localhost` é exceção). O `@vitejs/plugin-basic-ssl` gera um cert self-signed; aceite o aviso na primeira vez em cada device.
- Na primeira vez que o cliente entrar pelo celular, ele provavelmente vai precisar abrir `https://192.168.1.101:5173/` direto pra aceitar o cert, antes de seguir o link real.
- O `PUBLIC_BUYER_URL` é o que o `signalingServer` injeta no `SessionCreateResponse.url` — é o link que aparece no QR/share do app vendedor.

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
