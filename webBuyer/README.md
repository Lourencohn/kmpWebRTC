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

## Configuração

Defina o servidor de sinalização via `.env` (veja `.env.example`):

```
VITE_SIGNALING_BASE=http://localhost:8080
```

Sem essa variável o webBuyer assume `http://localhost:8080`.

## Estrutura

```
webBuyer/
├── index.html               ← entry
├── src/
│   ├── main.ts              ← boot: lê ?t=<token>, chama /session/{token}
│   ├── views/
│   │   └── arrival.ts       ← landing, loading, arrival, error
│   └── styles/
│       ├── tokens.css       ← cores/raios/sombras (espelho do design system)
│       └── app.css          ← layout base + cards
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## Roadmap

- **M0–M3** (entregue): wordmark, build verde, tela de chegada com `GET /session/{token}`, CTA de microfone.
- **M4**: share intent / QR code, retomar última sessão, estados de erro mais finos.
- **M5+**: WebRTC nativo do browser, peer + data channel, sincronização com o vendedor.
