import { SampleCatalog, type Product } from '../data/catalog'

export type CartLine = {
  productId: string
  size: string
  units: number
  updatedAtMs: number
}

export type CartSnapshot = {
  lines: CartLine[]
  totalUnits: number
  totalCents: number
}

export type CartListener = (snapshot: CartSnapshot) => void

function lineKey(productId: string, size: string): string {
  return `${productId}/${size}`
}

function parsePriceCents(price: string): number {
  const numeric = price
    .replace(/[^\d,.]/g, '')
    .replace(/\./g, '')
    .replace(',', '.')
  const value = Number.parseFloat(numeric)
  if (!Number.isFinite(value)) return 0
  return Math.round(value * 100)
}

function priceCentsFor(productId: string): number {
  const product = SampleCatalog.products.find((p: Product) => p.ref === productId)
  return product ? parsePriceCents(product.price) : 0
}

export class CartStore {
  private readonly lines = new Map<string, CartLine>()
  private readonly listeners = new Set<CartListener>()

  apply(productId: string, size: string, units: number, updatedAtMs: number): CartLine | null {
    const key = lineKey(productId, size)
    if (units <= 0) {
      const removed = this.lines.get(key) ?? null
      if (removed) this.lines.delete(key)
      this.emit()
      return removed
    }
    const line: CartLine = { productId, size, units, updatedAtMs }
    this.lines.set(key, line)
    this.emit()
    return line
  }

  get(productId: string, size: string): CartLine | null {
    return this.lines.get(lineKey(productId, size)) ?? null
  }

  linesFor(productId: string): CartLine[] {
    return Array.from(this.lines.values()).filter((l) => l.productId === productId)
  }

  snapshot(): CartSnapshot {
    const lines = Array.from(this.lines.values()).sort(
      (a, b) => a.updatedAtMs - b.updatedAtMs,
    )
    const totalUnits = lines.reduce((sum, l) => sum + l.units, 0)
    const totalCents = lines.reduce(
      (sum, l) => sum + priceCentsFor(l.productId) * l.units,
      0,
    )
    return { lines, totalUnits, totalCents }
  }

  subscribe(listener: CartListener): () => void {
    this.listeners.add(listener)
    listener(this.snapshot())
    return () => this.listeners.delete(listener)
  }

  private emit(): void {
    const snap = this.snapshot()
    this.listeners.forEach((l) => l(snap))
  }
}

export function formatBrl(cents: number): string {
  const whole = Math.floor(cents / 100)
  const fractional = String(cents % 100).padStart(2, '0')
  const wholeWithDots = whole
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return `R$ ${wholeWithDots},${fractional}`
}

export function unitPriceCentsFor(productId: string): number {
  return priceCentsFor(productId)
}
