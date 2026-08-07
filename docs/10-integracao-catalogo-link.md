# Integração TrovataCast × Catálogo Link — pauta de reunião

> Documento de apoio para a conversa com o desenvolvedor do SFA.
> Objetivo: sair da reunião com decisões arquiteturais tomadas, donos definidos e riscos mapeados — não com "vamos pensar".

---

## 1. O que eu já sei da stack de vocês (para não gastar reunião com o óbvio)

Levantei isso lendo `sfa_back` e `sfa_front`. Confirmar rapidamente e seguir.

**Backend (`sfa_back`)**
- Laravel 12 / PHP 8.2, PostgreSQL **multi-tenant por schema** (`SetTenantSchema`, `SetTenantSchemaById`, `SchemaService::setSchema($empresaId)`).
- Autenticação interna via **Keycloak** (`KeycloakMiddleware`, `KeycloakIntegrationMiddleware`, `CheckRolesMiddleware`).
- Catálogo público em dois modelos convivendo:
  - **legado**: `validateJwtMiddleware` + `validateCatalogoLinkJwtMiddleware` (JWT carregando `catalogo_link_id` / `empresa_id`);
  - **novo**: `ValidateEmpresa` + `SetTenantSchema` + `ValidateUUIDMiddleware` sob `catalogos-links/{empresa_slug}/{catalogo_uuid}` e `share/...`.
- Carrinho é **cidadão de primeira classe no servidor**: `CatalogoCarrinho`, `CatalogoCarrinhoItem`, `CatalogoCarrinhoGradeItem`, com regras de desconto, tabela de preço (`CatalogoLinkTabelaPreco`), prazos, tipo de venda, reserva de estoque, `gerarPedido`, PDF (dompdf) e integração com o ROMA.
- Já existem sinais de tempo real latentes: `definirDigitando`, `definirProntoParaEnvio`, `NotifyVendedorDialog`, `relatorio-visualizacao`.
- Sem broadcast: `BROADCAST_CONNECTION=log`, sem Reverb/Pusher/Soketi. `REDIS_HOST=127.0.0.1`, `CACHE_STORE=database`, `QUEUE_CONNECTION=database`.
- Infra: **Kubernetes** (ECR `us-east-1`, nginx ingress, cert-manager/letsencrypt, HPA, queue worker, scheduler, Prometheus/alertmanager). `api.trovata.app.br` e `api-staging.trovata.app.br`.
- Existe um serviço **Go** (`GO_URL`) usado para sincronização de base.

**Frontend (`sfa_front`)**
- Vue 3 + Vite + TS, Tailwind + shadcn-vue/radix, TanStack Query, Vuex, vue-router, vue-i18n, `keycloak-js`.
- Catálogo público em `/catalogo-link-view/{empresa_slug}/{catalogo_uuid}` (`HomePage`, `CategoriaPage`, `CarrinhoPage`, `FavoritosPage`) e resumo em `/catalogo-link-resume/...`.
- **Dois caminhos de deploy coexistindo**: workflow de ECR + `k8s/ingress.yaml` (`trovata.app.br`) **e** `vercel.json` + `middleware.js` (edge middleware que faz prerender para crawlers batendo em `api.trovata.app.br`).
- Whitelabel por empresa via `CatalogoLinkCustomizado` (`catalogos-links-customizados`).

**TrovataCast (meu lado, hoje)**
- App do vendedor em KMP/Compose (iOS + Android), buyer web em TS vanilla, sinalização em Ktor.
- Sinalização hospedada no **Fly.io** (`trovatacast-signaling.fly.dev`, região `gru`, 1 máquina de 256MB, **salas em memória**), buyer no **Cloudflare Pages** (`trovatacast-buyer.pages.dev`).
- DataChannel já carrega: `Scroll`, `PointAt`, `Navigate`, `CartUpdate`, `Mute`.
- Carrinho e pedido hoje vivem em SQLDelight local + `POST /order` em memória. **É exatamente essa parte que morre na integração.**

---

## 2. A pergunta-mãe (define todo o resto)

