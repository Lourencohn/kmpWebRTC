import { fetchSnapshot, SnapshotUnavailable } from './api/fetchSnapshot'
import { buildDeepLink, buildOrderDraft, buildOrderSummary, buildWhatsAppShare } from './cart/orderDraft'
import { StorefrontCart } from './cart/storefrontCart'
import { sampleSnapshot } from './data/sampleSnapshot'
import { productIndex, type Snapshot, type SnapshotProduct } from './data/snapshot'
import { productIdFromAnchor, type DataChannelMessage } from './protocol/dataChannel'
import { clearLastSession, loadLastSession, saveLastSession } from './storage/lastSession'
import { mountCartSheet, type CartSheetView } from './ui/cartSheet'
import { renderLoading } from './ui/loadingScreen'
import { mountProductDetail, type ProductDetailView } from './ui/productDetailSheet'
import { mountSendSheet, type SendSheetView } from './ui/sendSheet'
import { renderStorefront, type StorefrontView } from './ui/storefront'
import { startLiveSession } from './webrtc/live'

function appRoot(): HTMLElement {
  const element = document.getElementById('app')
  if (!element) {
    throw new Error('app root not found')
  }
  return element
}

const root = appRoot()

const HOSTED_SIGNALING_BASE = 'https://trovatacast-signaling.fly.dev'

const SERVER_BASE =
  (import.meta.env.VITE_SIGNALING_BASE as string | undefined) ?? HOSTED_SIGNALING_BASE

const DEMO_TOKEN = 'demo'

function tokenFromUrl(): string | null {
  const params = new URLSearchParams(window.location.search)
  return params.get('t')?.trim() || null
}

async function loadSnapshot(token: string | null): Promise<Snapshot> {
  if (!token) return sampleSnapshot(DEMO_TOKEN)
  try {
    return await fetchSnapshot(SERVER_BASE, token)
  } catch (err) {
    if (err instanceof SnapshotUnavailable && import.meta.env.DEV) {
      console.warn('[trovata/web] snapshot unavailable, using fixture', err.kind, err.message)
    }
    return sampleSnapshot(token)
  }
}

type OpenSheets = {
  detail: ProductDetailView | null
  cart: CartSheetView | null
  send: SendSheetView | null
}

function start(snapshot: Snapshot, liveToken: string | null): void {
  const products = productIndex(snapshot)
  const cart = new StorefrontCart(products)

  const sheets: OpenSheets = { detail: null, cart: null, send: null }

  const closeDetail = (): void => {
    sheets.detail?.destroy()
    sheets.detail = null
  }
  const closeCart = (): void => {
    sheets.cart?.destroy()
    sheets.cart = null
  }
  const closeSend = (): void => {
    sheets.send?.destroy()
    sheets.send = null
  }

  const openDetail = (product: SnapshotProduct): void => {
    closeDetail()
    const initialQty = new Map<string, number>()
    cart.linesFor(product.productId).forEach((line) => {
      if (line.sizeId != null) initialQty.set(String(line.sizeId), line.qty)
    })
    sheets.detail = mountProductDetail(storefront.host(), product, snapshot.collectionLabel, initialQty, {
      onSet: (size, qty) => {
        cart.set(product.productId, size.sizeId, size.label, qty, Date.now())
      },
      onDismiss: closeDetail,
    })
  }

  const openCart = (): void => {
    if (sheets.cart) return
    sheets.cart = mountCartSheet(storefront.host(), products, cart.state(), {
      onRemove: (productId, sizeId) => cart.set(productId, sizeId, '', 0, Date.now()),
      onSend: () => {
        closeCart()
        openSend()
      },
      onDismiss: closeCart,
    })
  }

  const openSend = (): void => {
    closeSend()
    const cartState = cart.state()
    if (cartState.totalUnits === 0) return
    const summary = buildOrderSummary(snapshot, cartState, products)
    const deepLink = buildDeepLink(buildOrderDraft(snapshot, cartState.lines))
    const whatsappUrl = buildWhatsAppShare(summary, deepLink)
    sheets.send = mountSendSheet(storefront.host(), products, cartState, snapshot.sellerName, {
      onConfirm: () => {
        window.location.href = whatsappUrl
      },
      onCopy: () => copyToClipboard(`${summary}\n\n${deepLink}`),
      onDismiss: closeSend,
    })
  }

  const storefront: StorefrontView = renderStorefront(root, snapshot, {
    onOpenProduct: openDetail,
    onOpenCart: openCart,
  })

  cart.subscribe((state) => {
    storefront.setCart(state)
    sheets.cart?.update(state)
  })

  if (liveToken) {
    followLiveSession(snapshot, liveToken, products, openDetail)
  }
}

function pointedProductId(message: DataChannelMessage): number | null {
  if (message.type === 'scroll') return message.anchor.produtoPreId ?? null
  if (message.type === 'pointAt') return productIdFromAnchor(message.target)
  return null
}

function followLiveSession(
  snapshot: Snapshot,
  liveToken: string,
  products: Map<number, SnapshotProduct>,
  openDetail: (product: SnapshotProduct) => void,
): void {
  let lastFocusId: number | null = null

  const scrollToProduct = (productId: number): void => {
    const product = products.get(productId)
    if (!product) return
    const card = root.querySelector<HTMLElement>(`[data-ref="${CSS.escape(product.ref)}"]`)
    card?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  const followSellerFocus = (message: DataChannelMessage): void => {
    if (message.type === 'navigate') {
      const focusedId = message.view.focus?.produtoPreId
      if (focusedId == null) return
      const product = products.get(focusedId)
      if (product) openDetail(product)
      lastFocusId = focusedId
      return
    }
    const pointedId = pointedProductId(message)
    if (pointedId == null || pointedId === lastFocusId) return
    scrollToProduct(pointedId)
    lastFocusId = pointedId
  }

  void startLiveSession(SERVER_BASE, liveToken, snapshot.customerName ?? 'Cliente')
    .then((live) => {
      live.onStatus((status) => {
        if (status === 'connected') {
          saveLastSession({ token: snapshot.token, openedAtMs: Date.now(), joined: true })
        }
      })
      live.onMessage(followSellerFocus)
    })
    .catch((err) => {
      console.warn('[trovata/web] sessão ao vivo indisponível', err)
    })
}

async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}

async function boot(): Promise<void> {
  const token = tokenFromUrl()
  renderLoading(root, token ?? DEMO_TOKEN)
  try {
    const snapshot = await loadSnapshot(token)
    saveLastSession({ token: snapshot.token, openedAtMs: Date.now(), joined: false })
    start(snapshot, token)
  } catch (err) {
    clearLastSession()
    if (import.meta.env.DEV) console.error('[trovata/web] boot failed', err)
    root.innerHTML = `<section class="vitrine-error"><h1>Não foi possível abrir o catálogo</h1><p>Confira o link enviado pelo vendedor e tente de novo.</p></section>`
  }
}

void boot()

if (import.meta.env.DEV) {
  console.info('[trovata/web] vitrine booted', { server: SERVER_BASE, lastSession: loadLastSession() })
}
