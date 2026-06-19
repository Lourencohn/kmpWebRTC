import { SampleCatalog } from './catalog'

export type CommercialContext = {
  priceTableId: number
  typeSaleId?: number | null
  billingTypeId?: number | null
  label: string
}

export type SnapshotSize = {
  sizeId: number
  label: string
  available?: number | null
}

export type SnapshotProduct = {
  productId: number
  colorId?: number | null
  ref: string
  name: string
  brand?: string | null
  imageUrl?: string | null
  priceCents: number
  available?: number | null
  sizes: SnapshotSize[]
}

export type Snapshot = {
  token: string
  sellerName: string
  customerName: string | null
  customerShop: string | null
  context: CommercialContext
  collectionLabel: string
  products: SnapshotProduct[]
  createdAtMs: number
  expiresAtMs: number
}

export type CartLine = {
  productId: number
  colorId?: number | null
  sizeId?: number | null
  sizeLabel: string
  qty: number
}

export type CartDraft = {
  sessionToken: string
  customerId?: number | null
  customerName: string
  context: CommercialContext
  lines: CartLine[]
  note?: string | null
  tsMs: number
}

const DEEP_LINK_SCHEME = 'trovatacoletor://pedido'

export function parsePriceCents(price: string): number {
  const numeric = price.replace(/[^\d,.]/g, '').replace(/\./g, '').replace(',', '.')
  const value = Number.parseFloat(numeric)
  if (!Number.isFinite(value)) return 0
  return Math.round(value * 100)
}

export function formatBrl(cents: number): string {
  const whole = Math.floor(cents / 100)
  const fractional = String(cents % 100).padStart(2, '0')
  const wholeWithDots = whole.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return `R$ ${wholeWithDots},${fractional}`
}

export function toBase64Url(input: string): string {
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

const STOCK_SEEDS = [12, 8, 6, 4, 10, 7]
const SIZE_GRADES: Record<string, string[]> = {
  default: ['Único'],
  apparel: ['P', 'M', 'G', 'GG'],
}

function fixtureSizes(productSizes: string[], offset: number): SnapshotSize[] {
  const labels =
    productSizes.length === 1 && productSizes[0]!.toLowerCase() === 'único'
      ? SIZE_GRADES.default!
      : productSizes
  return labels.map((label, i) => ({
    sizeId: offset * 10 + i + 1,
    label,
    available: STOCK_SEEDS[(offset + i) % STOCK_SEEDS.length]!,
  }))
}

export function fixtureSnapshot(token: string): Snapshot {
  const products: SnapshotProduct[] = SampleCatalog.products.map((p, index) => {
    const sizes = fixtureSizes(p.sizes, index)
    const available = sizes.reduce((sum, s) => sum + (s.available ?? 0), 0)
    return {
      productId: index + 1,
      ref: p.ref,
      name: p.name,
      brand: p.ref.split('-')[0] ?? null,
      imageUrl: p.image ?? null,
      priceCents: parsePriceCents(p.price),
      available,
      sizes,
    }
  })
  const now = Date.now()
  return {
    token,
    sellerName: 'Atelier Norte',
    customerName: null,
    customerShop: null,
    context: { priceTableId: 1, label: SampleCatalog.collection },
    collectionLabel: SampleCatalog.collection,
    products,
    createdAtMs: now,
    expiresAtMs: now + 7 * 24 * 60 * 60 * 1000,
  }
}

export function productIndex(snapshot: Snapshot): Map<number, SnapshotProduct> {
  return new Map(snapshot.products.map((p) => [p.productId, p]))
}
