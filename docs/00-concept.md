# TrovataCast — Documento Conceitual

> **Este é o documento-fonte do projeto.** Foi escrito antes do design e da arquitetura. Quando houver conflito entre os outros documentos e este, **este vence**.

## A origem do problema

Existe hoje uma lacuna enorme entre o jeito que o comércio B2B acontece na prática e as ferramentas que existem para apoiá-lo. Distribuidoras, representantes comerciais, fabricantes e lojistas de atacado realizam a maior parte das suas vendas através de ligações telefônicas, mensagens de voz e catálogos em PDF enviados por aplicativos de conversa. O vendedor liga, descreve o produto com palavras, o cliente tenta imaginar o que está sendo oferecido, pede para mandar foto, a foto chega sem contexto, e o pedido é digitado à mão depois que a ligação encerra. Esse ciclo se repete milhares de vezes por dia em todo o Brasil, e ninguém ainda construiu algo que resolva de verdade.

---

## O que é o TrovataCast

O TrovataCast é um aplicativo móvel criado para transformar o momento da venda em uma experiência compartilhada entre vendedor e cliente. A premissa central é simples: quando duas pessoas conseguem ver a mesma coisa ao mesmo tempo e interagir com ela juntas, a venda se torna mais rápida, mais clara e mais humana. O aplicativo cria um espaço de encontro digital onde o catálogo de produtos deixa de ser um arquivo enviado e passa a ser um ambiente navegado em conjunto, em tempo real, durante uma conversa ao vivo.

---

## O conceito de co-presença

O grande diferencial conceitual do TrovataCast não é o catálogo, nem a chamada de vídeo — é a co-presença. Co-presença é a sensação de que as duas pessoas estão no mesmo lugar olhando para a mesma coisa, mesmo estando em cidades diferentes. Quando o vendedor rola o catálogo, o cliente vê o mesmo movimento na sua tela. Quando o vendedor aponta para um detalhe de um produto, um indicador visual aparece na tela do cliente exatamente no mesmo ponto. Quando o cliente adiciona um item ao carrinho, o vendedor vê isso acontecer em tempo real.

Essa sincronia elimina a principal fricção da venda remota: a desconexão entre o que está sendo dito e o que está sendo visto. A comunicação se torna densa e objetiva porque os dois lados compartilham o mesmo referencial visual no mesmo instante.

---

## As premissas do produto

O TrovataCast foi concebido sobre quatro premissas inegociáveis que guiam cada decisão de produto.

A primeira é que o cliente não pode precisar instalar nada para participar de uma sessão. O vendedor envia um link, o cliente abre no navegador do celular e já está dentro. Qualquer barreira de instalação quebra o fluxo comercial antes mesmo de começar.

A segunda é que a comunicação deve ser direta, sem intermediários armazenando ou processando o conteúdo da conversa. O vídeo, o áudio e os dados de sincronização do catálogo trafegam diretamente entre os dois dispositivos. Isso não é só uma escolha técnica — é uma garantia de privacidade e de que a experiência não se degrada conforme o número de usuários cresce.

A terceira premissa é que o pedido deve nascer dentro da chamada, não depois dela. O ato de adicionar um produto, ajustar quantidade e confirmar deve acontecer enquanto os dois estão conversando, com o vendedor podendo ver e validar cada escolha em tempo real. O pedido confirmado no final da chamada já está completo, sem nenhuma digitação posterior.

A quarta premissa é que o histórico deve ser construído automaticamente. Cada sessão gera um registro estruturado: quais produtos foram apresentados, quanto tempo cada um ficou em foco, quais itens entraram e saíram do carrinho, e qual pedido foi fechado. Esse histórico não requer nenhuma ação manual de nenhum dos lados.

---

## A experiência do vendedor

Para o vendedor, o TrovataCast funciona como um estúdio de apresentação no bolso. Antes da chamada, ele organiza os produtos que quer mostrar e define a ordem da apresentação. Durante a chamada, ele navega pelo catálogo sabendo que o cliente está vendo exatamente a mesma tela. Ele pode destacar um produto, fazer o cliente focar em um detalhe específico, empurrar uma oferta especial que aparece em destaque na tela do outro lado.

O vendedor também enxerga o comportamento do cliente em tempo real: vê quando ele hesita em um produto, quando toca para ampliar uma foto, quando começa a digitar uma quantidade. Essa visibilidade transforma a dinâmica da negociação — o vendedor não precisa adivinhar o interesse, ele vê.

