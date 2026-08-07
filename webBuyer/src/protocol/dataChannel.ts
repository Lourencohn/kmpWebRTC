export type CatalogRoute =
  | { type: 'inicio' }
  | { type: 'menu' }
  | { type: 'todos' }
  | { type: 'secao'; tabela: string; tabelaId: string }
  | { type: 'carrinho' }
  | { type: 'favoritos' }

export type ProductFocus = {
  produtoPreId: number
  produtoPre1Id?: number
  complemento1Id?: number
}

export type ViewState = {
  route: CatalogRoute
  query?: Record<string, string>
  focus?: ProductFocus
}

export type ScrollAnchor = {
  page?: number
  produtoPreId?: number
  itemOffsetRatio?: number
  viewportRatio?: number
}

export type CartChangeReason =
  | 'itemAdded'
  | 'itemRemoved'
  | 'quantityChanged'
  | 'prazoChanged'
  | 'cleared'
  | 'finalized'

export type CartChangeHint = {
  produtoPreId?: number
  unitsDelta?: number
  label?: string
}

export type DataChannelMessage =
  | { type: 'mute'; muted: boolean; ts: number; from: string }
  | { type: 'navigate'; view: ViewState; ts: number; from: string }
  | { type: 'scroll'; anchor: ScrollAnchor; ts: number; from: string }
  | {
      type: 'pointAt'
      target: string
      xRatio?: number
      yRatio?: number
      ts: number
      from: string
      durationMs?: number
    }
  | {
      type: 'cartInvalidated'
      carrinhoId: number
      reason: CartChangeReason
      ts: number
      from: string
      hint?: CartChangeHint
    }
  | { type: 'orderPlaced'; carrinhoId: number; ts: number; from: string; pedidoId?: string }

export const SYNCED_QUERY_KEYS = [
  'categoria',
  'grupo_produto',
  'subgrupo_produto',
  'marca',
  'search',
  'page',
  'total',
  'sort',
  'direction',
] as const

export function retainSyncedQuery(query: Record<string, string>): Record<string, string> {
  const kept: Record<string, string> = {}
  for (const key of SYNCED_QUERY_KEYS) {
    const value = query[key]
    if (value != null && value !== '') kept[key] = value
  }
  return kept
}

export function productAnchor(produtoPreId: number, complemento1Id?: number): string {
  return complemento1Id == null
    ? `produto:${produtoPreId}`
    : `produto:${produtoPreId}:cor:${complemento1Id}`
}

export function cartItemAnchor(itemId: number): string {
  return `carrinho:item:${itemId}`
}

export function actionAnchor(name: string): string {
  return `acao:${name}`
}

export function produtoPreIdOfAnchor(target: string): number | null {
  const parts = target.split(':')
  if (parts.length < 2 || parts[0] !== 'produto') return null
  const id = Number(parts[1])
  return Number.isFinite(id) ? id : null
}

export function encodeDataChannelMessage(message: DataChannelMessage): string {
  return JSON.stringify(message)
}

const CART_CHANGE_REASONS: readonly string[] = [
  'itemAdded',
  'itemRemoved',
  'quantityChanged',
  'prazoChanged',
  'cleared',
  'finalized',
]

function isFiniteNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

function optionalNumber(value: unknown): number | undefined {
  return isFiniteNumber(value) ? value : undefined
}

function parseRoute(raw: unknown): CatalogRoute | null {
  if (!raw || typeof raw !== 'object') return null
  const route = raw as Record<string, unknown>
  switch (route.type) {
    case 'inicio':
    case 'menu':
    case 'todos':
    case 'carrinho':
    case 'favoritos':
      return { type: route.type }
    case 'secao':
      if (typeof route.tabela === 'string' && typeof route.tabelaId === 'string') {
        return { type: 'secao', tabela: route.tabela, tabelaId: route.tabelaId }
      }
      return null
    default:
      return null
  }
}

function parseQuery(raw: unknown): Record<string, string> | undefined {
  if (!raw || typeof raw !== 'object') return undefined
  const entries = Object.entries(raw as Record<string, unknown>).filter(
    (entry): entry is [string, string] => typeof entry[1] === 'string',
  )
  return entries.length > 0 ? Object.fromEntries(entries) : undefined
}

function parseFocus(raw: unknown): ProductFocus | undefined {
  if (!raw || typeof raw !== 'object') return undefined
  const focus = raw as Record<string, unknown>
  if (!isFiniteNumber(focus.produtoPreId)) return undefined
  return {
    produtoPreId: focus.produtoPreId,
    produtoPre1Id: optionalNumber(focus.produtoPre1Id),
    complemento1Id: optionalNumber(focus.complemento1Id),
  }
}

function parseView(raw: unknown): ViewState | null {
  if (!raw || typeof raw !== 'object') return null
  const view = raw as Record<string, unknown>
  const route = parseRoute(view.route)
  if (!route) return null
  return { route, query: parseQuery(view.query), focus: parseFocus(view.focus) }
}

function parseAnchor(raw: unknown): ScrollAnchor | null {
  if (!raw || typeof raw !== 'object') return null
  const anchor = raw as Record<string, unknown>
  return {
    page: optionalNumber(anchor.page),
    produtoPreId: optionalNumber(anchor.produtoPreId),
    itemOffsetRatio: optionalNumber(anchor.itemOffsetRatio),
    viewportRatio: optionalNumber(anchor.viewportRatio),
  }
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
  if (typeof obj.from !== 'string' || !isFiniteNumber(obj.ts)) return null
  const ts = obj.ts
  const from = obj.from

  switch (obj.type) {
    case 'mute':
      if (typeof obj.muted !== 'boolean') return null
      return { type: 'mute', muted: obj.muted, ts, from }

    case 'navigate': {
      const view = parseView(obj.view)
      if (!view) return null
      return { type: 'navigate', view, ts, from }
    }

    case 'scroll': {
      const anchor = parseAnchor(obj.anchor)
      if (!anchor) return null
      return { type: 'scroll', anchor, ts, from }
    }

    case 'pointAt':
      if (typeof obj.target !== 'string' || obj.target === '') return null
      return {
        type: 'pointAt',
        target: obj.target,
        xRatio: optionalNumber(obj.xRatio),
        yRatio: optionalNumber(obj.yRatio),
        ts,
        from,
        durationMs: optionalNumber(obj.durationMs),
      }

    case 'cartInvalidated': {
      if (!isFiniteNumber(obj.carrinhoId)) return null
      if (typeof obj.reason !== 'string' || !CART_CHANGE_REASONS.includes(obj.reason)) return null
      const rawHint = obj.hint
      const hint =
        rawHint && typeof rawHint === 'object'
          ? {
              produtoPreId: optionalNumber((rawHint as Record<string, unknown>).produtoPreId),
              unitsDelta: optionalNumber((rawHint as Record<string, unknown>).unitsDelta),
              label:
                typeof (rawHint as Record<string, unknown>).label === 'string'
                  ? ((rawHint as Record<string, unknown>).label as string)
                  : undefined,
            }
          : undefined
      return {
        type: 'cartInvalidated',
        carrinhoId: obj.carrinhoId,
        reason: obj.reason as CartChangeReason,
        ts,
        from,
        hint,
      }
    }

    case 'orderPlaced':
      if (!isFiniteNumber(obj.carrinhoId)) return null
      return {
        type: 'orderPlaced',
        carrinhoId: obj.carrinhoId,
        ts,
        from,
        pedidoId: typeof obj.pedidoId === 'string' ? obj.pedidoId : undefined,
      }

    default:
      return null
  }
}
