export type OrderLine = {
  productId: string
  size: string
  units: number
  unitPriceCents: number
}

export type DataChannelMessage =
  | { type: 'mute'; muted: boolean; from: string }
  | { type: 'scroll'; productId: string; offset: number; ts: number; from: string }
  | { type: 'pointAt'; productId: string; ts: number; from: string; durationMs?: number }
  | { type: 'navigate'; productId: string; ts: number; from: string }
  | { type: 'cartUpdate'; productId: string; size: string; units: number; ts: number; from: string }
  | {
      type: 'orderConfirm'
      orderId: string
      ts: number
      from: string
      lines: OrderLine[]
      totalCents: number
    }

export function encodeDataChannelMessage(message: DataChannelMessage): string {
  return JSON.stringify(message)
}

export function decodeDataChannelMessage(raw: string): DataChannelMessage | null {
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    return null
  }
  if (!parsed || typeof parsed !== 'object') return null
  const obj = parsed as Record<string, unknown>
  switch (obj.type) {
    case 'mute':
      if (typeof obj.muted === 'boolean' && typeof obj.from === 'string') {
        return { type: 'mute', muted: obj.muted, from: obj.from }
      }
      return null
    case 'scroll':
      if (
        typeof obj.productId === 'string' &&
        typeof obj.offset === 'number' &&
        typeof obj.ts === 'number' &&
        typeof obj.from === 'string'
      ) {
        return {
          type: 'scroll',
          productId: obj.productId,
          offset: obj.offset,
          ts: obj.ts,
          from: obj.from,
        }
      }
      return null
    case 'pointAt':
      if (
        typeof obj.productId === 'string' &&
        typeof obj.ts === 'number' &&
        typeof obj.from === 'string'
      ) {
        return {
          type: 'pointAt',
          productId: obj.productId,
          ts: obj.ts,
          from: obj.from,
          durationMs: typeof obj.durationMs === 'number' ? obj.durationMs : undefined,
        }
      }
      return null
    case 'navigate':
      if (
        typeof obj.productId === 'string' &&
        typeof obj.ts === 'number' &&
        typeof obj.from === 'string'
      ) {
        return { type: 'navigate', productId: obj.productId, ts: obj.ts, from: obj.from }
      }
      return null
    case 'cartUpdate':
      if (
        typeof obj.productId === 'string' &&
        typeof obj.size === 'string' &&
        typeof obj.units === 'number' &&
        typeof obj.ts === 'number' &&
        typeof obj.from === 'string'
      ) {
        return {
          type: 'cartUpdate',
          productId: obj.productId,
          size: obj.size,
          units: obj.units,
          ts: obj.ts,
          from: obj.from,
        }
      }
      return null
    case 'orderConfirm':
      if (
        typeof obj.orderId === 'string' &&
        typeof obj.ts === 'number' &&
        typeof obj.from === 'string' &&
        typeof obj.totalCents === 'number' &&
        Array.isArray(obj.lines)
      ) {
        const lines: OrderLine[] = []
        for (const raw of obj.lines as unknown[]) {
          if (!raw || typeof raw !== 'object') return null
          const line = raw as Record<string, unknown>
          if (
            typeof line.productId !== 'string' ||
            typeof line.size !== 'string' ||
            typeof line.units !== 'number' ||
            typeof line.unitPriceCents !== 'number'
          ) {
            return null
          }
          lines.push({
            productId: line.productId,
            size: line.size,
            units: line.units,
            unitPriceCents: line.unitPriceCents,
          })
        }
        return {
          type: 'orderConfirm',
          orderId: obj.orderId,
          ts: obj.ts,
          from: obj.from,
          lines,
          totalCents: obj.totalCents,
        }
      }
      return null
    default:
      return null
  }
}
