import { formatBrl } from '../data/formatBrl'
import type { CommercialContext, Snapshot, SnapshotProduct } from '../data/snapshot'
import type { CartEntry, CartState } from './storefrontCart'

type CartLine = {
  productId: number
  colorId?: number | null
  sizeId?: number | null
  sizeLabel: string
  qty: number
}

type CartDraft = {
  sessionToken: string
  customerId?: number | null
  customerName: string
  context: CommercialContext
  lines: CartLine[]
  note?: string | null
  tsMs: number
}

const DEEP_LINK_SCHEME = 'trovatacoletor://pedido'

function toCartLine(entry: CartEntry): CartLine {
  return {
    productId: entry.productId,
    sizeId: entry.sizeId,
    sizeLabel: entry.sizeLabel,
    qty: entry.qty,
  }
}

export function buildOrderDraft(snapshot: Snapshot, entries: CartEntry[]): CartDraft {
  return {
    sessionToken: snapshot.token,
    customerId: null,
    customerName: snapshot.customerName ?? 'Cliente',
    context: snapshot.context,
    lines: entries.map((entry) => toCartLine(entry)),
    tsMs: Date.now(),
  }
}

export function buildOrderSummary(
  snapshot: Snapshot,
  state: CartState,
  products: Map<number, SnapshotProduct>,
): string {
  const header = `Pedido — ${snapshot.collectionLabel}\n${state.totalUnits} itens · ${formatBrl(state.totalCents)}`
  const lines = state.lines
    .map((line) => {
      const product = products.get(line.productId)
      const name = product?.name ?? `Produto ${line.productId}`
      return `• ${name} — ${line.sizeLabel} x${line.qty}`
    })
    .join('\n')
  return `${header}\n${lines}`
}

function toBase64Url(input: string): string {
  const utf8 = typeof TextEncoder !== 'undefined'
    ? Array.from(new TextEncoder().encode(input), (b) => String.fromCharCode(b)).join('')
    : unescape(encodeURIComponent(input))
  return btoa(utf8).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

export function buildDeepLink(draft: CartDraft): string {
  const payload = toBase64Url(JSON.stringify(draft))
  return `${DEEP_LINK_SCHEME}#d=${payload}`
}

export function buildWhatsAppShare(summary: string, deepLink: string): string {
  const text = `${summary}\n\n${deepLink}`
  return `https://api.whatsapp.com/send?text=${encodeURIComponent(text)}`
}
