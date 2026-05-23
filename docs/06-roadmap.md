# Roadmap de construção

> Ordem sugerida para o Claude Code montar o produto.
> Cada milestone tem um critério de aceitação claro. **Não pule etapas.** Um milestone fraco prejudica todos os seguintes.

---

## Milestone 0 — Esqueleto e tooling

**Objetivo**: repositório compilando, pipeline rodando, app aberto em "Hello, Trovata".

- [ ] Setup do projeto KMP (Compose Multiplatform) seguindo `docs/04-architecture.md`.
- [ ] `gradle/libs.versions.toml` com todas as libs do `docs/03-stack-kmp-webrtc.md`.
- [ ] Theme.kt com tokens de `docs/02-design-system.md`.
- [ ] Fontes Geist + Geist Mono empacotadas.
- [ ] App Android abre com tela em branco e a wordmark TrovataCast.
- [ ] App iOS abre com a mesma tela (via `iosApp/`).
- [ ] Servidor Ktor mínimo: `GET /health → "ok"`.
- [ ] Web app vanilla TS: `index.html` com a mesma wordmark.
- [ ] CI: build Android + iOS + web + server em PR.

**Aceitação**: 4 superfícies abrem com a wordmark. CI verde.

---

## Milestone 1 — Componentes do design system

**Objetivo**: biblioteca de componentes Compose reproduzindo o protótipo.

- [ ] `Pill`, `Btn`, `Icon`, `Avatar`, `Card`, `SectionLabel`.
- [ ] `ProductCard` com todas as variantes (sm/md/lg, highlight, pointed, inCart).
- [ ] `ProductRow`.
- [ ] `VideoTile` com fallback gradiente.
- [ ] `RemotePointer`.
- [ ] `TabBar` iOS-style.
- [ ] Garments (silhuetas SVG) — substituir depois por fotos reais.
- [ ] Catalog de exemplo (hardcoded) idêntico ao do protótipo.
- [ ] Storybook-like: tela `DesignSystemPreview` que renderiza tudo.

**Aceitação**: side-by-side com o protótipo, nenhuma divergência visual perceptível em telas chave.

---

## Milestone 2 — Tela "Sessões" (Home do vendedor)

**Objetivo**: app abre, mostra a Home funcional.

- [ ] `SessionsScreen` com hardcoded data igual ao protótipo (`SellerHome`).
- [ ] Navegação básica (Voyager ou Decompose).
- [ ] Tap em "Atender" navega para `LiveSellerScreen` (placeholder).
- [ ] Tab bar funcionando (4 tabs, 3 são placeholders).

**Aceitação**: navegação manual entre as 4 abas. Home renderiza com 3 sessões agendadas + 2 encerradas.

---

## Milestone 3 — Preparar sessão + criar link

**Objetivo**: vendedor consegue montar uma sessão e gerar link.

- [ ] `PrepScreen` com seleção visual de produtos + chips de filtro.
- [ ] `InviteScreen` com link mock + botões de share.
- [ ] Servidor: `POST /sessions` retorna `{ sessionId, token, url }`.
- [ ] App chama API, recebe URL, mostra na tela.
- [ ] Persistência local da sessão criada (SQLDelight).

**Aceitação**: clicar em "Iniciar nova sessão" → escolhe cliente + produtos → recebe link → link é uma URL real do servidor de dev.

---

## Milestone 4 — Web buyer mínimo

**Objetivo**: cliente abre o link e vê uma tela de boas-vindas.

- [ ] `BuyerArrival` em TS + CSS.
- [ ] Recebe token via URL.
- [ ] `GET /sessions/<token>` valida o token → mostra dados do vendedor + sessão.
- [ ] CTA "Entrar com áudio" → pede permissão de mic (sem fazer nada útil ainda).

**Aceitação**: pegar o link gerado no M3, abrir no celular → vê a tela de chegada com nome real do vendedor.

---

## Milestone 5 — Sinalização + handshake WebRTC

**Objetivo**: dois aparelhos se conectam P2P, sem mídia ainda. Data channel aberto.

- [ ] WebSocket no servidor: `/ws/session/<token>` orquestra `offer/answer/ice`.
- [ ] `PeerConnection` no Android (actual).
- [ ] `PeerConnection` no iOS (actual). Configurar Podfile com `WebRTC.framework`.
- [ ] `PeerConnection` no web buyer (nativo).
- [ ] Vendedor cria offer → cliente responde answer → DC abre.
- [ ] DC envia `Presence` ping a cada 5s.

**Aceitação**: logs nos dois lados mostram "DC open"; ping/pong via DC funciona; ICE restart manual reconecta.

---

## Milestone 6 — Áudio P2P

- [ ] `getUserMedia` nos dois lados (mic).
- [ ] Trocar tracks de áudio.
- [ ] Vendedor pode mutar; cliente vê estado.

**Aceitação**: chamada de voz real entre dois celulares de testers.

---

## Milestone 7 — Co-presença básica

**Objetivo**: scroll e ponteiro sincronizados.

- [ ] `LiveSellerScreen` com catálogo real (do servidor) usando ProductCard.
- [ ] `BuyerLive` (web) com o mesmo catálogo.
- [ ] DC envia `Scroll` (lossy, ~30Hz) e `PointAt` (ordered).
- [ ] Lado remoto: animação suave de scroll e do RemotePointer.
- [ ] Botão "Apontar" no vendedor entra em modo: tap em produto → DC `PointAt` → cliente vê halo.

