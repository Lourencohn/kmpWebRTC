import { describe, expect, it } from 'vitest'
import {
  decodeDataChannelMessage,
  encodeDataChannelMessage,
  type DataChannelMessage,
} from '../dataChannel'

describe('dataChannel codec', () => {
  it('roundtrips mute', () => {
    const original: DataChannelMessage = { type: 'mute', muted: true, from: 'seller-1' }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('roundtrips scroll', () => {
    const original: DataChannelMessage = {
      type: 'scroll',
      productId: 'p-1',
      offset: 0.42,
      ts: 1700000000000,
      from: 'seller-1',
    }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('roundtrips pointAt with explicit durationMs', () => {
    const original: DataChannelMessage = {
      type: 'pointAt',
      productId: 'p-2',
      ts: 1700000000000,
      from: 'seller-1',
      durationMs: 4000,
    }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('decodes pointAt without durationMs as undefined', () => {
    const raw = '{"type":"pointAt","productId":"p","ts":1,"from":"x"}'
    expect(decodeDataChannelMessage(raw)).toEqual({
      type: 'pointAt',
      productId: 'p',
      ts: 1,
      from: 'x',
      durationMs: undefined,
    })
  })

  it('rejects malformed json', () => {
    expect(decodeDataChannelMessage('not json')).toBeNull()
  })

  it('rejects unknown discriminator', () => {
    expect(decodeDataChannelMessage('{"type":"ghost"}')).toBeNull()
  })

  it('rejects scroll with wrong field types', () => {
    expect(
      decodeDataChannelMessage('{"type":"scroll","productId":1,"offset":"x","ts":0,"from":"y"}'),
    ).toBeNull()
  })

  it('roundtrips navigate', () => {
    const original: DataChannelMessage = {
      type: 'navigate',
      productId: 'AN-088',
      ts: 1_700_000_000_500,
      from: 'buyer-xyz',
    }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('roundtrips cartUpdate', () => {
    const original: DataChannelMessage = {
      type: 'cartUpdate',
      productId: 'AN-104',
      size: 'M',
      units: 12,
      ts: 1_700_000_000_750,
      from: 'buyer-xyz',
    }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('cartUpdate with zero units decodes', () => {
    const raw =
      '{"type":"cartUpdate","productId":"AN-104","size":"G","units":0,"ts":1,"from":"buyer"}'
    const decoded = decodeDataChannelMessage(raw)
    expect(decoded).toEqual({
      type: 'cartUpdate',
      productId: 'AN-104',
      size: 'G',
      units: 0,
      ts: 1,
      from: 'buyer',
    })
  })

  it('rejects cartUpdate without size', () => {
    const raw = '{"type":"cartUpdate","productId":"p","units":1,"ts":1,"from":"x"}'
    expect(decodeDataChannelMessage(raw)).toBeNull()
  })

  it('roundtrips orderConfirm with multiple lines', () => {
    const original: DataChannelMessage = {
      type: 'orderConfirm',
      orderId: 'ORD-abc-XYZ',
      ts: 1_700_000_000_900,
      from: 'seller-1',
      lines: [
        { productId: 'AN-104', size: 'M', units: 12, unitPriceCents: 8990 },
        { productId: 'AN-088', size: 'G', units: 4, unitPriceCents: 15900 },
      ],
      totalCents: 12 * 8990 + 4 * 15900,
    }
    expect(decodeDataChannelMessage(encodeDataChannelMessage(original))).toEqual(original)
  })

  it('orderConfirm with empty lines decodes', () => {
    const raw =
      '{"type":"orderConfirm","orderId":"ORD-empty","ts":1,"from":"seller","lines":[],"totalCents":0}'
    const decoded = decodeDataChannelMessage(raw)
    expect(decoded).toMatchObject({ type: 'orderConfirm', lines: [], totalCents: 0 })
  })

  it('rejects orderConfirm with malformed line', () => {
    const raw =
      '{"type":"orderConfirm","orderId":"X","ts":1,"from":"s","totalCents":0,"lines":[{"productId":"p","size":"M","units":"abc","unitPriceCents":1}]}'
    expect(decodeDataChannelMessage(raw)).toBeNull()
  })
})
