export type DataChannelMessage =
  | { type: 'mute'; muted: boolean; from: string }
  | { type: 'scroll'; productId: string; offset: number; ts: number; from: string }
  | { type: 'pointAt'; productId: string; ts: number; from: string; durationMs?: number }

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
    default:
      return null
  }
}
