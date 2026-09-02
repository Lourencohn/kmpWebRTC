# PR no Catálogo Link: televenda ao vivo

> Documento para abrir o pull request no `sfa_front`.
> Branch: `feature/catalogo-link-televenda-ao-vivo`, criada a partir de `origin/staging`.
> Alvo do PR: `staging`.

---

## 1. O que este PR entrega

Hoje o Catálogo Link é uma vitrine que o cliente navega sozinho. O vendedor manda o link pelo WhatsApp, liga por fora, e vai descrevendo por voz o que o cliente deveria estar vendo. Os dois olham a mesma vitrine, mas cada um por sua conta: ninguém sabe em que produto o outro está, e o pedido só existe depois que alguém digita.

Este PR acrescenta uma camada opcional que liga essas duas pontas. Quando o link chega com `?live=<token>`, a mesma página passa a ter áudio com o vendedor e co-presença: o vendedor abre um produto e o modal abre na tela do cliente, o cliente adiciona uma quantidade e o vendedor vê na hora. O carrinho continua sendo o carrinho do Catálogo Link, gravado pelas rotas que já existem, com as mesmas regras de preço, estoque e múltiplo de venda. Nada de venda nova foi implementado aqui: a página só ganhou consciência de que existe alguém do outro lado.

Sem o parâmetro `?live=` na URL, absolutamente nada disso liga. É o ponto mais importante para a revisão e está detalhado na seção 6.

O aplicativo do vendedor é um projeto separado (TrovataCast, Kotlin Multiplatform) e conversa com esta camada por WebRTC. Ele não é alterado por este PR.

---

## 2. Como funciona, em uma passada

O vendedor cria a sessão no aplicativo. Um servidor de sinalização devolve um token curto e a URL do convite, que é a URL normal do catálogo link com `?live=<token>` no fim. O cliente abre esse link no navegador do celular, sem instalar nada.

A partir daí a página conversa direto com o aplicativo do vendedor, ponto a ponto, por WebRTC. O servidor de sinalização só serve para os dois se acharem: ele não vê áudio nem dados de catálogo. Por esse canal trafegam o áudio da conversa e mensagens pequenas de estado, como "abri o produto 4821" ou "o carrinho mudou".

Quando uma dessas mensagens de carrinho chega, a página não recebe dados de carrinho pelo canal. Ela apenas invalida as consultas do TanStack Query e busca de novo na API, exatamente como faria depois de uma ação do próprio usuário. O backend continua sendo a única fonte de verdade sobre o carrinho.

---

## 3. Arquivo por arquivo

### 3.1 A camada isolada: `src/live/`

Toda a mecânica nova mora nesta pasta, que não é importada por nenhuma tela existente. Quem a aciona é só o composable da seção 3.2.

**`protocol.ts`** define o contrato das mensagens que trafegam entre vendedor e cliente: navegação, rolagem, apontar, carrinho invalidado e pedido fechado. Também define o `CatalogRoute`, que foi desenhado em cima das rotas que este repositório já tem (`inicio`, `menu`, `todos`, `:type/:typeId`, `carrinho`, `favoritos`), de modo que a tradução entre os dois lados é direta, sem mapeamento inventado. As funções de âncora (`productAnchor`, `produtoPreIdOfAnchor`) padronizam como um produto é referenciado nas mensagens.

**`signaling-messages.ts`** e **`signaling-client.ts`** são o cliente WebSocket do servidor de sinalização: apresentação na sala, troca de oferta e resposta SDP, candidatos ICE e saída. O cliente enfileira mensagens enquanto o socket não abriu, para não perder a primeira troca.

**`peer.ts`** é a conexão WebRTC em si: cria a `RTCPeerConnection`, negocia, publica os candidatos, anexa o áudio local, expõe o canal de dados e trata mudo. Aceita servidores ICE vindos de fora, o que permite ligar um TURN depois sem tocar no código.

**`session.ts`** amarra os três acima em um transporte pronto para uso: pede o microfone, busca os servidores ICE do servidor de sinalização, cria o par, e devolve uma interface simples de enviar e receber mensagens.

**`transport.ts`** é a interface `LiveTransport`, que existe porque há dois jeitos de estar nesta página. O cliente entra pelo navegador e fala WebRTC. O vendedor, numa etapa seguinte do aplicativo, vai abrir esta mesma página dentro de uma WebView, e nesse caso quem já tem a conexão WebRTC é o aplicativo nativo: abrir uma segunda seria conflito. A interface permite que a lógica de tela seja a mesma nos dois casos e só o transporte mude.

