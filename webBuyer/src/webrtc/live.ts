import {
  decodeDataChannelMessage,
  encodeDataChannelMessage,
  type CartChangeHint,
  type CartChangeReason,
  type DataChannelMessage,
} from '../protocol/dataChannel'
import { SignalingClient } from '../signaling/client'
import { PeerSession, type PeerStatus } from './peer'

export type LiveSession = {
  peerId: string
  onMessage(handler: (message: DataChannelMessage) => void): void
  onStatus(handler: (status: PeerStatus) => void): void
  sendCartInvalidated(carrinhoId: number, reason: CartChangeReason, hint?: CartChangeHint): void
  close(): void
}

function signalingUrl(serverBase: string, token: string): string {
  const wsBase = serverBase
    .replace(/^http:/, 'ws:')
    .replace(/^https:/, 'wss:')
    .replace(/\/$/, '')
  return `${wsBase}/ws/session/${encodeURIComponent(token)}`
}

function randomPeerId(): string {
  const suffix =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID().slice(0, 8)
      : Math.random().toString(36).slice(2, 10)
  return `buyer-${suffix}`
}

function ensureAudioSink(): HTMLAudioElement {
  const existing = document.getElementById('remote-audio')
  if (existing instanceof HTMLAudioElement) return existing
  const element = document.createElement('audio')
  element.id = 'remote-audio'
  element.autoplay = true
  element.setAttribute('playsinline', '')
  element.style.display = 'none'
  document.body.appendChild(element)
  return element
}

async function fetchIceServers(serverBase: string, token: string): Promise<RTCIceServer[] | undefined> {
  try {
    const response = await fetch(`${serverBase.replace(/\/$/, '')}/session/${encodeURIComponent(token)}`, {
      headers: { Accept: 'application/json' },
    })
    if (!response.ok) return undefined
    const body = (await response.json()) as { iceServers?: RTCIceServer[] }
    const servers = body.iceServers
    return Array.isArray(servers) && servers.length > 0 ? servers : undefined
  } catch {
    return undefined
  }
}

export async function startLiveSession(
  serverBase: string,
  token: string,
  displayName: string,
): Promise<LiveSession> {
  const peerId = randomPeerId()
  const iceServers = await fetchIceServers(serverBase, token)
  const messageHandlers: ((message: DataChannelMessage) => void)[] = []
  const statusHandlers: ((status: PeerStatus) => void)[] = []

  let localAudio: MediaStream | null = null
  try {
    localAudio = await navigator.mediaDevices.getUserMedia({ audio: true })
  } catch (err) {
    console.warn('[trovata/web] microfone indisponível, seguindo só com áudio de recepção', err)
  }

  const audioSink = ensureAudioSink()

  const signaling = new SignalingClient(signalingUrl(serverBase, token), {
    peerId,
    role: 'Buyer',
    displayName,
  })

  const peer = new PeerSession(
    signaling,
    { peerId, selfRole: 'Buyer', localAudio, iceServers },
    {
      onStatus: (status, detail) => {
        console.info('[trovata/web] peer', status, detail ?? '')
        statusHandlers.forEach((handler) => handler(status))
      },
      onDataChannelOpen: () => console.info('[trovata/web] canal de dados aberto'),
      onRemoteAudio: (stream) => {
        audioSink.srcObject = stream
        void audioSink.play().catch(() => undefined)
      },
      onDataChannelMessage: (raw) => {
        const message = decodeDataChannelMessage(raw)
        if (!message) return
        messageHandlers.forEach((handler) => handler(message))
      },
    },
  )

  peer.start()
  signaling.start()

  return {
    peerId,
    onMessage: (handler) => {
      messageHandlers.push(handler)
    },
    onStatus: (handler) => {
      statusHandlers.push(handler)
    },
    sendCartInvalidated: (carrinhoId, reason, hint) => {
      peer.send(
        encodeDataChannelMessage({
          type: 'cartInvalidated',
          carrinhoId,
          reason,
          ts: Date.now(),
          from: peerId,
          hint,
        }),
      )
    },
    close: () => {
      peer.close()
      signaling.close()
      localAudio?.getTracks().forEach((track) => track.stop())
    },
  }
}
