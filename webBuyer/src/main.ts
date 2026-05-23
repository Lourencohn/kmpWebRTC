import { submitOrder } from './api/orders'
import { fetchSession, SessionFetchError, type SessionInfo } from './api/sessions'
import { CartStore } from './cart/store'
import {
  decodeDataChannelMessage,
  encodeDataChannelMessage,
  type DataChannelMessage,
} from './protocol/dataChannel'
import { SignalingClient } from './signaling/client'
import { clearLastSession, loadLastSession, saveLastSession } from './storage/lastSession'
import { mountCartSheet, type CartSheetView } from './ui/cartSheet'
import { mountOrderSummary, type OrderSummaryView } from './ui/orderSummary'
import { findProduct, mountProductDetail, type ProductDetailView } from './ui/productDetail'
import { PeerSession, type PeerStatus } from './webrtc/peer'
import {
  renderArrival,
  renderError,
  renderLanding,
  renderLiveCall,
  renderLoading,
  type LiveCallStatus,
  type LiveCallView,
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
        ensureWebRtcSupported()
        const audio = await requestMic()
        try {
          saveLastSession({ token: info.token, openedAtMs: Date.now(), joined: true })
          startPeer(info, audio)
        } catch (err) {
          audio.getTracks().forEach((t) => t.stop())
          throw err
        }
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
  if (!window.isSecureContext) {
    throw new Error('Abra esta página por HTTPS para liberar o microfone.')
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new Error('Este navegador não expõe o microfone. Tente Chrome ou Safari atualizados.')
  }
  return navigator.mediaDevices.getUserMedia({ audio: true, video: false })
}

function ensureWebRtcSupported(): void {
  if (typeof window.RTCPeerConnection !== 'function') {
    throw new Error('Este navegador não suporta WebRTC. Atualize o app ou use outro navegador.')
  }
  if (typeof window.WebSocket !== 'function') {
    throw new Error('Este navegador não suporta WebSocket.')
  }
}

function peerIdFor(token: string): string {
  return `buyer-${token}-${Math.random().toString(36).slice(2, 7)}`
}

function mapPeerStatus(status: PeerStatus): LiveCallStatus {
  switch (status) {
    case 'connected':
      return 'connected'
    case 'failed':
      return 'failed'
    case 'closed':
      return 'closed'
    default:
      return 'connecting'
  }
}

type SheetController = {
  cart: CartSheetView | null
  detail: ProductDetailView | null
  detailProductId: string | null
  summary: OrderSummaryView | null
}

function buildDataChannelHandler(
  view: LiveCallView,
  cart: CartStore,
  sheets: SheetController,
  openDetail: (productId: string) => void,
  showSummary: (payload: {
    orderId: string
    ts: number
    from: string
    lines: import('./protocol/dataChannel').OrderLine[]
    totalCents: number
  }) => void,
): (raw: string) => void {
  let pointTimer: ReturnType<typeof setTimeout> | null = null
  return (raw) => {
    const msg = decodeDataChannelMessage(raw)
    if (!msg) return
    switch (msg.type) {
      case 'mute':
        view.setRemoteMuted(msg.muted)
        return
      case 'scroll':
        view.scrollToProduct(msg.productId, msg.offset)
        return
      case 'pointAt':
        view.setPointedProduct(msg.productId)
        if (pointTimer) clearTimeout(pointTimer)
        const duration = msg.durationMs ?? 3_000
        pointTimer = setTimeout(() => view.setPointedProduct(null), duration)
        return
      case 'navigate':
        openDetail(msg.productId)
        return
      case 'cartUpdate':
        cart.apply(msg.productId, msg.size, msg.units, msg.ts)
        if (sheets.detail && sheets.detailProductId === msg.productId) {
          sheets.detail.setCurrentUnits(msg.size, msg.units)
        }
        return
      case 'orderConfirm':
        showSummary({
          orderId: msg.orderId,
          ts: msg.ts,
          from: msg.from,
          lines: msg.lines,
          totalCents: msg.totalCents,
        })
        return
    }
  }
}

