# Status atual

> Snapshot do estado da construção. Atualizar sempre que fechar um milestone.

---

## Resumo

| Milestone | Status | PR / commit | Notas |
|---|---|---|---|
| M0 — Esqueleto e tooling | ✅ concluído | init scaffold | 5 superfícies sobem, 5 jobs CI verdes |
| M1 — Componentes do design system | 🚧 próximo | — | Pill, Btn, ProductCard, VideoTile, RemotePointer |
| M2 — Sessões (Home do vendedor) | ⏳ pendente | — | depende de M1 |
| M3 → M12 | ⏳ pendente | — | ver `docs/06-roadmap.md` |

---

## O que foi entregue no M0

### Estrutura
```
trovatacast/
├── protocol/                ← SessionEvent + Codec compartilhado
├── composeApp/              ← KMP Android + iOS targets
├── iosApp/                  ← Bootstrap Xcode (XcodeGen)
├── webBuyer/                ← Vite + TypeScript
├── signalingServer/         ← Ktor (porta 8080)
├── gradle/libs.versions.toml← Catálogo central de versões
├── .github/
│   ├── workflows/ci.yml     ← 5 jobs em paralelo
│   ├── dependabot.yml
│   ├── CODEOWNERS
│   ├── pull_request_template.md
│   └── ISSUE_TEMPLATE/
├── CONTRIBUTING.md
├── bootstrap.sh             ← setup auto-suficiente (baixa gradle se preciso)
└── README.md
```

### Tokens do design system (em código)
- Cores: `app.trovata.cast.theme.LightColors` (Kotlin) + `webBuyer/src/styles/tokens.css` (CSS).
- Tipografia: `app.trovata.cast.theme.DefaultType` (Geist fallback → sistema).
- Raios e espaçamentos: `app.trovata.cast.theme.DefaultRadii` / `DefaultSpacing`.
- Acesso via `TrovataTokens.{colors,type,radii,spacing}` em qualquer composable.

### Aceitação validada
- `./gradlew :protocol:jvmTest` → 2 testes verdes.
- `./gradlew :signalingServer:test` → 1 teste verde, `GET /health` retorna `200 ok`.
- `(cd webBuyer && npm run build)` → bundle gerado em `dist/`.
- iOS: `iosApp.xcodeproj` gerado por XcodeGen, Compose UI bridgeada via `MainViewControllerKt.MainViewController()`.

### Dívidas conhecidas (não-bloqueantes)
- **Fontes Geist**: slots prontos, TTFs não versionados (licença). Ver `composeApp/src/commonMain/composeResources/files/fonts/README.md`.
- **Pendências para CI**: jobs já configuradas mas não rodaram em PR ainda (sem remote configurado).
- **Coturn/TURN server**: não setup; vai entrar antes do M5 (sinalização + handshake).
- **Persistência server-side**: Postgres + Exposed + Flyway declarados em libs.versions.toml mas não wired (entra em M3).

---

## Próximo milestone — M1: Componentes do design system

Critério de aceitação do roadmap: *side-by-side com o protótipo, nenhuma divergência visual perceptível em telas chave.*

### Componentes a construir (ordem sugerida)
1. **Pill** — `commonMain/ui/components/Pill.kt`. Variantes: `neutral`, `brand`, `jade`, `live` (pulsa), `ghost`, `dark`. Ícone opcional 12dp.
2. **Btn** — `Btn.kt`. Variantes: `primary`, `jade`, `soft`, `ghost`, `surface`, `dark`, `danger`. Tamanhos sm/md/lg.
3. **Icon** + Icons object — adaptar do `prototype/ui.jsx` (Lucide-style, stroke 1.6-1.8).
4. **Avatar** — iniciais sobre fundo `oklch(86% 0.07 hue)`. Hue por cliente.
5. **Card** — base com `r3` + `sh1`.
6. **SectionLabel** — small caps Ink4 + ação opcional.
7. **ProductCard** — todas as variantes (sm/md/lg, highlight, pointed, inCart). Cor de fundo da peça (sand/sage/terracota/...).
8. **ProductRow** — versão linha.
9. **VideoTile** — gradiente radial simulando rosto. Aspecto 1:1.34.
10. **RemotePointer** — seta + label. Cor por usuário (hue 30 vendedor / 210 cliente).
11. **TabBar** iOS-style — 4 abas (Sessões, Catálogo, Clientes, Insights).
12. **Garments** — silhuetas SVG (`shirt`, `polo`, `tee`, `dress`, `jacket`, `pants`, `shoe`, `sweater`, `skirt`).
13. **`DesignSystemPreview`** — tela storybook-like renderizando todos.

### Onde mora cada coisa
- Componentes Compose: `composeApp/src/commonMain/kotlin/app/trovata/cast/ui/components/`.
- Ícones: `composeApp/src/commonMain/kotlin/app/trovata/cast/ui/icons/Icons.kt`.
- Catálogo hardcoded (pra preview): `composeApp/src/commonMain/kotlin/app/trovata/cast/data/sample/SampleCatalog.kt`.
- Preview screen: `composeApp/src/commonMain/kotlin/app/trovata/cast/ui/screens/preview/DesignSystemScreen.kt`.

### Como atacar
- Branch: `feat/m1-design-system`.
- PR único cobrindo todos os componentes (M1 é uma unidade conceitual).
- Para cada componente: abrir o protótipo HTML correspondente, ler `prototype/ui.jsx` / `prototype/catalog.jsx`, escrever o equivalente em Compose, comparar lado a lado.
- Testes: screenshot tests opcionais; comparação visual manual é o critério principal.

---

## Histórico

| Data | Marco |
|---|---|
| 2026-05-20 | M0 fechado: estrutura KMP + 4 superfícies + CI |
