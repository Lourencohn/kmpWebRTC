export type SessionInfo = {
  token: string
  sellerName: string
  collectionLabel: string
  clientName: string | null
  clientShop: string | null
  scheduledFor: string | null
  productCount: number
  createdAtMs: number
  expiresAtMs: number
}

export type SessionFetchErrorKind =
  | 'network'
  | 'not_found'
  | 'expired'
  | 'server'
  | 'malformed'

export class SessionFetchError extends Error {
  constructor(
    readonly kind: SessionFetchErrorKind,
    message: string,
    readonly status?: number,
  ) {
    super(message)
    this.name = 'SessionFetchError'
  }
}

type FetchLike = typeof fetch

export type FetchSessionDeps = {
  fetchImpl?: FetchLike
  now?: () => number
}

export async function fetchSession(
  baseUrl: string,
  token: string,
  deps: FetchSessionDeps = {},
): Promise<SessionInfo> {
  const fetchImpl = deps.fetchImpl ?? fetch
  const now = deps.now ?? Date.now
  const url = `${baseUrl.replace(/\/$/, '')}/session/${encodeURIComponent(token)}`

  let response: Response
  try {
    response = await fetchImpl(url, {
      headers: { Accept: 'application/json' },
    })
  } catch {
    throw new SessionFetchError(
      'network',
      'Sem conexão com o servidor. Tente novamente em alguns segundos.',
      undefined,
    )
  }

  if (response.status === 404) {
    throw new SessionFetchError('not_found', 'Sessão não encontrada ou já expirada.', 404)
  }
  if (response.status >= 500) {
    throw new SessionFetchError('server', `Servidor respondeu ${response.status}.`, response.status)
  }
  if (!response.ok) {
    throw new SessionFetchError('server', `Resposta inesperada ${response.status}.`, response.status)
  }

  let body: unknown
  try {
    body = await response.json()
  } catch {
    throw new SessionFetchError('malformed', 'Resposta inválida do servidor.', response.status)
  }

  if (!isSessionInfo(body)) {
    throw new SessionFetchError('malformed', 'Dados da sessão estão incompletos.', response.status)
  }
  if (body.expiresAtMs < now()) {
    throw new SessionFetchError('expired', 'Esse convite já passou da validade.', response.status)
  }
  return body
}

function isSessionInfo(value: unknown): value is SessionInfo {
  if (!value || typeof value !== 'object') return false
  const v = value as Record<string, unknown>
  return (
    typeof v.token === 'string' &&
    typeof v.sellerName === 'string' &&
    typeof v.collectionLabel === 'string' &&
    typeof v.productCount === 'number' &&
    typeof v.createdAtMs === 'number' &&
    typeof v.expiresAtMs === 'number'
  )
}