function startPeer(info: SessionInfo, audio: MediaStream): void {
  const peerId = peerIdFor(info.token)
  const signaling = new SignalingClient(signalingUrlFor(info.token), {
    peerId,
    role: 'Buyer',
    displayName: info.clientName ?? undefined,
  })

  const cart = new CartStore()
  const sheets: SheetController = {
    cart: null,
    detail: null,
    detailProductId: null,
    summary: null,
  }
  let session: PeerSession | null = null
  let cleanedUp = false
  const cleanup = (reason: string) => {
    if (cleanedUp) return
    cleanedUp = true
    sheets.cart?.destroy()
    sheets.detail?.destroy()
    sheets.summary?.destroy()
    session?.close(reason)
    signaling.close(reason)
    audio.getTracks().forEach((t) => t.stop())
  }

  const sendCartUpdate = (productId: string, size: string, units: number) => {
    const msg: DataChannelMessage = {
      type: 'cartUpdate',
      productId,
      size,
      units,
      ts: Date.now(),
      from: peerId,
    }
    session?.send(encodeDataChannelMessage(msg))
  }

  const sendNavigate = (productId: string) => {
    const msg: DataChannelMessage = {
      type: 'navigate',
      productId,
      ts: Date.now(),
      from: peerId,
    }
    session?.send(encodeDataChannelMessage(msg))
  }

  const openDetail = (productId: string) => {
    const product = findProduct(productId)
    if (!product) return
    if (sheets.detail) {
      sheets.detail.destroy()
      sheets.detail = null
    }
    const lines = cart.linesFor(productId).map((l) => ({ size: l.size, units: l.units }))
    sheets.detailProductId = productId
    sheets.detail = mountProductDetail(view.host(), product, lines, {
      onAdd: (size, units) => {
        const ts = Date.now()
        cart.apply(productId, size, units, ts)
        sendCartUpdate(productId, size, units)
        sheets.detail?.setCurrentUnits(size, units)
      },
      onDismiss: () => {
        sheets.detail?.destroy()
        sheets.detail = null
        sheets.detailProductId = null
      },
    })
  }

  const openCart = () => {
    if (sheets.cart) return
    sheets.cart = mountCartSheet(view.host(), cart.snapshot(), {
      onRemove: (productId, size) => {
        const ts = Date.now()
        cart.apply(productId, size, 0, ts)
        sendCartUpdate(productId, size, 0)
      },
      onDismiss: () => {
        sheets.cart?.destroy()
        sheets.cart = null
      },
    })
  }

  const showSummary = (payload: {
    orderId: string
    ts: number
    from: string
    lines: import('./protocol/dataChannel').OrderLine[]
    totalCents: number
  }) => {
    sheets.detail?.destroy()
    sheets.detail = null
    sheets.detailProductId = null
    sheets.cart?.destroy()
    sheets.cart = null
    if (sheets.summary) {
      sheets.summary.destroy()
      sheets.summary = null
    }
    sheets.summary = mountOrderSummary(view.host(), payload, info.sellerName, {
      onClose: () => {
        cleanup('order_closed')
        showLanding()
      },
    })
    void submitOrder(SERVER_BASE, {
      orderId: payload.orderId,
      sessionToken: info.token,
      tsMs: payload.ts,
      totalCents: payload.totalCents,
      lines: payload.lines,
      source: 'Buyer',
      clientName: info.clientName ?? undefined,
    }).then((result) => {
      if (!result.ok && import.meta.env.DEV) {
        console.warn('[trovatacast/web] submit order failed', result)
      }
    })
  }

  const view = renderLiveCall(root!, info, {
    onToggleMute: (muted) => session?.setLocalMuted(muted),
    onHangup: () => {
      cleanup('hangup')
      showLanding()
    },
    onOpenProduct: (productId) => {
      openDetail(productId)
      sendNavigate(productId)
    },
    onOpenCart: () => openCart(),
  })

  cart.subscribe((snapshot) => {
    view.setCart(snapshot)
    sheets.cart?.update(snapshot)
  })

  session = new PeerSession(
    signaling,
    {
      peerId,
      selfRole: 'Buyer',
      localAudio: audio,
    },
    {
      onStatus: (status, detail) => view.setStatus(mapPeerStatus(status), detail),
      onRemoteAudio: (stream) => attachRemoteAudio(stream),
      onDataChannelMessage: buildDataChannelHandler(view, cart, sheets, openDetail, showSummary),
    },
  )

  view.setStatus('connecting')
  signaling.start()
  session.start()

  window.addEventListener('beforeunload', () => cleanup('navigated_away'))
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
  audio.muted = false
  audio.volume = 1
  const playback = audio.play()
  if (playback && typeof playback.catch === 'function') {
    playback.catch((err) => {
      console.warn('[trovatacast/web] remote audio autoplay blocked', err)
    })
  }
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
