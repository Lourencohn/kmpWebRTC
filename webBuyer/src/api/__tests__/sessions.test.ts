import { describe, expect, it, vi } from 'vitest'
import { SessionFetchError, fetchSession, type SessionInfo } from '../sessions'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function sampleInfo(overrides: Partial<SessionInfo> = {}): SessionInfo {
  return {
    token: 'abc123XY',
    empresaSlug: 'atelier-norte',
    catalogoUuid: '5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f',
    sellerName: 'Marina Prado',
    catalogoNome: 'Outono 26',
    clientName: 'Diego Albuquerque',
    createdAtMs: 1_700_000_000_000,
    expiresAtMs: 1_700_014_400_000,
    ...overrides,
  }
}

describe('fetchSession', () => {
  it('devolve SessionInfo no caminho feliz', async () => {
    const info = sampleInfo()
    const fetchImpl = vi.fn().mockResolvedValue(jsonResponse(200, info))
    const result = await fetchSession('http://srv', 'abc', {
      fetchImpl: fetchImpl as unknown as typeof fetch,
      now: () => info.expiresAtMs - 1,
    })
    expect(result).toEqual(info)
    expect(fetchImpl).toHaveBeenCalledWith(
      'http://srv/session/abc',
      expect.objectContaining({ headers: { Accept: 'application/json' } }),
    )
  })

  it('remove trailing slash do baseUrl', async () => {
    const info = sampleInfo()
    const fetchImpl = vi.fn().mockResolvedValue(jsonResponse(200, info))
    await fetchSession('http://srv/', 'abc', {
      fetchImpl: fetchImpl as unknown as typeof fetch,
      now: () => info.expiresAtMs - 1,
    })
    expect(fetchImpl).toHaveBeenCalledWith('http://srv/session/abc', expect.any(Object))
  })

  it('mapeia 404 para not_found', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      jsonResponse(404, { code: 'not_found', message: 'x' }),
    )
    await expect(
      fetchSession('http://srv', 'zzz', { fetchImpl: fetchImpl as unknown as typeof fetch }),
    ).rejects.toMatchObject({ kind: 'not_found', status: 404 })
  })

  it('mapeia 500 para server', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(jsonResponse(500, { code: 'oops' }))
    await expect(
      fetchSession('http://srv', 'abc', { fetchImpl: fetchImpl as unknown as typeof fetch }),
    ).rejects.toMatchObject({ kind: 'server', status: 500 })
  })

  it('mapeia erro de rede para network', async () => {
    const fetchImpl = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'))
    await expect(
      fetchSession('http://srv', 'abc', { fetchImpl: fetchImpl as unknown as typeof fetch }),
    ).rejects.toMatchObject({ kind: 'network' })
  })

  it('mapeia JSON inválido para malformed', async () => {
    const broken = new Response('not-json', {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
    const fetchImpl = vi.fn().mockResolvedValue(broken)
    await expect(
      fetchSession('http://srv', 'abc', { fetchImpl: fetchImpl as unknown as typeof fetch }),
    ).rejects.toMatchObject({ kind: 'malformed' })
  })

  it('mapeia payload com schema errado para malformed', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(jsonResponse(200, { hello: 'world' }))
    await expect(
      fetchSession('http://srv', 'abc', { fetchImpl: fetchImpl as unknown as typeof fetch }),
    ).rejects.toMatchObject({ kind: 'malformed' })
  })

  it('detecta sessão expirada via expiresAtMs', async () => {
    const info = sampleInfo({ expiresAtMs: 1_000 })
    const fetchImpl = vi.fn().mockResolvedValue(jsonResponse(200, info))
    await expect(
      fetchSession('http://srv', 'abc', {
        fetchImpl: fetchImpl as unknown as typeof fetch,
        now: () => 9_999,
      }),
    ).rejects.toBeInstanceOf(SessionFetchError)
  })
})
