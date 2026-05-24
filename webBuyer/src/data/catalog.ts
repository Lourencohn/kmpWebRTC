export type ProductTag = 'novo' | 'topVenda' | 'preVenda'

export type GarmentKind =
  | 'shirt'
  | 'polo'
  | 'tee'
  | 'dress'
  | 'jacket'
  | 'pants'
  | 'shoe'
  | 'sweater'
  | 'skirt'

export type GarmentTint = {
  background: string
  foreground: string
}

export type Product = {
  ref: string
  name: string
  garment: GarmentKind
  tintIndex: number
  price: string
  moq: number
  sizes: string[]
  colorCount: number
  tag?: ProductTag
  image?: string
}

export const FashionTints: GarmentTint[] = [
  { background: '#EEEAE0', foreground: '#7C6E58' },
  { background: '#E6EAE5', foreground: '#5C6C5F' },
  { background: '#E8E4DC', foreground: '#534234' },
  { background: '#DDE4E6', foreground: '#3A5260' },
  { background: '#E8DEDA', foreground: '#8B4C44' },
  { background: '#E0DCD2', foreground: '#3D3833' },
  { background: '#F0E7D6', foreground: '#7A5A2C' },
  { background: '#D8DDD2', foreground: '#3C4530' },
]

export function tintFor(index: number): GarmentTint {
  const wrapped = ((index % FashionTints.length) + FashionTints.length) % FashionTints.length
  return FashionTints[wrapped]!
}

export const SampleCatalog = {
  collection: 'Verão 26 · Atelier Norte',
  products: [
    { ref: 'CH-3485059', name: 'Bolsa Contemporâneo Couro', garment: 'shirt', tintIndex: 2, price: 'R$ 189,90', moq: 4, sizes: ['Único'], colorCount: 2, tag: 'novo', image: '/products/product_ch_3485059.webp' },
    { ref: 'CH-3485087', name: 'Bolsa Elegance Monograma', garment: 'shirt', tintIndex: 2, price: 'R$ 249,00', moq: 4, sizes: ['Único'], colorCount: 1, tag: 'topVenda', image: '/products/product_ch_3485087.webp' },
    { ref: 'CH-3484980', name: 'Bolsa Matelassê Cute', garment: 'shirt', tintIndex: 5, price: 'R$ 199,00', moq: 6, sizes: ['Único'], colorCount: 1, image: '/products/product_ch_3484980.webp' },
    { ref: 'CH-3485025', name: 'Bolsa New Cristal Nude', garment: 'shirt', tintIndex: 0, price: 'R$ 219,00', moq: 4, sizes: ['Único'], colorCount: 2, tag: 'novo', image: '/products/product_ch_3485025.webp' },
    { ref: 'CH-3485278', name: 'Bolsa Couro Texturizado', garment: 'shirt', tintIndex: 2, price: 'R$ 169,00', moq: 6, sizes: ['Único'], colorCount: 1, image: '/products/product_ch_3485278.webp' },
    { ref: 'CH-3485310', name: 'Bolsa Carbono Trama', garment: 'shirt', tintIndex: 5, price: 'R$ 229,00', moq: 4, sizes: ['Único'], colorCount: 1, tag: 'preVenda', image: '/products/product_ch_3485310.webp' },
    { ref: 'LEE-2842', name: 'Bolsa Cute Bear Bege', garment: 'shirt', tintIndex: 6, price: 'R$ 179,00', moq: 4, sizes: ['Único'], colorCount: 1, image: '/products/product_lee_2842.webp' },
    { ref: 'DM-2025', name: 'Bolsa Dumond Shopping', garment: 'shirt', tintIndex: 3, price: 'R$ 269,00', moq: 3, sizes: ['Único'], colorCount: 1, tag: 'topVenda', image: '/products/product_dm_2025.webp' },
  ] satisfies Product[],
} as const

export const ProductSwatchPalette: string[] = [
  '#3F4744',
  '#A5806A',
  '#C7B79B',
  '#5E5B57',
]

export function tagLabel(tag: ProductTag): string {
  switch (tag) {
    case 'novo':
      return 'Novo'
    case 'topVenda':
      return 'Top venda'
    case 'preVenda':
      return 'Pré-venda'
  }
}