**Aceitação**: vendedor rola o catálogo → cliente rola junto. Vendedor aponta → cliente vê halo laranja.

---

## Milestone 8 — Carrinho ao vivo

- [ ] Cliente abre detalhe de produto → DC `Navigate` → vendedor navega.
- [ ] `BuyerProductDetail` com stepper de tamanhos.
- [ ] DC `CartUpdate` → vendedor vê toast "Diego adicionou 12un".
- [ ] Carrinho dock no cliente mostra subtotal.
- [ ] Vendedor pode abrir gaveta de carrinho.

**Aceitação**: pedido se constrói em tempo real visto pelos dois lados.

---

## Milestone 9 — Encerrar com pedido pronto

> Dividido em duas fases. Fase 1 (concluída) cobre o fluxo de confirmação P2P;
> fase 2 (pendente) cobre persistência, PDF e métricas. Detalhes em `docs/07-status.md`.

### Fase 1 — confirmação P2P (concluída)
- [x] DC `OrderConfirm(orderId, lines, totalCents)` com `OrderLine` (round-trip Kotlin + TS).
- [x] Botão **Confirmar pedido** na gaveta do vendedor.
- [x] `OrderSummaryOverlay` no vendedor + `mountOrderSummary` no cliente.
- [x] Tap "Fechar e encerrar" / "Fechar" → hangup + retorno pra `InviteScreen` / landing.

### Fase 2 — persistência + recibo (parcial)
- [x] `OrderEntity` + `OrderLineEntity` em SQLDelight (vendedor); `OrderRepository.persist(summary)`.
- [x] Seção "Pedidos fechados hoje" na `SellerHomeScreen` listando os Orders locais.
- [x] `POST /order` no signaling server (em memória, seguindo padrão `SessionStore`); cliente envia opcionalmente via fetch após receber `OrderConfirm`.
- [x] `BuyerConfirmation` (cliente) com botão "Salvar PDF" via `window.print()` + stylesheet `@media print`.
- [ ] `SummaryScreen` (vendedor) com métricas da sessão (tempo em foco por SKU, contagem de `PointAt`, duração total) — precisa de `SessionEventLog` no commonMain (movido para M12).
- [ ] Vendedor recebe push do pedido (movido para M19; depende de canal externo).

**Aceitação**: encerrar a chamada gera um Order persistido localmente (Fase 2 inicial) e no servidor (Fase 2 final); PDF baixável pelo cliente; tela de resumo do vendedor com métricas mínimas.

---

## Milestone 10 — Reconexão + qualidade

- [ ] ICE restart automático.
- [ ] Snapshot do estado via DC após reconexão.
- [ ] Banner "Reconectando..." UX cuidado.
- [ ] `getStats()` cada 5s; modo economia abaixo de 100kbps.

**Aceitação**: matar avião-mode 10s e voltar → sessão retoma sem perder estado.

---

## Milestone 11 — Catálogo assíncrono

- [ ] `BuyerAsync` (web) vive após a chamada acabar.
- [ ] Cliente edita carrinho → WS notifica vendedor.
- [ ] Vendedor recebe push e vê alteração no resumo.

**Aceitação**: cliente reabre o link após chamada → vê carrinho preservado, edita, vendedor recebe push.

---

## Milestone 12 — Pipeline + insights

- [ ] `PipelineScreen` com "Prontos para abordar" calculados a partir das sessões locais.
- [ ] Métricas globais (mês, sessões, conversão).
- [ ] "O que mais ficou em foco" agregado por SKU.

**Aceitação**: depois de 5+ sessões fake, pipeline aparece com sugestões coerentes.

---

## Onda 2 (depois do MVP funcionar end-to-end)

- M13 — Sala de espera.
- M14 — Oferta na hora.
- M15 — Desenho no produto.
- M16 — Editor de catálogo + onboarding com import CSV.
- M17 — Cadastro de cliente + KYC leve.
- M18 — Pagamento / faturamento (integrar Asaas ou Iugu).
- M19 — Status do pedido + push.

---

## Onda 3 (médio prazo)

- M20 — Vídeo curto do produto.
- M21 — Notas privadas do vendedor.
- M22 — Cliente chama um sócio (3 pontas).
- M23 — Highlights da call.
- M24 — Drop assíncrono.
- M25 — Handoff entre reps.
- M26 — Visão do gerente.

---

## Onda 4 (longo prazo)

- M27 — Catálogo personalizado por cliente.
- M28 — Sugestão de momento de abordagem.
- M29 — Combinações que vendem.

---

## Critérios de qualidade (aplicam a todos os milestones)

1. **Testes**: lógica de domínio + protocolo DC com cobertura ≥80%.
2. **Sem placeholders no `main`**: tudo que está em `main` funciona end-to-end.
3. **Comentário no commit explica a decisão**, não o que mudou.
4. **Cada milestone fecha com um PR** que pode ser revisado isoladamente.
5. **Performance**: ponteiro a 30Hz não pode causar jank visível em iPhone 12+.
6. **Acessibilidade básica**: contraste WCAG AA, alvos de toque ≥44dp.
