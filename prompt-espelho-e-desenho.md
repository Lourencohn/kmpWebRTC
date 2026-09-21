# Contexto para iniciar: o vendedor vê a mesma tela do cliente, e desenha nela

> Documento de handoff. Leia inteiro antes de escrever qualquer linha de código.
> Ele descreve duas capacidades a construir, o que já existe pronto nos dois repositórios e não deve ser reinventado, e as decisões que precisam ser tomadas antes de codar.
> Tudo aqui foi levantado lendo o código em 07/09/2026. O que é suposição está marcado como tal.

---

## 1. A missão, em duas frases

**Primeira:** durante a chamada, o vendedor deixa de ver uma reimplementação Compose do catálogo e passa a ver **literalmente a mesma página que o cliente**, a vitrine do Catálogo Link, embutida no app.

**Segunda:** o vendedor pode **desenhar por cima dessa tela** com o dedo, e o cliente vê o traço aparecer na tela dele, sobre o mesmo conteúdo.

As duas se sustentam mutuamente, e a ordem importa. Explicação na seção 6.

---

## 2. Os repositórios e o estado em que estão

| Caminho | O que é | Papel aqui |
|---|---|---|
| `~/Desenvolvimento/estudos/trovata/kmpWebRTC` | TrovataCast: app KMP do vendedor, sinalização Ktor, `protocol/` compartilhado | Recebe a WebView e o overlay de desenho do vendedor |
| `~/Desenvolvimento/estudos/trovata/sfa_front` | SPA Vue 3 do SFA, onde vive a vitrine pública do Catálogo Link | Recebe o overlay de desenho do cliente e os ajustes do modo embutido |
| `~/Desenvolvimento/estudos/trovata/sfa_back` | API Laravel 12 multi-tenant | Provavelmente não muda. Só entre nele se precisar confirmar contrato |

### Estado do git em 07/09/2026

**`kmpWebRTC`**: branch `main`, um commit local à frente de `origin/main`, árvore limpa.

**`sfa_front`**: branch `feature/catalogo-link-televenda-ao-vivo`, com o commit `715c3b39 feat(catalogo-link): televenda ao vivo com áudio e co-presença`. Essa branch está **17 commits à frente e 17 atrás de `origin/main`**, ou seja, o main andou bastante desde que ela nasceu.

### O que fazer com git antes de qualquer coisa

```
cd ~/Desenvolvimento/estudos/trovata/sfa_front
git fetch --all
git status -sb
```

O clone local costuma ficar atrás do origin, e isso já causou retrabalho neste projeto. Faça o fetch e olhe o resultado antes de afirmar o que existe no código.

Para atualizar a branch de trabalho, **mescle `origin/main` dentro dela**, nunca o contrário:

```
git merge origin/main
```

Se precisar de uma branch nova para os ajustes, crie a partir do main remoto sem herdar o tracking dele, porque um `git push` sem argumento numa branch que rastreia `origin/main` empurra direto para a branch protegida:

```
git fetch origin
git checkout -b feat/nome origin/main --no-track
git push -u origin feat/nome
```

Depois de qualquer push, confira que a protegida não andou: `git log --oneline -1 origin/main`.

Nunca commite nem faça merge direto em `main` ou `staging` do `sfa_front`. Integração só por pull request.

---

## 3. O que já existe e não deve ser reinventado

### 3.1 A ponte para a WebView já está escrita, e está inerte esperando o app

O PR `715c3b39` do `sfa_front` entregou `src/live/` inteiro, 1073 linhas, incluindo a peça central desta tarefa:

**`src/live/transport.ts`** define a interface `LiveTransport`, que é como o resto da página fala com a sessão ao vivo, sem saber se por baixo há WebRTC ou um aplicativo hospedeiro:

```ts
export interface LiveTransport {
  readonly peerId: string
  readonly role: LiveRole            // 'buyer' | 'seller'
  readonly handlesAudio: boolean
  onMessage(handler: (message: DataChannelMessage) => void): () => void
  onStatus(handler: (status: LiveStatus) => void): () => void
  send(message: DataChannelMessage): void
  setMuted(muted: boolean): void
  isMuted(): boolean
  hasMicrophone(): boolean
  close(): void
}
```

