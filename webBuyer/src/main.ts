import { fetchSession, SessionFetchError } from './api/sessions'
import { clearLastSession, loadLastSession, saveLastSession } from './storage/lastSession'
import {
  renderArrival,
  renderError,
  renderLanding,
  renderLoading,
} from './views/arrival'

const root = document.getElementById('app')
if (!root) {
  throw new Error('app root not found')
}

const SERVER_BASE =
  (import.meta.env.VITE_SIGNALING_BASE as string | undefined) ?? 'http://localhost:8080'

function tokenFromUrl(): string | null {
  const params = new URLSearchParams(window.location.search)
  return params.get('t')?.trim() || null
}

function setTokenInUrl(token: string): void {
  const url = new URL(window.location.href)
  url.searchParams.set('t', token)
  window.history.replaceState(null, '', url.toString())
}

async function load(token: string): Promise<void> {
  renderLoading(root!, token)
  try {
    const info = await fetchSession(SERVER_BASE, token)
    saveLastSession({ token: info.token, openedAtMs: Date.now(), joined: false })
    renderArrival(root!, info, {
      onJoinAudio: async () => {
        await requestMic()
        saveLastSession({ token: info.token, openedAtMs: Date.now(), joined: true })
      },
    })
  } catch (err) {
    const fail =
      err instanceof SessionFetchError
        ? err
        : new SessionFetchError('server', 'Falha inesperada ao abrir a sessão.')
    if (fail.kind === 'not_found' || fail.kind === 'expired') {
      clearLastSession()
    }
    renderError(root!, fail.kind, fail.message, token, {
      onRetry: () => {
        if (fail.kind === 'not_found' || fail.kind === 'expired') {
          showLanding()
        } else {
          void load(token)
        }
      },
    })
  }
}

function showLanding(): void {
  const last = loadLastSession()
  renderLanding(root!, last?.token, {
    onReopenLast: (token) => {
      setTokenInUrl(token)
      void load(token)
    },
  })
}

async function requestMic(): Promise<void> {
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new Error('Mídia não suportada')
  }
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
  stream.getTracks().forEach((track) => track.stop())
}

function boot(): void {
  const token = tokenFromUrl()
  if (!token) {
    showLanding()
    return
  }
  void load(token)
}

boot()

if (import.meta.env.DEV) {
  console.info('[trovatacast/web] booted', { server: SERVER_BASE })
}