**`bridge.ts`** é a implementação para esse segundo caso. Quando a página é aberta pelo aplicativo com `?live=<token>&embed=seller`, ela não abre WebSocket nem WebRTC: ela conversa com o aplicativo hospedeiro por uma ponte JavaScript. O contrato é mínimo de propósito, três nomes: o aplicativo injeta `window.TrovataLive.postMessage(json)` para receber o que a página envia, e chama `window.__trovataLiveReceive(json)` e `window.__trovataLiveStatus(status)` para entregar o que vem do cliente. Isso funciona tanto com `addJavascriptInterface` no Android quanto com `WKScriptMessageHandler` no iOS. Se a ponte não estiver presente, `hasNativeBridge()` devolve falso e o modo embutido nem é considerado.

**`config.ts`** concentra o nome dos parâmetros de URL, o endereço do servidor de sinalização e a leitura dos dados da sessão (nome do vendedor, nome e e-mail do cliente). O endereço é tratado na seção 5.

**`routes.ts`** traduz entre o `CatalogRoute` do protocolo e as rotas do Vue Router nos dois sentidos. Um detalhe que vale ler com atenção: ao montar a rota de destino, ele reinjeta o token `live` na query. Sem isso, a primeira navegação comandada pelo vendedor apagaria o token da URL e derrubaria a própria sessão.

**`focus.ts`** e **`cart-events.ts`** são dois estados compartilhados minúsculos. Servem para as telas conversarem com a camada live sem precisar receber propriedades atravessando a árvore de componentes. `focus.ts` diz qual produto o vendedor está mostrando; `cart-events.ts` avisa que o carrinho mudou por ação local. São dois `ref` e duas funções, e existem justamente para manter o diff nas telas existentes na casa de uma linha.

### 3.2 O composable: `src/composables/use-live-session.ts`

É o cérebro da camada e o único ponto que junta tudo. Ele lê o token da URL, decide entre o transporte WebRTC e a ponte nativa, cuida do ciclo de vida da sessão e traduz mensagem em ação de tela.

Ao receber uma navegação do vendedor, ele empurra a rota correspondente, marca o produto em foco e destaca o card. Ao receber um "apontar", só destaca. Ao receber um aviso de carrinho, invalida as consultas. No sentido contrário, publica a navegação do próprio cliente quando ele muda de tela, e publica a mudança de carrinho quando ele adiciona algo.

Duas decisões merecem explicação.

A primeira é a **entrada explícita**. A sessão não conecta sozinha ao carregar a página: ela abre em estado de convite e só conecta quando a pessoa toca em "Entrar na chamada". Isso não é preferência de desenho, é o que faz o áudio funcionar. Navegadores negam o microfone quando ele é pedido sem gesto do usuário, e a política de autoplay bloqueia o áudio que chega pelo mesmo motivo. Pedir os dois dentro do toque é o que torna a chamada confiável, e de quebra o cliente escolhe entrar em vez de ter o microfone requisitado de surpresa.

A segunda é o **login automático do carrinho**. Quando a página abre com `?live=`, o composable faz o login público com o e-mail já cadastrado no catálogo link, usando a mesma mutation `useLoginEmail` e gravando a sessão pelo mesmo `SessionLoginManager` que o `LoginPublicoDialog` usaria. Sem isso, o modal de produto abriria com o botão Adicionar sem carrinho onde gravar, e a televenda não sairia do lugar. Como é o mesmo e-mail que o aplicativo do vendedor usa para abrir o carrinho, os dois lados operam o mesmo registro. Se o catálogo link não tiver e-mail cadastrado, o login não acontece e a página segue como sempre foi, com o diálogo de login normal.

### 3.3 A barra de chamada: `src/components/features/live/LiveCallBar.vue`

Componente de apresentação, sem lógica de conexão. Mostra o estado da sessão, o nome do vendedor e as ações disponíveis em cada fase: entrar, tentar de novo, ativar microfone, silenciar e sair. Fica fixo no topo e só é renderizado quando há sessão ativa em modo cliente. No modo embutido ele não aparece, porque quem desenha os controles de chamada é o aplicativo.

### 3.4 As cinco alterações em arquivos existentes

