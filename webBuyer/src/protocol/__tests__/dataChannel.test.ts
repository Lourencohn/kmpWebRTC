import { describe, expect, it } from 'vitest'
import {
  actionAnchor,
  decodeDataChannelMessage,
  encodeDataChannelMessage,
  productAnchor,
  produtoPreIdOfAnchor,
  retainSyncedQuery,
  type DataChannelMessage,
} from '../dataChannel'

describe('dataChannel codec', () => {
  it('roundtrips mute', () => {
    const original: DataChannelMessage = { type: 'mute', muted: true, ts: 1, from: 'seller-1' }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('roundtrips scroll anchored on a product', () => {
    const original: DataChannelMessage = {
      type: 'scroll',
      anchor: { page: 2, produtoPreId: 8813, itemOffsetRatio: 0.42, viewportRatio: 0.31 },
      ts: 1700000000000,
      from: 'seller-1',
    }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('decodes scroll without product as viewport ratio only', () => {
    const raw = '{"type":"scroll","anchor":{"viewportRatio":0.75},"ts":1,"from":"buyer"}'
    expect(decodeDataChannelMessage(raw)).toEqual({
      type: 'scroll',
      anchor: {
        page: undefined,
        produtoPreId: undefined,
        itemOffsetRatio: undefined,
        viewportRatio: 0.75,
      },
      ts: 1,
      from: 'buyer',
    })
  })

  it('roundtrips pointAt targeting a product', () => {
    const original: DataChannelMessage = {
      type: 'pointAt',
      target: productAnchor(8813, 44),
      xRatio: 0.2,
      yRatio: 0.8,
      ts: 1700000000000,
      from: 'seller-1',
      durationMs: 4000,
    }
    const decoded = decodeDataChannelMessage(encodeDataChannelMessage(original))
    expect(decoded).toEqual(original)
    expect(produtoPreIdOfAnchor(productAnchor(8813, 44))).toBe(8813)
    expect(produtoPreIdOfAnchor(actionAnchor('finalizar'))).toBeNull()
  })

  it('rejects pointAt without target', () => {
    expect(decodeDataChannelMessage('{"type":"pointAt","ts":1,"from":"x"}')).toBeNull()
  })

  it('roundtrips navigate with route, query and focus', () => {
    const original: DataChannelMessage = {
      type: 'navigate',
      view: {
        route: { type: 'secao', tabela: 'grupo_produto', tabelaId: '17' },
        query: { search: 'camisa', page: '2' },
        focus: { produtoPreId: 8813, produtoPre1Id: 4410, complemento1Id: 44 },
      },
      ts: 1_700_000_000_500,
      from: 'buyer-xyz',
    }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('roundtrips navigate to a plain route', () => {
    const raw = '{"type":"navigate","view":{"route":{"type":"carrinho"}},"ts":1,"from":"seller"}'
    expect(decodeDataChannelMessage(raw)).toEqual({
      type: 'navigate',
      view: { route: { type: 'carrinho' }, query: undefined, focus: undefined },
      ts: 1,
      from: 'seller',
    })
  })

  it('rejects navigate with unknown route', () => {
    const raw = '{"type":"navigate","view":{"route":{"type":"checkout"}},"ts":1,"from":"x"}'
    expect(decodeDataChannelMessage(raw)).toBeNull()
  })

  it('roundtrips cartInvalidated', () => {
    const original: DataChannelMessage = {
      type: 'cartInvalidated',
      carrinhoId: 90112,
      reason: 'itemAdded',
      ts: 1_700_000_000_750,
      from: 'buyer-xyz',
      hint: { produtoPreId: 8813, unitsDelta: 12, label: 'Camisa Linho' },
    }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('rejects cartInvalidated with unknown reason', () => {
    const raw = '{"type":"cartInvalidated","carrinhoId":1,"reason":"explodiu","ts":1,"from":"x"}'
    expect(decodeDataChannelMessage(raw)).toBeNull()
  })

  it('rejects cartInvalidated without carrinhoId', () => {
    const raw = '{"type":"cartInvalidated","reason":"cleared","ts":1,"from":"x"}'
    expect(decodeDataChannelMessage(raw)).toBeNull()
  })

  it('roundtrips orderPlaced', () => {
    const original: DataChannelMessage = {
      type: 'orderPlaced',
      carrinhoId: 90112,
      ts: 1_700_000_000_900,
      from: 'seller-1',
      pedidoId: 'PED-2026-4471',
    }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('rejects malformed json', () => {
    expect(decodeDataChannelMessage('not json')).toBeNull()
  })

  it('rejects unknown discriminator', () => {
    expect(decodeDataChannelMessage('{"type":"ghost","ts":1,"from":"x"}')).toBeNull()
  })

  it('rejects the retired cartUpdate shape', () => {
    const raw =
      '{"type":"cartUpdate","productId":"AN-104","size":"M","units":12,"ts":1,"from":"buyer"}'
    expect(decodeDataChannelMessage(raw)).toBeNull()
  })

  it('never syncs the live token through the query', () => {
    const synced = retainSyncedQuery({
      search: 'camisa',
      page: '3',
      live: 'kP3xq9Trz',
      token: 'jwt-legado',
      utm_source: 'whatsapp',
    })
    expect(synced).toEqual({ search: 'camisa', page: '3' })
  })
})
