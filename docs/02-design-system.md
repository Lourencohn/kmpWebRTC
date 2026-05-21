# Sistema visual

> Referência para reconstruir o protótipo em Compose Multiplatform.
> A fonte viva é `prototype/styles.css` + `prototype/ui.jsx` + `prototype/catalog.jsx`.

---

## 1. Tokens de cor

```kotlin
// Background / neutros (off-white quente)
val Bg          = Color(0xFFFAFAF6)
val Surface     = Color(0xFFFFFFFF)
val Surface2    = Color(0xFFF4F4EE)
val Surface3    = Color(0xFFECECE4)
val Line        = Color(0xFFE5E4DC)
val LineStrong  = Color(0xFFD6D5CB)

// Tinta (texto)
val Ink   = Color(0xFF0E1116)
val Ink2  = Color(0xFF2B313A)
val Ink3  = Color(0xFF5A6470)
val Ink4  = Color(0xFF8A93A0)
val Ink5  = Color(0xFFB6BCC4)

// Marca — azul calmo (primário)
val Brand     = Color(0xFF2456E0)
val Brand2    = Color(0xFF1A41AE)
val BrandTint = Color(0xFFEAEFFE)
val BrandRing = Color(0x382456E0)  // 22% alpha

// Co-presença / confirmação — jade
val Jade     = Color(0xFF0E8F6E)
val Jade2    = Color(0xFF0A6E55)
val JadeTint = Color(0xFFDFF3EC)

// Status
val Live = Color(0xFFE5484D)   // "ao vivo" / urgente
val Warn = Color(0xFFC7711D)
```

**Regras de uso**
- **Brand** = ações principais, links, indicador "está vendo", sync banner.
- **Jade** = qualquer coisa que diga "confirmado", "atender", "carrinho cresceu", "pedido pronto".
- **Live** = pulso de "ao vivo", chamadas em espera, urgência verdadeira.
- **Surface2/3** = chips, fundos de "tipo" sem peso visual.

---

## 2. Tipografia

- **Geist** — sans serif principal. Pesos: 300, 400, 500, 600, 700.
- **Geist Mono** — números, SKUs, preços, tempos. Pesos: 400, 500.

```kotlin
object Type {
    val displayLg = TextStyle(font = Geist, weight = W600, size = 38.sp, lineHeight = 40.sp, letterSpacing = (-0.03).em)
    val title     = TextStyle(font = Geist, weight = W600, size = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.025).em)
    val heading   = TextStyle(font = Geist, weight = W600, size = 18.sp, lineHeight = 22.sp, letterSpacing = (-0.02).em)
    val subhead   = TextStyle(font = Geist, weight = W600, size = 15.sp, lineHeight = 19.sp, letterSpacing = (-0.01).em)
    val body      = TextStyle(font = Geist, weight = W400, size = 13.5.sp, lineHeight = 19.sp)
    val bodyEm    = TextStyle(font = Geist, weight = W500, size = 13.5.sp, lineHeight = 19.sp)
    val caption   = TextStyle(font = Geist, weight = W500, size = 12.sp, lineHeight = 15.sp, color = Ink3)
    val label     = TextStyle(font = Geist, weight = W600, size = 11.sp, lineHeight = 13.sp, letterSpacing = 0.08.em, textTransform = UPPER)
    val mono      = TextStyle(font = GeistMono, weight = W500, size = 12.sp, letterSpacing = 0.04.em)
    val monoBig   = TextStyle(font = GeistMono, weight = W700, size = 20.sp, letterSpacing = (-0.02).em)
}
```

**Regra**: SKU (`AN-217`), preço (`R$ 119,00`), métricas (`R$ 142k`), tempo (`14:30`) → sempre **Geist Mono**.

---

## 3. Espaçamento e raios

```kotlin
object R {
    val r1 = 6.dp;  val r2 = 10.dp; val r3 = 14.dp; val r4 = 20.dp; val r5 = 28.dp
}
object Pad {
    val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp
}
```

Cards: `r3` (14dp). Botões: `r2` (10dp pequeno) / `r3` (14dp grande). Pills: pill 999dp.

---

## 4. Sombras

