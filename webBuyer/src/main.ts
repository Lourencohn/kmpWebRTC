import { fetchSession, SessionFetchError, type SessionInfo } from './api/sessions'
import { SignalingClient } from './signaling/client'
import { clearLastSession, loadLastSession, saveLastSession } from './storage/lastSession'
import { PeerSession, type PeerStatus } from './webrtc/peer'
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

function signalingUrlFor(token: string): string {
  const wsBase = SERVER_BASE
    .replace(/^http:\/\//, 'ws://')
    .replace(/^https:\/\//, 'wss://')
    .replace(/\/$/, '')
  return `${wsBase}/ws/session/${encodeURIComponent(token)}`
}

async function load(token: string): Promise<void> {
  renderLoading(root!, token)
  try {
    const info = await fetchSession(SERVER_BASE, token)
    saveLastSession({ token: info.token, openedAtMs: Date.now(), joined: false })
    renderArrival(root!, info, {
      onJoinAudio: async () => {
        const audio = await requestMic()
        saveLastSession({ token: info.token, openedAtMs: Date.now(), joined: true })
        startPeer(info, audio)
      },
    })
  } catch (err) {
    const fail =
      err instanceof SessionFetchError
        ? err
        : new SessionFetchError('server', 'Falha inesperada ao abrir a sessão.')
    if (fail.kind === 'not_found' || fail.kind === 'expired') clearLastSession()
    renderError(root!, fail.kind, fail.message, token, {
      onRetry: () => {
        if (fail.kind === 'not_found' || fail.kind === 'expired') showLanding()
        else void load(token)
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

async function requestMic(): Promise<MediaStream> {
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new Error('Mídia não suportada')
  }
  return navigator.mediaDevices.getUserMedia({ audio: true })
}

function peerIdFor(token: string): string {
  return `buyer-${token}-${Math.random().toString(36).slice(2, 7)}`
}

function handleDataChannelMessage(raw: string): void {
  let parsed: { type?: string; muted?: boolean } | null = null
  try {
    parsed = JSON.parse(raw)
  } catch {
    return
  }
  if (parsed?.type === 'mute' && typeof parsed.muted === 'boolean') {
    setRemoteMuteIndicator(parsed.muted)
  }
}

function setRemoteMuteIndicator(muted: boolean): void {
  let chip = document.getElementById('remote-mute-chip')
  if (!chip) {
    chip = document.createElement('div')
    chip.id = 'remote-mute-chip'
    chip.className = 'remote-mute-chip'
    document.body.appendChild(chip)
  }
  chip.textContent = muted ? 'Vendedor sem áudio' : ''
  chip.dataset.muted = muted ? 'true' : 'false'
}

function startPeer(info: SessionInfo, audio: MediaStream): void {
  const peerId = peerIdFor(info.token)
  const signaling = new SignalingClient(signalingUrlFor(info.token), {
    peerId,
    role: 'Buyer',
    displayName: info.clientName ?? undefined,
  })
  const session = new PeerSession(
    signaling,
    {
      peerId,
      selfRole: 'Buyer',
      localAudio: audio,
    },
    {
      onStatus: (status, detail) => updateConnectionBanner(status, detail),
      onRemoteAudio: (stream) => attachRemoteAudio(stream),
      onDataChannelOpen: () => updateConnectionBanner('connected', 'dc_open'),
      onDataChannelMessage: (raw) => handleDataChannelMessage(raw),
    },
  )

  const banner = ensureBanner()
  banner.dataset.state = 'connecting'
  banner.textContent = 'Conectando ao vendedor…'

  signaling.start()
  session.start()

  window.addEventListener('beforeunload', () => {
    session.close('navigated_away')
    signaling.close('navigated_away')
    audio.getTracks().forEach((t) => t.stop())
  })
}

function ensureBanner(): HTMLElement {
  let banner = document.getElementById('connection-banner') as HTMLElement | null
  if (!banner) {
    banner = document.createElement('div')
    banner.id = 'connection-banner'
    banner.className = 'connection-banner'
    document.body.appendChild(banner)
  }
  return banner
}

function updateConnectionBanner(status: PeerStatus, detail?: string): void {
  const banner = ensureBanner()
  banner.dataset.state = status
  switch (status) {
    case 'idle':
      banner.textContent = 'Aguardando…'
      break
    case 'negotiating':
      banner.textContent = 'Conectando ao vendedor…'
      break
    case 'connected':
      banner.textContent = detail === 'dc_open' ? 'Conectado · pronto pra trocar áudio' : 'Conectado'
      break
    case 'failed':
      banner.textContent = `Conexão caiu (${detail ?? 'erro'})`
      break
    case 'closed':
      banner.textContent = 'Sessão encerrada'
      break
  }
}

function attachRemoteAudio(stream: MediaStream): void {
  let audio = document.getElementById('remote-audio') as HTMLAudioElement | null
  if (!audio) {
    audio = document.createElement('audio')
    audio.id = 'remote-audio'
    audio.autoplay = true
    audio.setAttribute('playsinline', '')
    document.body.appendChild(audio)
  }
  audio.srcObject = stream
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
