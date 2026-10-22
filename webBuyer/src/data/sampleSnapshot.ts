import {
  DEFAULT_SNAPSHOT_TTL_MS,
  type Snapshot,
  type SnapshotProduct,
  type SnapshotSize,
} from './snapshot'

type SampleProduct = {
  ref: string
  name: string
  priceCents: number
  sizes: string[]
  image: string
}

const SAMPLE_SELLER = 'Atelier Norte'
const SAMPLE_COLLECTION = 'Verão 26 · Atelier Norte'

const SAMPLE_PRODUCTS: SampleProduct[] = [
  { ref: 'CH-3485059', name: 'Bolsa Contemporâneo Couro', priceCents: 18990, sizes: ['Único'], image: '/products/product_ch_3485059.webp' },
  { ref: 'CH-3485087', name: 'Bolsa Elegance Monograma', priceCents: 24900, sizes: ['Único'], image: '/products/product_ch_3485087.webp' },
  { ref: 'CH-3484980', name: 'Bolsa Matelassê Cute', priceCents: 19900, sizes: ['Único'], image: '/products/product_ch_3484980.webp' },
  { ref: 'CH-3485025', name: 'Bolsa New Cristal Nude', priceCents: 21900, sizes: ['Único'], image: '/products/product_ch_3485025.webp' },
  { ref: 'CH-3485278', name: 'Bolsa Couro Texturizado', priceCents: 16900, sizes: ['Único'], image: '/products/product_ch_3485278.webp' },
  { ref: 'CH-3485310', name: 'Bolsa Carbono Trama', priceCents: 22900, sizes: ['Único'], image: '/products/product_ch_3485310.webp' },
  { ref: 'LEE-2842', name: 'Bolsa Cute Bear Bege', priceCents: 17900, sizes: ['Único'], image: '/products/product_lee_2842.webp' },
  { ref: 'DM-2025', name: 'Bolsa Dumond Shopping', priceCents: 26900, sizes: ['Único'], image: '/products/product_dm_2025.webp' },
]

const STOCK_SEEDS = [12, 8, 6, 4, 10, 7]

function sampleSizes(labels: string[], productPosition: number): SnapshotSize[] {
  return labels.map((label, sizePosition) => ({
    sizeId: productPosition * 10 + sizePosition + 1,
    label,
    available: STOCK_SEEDS[(productPosition + sizePosition) % STOCK_SEEDS.length]!,
  }))
}

function toSnapshotProduct(sample: SampleProduct, position: number): SnapshotProduct {
  const sizes = sampleSizes(sample.sizes, position)
  return {
    productId: position + 1,
    ref: sample.ref,
    name: sample.name,
    brand: sample.ref.split('-')[0] ?? null,
    imageUrl: sample.image,
    priceCents: sample.priceCents,
    available: sizes.reduce((sum, size) => sum + (size.available ?? 0), 0),
    sizes,
  }
}

export function sampleSnapshot(token: string): Snapshot {
  const now = Date.now()
  return {
    token,
    sellerName: SAMPLE_SELLER,
    customerName: null,
    customerShop: null,
    context: { priceTableId: 1, label: SAMPLE_COLLECTION },
    collectionLabel: SAMPLE_COLLECTION,
    products: SAMPLE_PRODUCTS.map((sample, position) => toSnapshotProduct(sample, position)),
    createdAtMs: now,
    expiresAtMs: now + DEFAULT_SNAPSHOT_TTL_MS,
  }
}
