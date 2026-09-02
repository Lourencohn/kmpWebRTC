# Próximas telas: o caminho de televenda

> Documento de rota para a fase seguinte do TrovataCast, escrito depois da release de demonstração de 02/09/2026.
> Serve para dois públicos: para decidir internamente o que construir primeiro, e para levar ao time do SFA as três coisas que dependem deles.
> Cada ideia aqui diz de qual endpoint ela vive. O que não tem endpoint está no bloco D, separado de propósito.

---

## 1. Onde o app está depois desta release

O app parou de ser um protótipo com dado de exemplo e passou a ser três abas que só mostram o que existe de verdade:

| Aba | De onde vem o dado |
|---|---|
| Sessões | `SessionsRepository` local, mais os pedidos fechados do dia |
| Catálogos | `GET empresa/{slug}/catalogos-links`, os catálogos link reais do vendedor |
| Clientes | `ClientsRepository`, sincronizado do `api-int` |

Dentro da chamada, a vitrine e a grade vêm das rotas públicas do catálogo link, e o carrinho é o `CatalogoCarrinho` do próprio catálogo. O app não mantém carrinho paralelo nem pedido próprio.

O que ficou de fora dessa release não foi cortado por escopo, foi cortado por não ter fonte: painéis de receita que liam de um repositório que não recebe mais nada, e um navegador de catálogo local que não participa da televenda.

## 2. O critério que ordena a fila

Três regras decidem o que entra primeiro, e todas já estão no `CLAUDE.md`.

**Co-presença é o produto.** Entre uma funcionalidade que melhora o momento em que os dois estão juntos na tela e uma que melhora o catálogo em geral, a primeira ganha.

**O app não faz a venda.** O motor comercial é o Catálogo Link. O app lista, compartilha, televende e devolve. Toda ideia que empurre regra de preço, desconto ou fechamento para dentro do app está indo na direção errada.

**Nenhuma tela sem dado real.** Foi o critério desta release e continua valendo. Uma tela bonita que mostra zero em produção custa mais credibilidade do que a ausência dela.

A isso soma-se um critério prático: o app é usado na rua, com uma mão e com rede ruim. Funcionalidade que só faz sentido sentado no escritório pertence ao front web, não aqui.

---

## 3. Bloco A: telas novas que já têm dado pronto

São as quatro que não dependem de ninguém. O backend já responde tudo que elas precisam.

### 3.1 Carrinhos abertos

É a tela que falta, e a que mais muda o que o app significa.

Hoje o app só serve depois que o vendedor já decidiu ligar para alguém. Ele não ajuda a decidir para quem ligar. Só que o dado que responde isso já existe e está parado: cliente que entrou no catálogo, montou meio carrinho e sumiu.

`GET empresa/{empresa_slug}/carrinhos` devolve exatamente isso. O `ListCatalogoCarrinhoUseCase` chama `searchByVisibilidade` passando `usuario_id` e `roles`, ou seja, o escopo por vendedor já acontece no servidor: o app manda o Bearer do vendedor e recebe os carrinhos dele, sem precisar filtrar do lado de cá.

O `CarrinhoListagemGeralResource` traz nome do cliente, e-mail, `cliente_id`, `catalogo_link_id` e o objeto do catálogo link, situação, contagem de itens, quantidade total, valor total, descontos, `created_at` e `updated_at`. Os filtros aceitos são `situacao`, `vazio`, `search`, `start_date`, `end_date`, `sort` e `direction`, e existe `GET empresa/{empresa_slug}/carrinhos/total` para o contador.

Com isso a tela se desenha sozinha: uma lista ordenada por carrinho parado há mais tempo, cada linha dizendo "Loja Bella, 12 itens, R$ 4.200, sem mexer há 3 dias", e um botão **Chamar ao vivo** que cria a sessão já naquele catálogo link e com aquele e-mail de cliente. Todo o caminho de convite que existe hoje é reaproveitado; muda só o ponto de partida.

O ganho não é a tela, é o motivo de abrir o app. Ele deixa de ser ferramenta de quem já sabe o que quer e vira a fila de trabalho do dia.

### 3.2 Ficha do catálogo link, com o relatório de visualização