> **O TrovataCast entra como uma _feature_ do Catálogo Link — "sessão ao vivo" em cima de um catálogo e um carrinho que já existem — ou como produto paralelo que só compartilha domínio e marca?**

Minha proposta é a primeira: o vendedor abre uma sessão ao vivo **sobre um `catalogo_link` existente**, e o carrinho da sessão **é** um `CatalogoCarrinho`. Nada de carrinho paralelo.

Se ele concordar com isso na primeira meia hora, o resto da pauta é execução. Se discordar, o resto muda.

---

## 3. Se só der tempo para cinco perguntas

1. **Quem é a fonte da verdade do carrinho durante a chamada?** (Minha aposta: o Laravel, sempre. O DataChannel carrega só o efêmero.)
2. **Existe hoje algum canal de tempo real na plataforma?** Não achei nenhum. Se não existe, quem decide o padrão do primeiro — e vocês aceitam um runtime novo (JVM/Go/Node) no cluster?
3. **O front de produção é o Vercel ou o cluster?** Os dois caminhos estão no repositório e isso decide onde o buyer ao vivo mora.
4. **Como o app mobile do vendedor autentica no Keycloak?** Existe client configurado para mobile (PKCE), ou preciso que criem?
5. **Quem banca o TURN?** Sem TURN, uma fatia relevante das sessões em 4G simplesmente não conecta. Não é opcional.

---

## 4. Bloco A — Carrinho, preço e pedido (a decisão arquitetural central)

O TrovataCast hoje trata o carrinho como estado P2P. O Catálogo Link trata como entidade transacional com regra de negócio pesada (desconto, grade, estoque, prazo, tabela de preço). **Não dá para ter os dois.** Preciso entender o custo de fazer o backend ser a fonte da verdade dentro de uma chamada ao vivo.

- Toda a regra de preço/desconto/grade vive no backend, ou existe alguma parte já replicada no cliente que eu possa reaproveitar?
- Qual a **latência típica (p50/p95)** de `POST catalogos-links/{slug}/{uuid}/carrinhos/itens` em produção? Dentro de uma chamada, acima de ~300ms o cliente percebe que "travou".
- Existe endpoint de **snapshot completo do carrinho** (itens + totais + descontos aplicados) para hidratar os dois lados ao entrar na sessão? Ou preciso montar de várias chamadas?
- **Concorrência**: vendedor e cliente mexendo no mesmo `carrinho_id` ao mesmo tempo é o caso normal aqui, não a exceção. Existe versionamento / optimistic locking / checagem de `updated_at`? O que acontece hoje se duas requisições alteram o mesmo item simultaneamente — last-write-wins silencioso?
- **Reserva de estoque** (vi `RESERVA_ESTOQUE.md` e `SISTEMA_RESERVA_ESTOQUE.md`): em que momento a reserva dispara? Se a sessão ao vivo reservar durante a chamada, qual o TTL e o que acontece se a chamada cair?
- `gerarPedido` é **idempotente**? Numa sessão ao vivo, vendedor e cliente podem apertar "fechar" quase juntos.
- `updateOrCreateMultiple` / `itens-multiple` aguentam ser chamados em rajada (o vendedor montando o carrinho ao vivo enquanto conversa)? Tem rate limit?
- Um carrinho pode ter **dois atores identificados** (vendedor + cliente) ou o modelo assume um cliente só? Preciso registrar "quem adicionou o item" para o resumo da sessão.

**O que eu proponho como divisão:**

| Trafega pelo DataChannel P2P (efêmero, nunca persistido) | Passa pelo backend (fonte da verdade) |
|---|---|
| Posição do ponteiro, scroll sincronizado, foco de produto | Adicionar/remover item, quantidade, grade |
| Áudio e vídeo | Preço, desconto, prazo, tabela |
| "Estou olhando isto agora" | Estoque e reserva |
| Estados de UI (mute, sheet aberto) | Fechamento do pedido |

---

## 5. Bloco B — Identidade, tenant e autorização

