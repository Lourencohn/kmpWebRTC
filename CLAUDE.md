# TrovataCast, contexto para Claude Code

> Este repositório contém o **TrovataCast**, aplicativo de venda B2B com co-presença em tempo real, construído em **Kotlin Multiplatform** com **WebRTC**. Este é o único documento do projeto.

---

## 1. O que é o TrovataCast

Distribuidoras, representantes e atacadistas brasileiros vendem hoje por ligação e PDF no WhatsApp. O **TrovataCast** transforma esse momento numa sessão ao vivo em que **vendedor e cliente veem a mesma vitrine, ao mesmo tempo, em telas diferentes**, com áudio P2P, navegação sincronizada, desenho por cima da tela e carrinho em tempo real.

- **Vendedor**: app nativo (iOS e Android via KMP e Compose).
- **Cliente**: não instala nada, abre o link da vitrine do Catálogo Link no navegador do celular.
- **Pedido**: nasce dentro da chamada, sem digitação posterior.
- **Privacidade**: áudio e estado da vitrine trafegam direto entre os dois dispositivos (WebRTC P2P).

O app faz login no SFA, mostra sessões, carrinhos abertos, catálogos-links, gravações e clientes, cria convites, conduz a chamada sobre a vitrine do Catálogo Link e grava apresentações para o cliente assistir depois.

Categoria-alvo do MVP: **moda atacado**.

---

## 2. Decisões já tomadas (não revisitar sem motivo forte)

| Decisão | Valor |
| --- | --- |
| Linguagem | Kotlin (KMP) |
| UI cross-platform | Compose Multiplatform (iOS e Android) |
| Comunicação tempo-real | WebRTC com `webrtc-kmp` |
| Cliente (buyer) | Vitrine do Catálogo Link (`sfa_front`) com o módulo ao vivo. `webBuyer/` é um cliente de demonstração |
| Sinalização | Ktor server (Kotlin) com WebSocket |
| Persistência local | SQLDelight (compartilhado entre iOS e Android) |
| DI | Koin |
| Navegação e estado de tela | Voyager (`Screen` e `ScreenModel`) |
| Network | Ktor client |
| Idioma da UI | Português (BR) |
| Paleta | Azul `#2456E0` e Jade `#0E8F6E` sobre off-white quente |
| Categoria do MVP | Moda atacado |

---

## 3. Módulos

| Pasta | O que é |
| --- | --- |
| `composeApp/` | App do vendedor (Android e iOS) |
| `protocol/` | Contrato de fio compartilhado entre app e servidor: mensagens de sinalização e do DataChannel |
| `signalingServer/` | Ktor: cria sessões (`/session`), faz a sinalização (`/ws/session/{token}`) e assina uploads das apresentações (`/presentations`) |
| `webBuyer/` | Cliente web de demonstração (Vite e TypeScript). Espelha à mão o contrato do `protocol` em `src/protocol` e `src/signaling` |
| `iosApp/` | Projeto Xcode (XcodeGen e CocoaPods) que hospeda o framework do `composeApp` |
| `prototype/` | Protótipo HTML usado como referência visual |

---

## 4. Princípios de execução

Quando você (Claude Code) for construir, mantenha:

1. **Co-presença é o produto.** Tudo que reforça "estamos vendo a mesma coisa, juntos" tem prioridade sobre features genéricas de catálogo ou chat.
2. **Mobile-first, mas mobile-real.** O vendedor está dirigindo, andando na feira, com 4G ruim. O cliente está no depósito. Tudo precisa funcionar com uma mão e sem tutorial.
3. **Cliente nunca instala nada.** Quebrar isso quebra o produto.
4. **P2P até onde der.** Mídia e estado da vitrine trafegam direto entre dispositivos. O servidor faz sinalização e assina uploads.
5. **Pedido nasce na chamada.** Nenhuma digitação posterior. Se um fluxo exige formulário pós-call, está errado.
6. **Histórico se constrói sozinho.** Cada sessão gera registro estruturado sem ação manual.
7. **Português brasileiro coloquial-profissional.** "Atender", "Convidar", "Apontando", "Mostrando", "Pedido pronto". Não "iniciar videochamada".

---

## 5. Estrutura e convenções de código

O código do app fica em `composeApp/src/commonMain/kotlin/app/trovata/cast/`:

- `data/`: fontes de dados e os modelos que elas produzem (`auth`, `local` sobre SQLDelight, `remote` e `remote/sfa` com os DTOs do SFA, `signaling`, `sync`).
- `feature/<nome>/`: uma pasta por funcionalidade, com rota, telas, ScreenModel e estado juntos.
- `ui/`: design system (`theme`, `components`, `icons`, `format`).
- `webrtc/`: `PeerSession`, a conexão P2P.
- `platform/`: `expect`/`actual` de Android e iOS.
- `di/`: módulos Koin, o único lugar que monta o grafo.

Regras:

