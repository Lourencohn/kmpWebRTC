import { DEFAULT_SNAPSHOT_TTL_MS, type Snapshot, type SnapshotProduct } from '../data/snapshot'

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
    sellerName: nonEmptyString(v.sellerName) ?? 'Vendedor',
    customerName: nonEmptyString(v.customerName) ?? null,
    customerShop: nonEmptyString(v.customerShop) ?? null,
    context: {
      priceTableId: finiteNumber(context.priceTableId) ?? 1,
      typeSaleId: finiteNumber(context.typeSaleId),
      billingTypeId: finiteNumber(context.billingTypeId),
      label: nonEmptyString(context.label) ?? nonEmptyString(v.collectionLabel) ?? 'Catálogo',
    },
    collectionLabel: nonEmptyString(v.collectionLabel) ?? 'Catálogo',
    products,
    createdAtMs: finiteNumber(v.createdAtMs) ?? Date.now(),
    expiresAtMs: finiteNumber(v.expiresAtMs) ?? Date.now() + DEFAULT_SNAPSHOT_TTL_MS,
  }
}

function mapProduct(raw: unknown): SnapshotProduct | null {
  if (!raw || typeof raw !== 'object') return null
  const p = raw as Record<string, unknown>
  const productId = finiteNumber(p.productId)
  const priceCents = finiteNumber(p.priceCents)
  if (productId == null || priceCents == null) return null
  const rawSizes = Array.isArray(p.sizes) ? p.sizes : []
  const sizes = rawSizes
    .map((s) => {
      const size = s as Record<string, unknown>
      const sizeId = finiteNumber(size.sizeId)
      const label = nonEmptyString(size.label)
      if (sizeId == null || label == null) return null
      return { sizeId, label, available: finiteNumber(size.available) }
    })
    .filter((s): s is { sizeId: number; label: string; available: number | null } => s !== null)
  return {
    productId,
    colorId: finiteNumber(p.colorId),
    ref: nonEmptyString(p.ref) ?? nonEmptyString(p.productCode) ?? `#${productId}`,
    name: nonEmptyString(p.name) ?? 'Produto',
    brand: nonEmptyString(p.brand),
    imageUrl: nonEmptyString(p.imageUrl),
    priceCents,
    available: finiteNumber(p.available),
    sizes: sizes.length > 0 ? sizes : [{ sizeId: productId * 10, label: 'Único', available: finiteNumber(p.available) }],
  }
}

function nonEmptyString(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null
}

function finiteNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}