- O modelo público novo (uuid na URL, sem JWT) é o definitivo? O legado com `validateCatalogoLinkJwtMiddleware` tem data para morrer? Não quero construir em cima do que vai ser desligado.
- **Vendedor no app mobile**: existe client Keycloak configurado para aplicação nativa (Authorization Code + PKCE)? Qual o tempo de vida do access/refresh token? O app precisa funcionar com rede ruim — dá para ter refresh longo?
- `VendedorController::getVendedorByCatalogoLink` sugere que um catálogo link tem vendedor dono. É 1:1? Como eu amarro "o vendedor X está atendendo ao vivo neste catálogo agora"?
- O comprador hoje entra só com o uuid do catálogo. Para a sessão ao vivo eu preciso de um **token de sessão curto** (a sessão dá acesso a áudio/vídeo do vendedor, não pode ser link eterno colável). Onde emitir esse token: endpoint novo no Laravel ou no serviço de sinalização?
- `clientes-liberados` e `CatalogoLinkClienteTemp`: a sessão ao vivo deve respeitar essa lista de liberação? Catálogo restrito vs aberto muda o fluxo de entrada.
- **Multi-tenant fora do Laravel**: `SetTenantSchema` resolve o schema por `empresa_id` a cada request. Um serviço de sinalização fora do Laravel não tem esse contexto. Qual o caminho aceito — o Laravel emite um token com `empresa_id`/schema embutido, ou o serviço faz introspecção contra um endpoint de vocês?
- Existe risco de um `catalogo_uuid` vazar e alguém entrar numa sessão alheia? Como vocês tratam isso hoje no catálogo público?

---

## 6. Bloco C — Tempo real e sinalização (infra)

Este é o bloco em que eu mais preciso da opinião dele, porque envolve introduzir algo que a plataforma ainda não tem.

- Confirmar: **hoje não existe nenhum WebSocket/broadcast em produção**, certo? (`BROADCAST_CONNECTION=log`, sem Reverb/Pusher). Se sim, essa integração vai ser o primeiro canal de tempo real da plataforma — e a escolha vira padrão para todo mundo depois.
- Opções na mesa, quero saber a preferência **de quem opera o cluster**:
  - **Laravel Reverb** — fica no ecossistema PHP, vocês operam com o que já conhecem, mas é mais um deployment stateful e o Reverb não é trivial de escalar horizontalmente.
  - **Serviço Kotlin/Ktor separado** — é o que já tenho funcionando, mas adiciona runtime JVM ao cluster e alguém precisa mantê-lo.
  - **Serviço Go** — vocês já têm um serviço Go na stack (`GO_URL`). Ele poderia hospedar a sinalização? Quem é o dono dele?
  - **Cloudflare Durable Objects** — sala como objeto durável, zero infra no cluster, casa com TURN da Cloudflare. Vocês já usam Cloudflare em algum ponto do caminho?
- Se for para o cluster: minhas salas hoje são **em memória**. Com HPA e mais de uma réplica isso quebra. Existe **Redis gerenciado de verdade** disponível (ElastiCache?) ou o `REDIS_HOST=127.0.0.1` do env é sidecar/não usado? Qual o caminho aprovado para estado compartilhado entre réplicas?
- O nginx ingress está com `proxy-read-timeout: 300`. Para WebSocket de sessão longa isso derruba a conexão a cada 5 minutos. Vocês já têm algum ingress com anotação de upgrade/timeout estendido, ou seria o primeiro?
- **HPA + conexões WebSocket**: scale-down mata pods com sessões ao vivo dentro. Existe `terminationGracePeriodSeconds` generoso, PodDisruptionBudget, algum padrão de drenagem?
- Tem WAF, rate limit ou algum proxy (Cloudflare em modo proxy?) na frente de `trovata.app.br` / `api.trovata.app.br` que possa interferir em WebSocket de longa duração?
- Observabilidade: vocês têm Prometheus + alertmanager. Que métricas eu preciso expor para o serviço de sinalização ser aceito em produção (salas ativas, conexões, falha de handshake)?

---

## 7. Bloco D — Domínios e roteamento

Hoje o TrovataCast vive em `trovatacast-buyer.pages.dev` + `trovatacast-signaling.fly.dev`. Quero migrar para o domínio de vocês.

