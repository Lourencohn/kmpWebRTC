import type { CartLine, SnapshotProduct } from '../data/snapshot'

export type CartEntry = {
  productId: number
  sizeId: number | null
  sizeLabel: string
  qty: number
  updatedAtMs: number
}

export type CartSnapshotView = {
  lines: CartEntry[]
  totalUnits: number
  totalCents: number
}

export type CartListener = (view: CartSnapshotView) => void

function lineKey(productId: number, sizeId: number | null): string {
  return `${productId}/${sizeId ?? '-'}`
}

export class VitrineCart {
  private readonly lines = new Map<string, CartEntry>()
  private readonly listeners = new Set<CartListener>()

  constructor(private readonly products: Map<number, SnapshotProduct>) {}

  priceCentsFor(productId: number): number {
    return this.products.get(productId)?.priceCents ?? 0
  }

  set(productId: number, sizeId: number | null, sizeLabel: string, qty: number, updatedAtMs: number): void {
    const key = lineKey(productId, sizeId)
    if (qty <= 0) {
      this.lines.delete(key)
    } else {
      this.lines.set(key, { productId, sizeId, sizeLabel, qty, updatedAtMs })
    }
    this.emit()
  }

  qtyFor(productId: number, sizeId: number | null): number {
    return this.lines.get(lineKey(productId, sizeId))?.qty ?? 0
  }

  linesFor(productId: number): CartEntry[] {
    return Array.from(this.lines.values()).filter((l) => l.productId === productId)
  }

  snapshot(): CartSnapshotView {
    const lines = Array.from(this.lines.values()).sort((a, b) => a.updatedAtMs - b.updatedAtMs)
    const totalUnits = lines.reduce((sum, l) => sum + l.qty, 0)
    const totalCents = lines.reduce((sum, l) => sum + this.priceCentsFor(l.productId) * l.qty, 0)
    return { lines, totalUnits, totalCents }
  }

  draftLines(): CartLine[] {
    return this.snapshot().lines.map((l) => ({
      productId: l.productId,
      sizeId: l.sizeId,
      sizeLabel: l.sizeLabel,
      qty: l.qty,
    }))
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
