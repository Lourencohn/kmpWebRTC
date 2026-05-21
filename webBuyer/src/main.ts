import { renderArrival, renderError, renderLanding, renderLoading } from './views/arrival'

const root = document.getElementById('app')
if (!root) {
  throw new Error('app root not found')
}

type SessionInfo = {
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

const SERVER_BASE =
  (import.meta.env.VITE_SIGNALING_BASE as string | undefined) ?? 'http://localhost:8080'

async function fetchSession(token: string): Promise<SessionInfo> {
  const response = await fetch(`${SERVER_BASE}/session/${encodeURIComponent(token)}`, {
    headers: { Accept: 'application/json' },
  })
  if (response.status === 404) {
    throw new Error('Sessão não encontrada ou já expirada')
  }
  if (!response.ok) {
    throw new Error(`Servidor respondeu ${response.status}`)
  }
  return (await response.json()) as SessionInfo
}

async function boot() {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('t')?.trim()

  if (!token) {
    renderLanding(root!)
    return
  }

  renderLoading(root!, token)
  try {
    const info = await fetchSession(token)
    renderArrival(root!, info)
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Falha ao carregar sessão'
    renderError(root!, message, token)
  }
}

void boot()

if (import.meta.env.DEV) {
  console.info('[trovatacast/web] booted', { server: SERVER_BASE })
}