A aba Catálogos hoje lista e convida. Falta o passo do meio: abrir um catálogo e entender o que está acontecendo nele antes de ligar.

`GET empresa/{empresa_slug}/catalogos-links/{catalogoLinkId}/relatorio-visualizacao` devolve três blocos, `itens`, `visualizacoes` e `adicionados`, cruzados por produto e por taxonomia (grupo de produtos, categoria, coleção, família comercial, linha, marca, nicho, gênero). Existe também `GET empresa/{empresa_slug}/catalogos-links/total-visualizacoes` para o número agregado.

Traduzido para a tela: ele abriu três vezes, olhou dezoito peças, voltou duas vezes em jeans, adicionou duas e nunca chegou no carrinho. Isso é pauta pronta antes da ligação e roteiro de ordem durante a ligação. É informação que o vendedor hoje não tem em nenhum lugar no celular, e que muda a primeira frase da conversa.

Complementa a ficha o que já existe na listagem: validade, quantidade de carrinhos e de visitas.

### 3.3 Favoritos do cliente como pauta da conversa

`GET catalogos-links/{empresa_slug}/{catalogo_uuid}/clientes/{email}/produtos-favoritos` é rota pública, e o app já carrega o e-mail do cliente dentro da sessão desde a Fase 5.

Dentro da chamada, isso é uma aba ao lado da vitrine: "o que ele separou". Começar pelo que o cliente já escolheu sozinho encurta a conversa e aumenta a chance de fechar, e reforça a sensação de que o vendedor chegou preparado. Fora da chamada, entra na ficha do catálogo.

### 3.4 Resolver o catálogo sem sair do app

Hoje, quando a lista mostra "Expirado", o vendedor trava: precisa de um desktop para renovar. Isso quebra a premissa de que o app funciona na rua.

As rotas existem e são todas privadas, com o mesmo Bearer que o app já usa:

- `PUT empresa/{empresa_slug}/catalogos-links/{catalogoLinkId}` para editar, inclusive validade
- `PATCH empresa/{empresa_slug}/catalogos-links/{catalogoLinkId}/situacao` para ativar e inativar
- `POST empresa/{empresa_slug}/catalogos-links/{catalogoLinkId}/copy` para duplicar um catálogo para outro cliente
- `POST empresa/{empresa_slug}/catalogos-links` para criar do zero

A ordem de valor é essa mesma: renovar validade e duplicar resolvem o bloqueio do dia a dia e são de baixo risco. Criar um catálogo do zero pelo celular é um formulário grande e provavelmente pertence ao web; vale medir a vontade real antes de construir.

---

## 4. Bloco B: dentro da chamada

A co-presença hoje funciona, mas é de mão única e reimplementada.

### 4.1 Abrir a vitrine do catálogo link em WebView, pela ponte que já existe

Esta é a ideia mais estruturante da lista.

O PR do `sfa_front` já entregou o `src/live/bridge.ts` e a interface `LiveTransport` inertes, esperando o app. Quando a página é aberta com `?live=<token>&embed=seller`, ela não abre WebSocket nem WebRTC: conversa com o aplicativo hospedeiro por três nomes de JavaScript (`window.TrovataLive.postMessage`, `window.__trovataLiveReceive`, `window.__trovataLiveStatus`), o que funciona com `addJavascriptInterface` no Android e `WKScriptMessageHandler` no iOS.

Ligar isso significa que o vendedor passa a ver literalmente a mesma página que o cliente, com a mesma vitrine, o mesmo modal de produto, a mesma grade e as mesmas regras de preço. "Estamos vendo a mesma coisa" deixa de ser uma reimplementação em Compose que precisa ser mantida em paralelo e passa a ser verdade.

O ganho secundário é de manutenção, e é grande: hoje o painel de produto do app e o modal do cliente são duas telas com regra própria, e cada mudança de regra no Catálogo Link precisa ser reescrita aqui. Vale medir o custo real antes: WebView tem preço em desempenho e em controle de gesto, e a barra de chamada continuaria nativa.

### 4.2 Ver o que o cliente está olhando

A chamada hoje é de mão única: o vendedor comanda e o cliente acompanha. Mas o cliente navega sozinho o tempo todo, e o vendedor fica no escuro justamente no momento em que precisaria falar.

