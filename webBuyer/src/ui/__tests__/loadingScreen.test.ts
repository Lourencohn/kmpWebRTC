import { beforeEach, describe, expect, it } from 'vitest'
import { renderLoading } from '../loadingScreen'

describe('renderLoading', () => {
  let root: HTMLElement

  beforeEach(() => {
    document.body.innerHTML = '<div id="app"></div>'
    root = document.getElementById('app')!
  })

  it('shows the loading state with the session token', () => {
    renderLoading(root, 'abc123')

    const section = root.querySelector<HTMLElement>('[data-view="loading"]')
    expect(section).not.toBeNull()
    expect(section!.dataset.token).toBe('abc123')
    expect(root.textContent).toContain('Carregando sessão')
  })

  it('escapes the token so it cannot inject markup', () => {
    renderLoading(root, '"><img src=x onerror=alert(1)>')

    expect(root.querySelector('img[src="x"]')).toBeNull()
    const section = root.querySelector<HTMLElement>('[data-view="loading"]')
    expect(section!.dataset.token).toContain('<img')
  })
})