**`src/live/bridge.ts`** implementa esse transporte falando com o aplicativo hospedeiro por **três nomes de JavaScript**, e nada além disso:

| Nome | Direção | Papel |
|---|---|---|
| `window.TrovataLive.postMessage(json)` | página para app | a página envia uma `DataChannelMessage` serializada |
| `window.__trovataLiveReceive(json)` | app para página | o app entrega uma mensagem recebida do cliente |
| `window.__trovataLiveStatus(status)` | app para página | o app informa `idle`, `negotiating`, `connected`, `failed` ou `closed` |

Esse desenho foi escolhido porque casa exatamente com `addJavascriptInterface` no Android e `WKScriptMessageHandler` no iOS.

**Quando a ponte é usada:** `src/composables/use-live-session.ts` decide pelo query param. A página entra em modo embutido quando a URL tem `?live=<token>&embed=seller` **e** `window.TrovataLive` existe. Nesse modo ela **não abre WebSocket nem WebRTC**, e a barra de chamada dela some (`showsCallBar` fica falso), porque quem desenha os controles é o app.

Ou seja: do lado do front, a Frente A já está feita. O que falta é o app do outro lado do vidro.

### 3.2 O protocolo já é simétrico nos dois repositórios

O mesmo contrato existe em Kotlin e em TypeScript, e os dois precisam andar juntos:

- `kmpWebRTC/protocol/src/commonMain/kotlin/app/trovata/cast/protocol/DataChannelMessage.kt`
- `sfa_front/src/live/protocol.ts`

Hoje trafegam: `mute`, `navigate`, `scroll`, `pointAt`, `cartInvalidated`, `orderPlaced`. Toda mensagem carrega `ts` e `from`.

### 3.3 O `pointAt` já resolveu o problema de coordenadas que o desenho vai enfrentar

Leia com atenção, porque é o precedente que evita você inventar um sistema pior:

```kotlin
@Serializable
@SerialName("pointAt")
data class PointAt(
    val target: String,
    val xRatio: Float = 0.5f,
    val yRatio: Float = 0.5f,
    override val ts: Long,
    override val from: String,
    val durationMs: Long = 3_000,
) : DataChannelMessage()
```

O ponteiro não viaja em pixels. Ele viaja como **um alvo mais uma proporção dentro daquele alvo**. O alvo é uma string produzida por `LiveAnchor` (`protocol/.../LiveAnchor.kt`), no formato `produto:123` ou `produto:123:cor:45`, e `xRatio`/`yRatio` são a posição relativa dentro do elemento daquele produto.

Isso existe porque as duas telas são diferentes: iPhone do vendedor e navegador Android do cliente, tamanhos, colunas e posições de rolagem diferentes. Coordenada absoluta não sobrevive a isso.

O renderizador do lado do app é `ui/components/RemotePointer.kt`, com 75 linhas.

### 3.4 O que o app tem hoje na chamada, e que a WebView põe em xeque

| Arquivo | Linhas | O que faz |
|---|---|---|
| `ui/screens/call/LiveCallScreen.kt` | 1035 | grade de produtos, painel de produto, gaveta de carrinho, barra da chamada |
| `feature/call/LiveCallScreenModel.kt` | 593 | abre o carrinho, carrega a vitrine, publica e recebe as mensagens |
| `feature/call/PeerSession.kt` | 423 | WebRTC, áudio e DataChannel. `publishScroll`, `publishPointAt`, `publishNavigate`, `publishCartInvalidated`, `publishOrderPlaced` |
| `data/remote/sfa/VitrineApi.kt` | 203 | busca vitrine e grade pelas rotas públicas |

O painel de produto do app e o modal do cliente são **duas telas com regra própria** que hoje precisam ser mantidas em paralelo. Cada mudança de regra de preço no Catálogo Link tem que ser reescrita no Compose. Essa dívida é o principal motivo econômico da Frente A.

---

## 4. Frente A: o vendedor vê a mesma vitrine

### O que construir

No app, uma WebView que abre a vitrine do catálogo link da sessão com `?live=<token>&embed=seller`, expõe `window.TrovataLive` com um `postMessage`, e recebe mensagens chamando `window.__trovataLiveReceive` e `window.__trovataLiveStatus`.

