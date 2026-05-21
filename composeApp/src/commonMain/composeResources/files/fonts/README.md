# Fontes — Geist + Geist Mono

O design system (`docs/02-design-system.md`) usa **Geist** (sans) e **Geist Mono**.
Por questão de licença, os arquivos não estão versionados — baixe e coloque aqui.

## Como obter

```bash
# Repo oficial: https://github.com/vercel/geist-font
# Coloque os TTFs abaixo neste diretório:
```

Arquivos esperados:
- `Geist-Light.ttf`        (300)
- `Geist-Regular.ttf`      (400)
- `Geist-Medium.ttf`       (500)
- `Geist-SemiBold.ttf`     (600)
- `Geist-Bold.ttf`         (700)
- `GeistMono-Regular.ttf`  (400)
- `GeistMono-Medium.ttf`   (500)

## Como ativar no app

Depois que os TTFs estiverem aqui, em
`composeApp/src/commonMain/kotlin/app/trovata/cast/theme/Type.kt`
troque:

```kotlin
val Geist: FontFamily = FontFamily.Default
val GeistMono: FontFamily = FontFamily.Monospace
```

por:

```kotlin
import app.trovata.cast.resources.Res
import app.trovata.cast.resources.Geist_Regular
import app.trovata.cast.resources.Geist_Medium
// ... etc
import org.jetbrains.compose.resources.Font

@Composable
fun geistFamily(): FontFamily = FontFamily(
    Font(Res.font.Geist_Light, weight = FontWeight.Light),
    Font(Res.font.Geist_Regular, weight = FontWeight.Normal),
    Font(Res.font.Geist_Medium, weight = FontWeight.Medium),
    Font(Res.font.Geist_SemiBold, weight = FontWeight.SemiBold),
    Font(Res.font.Geist_Bold, weight = FontWeight.Bold),
)
```

E mova os TTFs de `files/fonts/` para `composeResources/font/`
(diretório padrão do compose-resources para fontes).

> Enquanto isso, o app roda com a fonte sans do sistema como fallback fiel.