Ao final da chamada, o vendedor recebe um resumo automático da sessão com o pedido completo formatado, pronto para ser enviado ao sistema de gestão ou diretamente para o cliente como confirmação.

---

## A experiência do cliente

Para o cliente, a experiência é surpreendentemente simples. Ele recebe um link, abre, e de repente está em uma sessão onde o catálogo do fornecedor abre na sua tela enquanto o vendedor fala ao vivo. Ele pode navegar livremente, mas quando o vendedor quer mostrar algo específico, a tela se sincroniza automaticamente. Ele pode reagir a produtos silenciosamente enquanto a apresentação acontece, pode tocar em qualquer item para ver mais detalhes, e pode adicionar diretamente ao carrinho sem interromper a conversa.

No final, ele recebe o pedido confirmado no formato que preferir — um link para revisar, um PDF ou uma mensagem resumida. Não há formulário para preencher, não há e-mail para enviar. O pedido simplesmente existe, completo e correto.

---

## Diferenciais a médio prazo

À medida que o produto amadurece, surgem camadas de valor que vão além da transação individual.

O primeiro diferencial de médio prazo é a inteligência sobre o catálogo. Com o histórico de sessões acumulado, o sistema começa a revelar padrões: quais produtos mais vezes ficaram longamente em foco sem entrar no carrinho, quais combinações de produtos aparecem juntas nos pedidos, em qual momento da apresentação o cliente costuma perder interesse. Essas informações transformam o catálogo de uma lista estática em um instrumento de estratégia comercial.

O segundo diferencial é a personalização da sessão. Com base no histórico de compras de cada cliente, o vendedor pode abrir uma sessão com um catálogo pré-filtrado mostrando apenas os produtos relevantes para aquele perfil — sem precisar passar por itens fora do interesse do cliente. Cada apresentação se torna mais curta, mais precisa e mais eficaz.

O terceiro diferencial é o catálogo assíncrono pós-chamada. Após o encerramento da sessão, o cliente recebe um link do catálogo com os produtos apresentados já marcados e o carrinho preservado. Se ele quiser adicionar mais itens ou ajustar quantidades, pode fazer isso no seu próprio tempo, sem precisar remarcar uma chamada. O vendedor é notificado quando o cliente interage com esse catálogo e pode acompanhar o processo de decisão mesmo à distância.

---

## Diferenciais a longo prazo

No longo prazo, o TrovataCast deixa de ser apenas uma ferramenta de vendas e se torna a memória comercial da relação entre vendedor e cliente.

Cada sessão realizada constrói um histórico rico de intenções, preferências e padrões de compra que nenhum sistema de gestão tradicional captura. Esse histórico permite que o aplicativo sugira o momento certo de fazer uma nova abordagem — baseado no ciclo de compra de cada cliente, não em um calendário arbitrário de follow-up.

A longo prazo, o aplicativo também se torna uma plataforma para o vendedor gerir múltiplos clientes com inteligência: quem está no ciclo de compra, quem viu o catálogo mas não converteu, quem costuma comprar em determinada época do ano. Essa visão de pipeline, construída automaticamente a partir das sessões, é o que transforma o TrovataCast de um aplicativo de chamadas em um sistema de relacionamento comercial.

O produto final não é um catálogo com vídeo. É o registro vivo da relação comercial entre um vendedor e seus clientes, construído sessão a sessão, sem nenhum esforço manual de documentação.

---

## Por que mobile-first

A escolha pelo mobile como plataforma principal não é acidental — é estrutural. O vendedor está em movimento: no carro entre visitas, no corredor da feira, no escritório sem mesa fixa. O cliente está no depósito, na loja, no celular. A venda acontece nos interstícios do dia, não em frente a um computador. Um produto que exige desktop captura uma fração pequena do momento real da decisão comercial. Um produto que vive no bolso captura o momento exato em que a conversa acontece.

Além disso, o mobile impõe uma disciplina de design que beneficia o produto: cada tela precisa comunicar uma coisa com clareza, cada interação precisa ser intuitiva sem tutorial, cada fluxo precisa funcionar com uma mão enquanto a outra segura o produto ou dirige. Essa restrição força o produto a ser simples, e simplicidade é o que torna o TrovataCast adotável por vendedores que nunca usaram nada além do telefone e do WhatsApp.