- **Qual é a verdade do front hoje: Vercel ou o cluster?** O repositório tem workflow de ECR + ingress para `trovata.app.br` e ao mesmo tempo `vercel.json` + `middleware.js` de edge. Isso decide se o buyer ao vivo é uma rota do `sfa_front` ou uma aplicação servida à parte.
- Se o buyer ao vivo virar rota do `sfa_front` (algo como `/catalogo-link-view/{slug}/{uuid}/ao-vivo`), o `middleware.js` precisa ignorar essa rota no prerender de crawlers. Alguma restrição para mexer nele?
- Subdomínio para sinalização: `live.trovata.app.br` ou `rtc.trovata.app.br`. **Quem controla o DNS** (vi arquivos de Route53) e qual o processo e o tempo para criar um registro novo?
- Certificado: cert-manager já resolve para HTTP/WS. **TURN é outra história** — precisa de portas UDP, e o nginx ingress não passa UDP. Existe NLB ou algum caminho já usado para expor UDP (3478 + faixa de mídia)?
- Ambientes: existe `api-staging.trovata.app.br` e scripts de restore de produção. Consigo um **tenant de teste em staging** com catálogo, produtos e um vendedor, para desenvolver sem tocar em produção?

---

## 8. Bloco E — TURN/STUN e mídia

O item que ninguém lembra até a demo falhar na frente do cliente.

- STUN sozinho não basta. Em rede móvel brasileira e Wi-Fi corporativo com NAT simétrico, uma fatia relevante das chamadas só conecta via **TURN** (relay). Preciso de um.
- Opções: **coturn** auto-hospedado (precisa IP público, UDP e banda saindo do cluster), **Cloudflare Calls/TURN** (gerenciado, cobrado por GB) ou Twilio. Qual encaixa melhor no modelo de custo de vocês?
- Qual o volume esperado de sessões/dia por empresa? Isso define custo de relay e se vale hospedar.
- **Gravação**: existe alguma expectativa de gravar áudio/vídeo da chamada (compliance, treinamento, disputa de pedido)? Se sim, o modelo P2P puro não serve mais — entra servidor de mídia (SFU) e o custo e a complexidade multiplicam. Também entra LGPD, com consentimento explícito dos dois lados.
- Do lado de LGPD: hoje áudio e vídeo não passam pelo servidor, o que é uma vantagem forte de privacidade. Vocês têm alguma política ou parecer jurídico que eu precise respeitar aqui?

---

## 9. Bloco F — Frontend: reuso vs. reescrita

Meu buyer é TS vanilla. O de vocês é Vue 3 com um design system inteiro pronto.

- Faz mais sentido **portar a camada de co-presença para dentro do Vue** e reusar `CardProductComponent`, `CategoriaPage`, `CarrinhoPage` — ou renderizar uma view nova isolada? Minha inclinação é reusar os componentes de vocês e adicionar co-presença por cima; o catálogo tem que parecer o catálogo de sempre.
- `CatalogoLinkCustomizado` indica **whitelabel por empresa**. A UI ao vivo precisa respeitar a customização visual do cliente, ou pode ter identidade própria do TrovataCast?
- **Estado**: vocês usam TanStack Query + Vuex. Eventos remotos (scroll, ponteiro, carrinho alterado pelo outro lado) precisam entrar no cache do Query sem brigar com refetch/invalidação. Existe padrão de mutação otimista já usado no carrinho que eu deva seguir?
- **Bundle**: quanto pesa a página do catálogo hoje? WebRTC + co-presença em chunk lazy (carregado só ao entrar na sessão) é aceitável para vocês?
- Existe algum requisito de suporte a navegador antigo? WebRTC + `getUserMedia` exige HTTPS e navegador moderno — Safari iOS tem particularidades no autoplay de áudio.

---

## 10. Bloco G — App do vendedor

- **Os vendedores de campo usam o quê hoje?** O `sfa_front` no navegador do celular, um app nativo, um PWA? Se já existe algo instalado na mão deles, talvez o TrovataCast deva virar uma tela dentro daquilo, e não um segundo app.
- Se hoje é web no celular: eles topariam instalar um app nativo? Isso é uma barreira de adoção real e muda a estratégia.
- Existe processo de publicação nas lojas (contas Apple/Google da empresa, quem assina, quem sobe)?

