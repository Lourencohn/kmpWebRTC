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
})
