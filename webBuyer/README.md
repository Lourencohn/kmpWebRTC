# webBuyer — app web do cliente

Cliente abre via link `https://trovata.cast/s/<token>` no navegador do celular. **Não instala nada.**

## Dev

```bash
cd webBuyer
npm install
npm run dev
# abre em http://localhost:5173
```

## Build

```bash
npm run build
# saída em dist/
```

## Type check

```bash
npm run typecheck
```

## Testes

```bash
npm test            # roda vitest uma vez
npm run test:watch  # modo watch (jsdom)
```

Cobertura atual: `api/sessions`, `views/arrival`, `storage/lastSession`.

## Configuração

Defina o servidor de sinalização via `.env` (veja `.env.example`):

```
VITE_SIGNALING_BASE=http://localhost:8080
```

Sem essa variável o webBuyer assume `http://localhost:8080`.

## Estrutura

```
webBuyer/
├── index.html                       ← entry
├── src/
│   ├── main.ts                      ← boot: lê ?t=<token>, retoma última sessão, pede mic
│   ├── api/
│   │   ├── sessions.ts              ← fetchSession + SessionFetchError discriminada
│   │   └── __tests__/sessions.test.ts
│   ├── storage/
│   │   ├── lastSession.ts           ← save/load/clear via localStorage (com fallback null)
│   │   └── __tests__/lastSession.test.ts
│   ├── views/
│   │   ├── arrival.ts               ← landing | loading | arrival | error
│   │   └── __tests__/arrival.test.ts
│   └── styles/
│       ├── tokens.css               ← cores/raios/sombras (espelho do design system)
│       └── app.css                  ← layout base + cards + spinner
├── package.json
├── tsconfig.json
├── vite.config.ts
└── vitest.config.ts
```

## Roadmap

- **M0–M4** (entregue): boot, tela de chegada com `GET /session/{token}`, estados de erro finos, persistência da última sessão, CTA de mic.
- **M5+**: WebRTC nativo do browser, peer + data channel, sincronização com o vendedor.