O Compose Multiplatform não tem WebView comum. O projeto já usa o padrão `expect`/`actual` para código de plataforma, por exemplo `platform/Share.kt` com `ShareController` implementado em `androidMain` e `iosMain`. Siga esse padrão: Android WebView de um lado, `WKWebView` do outro.

A barra da chamada continua nativa, por cima da WebView: mudo, encerrar, estado da conexão, gaveta do carrinho se fizer sentido manter.

### Pontos a resolver antes de escrever código

**A URL da sessão.** O app já sabe montar o link do convite (`buildLiveInviteUrl` no `protocol/`), e o `StoredSessionRecord` guarda a URL. Confirme qual URL o vendedor deve abrir, e se ela é a mesma do cliente com `embed=seller` a mais.

**Login público do catálogo.** A vitrine é pública por uuid, mas existe login por e-mail (`POST catalogos-links/{slug}/{uuid}/login`) e catálogos com lista de clientes liberados. Descubra se a página embutida vai pedir esse login ao vendedor, e o que acontece se pedir. Isso pode exigir ajuste no `sfa_front`, e é o principal candidato à branch nova lá.

**Quem manda no carrinho.** Hoje o app abre o carrinho por conta própria (`LiveCallScreenModel.openCart`) e desenha a gaveta em Compose. Com a página embutida, a página também mexe no carrinho. Decida se o app para de manter carrinho próprio, se a gaveta some, e como as duas visões não brigam.

**Desempenho e gesto.** WebView tem custo em memória e em fluidez de rolagem, e captura gestos que o overlay de desenho vai querer. Meça antes de decidir que a Frente A substitui a grade Compose em vez de conviver com ela.

**iOS.** Não há Mac neste ambiente. O lado iOS só compila e roda na máquina de quem tiver um. Escreva o `actual` do iOS com cuidado e assuma que ele não será verificado aqui.

---

## 5. Frente B: o vendedor desenha e o cliente vê

### O problema central, que não é desenhar

Capturar um traço na tela é fácil: `Modifier.pointerInput` e um `Canvas` em Compose, `pointerdown/pointermove` e um `<canvas>` no navegador. O difícil é que **o traço precisa cair no mesmo lugar da tela do outro**, e as duas telas têm largura, altura, número de colunas e posição de rolagem diferentes.

Coordenada em pixels não sobrevive. Coordenada normalizada pela janela também não, se as duas janelas mostram conteúdos deslocados.

O `pointAt` já resolveu isso ancorando o ponto a um elemento identificado mais uma proporção dentro dele. Use o mesmo princípio, ou justifique por escrito por que não dá.

### Decisões que precisam de resposta antes do código

**Âncora do traço.** Um traço atravessa vários elementos e o espaço entre eles. Opções: ancorar o traço inteiro ao elemento onde ele começou; ancorar cada ponto ao elemento sob ele; ou ancorar ao documento inteiro, normalizando pela largura do conteúdo e pela posição absoluta de rolagem. A terceira é a mais simples e só funciona se os dois lados estiverem com o mesmo scroll, o que o `scroll` já sincroniza mas não garante.

**Efêmero ou permanente.** O `pointAt` tem `durationMs` e some sozinho. O desenho deve sumir depois de alguns segundos, ficar até o vendedor apagar, ou ficar preso ao produto e reaparecer quando voltar nele. Cada opção muda o contrato e a renderização.

**Volume de mensagens.** Um traço a 60 Hz gera muita mensagem. O `scroll` já usa amostragem (`sample(33)` no `PeerSession`). Faça o mesmo, e agrupe pontos em lotes por mensagem em vez de mandar ponto a ponto. Considere ordenação: perder um ponto do meio de um traço é aceitável, perder o "traço terminou" não é.

**Quem pode desenhar.** O pedido é vendedor desenha, cliente vê. Deixe o contrato simétrico mesmo assim, porque o campo `from` já existe e um dia o cliente vai querer apontar também.

**Apagar.** Precisa existir. Decida se é uma mensagem própria, se é um traço com modo apagador, ou se é só limpar tudo.

### O que muda em cada repositório