O protocolo já carrega `Navigate` nos dois sentidos e o composable do front já publica a navegação do próprio cliente. Falta o app exibir: uma faixa discreta dizendo "ele está em Vestidos" ou "ele voltou para o carrinho". Custo baixo, efeito grande na sensação de presença.

### 4.3 Comparar duas peças lado a lado

O momento clássico do atacado é "esse ou aquele". Hoje só dá para mostrar uma peça por vez, e a comparação acontece na memória do cliente. Uma tela de duas peças em paralelo, sincronizada nos dois lados, é uma funcionalidade pequena que resolve um momento concreto de venda.

### 4.4 Separar peça durante a conversa

Um gesto que marca a peça como candidata sem mexer no carrinho, visível nos dois lados, que no fim da chamada vira a lista do fechamento. Hoje o "depois eu vejo" se perde: ou entra no carrinho na hora, ou some. Pode ser implementado sobre os favoritos, que já são persistidos por e-mail de cliente, ou como estado efêmero da sessão.

### 4.5 Reconexão que reconcilia, e presença de reserva

A seção 4 do `docs/11-pr-catalogo-link-televenda.md` registra a limitação: se a conexão cai, os dois lados continuam gravando no mesmo carrinho e param de se avisar, e não há reconciliação automática ao voltar. Numa demonstração em 4G esse é o buraco mais visível. Uma faixa de "reconectando" que revalida tudo quando a conexão volta fecha o caso.

Como rede de segurança, o backend já tem sinal de presença próprio: `POST catalogos-links/{slug}/{uuid}/carrinhos/{carrinho_id}/definir-digitando` no lado público e `PATCH empresa/{slug}/catalogos-links/{id}/carrinhos/{id}/digitando` no privado. Dá para mostrar "ele está mexendo no carrinho" mesmo com o canal ponto a ponto fora do ar.

---

## 5. Bloco C: depois da chamada

### 5.1 O resumo que se escreve sozinho

Princípio 6 do `CLAUDE.md`: histórico se constrói sozinho. Duração da sessão, produtos mostrados, itens adicionados e por quem, valor final. Todos esses dados já passam pelo app durante a chamada e hoje se perdem quando a tela fecha. Não depende de nada novo no backend; depende de persistir o que já está em memória e de uma tela de leitura.

É também o que dá conteúdo à aba Sessões, que hoje lista sessões sem contar o que aconteceu em cada uma.

### 5.2 Mandar o resumo do carrinho pelo WhatsApp

`GET catalogos-links/{empresa_slug}/{catalogo_uuid}/carrinhos-resumo/{carrinho_id}/pdf` existe e cai em `gerarPdf`, que está implementado. Existe também `GET empresa/{empresa_slug}/carrinhos/{carrinho_uuid}/gerar-pdf` (`gerarPdfPublico`) e a rota de preview `GET share/catalogos-links/{empresa_slug}/{catalogo_uuid}/carrinhos-resumo/{carrinho_id}`.

O app já tem share nativo desde o M4. É o gesto que o vendedor faz hoje de qualquer jeito, só que manualmente e fora do sistema.

### 5.3 Fechamento do pedido: onde ele realmente mora

Aqui é preciso corrigir uma suposição, porque ela muda o desenho.

O app hoje encerra a chamada marcando o carrinho como pronto para envio (`definirProntoParaEnvio`), e isso está certo. A tentação natural seria dar o passo seguinte e gerar o pedido pelo app, já que existe a rota `POST empresa/{empresa_slug}/catalogos-links/{catalogoLinkId}/carrinhos/{carrinhoId}/gerarPedido`.

**Essa rota está quebrada.** Ela aponta para `CatalogoCarrinhoController::gerarPedido`, e esse método não existe no controller nem em lugar nenhum de `app/`. É o mesmo tipo de defeito já registrado em `docs/07-status.md` para `carrinhos-resumo/{carrinho_id}`, que aponta para `showPublic`, também inexistente.