São 54 linhas somadas e nenhuma removida.

**`src/layouts/PublicLayout.vue`** ganha a chamada do composable e a barra no template. Este é o ponto de montagem porque o layout envolve todas as telas da vitrine pública, o que permite que a sessão sobreviva à navegação entre elas. Se a camada fosse montada em uma página, ela morreria e reconectaria a cada troca de tela.

**`src/components/features/card-product/CardProductComponent.vue`** ganha um atributo `data-produto-pre-id` na raiz. É o que permite localizar um produto no DOM para rolar até ele e destacá-lo. Um atributo de dado, sem efeito visual ou comportamental.

**`src/assets/index.css`** ganha a classe do destaque e sua animação. Ficou no CSS global, e não no componente, para que a camada live possa aplicá-la a qualquer elemento marcado com o atributo acima sem depender de qual componente o renderizou.

**`src/views/public/SecaoPage.vue`** ganha um watcher que abre o modal de detalhes quando o vendedor mostra um produto, e o fecha quando o vendedor sai do produto. Reusa o `abrirDetalhesProduto` que já existia, então o modal aberto é exatamente o mesmo que o cliente abriria sozinho, com grade, cores, quantidade e o botão Adicionar.

**`src/components/features/adicao-produto/AdicaoProdutoDialog.vue`** avisa a camada live quando o carrinho muda, no `onSuccess` da adição, com o produto e a quantidade. É uma linha, mais o cálculo das unidades enviadas, e é o que faz o vendedor ver a alteração do cliente na hora.

---

## 4. O sincronismo do carrinho

Vale detalhar porque é o ponto mais delicado da revisão.

Os dois lados gravam no mesmo carrinho, pelas mesmas rotas do Catálogo Link. O canal ponto a ponto não carrega dados de carrinho, apenas o aviso de que algo mudou. Quem recebe o aviso vai buscar o estado novo na API.

Do lado do cliente, o aviso invalida o conjunto de consultas que o próprio login público já invalida (carrinho, itens, último carrinho, prazos, vitrine, favoritos e destaques), mais as consultas de grade pelo `invalidateGradeQueries` que este repositório já mantém. As grades entram porque, se o vendedor adicionar enquanto o cliente está com o modal aberto, é ali que as quantidades por tamanho precisam mudar. O contador da barra inferior acompanha porque ele vem do último carrinho, que está no conjunto.

Do lado do vendedor, o aplicativo recarrega o resumo do carrinho, a vitrine e a grade do produto em foco pelo mesmo motivo.

Uma limitação conhecida: isso trafega pelo canal ponto a ponto. Se a conexão cair, os dois continuam gravando no mesmo carrinho, mas param de se avisar, e cada tela fica com o que tinha até ali. Não há reconciliação automática no reconectar. É contornável revalidando tudo quando a conexão voltar, e ficou de fora deste PR de propósito, para não aumentar o escopo.

---

## 5. Configuração: `VITE_LIVE_SIGNALING_BASE`

A camada precisa saber onde fica o servidor de sinalização, que é quem entrega o token e faz vendedor e cliente se acharem. Isso está em `src/live/config.ts`:

```ts
const HOSTED_SIGNALING_BASE = 'https://trovatacast-signaling.fly.dev'

export function liveSignalingBase(): string {
  const configured = import.meta.env.VITE_LIVE_SIGNALING_BASE as string | undefined
  return configured?.trim() || HOSTED_SIGNALING_BASE
}
```

A variável é opcional. Sem ela, a camada usa o endereço acima, que é a instância atual do servidor de sinalização. Com ela, cada ambiente aponta para onde quiser, seguindo o mesmo padrão que este repositório já usa para API e Keycloak.

Para o ambiente de staging, basta acrescentar na Vercel:

```
VITE_LIVE_SIGNALING_BASE=https://trovatacast-signaling.fly.dev
```

Duas observações honestas para a revisão.

A primeira é que este repositório não versiona arquivo de exemplo de ambiente, então a variável está documentada aqui e não em um `.env.example`. Se preferirem, dá para criar o arquivo.

A segunda é mais importante e alguém vai perguntar: **o servidor de sinalização roda hoje no Fly.io, fora da infraestrutura da Trovata.** Para validar em staging isso é suficiente. Para produção, é uma decisão de infraestrutura que precisa ser tomada, e existem dois caminhos: hospedar o serviço junto do resto, ou manter fora e tratar como dependência externa. Vale registrar que ele não vê áudio nem conteúdo de catálogo, apenas os metadados de sessão e as mensagens de negociação WebRTC. Ainda assim, é uma dependência nova no caminho de uma funcionalidade de venda.