**`kmpWebRTC/protocol/`**: as mensagens novas, com `@Serializable` e `@SerialName`, junto das existentes em `DataChannelMessage.kt`. Este módulo tem testes; adicione os de serialização.

**`sfa_front/src/live/protocol.ts`**: o espelho exato em TypeScript, incluindo o parser defensivo que o arquivo já usa para as outras mensagens. Nada de confiar no formato recebido.

**App do vendedor**: captura do traço e publicação, mais o `publishDraw` equivalente aos outros no `PeerSession`.

**Cliente**: renderização do traço por cima da vitrine, no `sfa_front`.

**App do vendedor, de novo**: o vendedor precisa ver o próprio traço enquanto desenha, e isso é local, não vem de volta pelo canal.

---

## 6. Por que a ordem importa

A Frente B fica precisa quando as duas telas mostram o mesmo conteúdo. Se o vendedor desenha sobre a grade Compose e o cliente vê sobre a vitrine Vue, os dois layouts são diferentes e o traço só pode ser aproximado, ancorado a produtos, nunca ao espaço entre eles.

Com a Frente A no ar, os dois lados renderizam a mesma página, e aí o traço pode ser fiel.

Isso sugere fazer A antes de B. Mas A é o item de maior risco técnico da lista, e pode ser que ela não passe no teste de desempenho. Uma alternativa defensável é começar por B ancorado a produtos, que funciona nos dois cenários, e refinar depois. Decida com argumento, e registre a decisão.

---

## 7. Regras do projeto que valem aqui

- **Sem comentários no código.** Nome descritivo ou função extraída. Vale para Kotlin, TypeScript e Vue. A exceção é regra de negócio inexprimível em nome, e na dúvida não comente.
- **Português brasileiro** na UI e nas mensagens. Vocabulário do produto: "Apontando", "Mostrando", "Pedido pronto". Não invente termo novo quando a plataforma já tem um.
- **Não commite por conta própria.** Deixe as alterações prontas e staged, e sinalize. Quem commita é o dono do repositório.
- **Nenhuma menção a IA** em mensagem de commit, corpo de PR ou documentação.
- Conventional Commits curtos, escopo por módulo: `composeApp`, `protocol`, `signalingServer`, `docs`.
- Siga o padrão arquitetural existente antes de propor estrutura nova: MVVM com `StateFlow`, `expect`/`actual` para plataforma, Koin para injeção.
- Lógica de domínio e sincronização de estado têm cobertura de teste. Os do projeto rodam com `./gradlew :composeApp:testDebugUnitTest` e `:protocol:jvmTest`, e no front com `npm test`.

---

## 8. Como validar

Há um **Moto G22 conectado por USB** nesta máquina, com o app instalado. `~/Android/Sdk/platform-tools/adb devices` confirma.

```
./gradlew :composeApp:installDebug
~/Android/Sdk/platform-tools/adb shell monkey -p app.trovata.cast -c android.intent.category.LAUNCHER 1
```

O build é debug, então dá para ler o banco do app com `adb exec-out run-as app.trovata.cast cat databases/trovatacast.db`, que é como se descobriu, nesta base, que o sync estava 404 desde a virada para staging.

O app aponta para **staging**: `api-int-staging.trovata.app.br`, `api-staging.trovata.app.br` e o signaling em `trovatacast-signaling.fly.dev`.

Para testar a chamada de ponta a ponta você precisa dos dois lados: o app no aparelho e a vitrine aberta num navegador com o link da sessão.

**Uma limitação que vai aparecer:** não há servidor TURN configurado. Consultando uma sessão real no signaling, os `iceServers` são só dois STUN do Google. Em rede móvel com NAT simétrico a chamada não conecta, e falha em silêncio. Se o teste falhar em 4G, suspeite disso antes de suspeitar do seu código.

---

## 9. O que não fazer

- Não reescreva a vitrine em Compose para "melhorar". O objetivo é o oposto: parar de manter duas telas com a mesma regra.
- Não invente um segundo canal de comunicação. Tudo passa pelo DataChannel e pelo contrato do `protocol/`.
- Não coloque regra de preço, desconto ou fechamento dentro do app. O motor comercial é o Catálogo Link, e o app lista, compartilha, televende e devolve.
- Não use coordenada absoluta em pixels no contrato de desenho.
- Não mexa em comentários pré-existentes fora do escopo do que você está alterando.