- **Dependência entre camadas**: `feature` pode usar `data`, `ui`, `webrtc` e `platform`. `data` nunca usa `feature` nem `ui`. `ui` não conhece `data` nem `feature`, e o componente recebe tudo por parâmetro.
- **Rotas e telas**: cada destino é uma classe `*Route` do Voyager, no mesmo arquivo da tela. A rota é a camada de montagem e pode pegar dependências. O Composable de tela recebe estado e ações.
- **Estado**: `ScreenModel` do Voyager expondo `StateFlow<*UiState>`, registrado como `factory` no `ScreenModelModule` e obtido com `koinScreenModel`. Regra de negócio fica no ScreenModel, nunca na tela. As chamadas de rede e dados do usuário chegam ao ScreenModel como funções (`LoadOpenCartsFn`, `sellerProvider`), o que permite testar sem rede.
- **Coroutines**: rede e banco fora da thread principal (os repositórios usam `Dispatchers.Default`), nunca bloqueante.
- **Nomes**: código em inglês e textos de tela em português. Nomes de contrato externo seguem o contrato: DTOs do SFA, mensagens do `protocol`, colunas do banco e os identificadores de entidades do SFA (`empresaSlug`, `empresaId`, `catalogoUuid`, `catalogoLinkId`, `catalogoNome`, `carrinhoId`, `produtoPreId`, `tabelaPrecoId`, `prazoId`) em todas as camadas.
- **Arquivos grandes**: Composables privados separados por assunto no mesmo pacote. `internal` só quando outro arquivo usa.
- **Banco**: mudança de schema entra como nova migração `N.sqm`, nunca editando uma existente.
- **Contrato**: mudança no `protocol` exige a mesma mudança no espelho TypeScript do `webBuyer`.
- **Testes**: regra de domínio e ScreenModel têm teste no `commonTest`, no mesmo pacote do código, com nomes em inglês.

---

## 6. Configuração

URLs, realm e client do Keycloak, empresa padrão e versão chegam ao código pelo objeto gerado `app.trovata.cast.config.AppConfig`. A origem é `composeApp/environments/<ambiente>.properties`. O padrão é `staging`, e outro ambiente é escolhido com `-Ptrovata.environment=<nome>`. Nada de endereço fixo no código.

Para apontar o app para serviços locais, sobrescreva qualquer chave no `local.properties` (fora do Git) com o prefixo `trovata.`, por exemplo `trovata.signalingUrl=http://192.168.1.101:8080` ou `trovata.catalogWebUrlOverride=http://localhost:5173/`.

A versão (`trovata.versionName`, `trovata.versionCode`) fica no `gradle.properties`.

O servidor lê `PORT`, `PUBLIC_CATALOG_URL`, `SFA_API_URL`, `STATIC_DIR`, `ICE_STUN_URLS`, `ICE_TURN_URLS`, `ICE_TURN_USERNAME`, `ICE_TURN_CREDENTIAL`, `ICE_CLOUDFLARE_TURN_KEY_ID`, `ICE_CLOUDFLARE_TURN_API_TOKEN` e `PRESENTATION_S3_*`. Com a chave TURN da Cloudflare configurada, cada sessão recebe uma credencial TURN temporária gerada na hora, e se a Cloudflare falhar a sessão segue só com STUN. Para gravar apresentações localmente, `signalingServer/dev/storage-local.sh` sobe um S3 compatível em Docker. O deploy do servidor é no Fly.io (`fly.toml`, `Dockerfile` na raiz), e o do `webBuyer` é no Cloudflare Pages pelo workflow do GitHub Actions.

---

## 7. Como rodar e testar

```bash
./gradlew :composeApp:installDebug
./gradlew :signalingServer:run
cd webBuyer && npm install && npm run dev
cd iosApp && xcodegen generate && pod install && open iosApp.xcworkspace
```

No iOS, `DEVELOPMENT_TEAM` e `JAVA_HOME` em `iosApp/project.yml` e no `iosApp/Podfile` são da máquina de quem montou o projeto e precisam ser trocados.

Os mesmos comandos do CI:

```bash
./gradlew :protocol:jvmTest
./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug
./gradlew :signalingServer:test :signalingServer:build
cd webBuyer && npm run typecheck && npm test && npm run build
```

O framework iOS (`./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`) só compila no macOS.

Para ver o protótipo, rode `python3 -m http.server 8000` dentro de `prototype/` e abra `http://localhost:8000`.

---

## 8. Código limpo

Não adicione comentários durante os códigos, preservo muito o código limpo e organizado.

---

## 9. Convenções de commits e versionamento

- **Mensagens de commit curtas e sucintas.** Conventional Commits: `tipo(escopo): descrição em uma linha`. Sem corpo a não ser que indispensável; quando precisar, máximo 3 linhas.
- **Nada de `Co-Authored-By: Claude ...`** nem assinatura de geração por IA. Commits são meus.
- Escopo é o nome do módulo: `composeApp`, `iosApp`, `webBuyer`, `signalingServer`, `protocol`, `ci`, `github`.
- Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`.
- Exemplos OK: `feat(composeApp): pill component`, `fix(ci): cache gradle`, `chore: bump kotlin`.
- Branch a partir da `main` atualizada e sem tracking (`git checkout -b feat/assunto origin/main --no-track`). Nada vai direto para a `main`, só por pull request com o CI verde.