Um terceiro ponto de infraestrutura, para não haver surpresa: WebRTC entre redes diferentes costuma precisar de um servidor TURN quando os dois lados estão atrás de NAT restritivo, o que é comum em 4G. O código já aceita servidores ICE configuráveis, mas nenhum TURN está configurado hoje. Em rede boa a conexão fecha direto; em rede ruim ela pode não fechar.

---

## 6. Impacto sobre quem não usa a funcionalidade

Este é o ponto que a revisão deve verificar primeiro, e a resposta curta é: nenhum.

O composable é montado no layout público, mas a primeira coisa que ele faz é procurar o parâmetro `live` na URL. Sem ele, nenhuma conexão é aberta, nenhum login é feito, nenhuma consulta é invalidada, a barra não é renderizada e os watchers de foco e carrinho não têm o que publicar. O custo para um cliente comum é a leitura de um parâmetro de URL.

Nas telas existentes, o acréscimo é igualmente contido. O card de produto ganhou um atributo de dado. O CSS ganhou uma classe que só é aplicada por comando do vendedor. A página de seção ganhou um watcher que só reage a um estado que ninguém preenche fora de uma sessão ao vivo. O diálogo de adição ganhou uma notificação que, sem sessão ativa, não tem para quem ir.

Nenhuma linha foi removida de arquivo existente e nenhum comportamento atual foi alterado.

---

## 7. Como testar

O teste precisa de três peças: o aplicativo do vendedor, este front e o servidor de sinalização.

1. No aplicativo do vendedor, escolha um catálogo link **que tenha e-mail de cliente cadastrado** e gere o convite. Anote o token.
2. Entre na chamada pelo aplicativo antes de abrir a página do cliente.
3. Abra em outro dispositivo ou navegador: `/catalogo-link-view/{slug}/{uuid}?live={token}`. Use `localhost` ou HTTPS, porque o navegador só libera o microfone em contexto seguro.
4. Toque em "Entrar na chamada" e permita o microfone. Confirme que os dois lados se ouvem.
5. No aplicativo, abra um produto: o modal de detalhes deve abrir na tela do cliente. Feche no aplicativo: o modal deve fechar.
6. Adicione uma quantidade pelo modal do cliente: o carrinho do vendedor deve refletir sem recarregar.
7. Adicione pelo aplicativo: o contador e as quantidades do cliente devem refletir sem recarregar.

Verificações automáticas: `vue-tsc --noEmit -p tsconfig.app.json` e `eslint` passam limpos nos arquivos tocados. Vale notar que rodar `vue-tsc --noEmit` sem o `-p` não verifica nada neste repositório, porque o `tsconfig.json` da raiz usa project references com `files: []`.

Este repositório não tem suíte de testes configurada. A lógica de protocolo, sinalização e WebRTC portada para `src/live/` vem do cliente de referência do TrovataCast, onde é coberta por 50 testes automatizados. Duplicar essa cobertura aqui exigiria introduzir Vitest, o que aumentaria bastante o diff e ficou de fora.

---

## 8. Pontos em aberto

São coisas conhecidas que não entraram, para o revisor não precisar procurar.

**O modal só abre se o produto estiver na página que o cliente está vendo.** A mensagem carrega o identificador do produto, não a página dele. Se o vendedor mostrar um produto da página 3 e o cliente estiver na 1, o modal não abre. Resolver exige mandar a página junto e o cliente navegar até ela.

**A abertura de modal por comando remoto está implementada na página de seção**, que é onde o vendedor leva o cliente hoje. As telas de início e destaques têm modal próprio e não foram ligadas.

**Não há reconciliação após queda de conexão**, como descrito na seção 4.

**O modo embutido tem a ponte pronta, mas o aplicativo ainda não a usa.** Ele entra no PR agora, e não depois, porque a alternativa seria abrir um segundo pull request neste repositório mexendo nos mesmos arquivos. O código do modo embutido é inerte enquanto `window.TrovataLive` não existir.

---

## Sugestão de mensagem de commit

```
feat(catalogo-link): televenda ao vivo com áudio e co-presença
```