E, mesmo que existisse, não seria um botão. No `sfa_front`, gerar pedido é uma página inteira (`/empresa/:slug/catalogo-link/:catalogoLinkId/carrinhos/:carrinhoId/gerar-pedido`, `GerarPedidoPage`) que monta um `pedido_sub` com prazo, tabela de preço, data de entrega mínima, descontos permitidos e finalização própria (`POST empresa/{slug}/pedidos-subs/{id}/finalizar`). Portar isso para o app contraria diretamente a decisão de que o app não faz a venda.

O desenho certo é o que já está no ar, mais uma ponte: o app fecha a chamada deixando o carrinho pronto para envio e oferece abrir a página de gerar pedido no navegador, autenticado, para quem for continuar dali. Vale confirmar com o time do SFA se a rota quebrada é resto de refatoração ou funcionalidade planejada e não terminada.

---

## 6. Bloco D: o que depende do time do SFA

São quatro itens, e os dois primeiros valem uma conversa dedicada.

**Aviso quando o cliente abre o catálogo.** É a funcionalidade de maior valor da lista inteira e a única que não tem como existir hoje: saber, no momento em que acontece, que o cliente está com o catálogo aberto, e ligar enquanto ele ainda está lá. A plataforma não tem broadcast (`BROADCAST_CONNECTION=log`, sem Reverb, Pusher ou Soketi), então isso exige ou um webhook, ou uma fila, ou aceitar que o app consulte o relatório de visualização em segundo plano e dispare notificação local, que é a alternativa barata e imprecisa.

**TURN.** Sem servidor TURN, uma parte relevante das sessões em 4G simplesmente não conecta. O código já aceita servidores ICE por variável de ambiente no signaling, então é decisão de infraestrutura, não de código. Está detalhado na seção 5 do `docs/11-pr-catalogo-link-televenda.md` e no bloco C do `docs/10-integracao-catalogo-link.md`.

**Sala com mais de dois.** Hoje a sessão é ponto a ponto entre vendedor e cliente. O caso real de atacado costuma ter o comprador chamando o sócio ou a compradora de outra loja. Isso muda a topologia do WebRTC e não é ajuste pequeno.

**As duas rotas quebradas.** `gerarPedido` e `carrinhos-resumo/{carrinho_id}` apontam para métodos que não existem. Não bloqueiam nada do que está no ar hoje, mas convém registrar antes que alguém construa em cima.

---

## 7. Ordem sugerida

| Ordem | O que | Por que primeiro | Depende de |
|---|---|---|---|
| 1 | Carrinhos abertos | Dá motivo diário para abrir o app e não depende de ninguém | `GET empresa/{slug}/carrinhos` |
| 2 | Ficha do catálogo com relatório de visualização | É a munição que falta antes de ligar | `relatorio-visualizacao` |
| 3 | Ver o que o cliente está olhando | Fecha a mão única da co-presença, custo baixo | Protocolo atual |
| 4 | Resumo da sessão | Dá conteúdo à aba Sessões, nada novo no backend | Nada |
| 5 | Renovar e duplicar catálogo | Tira o vendedor do bloqueio em campo | `PUT`, `PATCH situacao`, `copy` |
| 6 | Reconciliação na reconexão | Buraco mais visível numa demonstração em rede ruim | Nada |
| 7 | WebView pela ponte | Paga dívida em vez de criar, mas é o item de maior risco técnico | `bridge.ts`, já entregue |

Favoritos, comparar peças, separar peça e PDF por WhatsApp entram junto de qualquer um dos blocos acima conforme a conveniência; são pequenos e independentes.

---

## 8. O que este documento não prova

Todos os endpoints citados foram verificados lendo rotas, controllers, resources e use cases do `sfa_back` no estado atual do repositório. Nenhum deles foi chamado. O formato exato de resposta, a paginação e o comportamento com dado real precisam ser confirmados contra staging antes de virar código, do mesmo jeito que a Fase 1 revelou o prefixo `/api` e o 500 mascarado em catálogo expirado.

As duas rotas quebradas foram identificadas por ausência do método no controller e por busca no diretório `app/`. Vale confirmar com quem mantém o `sfa_back` antes de tratar como defeito.

Nada aqui foi validado com vendedor de verdade. A ordem sugerida é baseada no que o produto promete e no custo de construir, não em pesquisa com usuário.