```kotlin
val sh1 = Shadow(blur = 2.dp, y = 1.dp, color = 0x0F0E1116)
val sh2 = Shadow(blur = 16.dp, y = 4.dp, color = 0x120E1116) + Shadow(blur = 3.dp, y = 1.dp, color = 0x0A0E1116)
val sh3 = Shadow(blur = 40.dp, y = 16.dp, color = 0x1A0E1116)
```

Cards normais: `sh1`. Modais e PiP: `sh2`. Hero CTAs ou highlight: `sh3`.

---

## 5. Componentes principais

### Botão
- Variantes: `primary` (azul brand), `jade` (verde, ações de "confirmar"), `soft` (azul transparente), `ghost`, `surface`, `dark`, `danger`.
- Tamanhos: sm (32dp), md (44dp), lg (52dp).
- Ícone opcional à esquerda.

### Pill
- Variantes: `neutral`, `brand`, `jade`, `live` (pulsa), `ghost`, `dark`.
- Ícone opcional, 12dp.

### Avatar
- Iniciais sobre fundo `oklch(86% 0.07 hue)`, texto `oklch(34% 0.10 hue)`.
- Hue do nome — escolher uma constante por cliente para ser consistente entre telas.

### VideoTile (PiP)
- Quando não há mídia real: gradiente radial simulando rosto + sombreado de "ombros".
- Aspecto vertical `1 : 1.34` por padrão.
- Label inferior com dot (verde = audio ok, cinza = mute).

### ProductCard
- Imagem do produto sobre cor da paleta de moda (sand, sage, terracota, walnut, slate-blue, mustard, moss, graphite).
- Bullets de cor (até 4) + "+N".
- Tag canto superior esquerdo: "Novo" (preto), "Top venda" (jade), "Pré-venda" (warn).
- Quando no carrinho: pill verde "✓ 12" canto superior direito.
- Quando vendedor "está apontando": borda dupla laranja + halo.

### RemotePointer
- Seta + label colorido. Cor por usuário (hue 30 = vendedor, hue 210 = cliente — combinam com o avatar).
- Z-index alto, ignora pointer events.

### Pipeline cards
- Card vazio com label small caps + valor mono grande + delta.
- Card de brand (azul) para destaque.

---

## 6. Padrões de tela

- **Top safe area iOS**: 56dp (status bar incluso).
- **Bottom safe area iOS**: 84dp (home indicator + tab bar).
- **Tab bar inferior**: Sessões, Catálogo, Clientes, Insights.
- **Header de tela**: título grande, subtítulo abaixo, ação à direita.
- **Section label**: small caps `Ink4` + action opcional.

---

## 7. Iconografia

- Stroke 1.6-1.8px, sem fill, `currentColor`.
- 22-24dp default, 18dp em rows.
- Família baseada em Lucide/Feather. Lista usada no protótipo em `prototype/ui.jsx` (objeto `Icons`).

---

## 8. Mocked-up garments (catálogo de moda)

O protótipo usa silhuetas SVG simples (`shirt`, `polo`, `tee`, `dress`, `jacket`, `pants`, `shoe`, `sweater`, `skirt`) sobre fundos pastel. **No app real**: substituir por fotos reais carregadas pelo vendedor no editor de catálogo (F15). Manter o framing 4:5 vertical, fundo limpo, peça centralizada.

---

## 9. Idioma e copywriting

- **Português brasileiro coloquial-profissional.**
- Verbos curtos: "Atender", "Convidar", "Apontando", "Mostrando", "Adicionar", "Reenviar".
- Nunca: "iniciar videochamada", "criar sessão de vendas", "compartilhar tela com cliente".
- Notificações em segunda pessoa: "Camila vai te ligar", "Você reagiu", "Diego adicionou 12un".
- Tempo: "agora", "hoje", "esta semana", "há 8s", "há 32 segundos".
- Quantidade: "12un", "32 SKUs", "min 6un".
- Moeda: `R$ 119,00` (sempre vírgula decimal, sem espaço extra). Em métricas: `R$ 142k`.

---

## 10. Modo dark

Não definido para o MVP. Manter o produto em modo claro. Tab bar em base ganha tratamento dark somente em live session (vide `SellerLive`).
