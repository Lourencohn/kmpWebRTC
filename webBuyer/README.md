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

## Estrutura

```
webBuyer/
├── index.html               ← entry; renderiza wordmark
├── src/
│   ├── main.ts              ← boot
│   └── styles/
│       ├── tokens.css       ← cores/raios/sombras (espelho do design system)
│       └── app.css          ← layout base
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## Roadmap

- **M0** (atual): wordmark, build TS+Vite verde.
- **M4**: tela de chegada (BuyerArrival) com nome do vendedor.
- **M5+**: WebRTC nativo do browser, peer + data channel, sincronização com o vendedor.