---

## 10. Onde procurar quando estiver em dúvida

| Pergunta | Arquivo |
|---|---|
| Por que esse produto existe | `docs/00-concept.md` |
| O que já foi construído e com que evidência | `docs/07-status.md` |
| O que o PR de televenda entregou no front | `docs/11-pr-catalogo-link-televenda.md`, seção 4 tem as limitações conhecidas |
| Quais telas vêm a seguir e de que endpoint cada uma vive | `docs/12-proximas-telas.md`, item 4.1 é a Frente A |
| Onde ler o contrato real do Catálogo Link | rotas em `sfa_back/routes/api/`, tipos em `sfa_front/src/types/` |
| Como esse contrato foi verificado antes | `docs/07-status.md`, seções sobre carrinhos abertos e sobre o sync 404 |

---

## 11. Antes de escrever código, responda

1. A página embutida vai exigir login do vendedor? Se sim, o ajuste é no `sfa_front` e pede branch nova lá.
2. A WebView substitui a grade Compose na chamada, ou convive com ela? O que acontece com a gaveta do carrinho?
3. Qual âncora o desenho usa, e o que acontece quando os dois lados estão em posições de rolagem diferentes?
4. O traço é efêmero ou fica? Se fica, fica preso a quê?
5. Qual a taxa de amostragem e o tamanho do lote de pontos por mensagem?

Traga essas respostas com o trecho de código que as sustenta, não com suposição. Neste projeto, conclusão sem evidência já custou retratação mais de uma vez.

---

## 12. Pendência registrada em 20/09/2026: volume da chamada no celular do cliente

**O relato.** O cliente que entra pelo link no celular aperta volume para baixo até o fim e a voz do vendedor continua audível. Do lado do vendedor o áudio ficou bom depois da correção do `CallAudioController` (seção 5.4 de `docs/13-espelho-e-desenho.md`). Decisão do dia: manter como está e implementar depois.

**A causa, medida no Moto G22 fazendo o papel de cliente pelo Chrome.** Quando a página abre o microfone, o Chrome coloca o Android em modo de chamada (`setMode(MODE_IN_COMMUNICATION) from package=com.android.chrome` no `dumpsys audio`). Nesse modo as teclas de volume passam a controlar `STREAM_VOICE_CALL`, que vai de 1 a 8 e nunca chega a zero, em vez de `STREAM_MUSIC`, que vai de 0 a 15. No teste, duas teclas para baixo deixaram mídia em 14 e chamada em 1, que já era o piso. Isso é comportamento do sistema: uma página web não consegue mudar qual fluxo a tecla controla nem baixar o piso.

**O que construir.** Um controle de volume dentro da página, na barra da chamada do cliente, multiplicando o áudio do vendedor de 0 a 100%.

- Onde: `sfa_front/src/components/features/live/LiveCallBar.vue` para o controle, `src/live/session.ts` para aplicar (hoje o áudio remoto vai para o `<audio id="trovata-live-remote-audio">` criado em `ensureAudioSink`), e `LiveTransport` ganha algo como `setRemoteVolume(value)`. No transporte embutido do vendedor (`bridge.ts`) é um no-op, porque lá o áudio é do app.
- Android com Chrome: basta `audioSink.volume = valor`.
- iPhone com Safari: `HTMLMediaElement.volume` é somente leitura. O caminho é WebAudio (`createMediaStreamSource` do stream remoto, `GainNode`, `destination`). No Chrome existe a pegadinha conhecida de que o stream remoto de WebRTC só alimenta o WebAudio se também estiver ligado a um elemento `<audio>` com `muted`. Não há iPhone neste ambiente, então esse lado precisa ser verificado por quem tiver um.
- Vocabulário: "Volume do vendedor". Não usar ícone sozinho sem `aria-label`.

**Como validar.** No celular do cliente, com a chamada viva, levar o controle a zero e confirmar silêncio; voltar a 100% e confirmar que o nível é o mesmo de antes. Conferir que as teclas do aparelho continuam funcionando por cima do controle. Branch a partir de `origin/staging` do `sfa_front`, sem tracking, como nas anteriores.
