# Fluxos do produto

> Mapa completo dos fluxos do TrovataCast: o que já foi prototipado (`prototype/index.html`), o que vem na próxima onda, e o que fica para depois.
>
> Cada fluxo é descrito em 4 partes: **objetivo · entrada · comportamento · saída**, mais notas técnicas relevantes quando aplicável.

---

## Convenções

- **V** = ponto de vista do **vendedor** (app nativo, iPhone/Android).
- **C** = ponto de vista do **cliente** (web no navegador do celular).
- **P2P** = trafega direto entre os dois aparelhos via WebRTC.
- **DC** = WebRTC Data Channel (estado do catálogo, ponteiro, carrinho).
- **SVR** = passa pelo backend (sinalização ou persistência).

---

# Onda 1 — já prototipado

## F1 · Sessões (V · Home)
- **Objetivo**: vendedor abre o app e tem visão imediata de quem está chamando agora, próximas sessões agendadas, e o que foi fechado hoje.
- **Entrada**: app abre em "Sessões" como aba default.
- **Comportamento**:
  - Card "Agora": cliente que já entrou no link e está esperando atendimento. Botão "Atender" inicia chamada.
  - Lista "Próximas": agendamentos com hora, tipo (reposição / primeira sessão / etc).
  - Lista "Encerradas hoje": pedidos fechados ou em revisão.
- **Saída**: tap em "Atender" → F4 (sessão ao vivo).

## F2 · Preparar sessão (V)
- **Objetivo**: montar a vitrine antes da chamada.
- **Entrada**: tap em "Iniciar nova sessão" + escolha do cliente.
- **Comportamento**:
  - Catálogo dividido em chips (Todos, Outono 26, Top venda, Pré-venda, Reposição).
  - Sugestões personalizadas baseadas no histórico do cliente.
  - Multi-seleção visual (check no canto).
  - Footer fixo mostra contagem ("5 produtos · 32 SKUs") + CTA "Gerar link".
- **Saída**: F3.
- **Notas técnicas**: o app monta um `SessionTemplate` local (não envia ao servidor ainda). Sugestões vêm de regras locais sobre histórico (SQLDelight).

## F3 · Convidar / criar link (V)
- **Objetivo**: gerar o link da sessão e mandar para o cliente.
- **Entrada**: vem de F2.
- **Comportamento**:
  - Card hero com o link (`trovata.cast/<token>`), copiar, WhatsApp, share sheet do SO.
  - Configurações: validade do link, visibilidade, tabela de preço, catálogo.
- **Saída**: tap em "Esperar Diego entrar" → estado de sala de espera (F4a, próxima onda).
- **Notas técnicas**:
  - App chama `POST /sessions` no servidor de sinalização (Ktor) → recebe `sessionId` + token.
  - URL é `https://trovata.cast/s/<token>` que abre o web app do cliente.

## F4 · Cliente entra pelo link (C)
- **Objetivo**: cliente abre o link e entra na sessão sem instalar nada.
- **Entrada**: cliente clica no link no WhatsApp.
- **Comportamento**:
  - Splash com avatar pulsante do vendedor, nome da empresa, descrição da sessão.
  - Card explicativo (TrovataCast, sem instalar, áudio + catálogo ao vivo).
  - CTAs: "Entrar com áudio" (default) ou "Só assistir, sem falar".
  - Permissão de microfone solicitada após tap.
- **Saída**: F5.
- **Notas técnicas**:
  - Web app vanilla, carrega via CDN. `getUserMedia({ audio: true })`.
  - Conecta WebSocket de sinalização com o `token` da URL.

## F5 · Sessão ao vivo · co-presença (V + C)
- **Objetivo**: o coração do produto. Vendedor e cliente vendo o mesmo catálogo, com áudio + ponteiro compartilhado + carrinho sincronizado.
- **Comportamento V**:
  - Header com avatar+nome do cliente, indicador "Ao vivo · 04:18".
  - Catálogo em grid (mesmo que o cliente vê).
  - Card de evento toast quando cliente adiciona ao carrinho.
  - PiP do cliente no canto inferior direito.
  - Barra de ações: mic, vídeo, **ponteiro** (modo apontar), camadas (ver carrinho), hang up.
- **Comportamento C**:
  - URL bar do Chrome no topo (lembrar: é web).
  - Header com PiP do vendedor, mic, hang up.
  - Banner azul "Camila está te mostrando: Tricot & camisaria".
  - Catálogo idêntico ao do vendedor; ponteiro laranja da Camila aparece sobre o produto que ela está apontando.
  - Strip "Você reagiu" mostra produtos que ele já curtiu.
  - Carrinho dock na base.
- **Notas técnicas** (P2P):
  - Áudio: WebRTC `MediaStream` (Opus, 48kHz, banda baixa).
  - Estado: WebRTC Data Channel `state`, mensagens JSON tipadas:
    - `cursor` (x, y, productId)
    - `scroll` (productId em vista, offset)
    - `pointAt` (productId)
    - `cartAdd` / `cartUpdate` (sku, qty, sizes)
    - `reaction` (productId, "heart" | "back")
  - Throttle ponteiro/scroll a ~30Hz.

