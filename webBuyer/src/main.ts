import { fetchSnapshot, SnapshotUnavailable } from './api/snapshot'
import { VitrineCart, type CartSnapshotView } from './cart/vitrineCart'
import {
  buildDeepLink,
  buildWhatsAppShare,
  fixtureSnapshot,
  formatBrl,
  productIndex,
  type CartDraft,
  type Snapshot,
  type SnapshotProduct,
} from './data/snapshot'
import { clearLastSession, loadLastSession, saveLastSession } from './storage/lastSession'
import { mountCartSheet, type CartSheetView } from './ui/vitrineCartSheet'
import { mountProductDetail, type DetailView } from './ui/vitrineProductDetail'
import { mountSendSheet, type SendView } from './ui/vitrineSend'
import { renderLoading } from './views/arrival'
import { renderVitrine, type VitrineView } from './views/vitrine'
import { startLiveSession } from './webrtc/live'

const root = document.getElementById('app')
if (!root) {
  throw new Error('app root not found')
}

const SERVER_BASE =
  (import.meta.env.VITE_SIGNALING_BASE as string | undefined) ??
  (import.meta.env.PROD ? window.location.origin : 'http://localhost:8080')

function tokenFromUrl(): string | null {
  const params = new URLSearchParams(window.location.search)
  return params.get('t')?.trim() || null
}

async function loadSnapshot(token: string | null): Promise<Snapshot> {
  if (!token) return fixtureSnapshot('demo')
  try {
    return await fetchSnapshot(SERVER_BASE, token)
  } catch (err) {
    if (err instanceof SnapshotUnavailable && import.meta.env.DEV) {
      console.warn('[trovata/web] snapshot unavailable, using fixture', err.kind, err.message)
    }
    return fixtureSnapshot(token)
  }
}

type Sheets = {
  detail: DetailView | null
  detailProductId: number | null
  cart: CartSheetView | null
  send: SendView | null
}

function start(snapshot: Snapshot, liveToken: string | null): void {
  const products = productIndex(snapshot)
  const cart = new VitrineCart(products)

  const sheets: Sheets = { detail: null, detailProductId: null, cart: null, send: null }

  const closeDetail = (): void => {
    sheets.detail?.destroy()
    sheets.detail = null
    sheets.detailProductId = null
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
    sheets.detailProductId = product.productId
    sheets.detail = mountProductDetail(view.host(), product, snapshot.collectionLabel, initialQty, {
      onSet: (size, qty) => {
        cart.set(product.productId, size.sizeId, size.label, qty, Date.now())
      },
      onDismiss: closeDetail,
    })
  }

  const openCart = (): void => {
    if (sheets.cart) return
    sheets.cart = mountCartSheet(view.host(), products, cart.snapshot(), {
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
    const view2 = cart.snapshot()
    if (view2.totalUnits === 0) return
    const draft = buildDraft(snapshot, cart.draftLines())
    const summary = buildSummary(snapshot, view2, products)
    const deepLink = buildDeepLink(draft)
    const whatsappUrl = buildWhatsAppShare(summary, deepLink)
    sheets.send = mountSendSheet(view.host(), products, view2, snapshot.sellerName, {
      onConfirm: () => {
        window.location.href = whatsappUrl
      },
      onCopy: () => copyToClipboard(`${summary}\n\n${deepLink}`),
      onDismiss: closeSend,
    })
  }

  const view: VitrineView = renderVitrine(root!, snapshot, {
    onOpenProduct: openDetail,
    onOpenCart: openCart,
  })

  cart.subscribe((snap) => {
    view.setCart(snap)
    sheets.cart?.update(snap)
  })

  if (liveToken) {
    const scrollToProduct = (productId: string): void => {
      const card = root!.querySelector<HTMLElement>(`[data-product-id="${productId}"]`)
      card?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
    void startLiveSession(SERVER_BASE, liveToken, snapshot.customerName ?? 'Cliente')
      .then((live) => {
        live.onStatus((status) => {
          if (status === 'connected') {
            saveLastSession({ token: snapshot.token, openedAtMs: Date.now(), joined: true })
          }
        })
        live.onMessage((message) => {
          if (
            message.type === 'navigate' ||
            message.type === 'scroll' ||
            message.type === 'pointAt'
          ) {
            scrollToProduct(message.productId)
          }
        })
      })
      .catch((err) => {
        console.warn('[trovata/web] sessão ao vivo indisponível', err)
      })
  }
}

function buildDraft(snapshot: Snapshot, lines: CartDraft['lines']): CartDraft {
  return {
    sessionToken: snapshot.token,
    customerId: null,
    customerName: snapshot.customerName ?? 'Cliente',
    context: snapshot.context,
    lines,
    tsMs: Date.now(),
  }
}

function buildSummary(
  snapshot: Snapshot,
  view: CartSnapshotView,
  products: Map<number, SnapshotProduct>,
): string {
  const header = `Pedido — ${snapshot.collectionLabel}\n${view.totalUnits} itens · ${formatBrl(view.totalCents)}`
  const lines = view.lines
    .map((line) => {
      const product = products.get(line.productId)
      const name = product?.name ?? `Produto ${line.productId}`
      return `• ${name} — ${line.sizeLabel} x${line.qty}`
    })
    .join('\n')
  return `${header}\n${lines}`
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
  renderLoading(root!, token ?? 'demo')
  try {
    const snapshot = await loadSnapshot(token)
    saveLastSession({ token: snapshot.token, openedAtMs: Date.now(), joined: false })
    start(snapshot, token)
  } catch (err) {
    clearLastSession()
    if (import.meta.env.DEV) console.error('[trovata/web] boot failed', err)
    root!.innerHTML = `<section class="vitrine-error"><h1>Não foi possível abrir o catálogo</h1><p>Confira o link enviado pelo vendedor e tente de novo.</p></section>`
  }
}

void boot()

if (import.meta.env.DEV) {
  console.info('[trovata/web] vitrine booted', { server: SERVER_BASE, lastSession: loadLastSession() })
}
