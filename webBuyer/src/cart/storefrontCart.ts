import type { SnapshotProduct } from '../data/snapshot'

export type CartEntry = {
  productId: number
  sizeId: number | null
  sizeLabel: string
  qty: number
  updatedAtMs: number
}

export type CartState = {
  lines: CartEntry[]
  totalUnits: number
  totalCents: number
}

type CartListener = (state: CartState) => void

function lineKey(productId: number, sizeId: number | null): string {
  return `${productId}/${sizeId ?? '-'}`
}

export class StorefrontCart {
  private readonly lines = new Map<string, CartEntry>()
  private readonly listeners = new Set<CartListener>()

  constructor(private readonly products: Map<number, SnapshotProduct>) {}

  set(productId: number, sizeId: number | null, sizeLabel: string, qty: number, updatedAtMs: number): void {
    const key = lineKey(productId, sizeId)
    if (qty <= 0) {
      this.lines.delete(key)
    } else {
      this.lines.set(key, { productId, sizeId, sizeLabel, qty, updatedAtMs })
    }
    this.emit()
  }

  linesFor(productId: number): CartEntry[] {
    return Array.from(this.lines.values()).filter((l) => l.productId === productId)
  }

  state(): CartState {
    const lines = Array.from(this.lines.values()).sort((a, b) => a.updatedAtMs - b.updatedAtMs)
    const totalUnits = lines.reduce((sum, l) => sum + l.qty, 0)
    const totalCents = lines.reduce((sum, l) => sum + this.priceCentsFor(l.productId) * l.qty, 0)
    return { lines, totalUnits, totalCents }
  }

  subscribe(listener: CartListener): () => void {
    this.listeners.add(listener)
    listener(this.state())
    return () => this.listeners.delete(listener)
  }

  private priceCentsFor(productId: number): number {
    return this.products.get(productId)?.priceCents ?? 0
  }

  private emit(): void {
    const state = this.state()
    this.listeners.forEach((l) => l(state))
  }
}