## F6 · Foco em um produto (V + C)
- **Objetivo**: navegação para uma página de detalhe, mantendo sincronização.
- **Comportamento V**:
  - Quando o cliente abre detalhe (tap num card), o vendedor é levado para a mesma tela.
  - Vê o que o cliente está fazendo: "Diego abriu esta página há 32 segundos. Está olhando a cor Verde-musgo e ampliou a foto 2× para ver o caimento da barra."
  - Pode "Sugerir oferta" ou "Fixar para Diego".
- **Comportamento C**:
  - Banner azul "Camila te trouxe a este produto".
  - Galeria de fotos com paginador.
  - Seletor de cor (pill).
  - Grade de tamanhos com stepper de quantidade.
  - Barra de "Adicionar" fixa na base.
- **Notas técnicas**:
  - Navegação é parte do estado `route` no DC. Quem dispara navega o outro automaticamente.
  - Comportamento do cliente (zoom, hover de cor, tempo na tela) é stream contínuo via DC.

## F7 · Encerramento — pedido pronto (V + C)
- **Objetivo**: pedido finalizado dentro da chamada, sem digitação posterior.
- **Comportamento V**:
  - Card de resumo: cliente, duração, pedido pronto.
  - Métricas: itens, tempo, total (vs média).
  - Lista do "Pedido fechado em chamada".
  - Bloco "Mostrados mas não pedidos" com motivos textuais — entrada para follow-up.
  - CTAs: PDF, "Enviar ao ERP".
- **Comportamento C**:
  - Check verde + "Pedido recebido · #TC-8842".
  - Lista de itens com qty.
  - Botões PDF + Compartilhar.
  - Card "O catálogo continua aberto" — link para F8.
- **Notas técnicas**:
  - Servidor persiste `Order` com snapshot completo dos preços e SKUs.
  - Resumo é gerado localmente a partir dos eventos da sessão (não precisa do servidor).

## F8 · Catálogo assíncrono pós-chamada (C)
- **Objetivo**: cliente continua sozinho após a call, com tudo preservado.
- **Comportamento**:
  - Header "Pós-chamada · você no comando".
  - Chips: Apresentados (4) / No carrinho (1) / Que eu reagi (2) / Coleção toda.
  - Cards com timestamp ("14:02") indicando quando o produto foi mostrado.
  - Card azul "Camila vai saber se você mexer no carrinho".
- **Notas técnicas**:
  - Cliente conectado por WebSocket. Mudanças no carrinho geram push notification ao vendedor.

## F9 · Pipeline + insights (V)
- **Objetivo**: TrovataCast como sistema de relacionamento, não só de chamadas.
- **Comportamento**:
  - Cards: mês em R$, sessões e taxa de conversão.
  - "Prontos para abordar" — sugestões com motivo em texto ("Costuma repor a cada 38 dias · 41 dias desde a última").
  - "O que mais ficou em foco" — produtos por tempo médio em foco × taxa de conversão.

---

# Onda 2 — próxima a construir (prioritários)

## F10 · Sala de espera (V + C)
- **Por quê**: gap óbvio do "ao vivo". Cliente abre o link, vendedor ainda não entrou. Hoje a UX morre aqui.
- **Comportamento V**: notificação push "Diego entrou na sala". Card "Aguardando você" no Home (já existe no protótipo, falta o outro lado).
- **Comportamento C**: tela de espera com música opcional, avatar do vendedor, "Camila vai entrar em instantes". Permite mandar áudio rápido enquanto espera.
- **Notas técnicas**: data channel já aberto antes do vendedor entrar (presença + chat).

## F11 · Reconexão / queda de rede (V + C)
- **Por quê**: a venda acontece em 4G ruim, na feira, no carro. Cair é o caso comum, não exceção.
- **Comportamento**:
  - Banner suave laranja "Reconectando..." mantendo o estado.
  - Última posição do catálogo preservada localmente.
  - Quando reconecta: snapshot do estado é trocado via DC.
- **Notas técnicas**:
  - WebRTC: ICE restart automático.
  - Estado do catálogo persistido em IndexedDB (cliente) e SQLDelight (vendedor).

## F12 · Qualidade de conexão + modo economia (V + C)
- **Comportamento**:
  - Indicador discreto no header (3 barras).
  - Quando ruim: oferece "Modo áudio só" + imagens em baixa.
- **Notas técnicas**: WebRTC `getStats()` → bitrate, packet loss, RTT. Thresholds: <100kbps → modo economia.

## F13 · Oferta na hora (V → C)
- **Por quê**: única vantagem real sobre WhatsApp + PDF — desconto pontual no exato momento de hesitação.
- **Comportamento V**: tap em "Sugerir oferta" → bottom sheet com slider de desconto, validade ("só nesta call", "até hoje", "esta semana"), aplicar.
- **Comportamento C**: card destacado aparece com animação leve sobre o produto: "Camila te ofereceu: 20un por R$95 · até hoje".
- **Notas técnicas**: oferta vive no estado da sessão; vira `OrderLineDiscount` se aceita.

