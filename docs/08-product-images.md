# Fotos reais de produto

Como adicionar fotos reais ao catálogo, em 3 passos.

## 1. Coloque a foto aqui

```
composeApp/src/commonMain/composeResources/drawable/product_ch_3485059.webp
```

**Importante**: o gerador de recursos do Compose Multiplatform **não** recurse em subpastas dentro de `drawable/`. Por isso as fotos ficam direto na raiz de `drawable/`, isoladas visualmente pelo prefixo `product_`.

Convenção de nome: `product_<ref>.webp`, em minúsculo, com `-` virando `_`.

Exemplos atuais (catálogo de bolsas):

| SKU         | Nome do arquivo            |
| ----------- | -------------------------- |
| CH-3485059  | `product_ch_3485059.webp`  |
| CH-3485087  | `product_ch_3485087.webp`  |
| CH-3484980  | `product_ch_3484980.webp`  |
| CH-3485025  | `product_ch_3485025.webp`  |
| CH-3485278  | `product_ch_3485278.webp`  |
| CH-3485310  | `product_ch_3485310.webp`  |
| LEE-2842    | `product_lee_2842.webp`    |
| DM-2025     | `product_dm_2025.webp`     |

## 2. Gere o `Res.drawable.*`

```bash
./gradlew :composeApp:generateComposeResClass
```

Isso cria a referência `Res.drawable.product_ch_3485059` (etc.) que pode ser usada de `commonMain`.

## 3. Associe a foto ao produto

Em `composeApp/src/commonMain/kotlin/app/trovata/cast/data/sample/SampleCatalog.kt`:

```kotlin
Product(
    ref = "CH-3485059",
    name = "Bolsa Contemporâneo Couro",
    // ...
    image = Res.drawable.product_ch_3485059,
)
```

E o import no topo:

```kotlin
import app.trovata.cast.resources.Res
import app.trovata.cast.resources.product_ch_3485059
```

`ProductCard` e `ProductRow` detectam `image != null` e renderizam a foto com `ContentScale.Crop`. Sem `image`, caem na silhueta SVG (`Garment`).

## Dimensões recomendadas

- **Aspecto**: 4:5 vertical (ex. 1200×1500) — combina com o slot do card.
- **Formato**: WebP (preferencial — ~70% menor que PNG) ou PNG. JPEG também funciona.
- **Resolução**: 1× = 1200×1500 px é suficiente. Para nitidez @2× em telas grandes, suba para 2000×2500.
- **Fundo**: limpo (off-white, papel kraft, sombra natural). Evite fundo branco puro — quebra contra o `--bg` do app.
- **Peso**: alvo ≤ 120kB por foto após compressão.

## Sem foto = fallback SVG

Não precisa preencher `image` em todos os produtos. Os que ficarem com `image = null` continuam renderizando a silhueta SVG (`Garment`). Útil para preencher um catálogo grande incrementalmente.

## Próximo passo (médio prazo)

Quando o vendedor for cadastrar produto pelo app (M16 — editor de catálogo), as fotos não vão mais ficar bundladas: vão ser carregadas do servidor via URL e cacheadas com Coil 3. Aí trocaremos `painterResource(image)` por `AsyncImage(url)` num único ponto (`ProductCard`/`ProductRow`).
