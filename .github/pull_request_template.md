## Resumo

<!-- O que muda e por quê. Cite o milestone do docs/06-roadmap.md se aplicável. -->

## Tipo de mudança

- [ ] Milestone do roadmap — número: M_
- [ ] Bug fix
- [ ] Melhoria de DX/infra
- [ ] Refactor
- [ ] Documentação

## Como testar

<!--
Passos manuais. Para mudanças visuais, inclua antes/depois do protótipo HTML
em paralelo com o app — co-presença é o produto.
-->

## Checklist

- [ ] Branch baseada em `main` atualizada
- [ ] CI verde nas 5 jobs (`protocol`, `android`, `ios-framework`, `server`, `web-buyer`)
- [ ] Sem novos comentários no código (preferência do projeto — só `*.md`)
- [ ] Textos em pt-BR coloquial-profissional (ver `docs/02-design-system.md` §9)
- [ ] Se mexeu em UI: paralelo visual com `prototype/` confere
- [ ] Se mexeu no protocolo do DC: spec espelhada entre `protocol/` (Kotlin) e `webBuyer/src/webrtc/events.ts` (TS)
