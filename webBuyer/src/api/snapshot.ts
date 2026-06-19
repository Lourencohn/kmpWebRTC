import type { Snapshot, SnapshotProduct } from '../data/snapshot'

export class SnapshotUnavailable extends Error {
  constructor(
    readonly kind: 'network' | 'not_found' | 'no_products' | 'malformed' | 'server',
    message: string,
  ) {
    super(message)
    this.name = 'SnapshotUnavailable'
  }
}

type FetchLike = typeof fetch

export async function fetchSnapshot(
  baseUrl: string,
  token: string,
  fetchImpl: FetchLike = fetch,
): Promise<Snapshot> {
  const url = `${baseUrl.replace(/\/$/, '')}/session/${encodeURIComponent(token)}`

  let response: Response
  try {
    response = await fetchImpl(url, { headers: { Accept: 'application/json' } })
  } catch {
    throw new SnapshotUnavailable('network', 'Sem conexão com o servidor.')
  }

  if (response.status === 404) throw new SnapshotUnavailable('not_found', 'Sessão não encontrada.')
  if (response.status >= 500) throw new SnapshotUnavailable('server', `Servidor respondeu ${response.status}.`)
  if (!response.ok) throw new SnapshotUnavailable('server', `Resposta inesperada ${response.status}.`)

  let body: unknown
  try {
    body = await response.json()
  } catch {
    throw new SnapshotUnavailable('malformed', 'Resposta inválida do servidor.')
  }

  const snapshot = mapSnapshot(token, body)
  if (!snapshot) throw new SnapshotUnavailable('no_products', 'Sessão sem catálogo embutido.')
  return snapshot
}

function mapSnapshot(token: string, body: unknown): Snapshot | null {
  if (!body || typeof body !== 'object') return null
  const v = body as Record<string, unknown>
  const rawProducts = v.products
  if (!Array.isArray(rawProducts) || rawProducts.length === 0) return null

  const products = rawProducts
    .map(mapProduct)
    .filter((p): p is SnapshotProduct => p !== null)
  if (products.length === 0) return null

  const context = (v.context ?? {}) as Record<string, unknown>
  return {
    token: typeof v.token === 'string' ? v.token : token,
    sellerName: str(v.sellerName) ?? 'Vendedor',
    customerName: str(v.customerName) ?? null,
    customerShop: str(v.customerShop) ?? null,
    context: {
      priceTableId: num(context.priceTableId) ?? 1,
      typeSaleId: num(context.typeSaleId),
      billingTypeId: num(context.billingTypeId),
      label: str(context.label) ?? str(v.collectionLabel) ?? 'Catálogo',
    },
    collectionLabel: str(v.collectionLabel) ?? 'Catálogo',
    products,
    createdAtMs: num(v.createdAtMs) ?? Date.now(),
    expiresAtMs: num(v.expiresAtMs) ?? Date.now() + 7 * 24 * 60 * 60 * 1000,
  }
}

function mapProduct(raw: unknown): SnapshotProduct | null {
  if (!raw || typeof raw !== 'object') return null
  const p = raw as Record<string, unknown>
  const productId = num(p.productId)
  const priceCents = num(p.priceCents)
  if (productId == null || priceCents == null) return null
  const rawSizes = Array.isArray(p.sizes) ? p.sizes : []
  const sizes = rawSizes
    .map((s) => {
      const size = s as Record<string, unknown>
      const sizeId = num(size.sizeId)
      const label = str(size.label)
      if (sizeId == null || label == null) return null
      return { sizeId, label, available: num(size.available) }
    })
    .filter((s): s is { sizeId: number; label: string; available: number | null } => s !== null)
  return {
    productId,
    colorId: num(p.colorId),
    ref: str(p.ref) ?? str(p.productCode) ?? `#${productId}`,
    name: str(p.name) ?? 'Produto',
    brand: str(p.brand),
    imageUrl: str(p.imageUrl),
    priceCents,
    available: num(p.available),
    sizes: sizes.length > 0 ? sizes : [{ sizeId: productId * 10, label: 'Único', available: num(p.available) }],
  }
}

function str(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null
}

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}
