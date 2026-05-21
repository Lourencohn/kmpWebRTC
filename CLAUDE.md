# TrovataCast — Contexto para Claude Code

> Este repositório é o ponto de partida para o desenvolvimento do **TrovataCast**, um aplicativo móvel de venda B2B com co-presença em tempo real, construído em **Kotlin Multiplatform** com **WebRTC**.
> Toda a fase de descoberta e design já foi feita. Este documento + a pasta `docs/` contêm tudo que você precisa para continuar.

---

## 0. Antes de começar — leia nesta ordem

1. **`docs/00-concept.md`** — o que é o produto, qual o problema, qual o conceito central (co-presença).
2. **`docs/01-product-flows.md`** — todos os fluxos do produto (já prototipados + a próxima onda).
3. **`docs/02-design-system.md`** — sistema visual (tokens, tipografia, componentes).
4. **`docs/03-stack-kmp-webrtc.md`** — escolhas técnicas e por quê.
5. **`docs/04-architecture.md`** — estrutura de módulos KMP + camadas.
6. **`docs/05-screens-reference.md`** — cada tela do protótipo mapeada para componentes Compose.
7. **`docs/06-roadmap.md`** — ordem de construção sugerida.

Em paralelo, abra `prototype/index.html` em qualquer navegador para ver as 7 cenas do protótipo lado a lado (iPhone do vendedor vs. navegador Android do cliente). É a referência visual viva.

---

## 1. O que é o TrovataCast (versão de 60 segundos)

Distribuidoras, representantes e atacadistas brasileiros vendem hoje por ligação + PDF no WhatsApp. O **TrovataCast** transforma esse momento numa sessão ao vivo onde **vendedor e cliente veem o mesmo catálogo, ao mesmo tempo, em telas diferentes** — com áudio P2P, ponteiro compartilhado, scroll sincronizado e carrinho em tempo real.

- **Vendedor**: app nativo (iOS + Android via KMP/Compose).
- **Cliente**: web (sem instalar nada) — abre o link no navegador do celular.
- **Pedido**: nasce dentro da chamada, sem digitação posterior.
- **Privacidade**: vídeo, áudio e estado do catálogo trafegam direto entre os dois dispositivos (WebRTC P2P).

Categoria-alvo do MVP: **moda atacado**.

---

## 2. Decisões já tomadas (não revisitar sem motivo forte)

| Decisão | Valor |
|---|---|
| Linguagem | Kotlin (KMP) |
| UI cross-platform | Compose Multiplatform (iOS + Android) |
| Comunicação tempo-real | WebRTC (lib: `webrtc-sdk-android` + `WebRTC.framework` para iOS, ou wrapper `webrtc-kmp`) |
| Cliente (buyer) | Web app vanilla com WebRTC nativo do browser |
| Sinalização | Ktor server (Kotlin) com WebSocket |
| Persistência local | SQLDelight (compartilhado entre iOS/Android) |
| DI | Koin |
| Network | Ktor client |
| Idioma da UI | Português (BR) |
| Paleta | Azul `#2456E0` + Jade `#0E8F6E` sobre off-white quente |
| Tipografia | Geist (sans) + Geist Mono (SKUs/preços) |
| Categoria do MVP | Moda atacado |

Detalhes e racional em `docs/03-stack-kmp-webrtc.md`.

---

## 3. O que está pronto vs. o que falta

> Status detalhado em `docs/07-status.md` (atualizar a cada milestone).

✅ **Pronto** (neste repositório)
- Conceito + premissas
- Sistema visual (cores, tipografia, componentes-chave)
- 7 cenas prototipadas em HTML (vendedor iPhone + cliente Android, lado a lado)
- Catálogo de exemplo (moda atacado — Coleção Outono 26 da "Atelier Norte")
- Roadmap priorizado de fluxos adicionais
- Decisões de stack documentadas
- **Milestone 0**: esqueleto KMP completo (`composeApp/`, `iosApp/`, `webBuyer/`, `signalingServer/`, `protocol/`) + CI + tokens do design system + wordmark renderizando nas 4 superfícies

🚧 **A construir** (pelo Claude Code)
- M1: componentes Compose do design system (Pill, Btn, ProductCard, VideoTile, RemotePointer)
- M2+: telas do vendedor, integração WebRTC, persistência, co-presença ao vivo

