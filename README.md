# TrovataCast

Aplicativo móvel de venda B2B com **co-presença em tempo real** — vendedor e cliente vendo o mesmo catálogo, ao mesmo tempo, em telas diferentes.

> **Stack**: Kotlin Multiplatform (Compose Multiplatform) + WebRTC P2P + Ktor.
> **Categoria-alvo do MVP**: moda atacado.
> **Status**: design completo. Construção em andamento.

---

## ⚡ Início rápido

```bash
# Abra o protótipo HTML (referência visual viva do produto)
cd prototype
python3 -m http.server 8000
# Abra http://localhost:8000 no navegador
```

Cada cena no protótipo mostra um par **iPhone do vendedor** + **navegador Android do cliente** lado a lado. Clique no nome de um artboard para ver em tela cheia.

---

## 📂 Estrutura

```
trovatacast/
├── CLAUDE.md              ← Ponto de entrada para Claude Code
├── docs/                  ← Toda a documentação do produto
│   ├── 00-concept.md
│   ├── 01-product-flows.md
│   ├── 02-design-system.md
│   ├── 03-stack-kmp-webrtc.md
│   ├── 04-architecture.md
│   ├── 05-screens-reference.md
│   └── 06-roadmap.md
└── prototype/             ← Protótipo HTML (referência viva)
    └── index.html
```

A estrutura de código KMP a ser criada está em `docs/04-architecture.md`.

---

## 🧭 Para continuar com Claude Code

1. Abra `CLAUDE.md` — instruções de entrada.
2. Leia os 7 docs na ordem indicada.
3. Abra `prototype/index.html` — referência visual.
4. Siga `docs/06-roadmap.md` — milestone por milestone.

---

## 🎯 Conceito em uma frase

> O TrovataCast não é um catálogo com vídeo. É o registro vivo da relação comercial entre um vendedor e seus clientes, construído sessão a sessão, sem nenhum esforço manual de documentação.

(Trecho de `docs/00-concept.md`.)
