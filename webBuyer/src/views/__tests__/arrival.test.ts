import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { SessionInfo } from '../../api/sessions'
import { renderArrival, renderError, renderLanding, renderLoading } from '../arrival'

function root(): HTMLElement {
  const el = document.createElement('main')
  document.body.appendChild(el)
  return el
}

function info(overrides: Partial<SessionInfo> = {}): SessionInfo {
  return {
    token: 'abc123XY',
    sellerName: 'Atelier Norte',
    collectionLabel: 'Outono 26',
    clientName: 'Diego Albuquerque',
    clientShop: 'Trama Multimarcas',
    scheduledFor: 'Hoje · 14:00',
    productCount: 5,
    createdAtMs: 0,
    expiresAtMs: 0,
    ...overrides,
  }
}

beforeEach(() => {
  document.body.innerHTML = ''
  Object.defineProperty(navigator, 'mediaDevices', {
    configurable: true,
    value: { getUserMedia: vi.fn().mockResolvedValue({}) },
  })
  Object.defineProperty(navigator, 'userAgent', {
    configurable: true,
    value: 'Mozilla/5.0 (Macintosh) AppleWebKit/605 Safari/605',
  })
})

describe('renderLanding', () => {
  it('mostra apenas o convite quando não há último token', () => {
    const el = root()
    renderLanding(el)
    expect(el.querySelector('[data-view="landing"]')).toBeTruthy()
    expect(el.querySelector('[data-action="reopen-last"]')).toBeNull()
  })

  it('mostra botão de retomar e dispara callback', () => {
    const el = root()
    const onReopenLast = vi.fn()
    renderLanding(el, 'lastTOKEN', { onReopenLast })
    const cta = el.querySelector<HTMLButtonElement>('[data-action="reopen-last"]')
    expect(cta).toBeTruthy()
    cta!.click()
    expect(onReopenLast).toHaveBeenCalledWith('lastTOKEN')
  })

  it('escapa o token contra HTML injection', () => {
    const el = root()
    renderLanding(el, '<img src=x>')
    expect(el.innerHTML).not.toContain('<img src=x>')
    expect(el.innerHTML).toContain('&lt;img src=x&gt;')
  })
})

describe('renderLoading', () => {
  it('renderiza estado de loading com token', () => {
    const el = root()
    renderLoading(el, 'tok')
    expect(el.querySelector('[data-view="loading"]')?.getAttribute('data-token')).toBe('tok')
    expect(el.querySelector('.arrival-spinner')).toBeTruthy()
  })
})

describe('renderError', () => {
  it('mostra título por tipo e dispara retry', () => {
    const el = root()
    const onRetry = vi.fn()
    renderError(el, 'network', 'sem rede', 'tok', { onRetry })
    expect(el.querySelector('[data-view="error"]')?.getAttribute('data-kind')).toBe('network')
    expect(el.textContent).toContain('Sem conexão')
    el.querySelector<HTMLButtonElement>('[data-action="retry"]')!.click()
    expect(onRetry).toHaveBeenCalled()
  })

  it('usa o label correto para expired', () => {
    const el = root()
    renderError(el, 'expired', 'velho', 'tok')
    const button = el.querySelector<HTMLButtonElement>('[data-action="retry"]')
    expect(button?.textContent).toBe('Tentar com outro link')
  })
})

describe('renderArrival', () => {
  it('renderiza nome do vendedor, contagem e token', () => {
    const el = root()
    renderArrival(el, info())
    expect(el.textContent).toContain('Oi, Diego')
    expect(el.textContent).toContain('Atelier Norte')
    expect(el.textContent).toContain('5 produtos')
    expect(el.textContent).toContain('abc123XY')
  })

  it('usa fallback de saudação quando sem clientName', () => {
    const el = root()
    renderArrival(el, info({ clientName: null }))
    expect(el.textContent).toContain('Tudo pronto')
  })

  it('pede mic ao clicar e atualiza label quando dá certo', async () => {
    const el = root()
    const onJoinAudio = vi.fn().mockResolvedValue(undefined)
    renderArrival(el, info(), { onJoinAudio })
    const cta = el.querySelector<HTMLButtonElement>('[data-action="join"]')!
    cta.click()
    await Promise.resolve()
    await Promise.resolve()
    expect(onJoinAudio).toHaveBeenCalled()
    expect(cta.textContent).toBe('Microfone pronto — aguardando vendedor')
  })

  it('reabilita CTA quando mic é negado', async () => {
    const el = root()
    const onJoinAudio = vi.fn().mockRejectedValue(new Error('denied'))
    renderArrival(el, info(), { onJoinAudio })
    const cta = el.querySelector<HTMLButtonElement>('[data-action="join"]')!
    cta.click()
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()
    expect(cta.disabled).toBe(false)
    expect(cta.textContent).toBe('Tentar de novo')
  })

  it('escapa scheduledFor', () => {
    const el = root()
    renderArrival(el, info({ scheduledFor: '<b>14:00</b>' }))
    expect(el.innerHTML).toContain('&lt;b&gt;14:00&lt;/b&gt;')
  })
})
