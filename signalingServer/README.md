# signalingServer — Ktor

Servidor leve do TrovataCast:
- **Sinalização WebRTC** (a partir do Milestone 5): WebSocket que orquestra `offer`/`answer`/`ICE`.
- **Persistência de sessões e pedidos** (a partir do Milestone 3): REST CRUD.
- **Princípio**: áudio, vídeo e estado do catálogo **nunca passam por aqui** — só metadados.

## Endpoints atuais

| Método | Path | Resposta |
|---|---|---|
| GET | `/health` | `200 ok` |
| GET | `/version` | `{"name":"trovatacast-signaling","version":"0.1.0"}` |

## Rodar localmente

```bash
./gradlew :signalingServer:run
# http://localhost:8080/health
```

Variável de ambiente opcional: `PORT` (default `8080`).

## Build de produção

```bash
./gradlew :signalingServer:buildFatJar
# saída: signalingServer/build/libs/trovatacast-signaling.jar
java -jar signalingServer/build/libs/trovatacast-signaling.jar
```

## Testes

```bash
./gradlew :signalingServer:test
```

## Deploy no Fly.io

Pré-requisitos: cadastro no Fly.io feito + cartão (a conta inicia em trial).

```bash
brew install flyctl
fly auth login

fly apps create trovatacast-signaling
fly secrets set PUBLIC_CATALOG_URL=https://trovata.app.br --app trovatacast-signaling
fly deploy
```

Configuração relevante (`fly.toml` na raiz do repo):
- Região primária: `gru` (São Paulo).
- Máquina: `shared-cpu-1x` 256MB (~US$ 2/mês).
- `auto_stop_machines = "off"` mantém o WebSocket sempre disponível.
- Health check em `/health`.

Verificar:

```bash
curl https://trovatacast-signaling.fly.dev/health
curl https://trovatacast-signaling.fly.dev/version
```

Limite de gasto: configure em `fly.io/dashboard/<org>/billing` (Spend Limit) para teto absoluto.

## Roadmap

- **M0** (atual): health-check, base do projeto.
- **M3**: `POST /sessions` cria sessão e retorna `{ sessionId, token, url }`. Persistência PostgreSQL.
- **M4**: `GET /sessions/<token>` para o web buyer validar token.
- **M5**: WebSocket `/ws/session/<token>` para sinalização WebRTC.
- **M9**: `POST /orders` quando uma sessão fecha pedido.
