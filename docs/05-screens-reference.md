# Referência de telas

> Cada tela do protótipo HTML mapeada para sua contraparte Compose, com notas de implementação.
>
> A referência viva é `prototype/index.html` — abra para ver tudo lado a lado.

| # | Tela | Lado | Protótipo (arquivo / componente) | Compose alvo |
|---|---|---|---|---|
| 1 | Sessões (Home) | V | `frames-seller.jsx` · `SellerHome` | `ui/screens/sessions/SessionsScreen.kt` |
| 2 | Preparar sessão | V | `frames-seller.jsx` · `SellerPrep` | `ui/screens/prep/PrepScreen.kt` |
| 3 | Convidar / link | V | `frames-seller.jsx` · `SellerCreateLink` | `ui/screens/prep/InviteScreen.kt` |
| 4 | Cliente entra | C | `frames-buyer.jsx` · `BuyerArrival` | `webBuyer/src/ui/views/arrival.ts` |
| 5a | Live — vendedor | V | `frames-seller.jsx` · `SellerLive` | `ui/screens/live/LiveSellerScreen.kt` |
| 5b | Live — cliente | C | `frames-buyer.jsx` · `BuyerLive` | `webBuyer/src/ui/views/live.ts` |
| 6a | Detalhe — vendedor | V | `frames-seller.jsx` · `SellerProductDetail` | `ui/screens/live/ProductDetailSellerScreen.kt` |
| 6b | Detalhe — cliente | C | `frames-buyer.jsx` · `BuyerProductDetail` | `webBuyer/src/ui/views/product.ts` |
| 7a | Resumo — vendedor | V | `frames-seller.jsx` · `SellerSummary` | `ui/screens/summary/SummaryScreen.kt` |
| 7b | Confirmação — cliente | C | `frames-buyer.jsx` · `BuyerConfirmation` | `webBuyer/src/ui/views/confirm.ts` |
| 8 | Catálogo assíncrono | C | `frames-buyer.jsx` · `BuyerAsync` | `webBuyer/src/ui/views/async.ts` |
| 9 | Pipeline | V | `frames-seller.jsx` · `SellerPipeline` | `ui/screens/pipeline/PipelineScreen.kt` |

---

## Como traduzir cada tela

1. **Abra o protótipo**: `prototype/index.html` no navegador.
2. **Identifique o componente JSX** correspondente (tabela acima).
3. **Mapeie**:
   - Cada `<Card>` → `Surface` com `border + shape + shadow` do `theme/`.
   - Cada `<Pill>` → `Pill(tone = ...)` (componente em `ui/components/`).
   - Cada `<Btn>` → `Btn(kind = ...)`.
   - Cada `<Icon d={Icons.xxx}>` → `Icon(Icons.xxx)`.
   - Cada `<RemotePointer>` → `RemotePointer(userId)` composable observando `pointer` do `LiveSessionState`.
   - Cada `<VideoTile>` → `VideoTile(track)` recebendo `VideoTrack` da peer connection.
4. **Sem mocks no componente** — todo dado vem de `viewModel.state.collectAsState()`.

---

## Estados específicos a preservar

### Live (5a / 5b)
- O ponteiro do **outro** lado precisa aparecer com 30Hz updates suaves (animação `spring(stiffness = Spring.StiffnessMedium)`).
- O highlight da borda do produto quando vendedor "está apontando" pulsa (boxShadow expansion).
- Toast "Diego adicionou 12un" aparece com slide-up + auto-dismiss em 3s.

### Detalhe (6a / 6b)
- A galeria de fotos tem 4 dots; quando o cliente arrasta, o dot ativo se move — e o vendedor vê o mesmo movimento.
- Stepper de tamanho atualiza o carrinho via DC instantaneamente; lado do vendedor incrementa o badge.

### Resumo (7a)
- Bloco "Mostrados mas não pedidos" tem motivo textual quando houver — gerado a partir de eventos `Reaction("back")` ou observações longas sem `CartUpdate`.

### Pipeline (9)
- "Sugestões TC" não são hardcoded — vêm de regras locais sobre eventos das últimas N sessões (ciclo médio, tempo desde última, intent não convertido).

---

## Componentes compartilhados que aparecem em várias telas

| Componente | Onde | Notas |
|---|---|---|
| `Avatar` | Home, Prep, Live, Pipeline | Cor por `client.avatarHue` |
| `VideoTile` | Live, Detalhe, Async | Quando sem stream: gradiente fallback |
| `ProductCard` | Prep, Live, Detalhe, Async, Resumo | Variantes sm/md/lg; props `highlight`, `pointed`, `inCart` |
| `ProductRow` | Resumo, Confirmação | Versão compacta horizontal |
| `RemotePointer` | Live, Detalhe | Por usuário, hue do avatar |
| `ChromeBar` | Web (todas as telas do cliente) | Simula URL bar do Chrome mobile |
| `BuyerCallHeader` | Web (live, detalhe) | PiP do vendedor + mic + hangup |
| `TabBar` | Sessões, Pipeline | iOS — apenas no app do vendedor |
| `Pill` | Toda parte | Variantes `neutral`, `brand`, `jade`, `live`, `ghost`, `dark` |
| `Btn` | Toda parte | Variantes `primary`, `jade`, `soft`, `ghost`, `surface`, `dark`, `danger` |
| `Card` | Toda parte | Wrapper de superfície com border + shadow |
| `SectionLabel` | Toda parte | Label small-caps + action opcional |

---

## Cuidados ao reconstruir em Compose

- **Aspect ratio dos produtos** = 4:5. Não é quadrado.
- **Animação do ponteiro** suave (não teleporte). Usar `animateOffsetAsState` ou `Animatable` com `spring`.
- **PiP do vídeo** sempre acima do conteúdo (`Modifier.zIndex(30f)`).
- **Bottom bars** respeitam safe area do iOS (24dp extra para home indicator).
- **Scroll** do catálogo no live tem que ser sincronizado: emite `Scroll` no DC, recebe `Scroll` do remoto → `LazyListState.animateScrollToItem` se o produto em vista do remoto sair da minha vista por mais de 1.5s.
