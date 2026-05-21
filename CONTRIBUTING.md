# Contribuindo com o TrovataCast

> Documento curto. A fonte canônica de **o que** construir é `docs/06-roadmap.md`. Aqui está **como**.

---

## Setup

```bash
./bootstrap.sh
```

Veja `README.md` para detalhes por superfície.

---

## Branch model

- `main` — sempre verde, sempre deployable.
- `feat/<milestone>-<slug>` — feature de roadmap, ex.: `feat/m1-design-system`.
- `fix/<curta-descricao>` — bugfix.
- `chore/<curta-descricao>` — infra/CI/deps.
- `docs/<curta-descricao>` — só documentação.

Cada PR resolve **um milestone** ou **um bug** — nada de "PR grande de fim de sprint".

---

## Commits

Convencional:

```
<tipo>(<escopo>): <título imperativo curto>

<corpo opcional explicando o porquê>
```

Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`.
Escopo: nome do módulo (`composeApp`, `webBuyer`, `signalingServer`, `protocol`, `iosApp`, `docs`, `ci`).

Exemplos:

```
feat(composeApp): adiciona ProductCard com 3 variantes
fix(signalingServer): valida token JWT antes de aceitar WS
chore(ci): cacheia node_modules do webBuyer
docs(roadmap): marca M2 como concluído
```

Não amende commits já pushed; faça commits novos.

---

## PRs

- Abra contra `main`.
- CI precisa estar verde nas 5 jobs.
- Use o template (`.github/pull_request_template.md`) — não apague seções.
- Auto-review: leia seu próprio diff antes de pedir revisão.
- Vincule a um milestone do roadmap (label `m0`, `m1`, ...) ou ao número da issue.

---

## Qualidade

- **Sem comentários em código**. Preferimos nomes explícitos + README. Comentários ficam em `*.md`.
- **Português brasileiro** na UI e em strings visíveis ao usuário. Código em inglês.
- **Testes**: lógica de domínio e protocolo de DC têm cobertura. UI é avaliada visualmente contra `prototype/`.
- **Sem placeholders na `main`** — tudo que merge tem que rodar end-to-end no escopo do milestone.

---

## Dúvidas?

Abra uma Discussion no GitHub. Issues são pra bugs e features.