Ordem detalhada: `docs/06-roadmap.md`. Status corrente: `docs/07-status.md`.

---

## 4. Princípios de execução

Quando você (Claude Code) for construir, mantenha:

1. **Co-presença é o produto.** Tudo que reforça "estamos vendo a mesma coisa, juntos" tem prioridade sobre features genéricas de catálogo ou chat.
2. **Mobile-first, mas mobile-real.** O vendedor está dirigindo, andando na feira, com 4G ruim. O cliente está no depósito. Tudo precisa funcionar com uma mão e sem tutorial.
3. **Cliente nunca instala nada.** Quebrar isso quebra o produto.
4. **P2P até onde der.** Mídia e estado do catálogo trafegam direto entre dispositivos. Servidor é só sinalização + persistência de pedido.
5. **Pedido nasce na chamada.** Nenhuma digitação posterior. Se um fluxo exige formulário pós-call, está errado.
6. **Histórico se constrói sozinho.** Cada sessão gera registro estruturado sem ação manual.
7. **Português brasileiro coloquial-profissional.** "Atender", "Convidar", "Apontando", "Mostrando", "Pedido pronto". Não "iniciar videochamada".

---

## 5. Convenções de código (a aplicar quando começar a escrever)

- **Estrutura de pacotes**: `app.trovata.cast.<module>.<layer>` — ex. `app.trovata.cast.session.domain`, `app.trovata.cast.catalog.ui`.
- **Nomenclatura**: features no plural quando agrupam várias telas (`sessions`, `catalog`, `clients`); singular para domínio único (`auth`, `pricing`).
- **Compose**: telas em `*Screen.kt`, componentes reutilizáveis em `*Card.kt` / `*Row.kt` / `*Pill.kt` espelhando o protótipo.
- **State**: MVVM com `StateFlow` na camada de viewmodel; UI puramente reativa.
- **Coroutines**: `Dispatchers.IO` para rede/disco; `Dispatchers.Main` para UI; nunca bloqueante.
- **Testes**: lógica de domínio + sync de estado têm cobertura. UI Compose com Paparazzi / screenshot tests onde fizer sentido.

---

## 6. Como rodar o protótipo HTML (referência viva)

```bash
cd prototype
# qualquer servidor estático serve
python3 -m http.server 8000
# abra http://localhost:8000
```

Cada cena é um par "iPhone do vendedor" + "Navegador do cliente" lado a lado. Clique no nome de um artboard para abri-lo em tela cheia.

---

## 7. Onde procurar quando estiver em dúvida

| Pergunta | Arquivo |
|---|---|
| "Por que esse fluxo existe?" | `docs/00-concept.md` |
| "Qual o comportamento de um fluxo?" | `docs/01-product-flows.md` |
| "Que cor / tamanho / espaçamento usar?" | `docs/02-design-system.md` |
| "Qual lib uso para X?" | `docs/03-stack-kmp-webrtc.md` |
| "Onde esse código deve morar?" | `docs/04-architecture.md` |
| "Como essa tela deveria parecer?" | `docs/05-screens-reference.md` + `prototype/` |
| "O que construir agora?" | `docs/06-roadmap.md` |

---

## 8. Contato com a fonte conceitual

O documento conceitual original (português, escrito pelo product owner) está preservado em `docs/00-concept.md`. Quando houver conflito entre o que está nos outros docs e o conceito original, **o conceito vence**. Os outros docs são derivações.

Não adicione comentários durante os códigos, preservo muito o código limpo e organizado.

---

## 9. Convenções de commits e versionamento

- **Mensagens de commit curtas e sucintas.** Conventional Commits: `tipo(escopo): descrição em uma linha`. Sem corpo a não ser que indispensável; quando precisar, máximo 3 linhas.
- **Nada de `Co-Authored-By: Claude ...`** nem assinaturas tipo "🤖 Generated with Claude Code". Commits são meus.
- Escopo é o nome do módulo: `composeApp`, `iosApp`, `webBuyer`, `signalingServer`, `protocol`, `docs`, `ci`, `github`.
- Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`.
- Exemplos OK: `feat(composeApp): pill component`, `fix(ci): cache gradle`, `chore: bump kotlin`.
- Detalhes do branch model em `CONTRIBUTING.md`.
