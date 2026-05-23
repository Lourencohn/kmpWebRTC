import { describe, expect, it, vi } from 'vitest'
import { submitOrder, type OrderSubmissionRequest } from '../orders'

function sample(): OrderSubmissionRequest {
  return {
    orderId: 'ORD-x',
    sessionToken: 'abc123',
    tsMs: 1700000000000,
    totalCents: 8990,
    lines: [
      { productId: 'AN-104', size: 'M', units: 1, unitPriceCents: 8990 },
    ],
    source: 'Buyer',
    clientName: 'Diego',
  }
}

describe('submitOrder', () => {
  it('returns ok with receivedAtMs on success', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ orderId: 'ORD-x', receivedAtMs: 99 }), {
        status: 202,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    const result = await submitOrder('http://localhost:8080', sample(), { fetchImpl })
    expect(result).toEqual({ ok: true, receivedAtMs: 99 })
    expect(fetchImpl).toHaveBeenCalledOnce()
    const [url, init] = fetchImpl.mock.calls[0]
    expect(url).toBe('http://localhost:8080/order')
    expect((init as RequestInit).method).toBe('POST')
  })

  it('strips trailing slash from baseUrl', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ orderId: 'ORD-x', receivedAtMs: 1 }), { status: 202 }),
    )
    await submitOrder('http://localhost:8080/', sample(), { fetchImpl })
    expect(fetchImpl.mock.calls[0][0]).toBe('http://localhost:8080/order')
  })

  it('maps 404 to session_unknown', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(new Response('', { status: 404 }))
    const result = await submitOrder('http://localhost:8080', sample(), { fetchImpl })
    expect(result).toEqual({
      ok: false,
      kind: 'session_unknown',
      message: expect.any(String),
    })
  })

  it('maps 400 to invalid_payload', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(new Response('', { status: 400 }))
    const result = await submitOrder('http://localhost:8080', sample(), { fetchImpl })
    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.kind).toBe('invalid_payload')
  })

  it('maps 500 to server', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(new Response('', { status: 500 }))
    const result = await submitOrder('http://localhost:8080', sample(), { fetchImpl })
    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.kind).toBe('server')
  })

  it('returns network kind on fetch throw', async () => {
    const fetchImpl = vi.fn().mockRejectedValue(new Error('offline'))
    const result = await submitOrder('http://localhost:8080', sample(), { fetchImpl })
    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.kind).toBe('network')
  })

  it('returns server kind on malformed json', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      new Response('not-json{', {
        status: 202,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    const result = await submitOrder('http://localhost:8080', sample(), { fetchImpl })
    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.kind).toBe('server')
  })

  it('returns server kind on schema mismatch', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ foo: 'bar' }), { status: 202 }),
    )
    const result = await submitOrder('http://localhost:8080', sample(), { fetchImpl })
    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.kind).toBe('server')
  })
})
