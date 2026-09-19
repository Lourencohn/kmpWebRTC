# PR no Catálogo Link: televenda ao vivo com vitrine embutida, desenho e quantidade sincronizada

> Descrição do pull request `feat/catalogo-link-televenda-desenho-ao-vivo` para a `main` do `sfa_front`. O texto abaixo da linha é o corpo do PR, pronto para colar. Escrito em 17/09/2026, depois da validação com o app no Moto G22 e um cliente em Chrome no desktop.

---

## O que este PR entrega

Este PR faz a vitrine pública do Catálogo Link funcionar como a tela compartilhada de uma televenda. O vendedor abre a mesma vitrine dentro do aplicativo TrovataCast, o cliente abre o link no navegador, e os dois passam a ver a mesma página: navegação, produto aberto, rolagem, carrinho e, agora, o contador de quantidade dentro do modal e um traço desenhado com o dedo por cima da vitrine.

Ele reúne duas entregas. A primeira é a televenda ao vivo em si, que já tinha sido revisada no PR #11 e mesclada na `staging` antiga, mas que a `staging` atual e a `main` não contêm. A segunda é o que veio depois: a vitrine embutida no app pela ponte `TrovataLive`, o desenho compartilhado, o rascunho de quantidade no modal e as correções encontradas na chamada real. Como a primeira entrega não está na `main`, ela vem junto aqui, sem alteração além das que o texto descreve.

Nada muda para quem abre o catálogo sem o parâmetro `live` na URL. Todo o código novo fica inerte nesse caso.

## Como funciona, em uma passada

A página lê `?live=<token>` e entra em modo de sessão. Sem `embed=seller`, ela é o lado do cliente: consulta o servidor de sinalização, pede o microfone, abre uma conexão WebRTC direta com o aplicativo do vendedor e usa um canal de dados para trocar mensagens pequenas. Com `?live=<token>&embed=seller` e um objeto `window.TrovataLive` presente, ela é o lado do vendedor dentro do aplicativo: não abre WebSocket nem WebRTC, e conversa com o app por três nomes de JavaScript. O app repassa cada mensagem para o cliente pelo canal de dados e entrega as do cliente de volta à página.

Tudo o que os dois lados trocam é uma mensagem com `type`, `ts` e `from`, definida em `src/live/protocol.ts` e espelhada em Kotlin no repositório do app. Os tipos são `mute`, `navigate`, `scroll`, `pointAt`, `cartInvalidated`, `orderPlaced`, `draw`, `drawClear` e `quantityDraft`.

## Arquivo por arquivo

### A camada isolada: `src/live/`

`protocol.ts` define os tipos de mensagem e um parser defensivo, `decodeDataChannelMessage`, que descarta qualquer payload fora do formato em vez de confiar no que chega pelo canal. Também define as âncoras: um ponteiro ou um traço nunca viaja em pixels, viaja como um alvo (`produto:123`, `produto:123:modal`, `imagem:<src>`, ou `viewport` como último recurso) mais uma proporção dentro do retângulo desse alvo. As duas telas têm tamanhos e colunas diferentes, e só a proporção dentro de um elemento sobrevive a isso.

`transport.ts` é a interface que o resto da página usa para falar com a sessão sem saber se por baixo há WebRTC ou o aplicativo. `session.ts`, `peer.ts`, `signaling-client.ts` e `signaling-messages.ts` implementam o transporte WebRTC do cliente. `bridge.ts` implementa o transporte do vendedor embutido: envia por `window.TrovataLive.postMessage`, recebe por `window.__trovataLiveReceive`, lê o estado da conexão por `window.__trovataLiveStatus`, e expõe `window.__trovataLiveSetDrawing` e `window.__trovataLiveClearDrawing` para o app ligar o modo de desenho e apagar. `config.ts` guarda o endereço do servidor de sinalização (`VITE_LIVE_SIGNALING_BASE`, com padrão para o hospedado) e os nomes dos parâmetros de URL.

`routes.ts` converte a rota atual do Vue Router em um `ViewState` neutro e vice-versa, preservando `live` e `embed` na query, e só sincroniza os parâmetros de filtro relevantes (`SYNCED_QUERY_KEYS`). `focus.ts` guarda o produto em foco remoto e o produto que a página abriu localmente, para o modal abrir e fechar nos dois lados sem eco. `cart-events.ts` é o ponto onde qualquer mutation de carrinho avisa a sessão, com o motivo da mudança.

`drawing.ts` é o estado dos traços: cada traço tem um id, um alvo, uma cor e uma lista de pontos em proporção. `dom-anchors.ts` resolve o alvo no DOM nas duas pontas: no início do traço, descobre o que está sob o dedo (imagem dentro do modal, o próprio modal com a rolagem interna descontada, o card do produto, o card mais próximo, ou a janela); ao renderizar, procura o mesmo elemento naquele momento e reprojeta a cada quadro, então rolagem e redimensionamento não deslocam o traço. `quantity-draft.ts` é o estado do rascunho de quantidade que viaja entre os modais.

### O composable: `src/composables/use-live-session.ts`

É o cérebro da sessão, usado uma vez pelo `PublicLayout`. Decide o papel pela URL, cria o transporte certo, faz o login público do carrinho com o e-mail do cliente da sessão quando a página entra embutida, e traduz cada mensagem recebida em ação na página: navegar, destacar um produto, abrir ou fechar o modal, rolar até um card, invalidar as consultas do carrinho, aplicar um traço, apagar, aplicar um rascunho de quantidade.

