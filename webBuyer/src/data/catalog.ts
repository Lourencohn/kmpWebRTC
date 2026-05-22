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
  collection: 'Coleção Outono · Atelier Norte',
  products: [
    { ref: 'AN-104', name: 'Blusa Tricot Canelado', garment: 'sweater', tintIndex: 0, price: 'R$ 89,90', moq: 6, sizes: ['PP','P','M','G','GG'], colorCount: 3, tag: 'novo' },
    { ref: 'AN-217', name: 'Camisa Linho Manga Longa', garment: 'shirt', tintIndex: 1, price: 'R$ 119,00', moq: 6, sizes: ['P','M','G','GG'], colorCount: 4 },
    { ref: 'AN-088', name: 'Vestido Midi Algodão', garment: 'dress', tintIndex: 4, price: 'R$ 159,00', moq: 4, sizes: ['P','M','G'], colorCount: 2, tag: 'topVenda' },
    { ref: 'AN-301', name: 'Polo Bordado Frente', garment: 'polo', tintIndex: 3, price: 'R$ 99,00', moq: 6, sizes: ['P','M','G','GG'], colorCount: 5 },
    { ref: 'AN-156', name: 'Calça Wide Alfaiataria', garment: 'pants', tintIndex: 5, price: 'R$ 169,00', moq: 4, sizes: ['36','38','40','42','44'], colorCount: 3 },
    { ref: 'AN-422', name: 'Jaqueta Sarja Oversized', garment: 'jacket', tintIndex: 2, price: 'R$ 229,00', moq: 3, sizes: ['P','M','G'], colorCount: 2, tag: 'preVenda' },
    { ref: 'AN-512', name: 'Camiseta Algodão Pima', garment: 'tee', tintIndex: 6, price: 'R$ 59,90', moq: 8, sizes: ['PP','P','M','G','GG'], colorCount: 6 },
    { ref: 'AN-077', name: 'Tênis Couro Curado', garment: 'shoe', tintIndex: 0, price: 'R$ 279,00', moq: 3, sizes: ['36','37','38','39','40','41','42','43'], colorCount: 2 },
    { ref: 'AN-621', name: 'Saia Plissada Midi', garment: 'skirt', tintIndex: 7, price: 'R$ 139,00', moq: 4, sizes: ['P','M','G'], colorCount: 3 },
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
