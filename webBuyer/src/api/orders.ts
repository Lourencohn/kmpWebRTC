import type { OrderLine } from '../protocol/order'

export type OrderSource = 'Buyer' | 'Seller'

export type OrderSubmissionRequest = {
  orderId: string
  sessionToken: string
  tsMs: number
  totalCents: number
  lines: OrderLine[]
  source: OrderSource
  clientName?: string
}

export type OrderSubmissionResponse = {
  orderId: string
  receivedAtMs: number
}

export type SubmitOrderResult =
  | { ok: true; receivedAtMs: number }
  | { ok: false; kind: 'network' | 'session_unknown' | 'invalid_payload' | 'server'; message: string }

type FetchLike = typeof fetch

export type SubmitOrderDeps = {
  fetchImpl?: FetchLike
}

export async function submitOrder(
  baseUrl: string,
  request: OrderSubmissionRequest,
  deps: SubmitOrderDeps = {},
): Promise<SubmitOrderResult> {
  const fetchImpl = deps.fetchImpl ?? fetch
  const url = `${baseUrl.replace(/\/$/, '')}/order`

  let response: Response
  try {
    response = await fetchImpl(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(request),
    })
  } catch {
    return { ok: false, kind: 'network', message: 'Falha de rede ao registrar pedido.' }
  }

  if (response.status === 404) {
    return { ok: false, kind: 'session_unknown', message: 'Sessão não encontrada ou expirada.' }
  }
  if (response.status === 400) {
    return { ok: false, kind: 'invalid_payload', message: 'Pedido inválido.' }
  }
  if (!response.ok) {
    return { ok: false, kind: 'server', message: `Servidor respondeu ${response.status}.` }
  }

  let body: unknown
  try {
    body = await response.json()
  } catch {
    return { ok: false, kind: 'server', message: 'Resposta inválida do servidor.' }
  }

  if (!isSubmissionResponse(body)) {
    return { ok: false, kind: 'server', message: 'Resposta inesperada do servidor.' }
  }
  return { ok: true, receivedAtMs: body.receivedAtMs }
}

function isSubmissionResponse(value: unknown): value is OrderSubmissionResponse {
  if (!value || typeof value !== 'object') return false
  const v = value as Record<string, unknown>
  return typeof v.orderId === 'string' && typeof v.receivedAtMs === 'number'
}