## F14 · Desenho no produto (V + C)
- **Por quê**: co-presença vira tátil. "Esse detalhe aqui" ganha forma.
- **Comportamento**: ambos podem rabiscar com o dedo sobre a foto do produto. Auto-fade depois de 2s. Cor por usuário.
- **Notas técnicas**: vetores enviados pelo DC (pontos + timestamps). Renderização canvas. ~50Hz.

## F15 · Editor de catálogo (V)
- **Por quê**: app só funciona se o vendedor consegue montar a operação sozinho. Sem isso, depende de TI.
- **Comportamento**: lista de produtos com edit. Trocar foto (camera/galeria), ajustar MOQ, tags, ordem. "Coleção do dia" como agrupamento ad-hoc.
- **Notas técnicas**:
  - Importação por planilha CSV/XLSX no onboarding.
  - Foto: upload assíncrono para storage; placeholder enquanto sobe.

## F16 · Onboarding do vendedor (V)
- **Comportamento**: 4 passos — empresa, importar catálogo (CSV ou começar do zero), tabela de preço, primeira sessão de teste com colega.
- **Por quê**: KMP-shared logic faz parser + mapper de CSV rodar igual em ambas plataformas.

## F17 · Cadastro do cliente / KYC leve (V)
- **Comportamento**: CNPJ + tabela de preço + limite de crédito + notas. Auto-complete de CNPJ via API pública (Receita).
- **Saída**: cliente cadastrado vira opção em F2.

## F18 · Pagamento / faturamento (V + C)
- **Comportamento**:
  - V: ao fechar pedido, escolhe forma (boleto, Pix, link).
  - C: recebe QR Code do Pix ou link no resumo + push de status.
- **Notas técnicas**: integração com PSP (Asaas, Iugu, ou Stripe BR). Backend gera cobrança.

## F19 · Status do pedido (V + C)
- **Comportamento**: separando → faturado → despachado → entregue. Push em cada mudança. Vendedor pode mudar manualmente ou via integração ERP.

---

# Onda 3 — médio prazo

## F20 · Vídeo curto do produto (V + C)
- Clip de 5-15s do modelo vestindo a peça, tocado em sync nos dois lados durante a apresentação.

## F21 · Notas privadas do vendedor (V)
- Anotações em produtos durante a call que só o vendedor vê. Vira contexto para o pipeline.

## F22 · Cliente chama um sócio (C → 3 pontas)
- Cliente compartilha o mesmo link com outra pessoa; sala passa a ter 3 ponteiros.
- **Notas técnicas**: WebRTC mesh para 3 pontas é viável. Acima disso, SFU.

## F23 · Highlights da call (V)
- Pós-chamada, vendedor marca 3 momentos relevantes (texto livre) que viram cards para a próxima abordagem.

## F24 · Drop assíncrono de coleção (V → C)
- Vendedor manda um "minicatálogo" sem chamada. Cliente abre quando quiser. Vendedor é notificado de cada interação.

## F25 · Handoff entre representantes
- Passar um cliente para outro vendedor preservando todo o histórico. Cobertura de férias / desligamento.

## F26 · Visão do gerente
- Escutar chamadas ao vivo (com consentimento), métricas por representante, coaching pós-call.

---

# Onda 4 — longo prazo

## F27 · Catálogo personalizado por cliente
- A partir do histórico, sessão abre com pré-filtro automático: "produtos relevantes para o Diego".

## F28 · Sugestão de momento de abordagem
- Baseado no ciclo de compra de cada cliente, app sugere o dia exato de chamar.

## F29 · Combinações que vendem
- "Quem leva tricot canelado costuma levar saia plissada" — sugestão durante a call.

---

# Eventos do Data Channel (resumo de referência)

```kotlin
sealed class SessionEvent {
    data class CursorMove(val userId: String, val x: Float, val y: Float, val productId: String?) : SessionEvent()
    data class Scroll(val userId: String, val productId: String, val offset: Float) : SessionEvent()
    data class PointAt(val userId: String, val productId: String) : SessionEvent()
    data class NavigateTo(val userId: String, val route: Route) : SessionEvent()
    data class CartUpdate(val sku: String, val qty: Int, val sizes: Map<String, Int>) : SessionEvent()
    data class Reaction(val userId: String, val productId: String, val kind: ReactionKind) : SessionEvent()
    data class OfferProposed(val sku: String, val newPrice: Money, val validUntil: Instant) : SessionEvent()
    data class DrawStroke(val userId: String, val productId: String, val points: List<Point>, val color: String) : SessionEvent()
    data class Presence(val userId: String, val state: PresenceState) : SessionEvent()
    data class StateSnapshot(val state: SessionState) : SessionEvent()  // após reconexão
}
```