---

## 11. Bloco H — Dados, eventos e migrations

- Já existem `CatalogoLinkVisualizacao`, `CatalogoLinkProdutoVisualizacao`, `CatalogoLinkItemVisualizacao` e `relatorio-visualizacao`. A sessão ao vivo gera eventos muito mais ricos (o que foi apontado, quanto tempo foi olhado, o que entrou no carrinho durante a conversa). **Reaproveito essas tabelas ou crio um conjunto novo de `sessao_*`?**
- Como vocês aplicam **migration em ambiente multi-schema**? Qual o custo e o risco de adicionar 2–3 tabelas novas para todos os tenants? (Vi `REGRAS_BANCO_DADOS.md` — vale ele me dar o resumo prático.)
- Convenções: nomes em português, `situacao` como char, uuid público + id interno. Confirmar para eu não destoar.
- Retenção: quanto tempo guardar o histórico de sessão? Tem política de retenção definida na plataforma?

---

## 12. Bloco I — Processo, ownership e roadmap

- **Onde o código mora**: módulo dentro dos repositórios de vocês, ou repositório separado consumindo a API pública? Se separado, como versionamos o contrato?
- Se eu precisar de 3 a 5 endpoints novos no Laravel, **qual o lead time realista** e quem escreve — eu abrindo PR no repositório de vocês, ou vocês implementando a partir de uma especificação minha?
- Quem revisa? Vi `commitlinterrc.json`, husky, PHPStan/Larastan, Pint. Alguma exigência de cobertura de teste para PR ser aceito?
- **O roadmap do Catálogo Link para os próximos dois trimestres colide ou combina com isso?** Se já existe algo planejado de notificação ou tempo real, é melhor construir junto do que em paralelo.
- Qual o apetite do time: isso é um experimento com um cliente-piloto, ou entra no produto para todos os tenants? A resposta muda quanto de infra vale construir agora.
- **Existe uma empresa-piloto candidata?** Uma sessão real com um cliente de verdade vale mais que três meses de arquitetura.

---

## 13. Três arquiteturas para colocar na mesa

Levar as três desenhadas ajuda a conversa a ser concreta em vez de abstrata.

**Opção A — Integrada ao cluster (minha recomendação)**
Buyer ao vivo como rota do `sfa_front`. Sinalização como serviço novo no cluster (Kotlin, Go ou Reverb — a decidir com quem opera), com Redis para estado das salas. Carrinho e pedido sempre no Laravel. DataChannel P2P só para o efêmero. TURN gerenciado.
Prós: uma URL, uma sessão, um carrinho, whitelabel de graça. Contras: introduz stateful no cluster e depende do time de infra.

**Opção B — Tudo no Laravel**
Reverb como canal único. Sem DataChannel de dados: só áudio/vídeo P2P, e todo o estado (ponteiro, scroll, carrinho) via WebSocket do Reverb.
Prós: um runtime só, time de vocês opera com o que conhece. Contras: ponteiro e scroll via servidor adicionam latência perceptível — é justamente o que faz a co-presença parecer mágica ou parecer quebrada.

**Opção C — Borda (Cloudflare)**
Sala como Durable Object, TURN da Cloudflare, Laravel só emite token de sessão e persiste o pedido.
Prós: zero infra stateful no cluster, escala sozinho, TURN no mesmo fornecedor. Contras: mais um fornecedor no desenho e um modelo de programação novo para o time.

---

## 14. O que precisa sair da reunião com dono e data

- [ ] Modelo escolhido: feature do Catálogo Link ou produto paralelo.
- [ ] Fonte da verdade do carrinho durante a sessão.
- [ ] Tecnologia e local da sinalização — e **quem opera**.
- [ ] Onde o buyer ao vivo é servido (Vercel ou cluster) e sob qual URL.
- [ ] Decisão sobre TURN (fornecedor e quem paga).
- [ ] Client Keycloak para o app mobile: existe ou precisa ser criado, e por quem.
- [ ] Acesso a staging com tenant de teste.
- [ ] Lista dos endpoints novos necessários, com dono e prazo.
- [ ] Empresa-piloto candidata.