No outro sentido, ele observa a página e publica: mudança de rota, produto aberto ou fechado localmente, rolagem (só no papel de vendedor, com o card centralizado e sem eco de uma rolagem recebida), toda mudança de carrinho com o motivo, cada lote de pontos do traço e cada mudança no contador do modal.

Dois detalhes de robustez que a chamada real exigiu: o modo embutido é pegajoso, porque o parâmetro `embed` cai da URL na primeira navegação interna; e enquanto o login do carrinho da sessão está pendente, o diálogo de login público não abre, e quando o login chega ele fecha. Sem isso a página do vendedor abria "Fazer Login" sozinha dentro do app.

### Componentes de sessão: `src/components/features/live/`

`LiveCallBar.vue` é a barra do cliente: entrar na chamada, estado, microfone, sair. Ela some no modo embutido, porque quem desenha os controles é o app. `LiveDrawLayer.vue` é um canvas fixo por cima da vitrine que renderiza os traços dos dois lados e captura o dedo quando o modo de desenho está ligado. Três decisões nele: o canvas tem `h-full w-full` explícitos, porque sem tamanho em CSS um canvas assume os atributos em pixels físicos e o traço aparece deslocado em telas com `devicePixelRatio` maior que 1; ele força `pointer-events: auto` quando ativo, porque o Radix põe `pointer-events: none` no `body` com um modal aberto; e ele interrompe a propagação do `pointerdown`, porque senão o `DismissableLayer` do Radix fecha o modal ao desenhar sobre ele. Os pontos são amostrados a cada 2 px e enviados em lotes a cada 50 ms.

### As alterações em arquivos existentes

`src/layouts/PublicLayout.vue` monta a barra e a camada de desenho, provê `openLoginDialog` com a trava do login pendente e passa a reavaliar a sessão de login quando o login da chamada chega.

`src/views/public/SecaoPage.vue` abre o modal do produto quando o foco remoto muda e informa o foco local ao abrir e fechar, para o modal acompanhar nos dois lados.

`src/components/features/card-product/CardProductComponent.vue` ganha `data-produto-pre-id` no card, que é a âncora de tudo: destaque, rolagem, ponteiro e traço.

`src/assets/index.css` traz o destaque pulsante do produto apontado.

`src/components/features/adicao-produto/AdicaoProdutoDialog.vue` passa a publicar cada mudança do contador de quantidade como `quantityDraft` e a aplicar o rascunho recebido para o mesmo produto, com um snapshot por diálogo que impede eco. A chave da grade é `complemento1_complemento2_complemento3`, derivada dos dados do produto, então é idêntica nos dois lados. O aviso de carrinho que esse arquivo fazia diretamente saiu daqui e foi para a camada de mutations, abaixo.

`src/queries/public/use-carrinho-itens.ts` e `src/queries/public/use-carrinho.ts` avisam a sessão no `onSuccess` de cada mutation pública: adicionar (grade e agrupamento), mudar quantidade, remover um ou vários itens, trocar prazo e finalizar, cada um com o motivo. Antes só a adição pela grade avisava, e mudar quantidade ou remover pela página do carrinho deixava o outro lado desatualizado.

## O sincronismo do carrinho

Os dois lados gravam no mesmo carrinho pelas mesmas rotas públicas. O canal não carrega dados de carrinho, só o aviso de que algo mudou e o motivo. Quem recebe invalida as consultas de carrinho, itens, último carrinho, prazos, vitrine, favoritos, destaques e grades, e busca de novo. Medido em staging: de 2,6 a 8,8 segundos para o outro lado refletir, e quase tudo isso é o tempo da própria API (as rotas de carrinho respondem entre 0,8 e 2,3 segundos cada); o canal soma entre 1,4 e 2,6 segundos.

O contador dentro do modal é diferente: é um rascunho da tela, não uma gravação. Ele viaja por `quantityDraft` a cada toque e o outro lado aplica direto. Medido: entre 43 e 143 ms nas duas direções. O lançamento no carrinho continua acontecendo só no "Adicionar".

## Como testar

1. Subir a vitrine com `npm run dev`. Em desenvolvimento, a API de staging não responde CORS para `localhost`, então use um proxy do Vite para `/api` e `VITE_API_BASE_URL` apontando para ele.
2. Abrir `catalogo-link-view/<slug>/<uuid>?live=<token>` num navegador: a barra "Entrar na chamada" aparece. Sem o parâmetro, nada aparece.
3. Com o app do vendedor na mesma sessão, entrar na chamada. Navegar, abrir um produto, rolar, mudar o contador do modal, adicionar, remover pela página do carrinho, e desenhar com o modo de desenho do app ligado. Cada ação reflete no outro lado.
4. O modo embutido só faz sentido dentro do app, que injeta `window.TrovataLive`. Fora dele, `embed=seller` é ignorado.

## Pontos em aberto

O modal só abre remotamente na página de seção; início e destaques têm modal próprio e não foram ligados. Não há reconciliação após queda de conexão: os dois continuam gravando no mesmo carrinho, mas param de se avisar até reconectar. O cliente não tem como apagar um desenho do vendedor; o traço some quando o vendedor apaga ou muda de rota. Sem servidor TURN, a chamada não conecta em NAT simétrico em rede móvel. O repositório não tem suíte de testes; a validação foi `vue-tsc`, eslint e a chamada de ponta a ponta com o aplicativo.
