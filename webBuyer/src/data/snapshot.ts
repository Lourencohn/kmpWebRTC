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

export const DEFAULT_SNAPSHOT_TTL_MS = 7 * 24 * 60 * 60 * 1000

export function productIndex(snapshot: Snapshot): Map<number, SnapshotProduct> {
  return new Map(snapshot.products.map((p) => [p.productId, p]))
}
