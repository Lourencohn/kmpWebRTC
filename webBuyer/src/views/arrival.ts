import { SampleCatalog } from '../data/catalog'
import { renderProductCard } from '../ui/productCard'
import type { SessionFetchErrorKind, SessionInfo } from '../api/sessions'

export type ArrivalActions = {
  onJoinAudio?: () => void | Promise<void>
  onRetry?: () => void
  onReopenLast?: (token: string) => void
}

function wordmark(): string {
  return `
    <div class="wordmark" aria-label="Trovata">
      <img class="wordmark-img" src="/trovata-logo.png" alt="Trovata" />
    </div>
  `
}

export function renderLanding(
  root: HTMLElement,
  lastToken?: string,
  actions: ArrivalActions = {},
): void {
  root.innerHTML = `
    <section class="arrival arrival--landing" data-view="landing">
      ${wordmark()}
      <p class="arrival-text">
        Abra o link enviado pelo seu vendedor para entrar na sessão.
      </p>
      ${
        lastToken
          ? `
        <button class="arrival-cta arrival-cta--ghost" type="button" data-action="reopen-last">
          Voltar para a última sessão
        </button>
        <p class="arrival-hint">Token: <code>${escapeHtml(lastToken)}</code></p>
        `
          : ''
      }
    </section>
  `
  if (lastToken && actions.onReopenLast) {
    root
      .querySelector<HTMLButtonElement>('[data-action="reopen-last"]')
      ?.addEventListener('click', () => actions.onReopenLast?.(lastToken))
  }
}

export function renderLoading(root: HTMLElement, token: string): void {
  root.innerHTML = `
    <section class="arrival arrival--loading" data-view="loading" data-token="${escapeHtml(token)}">
      ${wordmark()}
      <p class="arrival-text">Carregando sessão…</p>
      <div class="arrival-spinner" aria-hidden="true"></div>
    </section>
  `
}

export function renderError(
  root: HTMLElement,
  kind: SessionFetchErrorKind,
  message: string,
  token: string,
  actions: ArrivalActions = {},
): void {
  const titleByKind: Record<SessionFetchErrorKind, string> = {
    network: 'Sem conexão',
    not_found: 'Sessão indisponível',
    expired: 'Convite expirado',
    server: 'Servidor instável',
    malformed: 'Resposta confusa',
  }
  const retryLabel = kind === 'network' || kind === 'server' ? 'Tentar novamente' : 'Tentar com outro link'
  root.innerHTML = `
    <section class="arrival arrival--error" data-view="error" data-kind="${kind}">
      ${wordmark()}
      <h1 class="arrival-title">${escapeHtml(titleByKind[kind])}</h1>
      <p class="arrival-text">${escapeHtml(message)}</p>
      <p class="arrival-hint">Token: <code>${escapeHtml(token)}</code></p>
      <button class="arrival-cta" type="button" data-action="retry">${escapeHtml(retryLabel)}</button>
    </section>
  `
  root
    .querySelector<HTMLButtonElement>('[data-action="retry"]')
    ?.addEventListener('click', () => actions.onRetry?.())
}

export function renderArrival(
  root: HTMLElement,
  info: SessionInfo,
  actions: ArrivalActions = {},
): void {
  const firstName = info.clientName?.split(' ')[0]
  const greeting = firstName ? `Oi, ${firstName}` : 'Tudo pronto'
  const sellerLine = info.clientShop
    ? `${info.sellerName} chamou você de <strong>${escapeHtml(info.clientShop)}</strong>`
    : `${info.sellerName} está te esperando`
  root.innerHTML = `
    <section class="arrival" data-view="arrival" data-token="${escapeHtml(info.token)}">
      ${wordmark()}
      <span class="arrival-eyebrow">${escapeHtml(info.collectionLabel)}</span>
      <h1 class="arrival-title">${escapeHtml(greeting)}</h1>
      <p class="arrival-text">${sellerLine}.</p>
      <ul class="arrival-meta">
        <li><span>Sessão</span><code>${escapeHtml(info.token)}</code></li>
        <li><span>Catálogo</span>${info.productCount} produtos</li>
        ${info.scheduledFor ? `<li><span>Horário</span>${escapeHtml(info.scheduledFor)}</li>` : ''}
      </ul>
      <button class="arrival-cta" type="button" data-action="join">Entrar com áudio</button>
      <p class="arrival-hint">Você vai precisar liberar o microfone. Nada é gravado.</p>
      <p class="arrival-error" data-role="join-error" hidden></p>
    </section>
  `
  const cta = root.querySelector<HTMLButtonElement>('[data-action="join"]')
  const errorBox = root.querySelector<HTMLElement>('[data-role="join-error"]')
  const showError = (html: string): void => {
    if (!errorBox) return
    errorBox.innerHTML = html
    errorBox.hidden = false
    wireOpenExternalButton(errorBox)
  }
  const clearError = (): void => {
    if (!errorBox) return
    errorBox.innerHTML = ''
    errorBox.hidden = true
  }
  const inAppName = detectInAppBrowser()
  if (inAppName) {
    showError(inAppBrowserHtml(inAppName))
    if (cta) cta.disabled = true
  } else if (!hasMediaDevices()) {
    showError(unsupportedBrowserHtml())
    if (cta) cta.disabled = true
  } else {
    void checkMicPermissionDenied().then((denied) => {
      if (denied) showError(MIC_BLOCKED_HTML)
    })
  }
  cta?.addEventListener('click', async () => {
    cta.disabled = true
    cta.textContent = 'Pedindo permissão…'
    clearError()
    try {
      await actions.onJoinAudio?.()
      cta.textContent = 'Microfone pronto — aguardando vendedor'
    } catch (err) {
      console.error('[trovatacast/web] onJoinAudio failed', err, {
        ua: navigator.userAgent,
        secure: window.isSecureContext,
        hasMediaDevices: hasMediaDevices(),
      })
      cta.disabled = false
      cta.textContent = 'Tentar de novo'
      showError(describeJoinError(err))
    }
  })
}

export type LiveCallStatus = 'connecting' | 'connected' | 'failed' | 'closed'

export type LiveCallActions = {
  onToggleMute?: (nextMuted: boolean) => void
  onHangup?: () => void
}

export type LiveCallView = {
  setStatus(status: LiveCallStatus, detail?: string): void
  setRemoteMuted(muted: boolean): void
  setPointedProduct(ref: string | null): void
  scrollToProduct(ref: string, offset?: number): void
}

export function renderLiveCall(
  root: HTMLElement,
  info: SessionInfo,
  actions: LiveCallActions = {},
): LiveCallView {
  root.innerHTML = `
    <section class="live-call" data-view="live-call" data-status="connecting" data-local-muted="false" data-remote-muted="false">
      <header class="live-call-bar live-call-bar--top">
        <span class="live-call-pill" data-role="status-pill">
          <span class="live-call-pill-dot"></span>
          <span data-role="status-pill-label">Conectando…</span>
        </span>
        <span class="live-call-bar-meta" data-role="status-title">Aguardando vendedor</span>
        <span class="live-call-remote-mute" data-role="remote-mute" hidden>
          <span class="live-call-remote-mute-icon" aria-hidden="true">🔇</span>
          <span>Vendedor sem áudio</span>
        </span>
      </header>

      <div class="live-call-catalog" data-role="catalog">
        <div class="live-call-catalog-header">
          <span class="live-call-catalog-eyebrow">${escapeHtml(SampleCatalog.collection)}</span>
          <p class="live-call-catalog-text">${escapeHtml(info.sellerName)} está te mostrando este catálogo.</p>
        </div>
        <div class="product-grid" data-role="product-grid"></div>
      </div>

      <footer class="live-call-bar live-call-bar--bottom">
        <button
          type="button"
          class="live-call-mic"
          data-action="toggle-mute"
          aria-pressed="false"
          aria-label="Silenciar microfone">
          <span class="live-call-mic-icon" data-role="mic-icon">🎙️</span>
        </button>
        <div class="live-call-bar-info">
          <span class="live-call-bar-label" data-role="footer-label">Token ${escapeHtml(info.token)}</span>
        </div>
        <button type="button" class="live-call-hangup" data-action="hangup">
          <span class="live-call-hangup-icon" aria-hidden="true">⏹</span>
          <span>Encerrar</span>
        </button>
      </footer>
    </section>
  `

  const section = root.querySelector<HTMLElement>('.live-call')!
  const statusPillLabel = section.querySelector<HTMLElement>('[data-role="status-pill-label"]')!
  const statusTitle = section.querySelector<HTMLElement>('[data-role="status-title"]')!
  const footerLabel = section.querySelector<HTMLElement>('[data-role="footer-label"]')!
  const remoteMute = section.querySelector<HTMLElement>('[data-role="remote-mute"]')!
  const micBtn = section.querySelector<HTMLButtonElement>('[data-action="toggle-mute"]')!
  const micIcon = section.querySelector<HTMLElement>('[data-role="mic-icon"]')!
  const hangupBtn = section.querySelector<HTMLButtonElement>('[data-action="hangup"]')!
  const catalog = section.querySelector<HTMLElement>('[data-role="catalog"]')!
  const grid = section.querySelector<HTMLElement>('[data-role="product-grid"]')!

  const cardByRef = new Map<string, HTMLElement>()
  SampleCatalog.products.forEach((product) => {
    const card = renderProductCard(product)
    cardByRef.set(product.ref, card)
    grid.appendChild(card)
  })

  let localMuted = false

  micBtn.addEventListener('click', () => {
    localMuted = !localMuted
    section.dataset.localMuted = localMuted ? 'true' : 'false'
    micBtn.setAttribute('aria-pressed', localMuted ? 'true' : 'false')
    micBtn.setAttribute('aria-label', localMuted ? 'Reativar microfone' : 'Silenciar microfone')
    micIcon.textContent = localMuted ? '🚫' : '🎙️'
    actions.onToggleMute?.(localMuted)
  })

  hangupBtn.addEventListener('click', () => {
    actions.onHangup?.()
  })

  let currentPointed: string | null = null

  return {
    setStatus(status, detail) {
      section.dataset.status = status
      const pillByStatus: Record<LiveCallStatus, string> = {
        connecting: 'Conectando…',
        connected: 'Em chamada',
        failed: 'Conexão caiu',
        closed: 'Sessão encerrada',
      }
      const titleByStatus: Record<LiveCallStatus, string> = {
        connecting: `Conectando com ${info.sellerName}…`,
        connected: `Em chamada com ${info.sellerName}`,
        failed: 'Conexão caiu',
        closed: 'Sessão encerrada',
      }
      statusPillLabel.textContent = pillByStatus[status]
      statusTitle.textContent = titleByStatus[status]
      footerLabel.textContent = detail ?? `Token ${info.token}`
    },
    setRemoteMuted(muted) {
      section.dataset.remoteMuted = muted ? 'true' : 'false'
      remoteMute.hidden = !muted
    },
    setPointedProduct(ref) {
      if (currentPointed && currentPointed !== ref) {
        cardByRef.get(currentPointed)?.setAttribute('data-pointed', 'false')
      }
      currentPointed = ref
      if (ref) {
        const card = cardByRef.get(ref)
        card?.setAttribute('data-pointed', 'true')
        card?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    },
    scrollToProduct(ref, offset = 0) {
      const card = cardByRef.get(ref)
      if (!card) return
      const cardRect = card.getBoundingClientRect()
      const catalogRect = catalog.getBoundingClientRect()
      const targetY = catalog.scrollTop + (cardRect.top - catalogRect.top) + offset * cardRect.height
      catalog.scrollTop = targetY
    },
  }
}

const MIC_BLOCKED_HTML = `
  <strong>Microfone bloqueado neste navegador.</strong>
  <br />Toque no <strong>cadeado</strong> ao lado do endereço → <em>Permissões</em> → libere o <em>Microfone</em>, depois <strong>recarregue</strong> a página.
`

function hasMediaDevices(): boolean {
  return typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia
}

function detectInAppBrowser(): string | null {
  const ua = navigator.userAgent || ''
  if (/FBAN|FBAV|FB_IAB|FBIOS/i.test(ua)) return 'Facebook'
  if (/Instagram/i.test(ua)) return 'Instagram'
  if (/Messenger|MessengerLite/i.test(ua)) return 'Messenger'
  if (/Line\//i.test(ua)) return 'Line'
  if (/MicroMessenger/i.test(ua)) return 'WeChat'
  if (/TikTok|Bytedance|musical_ly/i.test(ua)) return 'TikTok'
  if (/wv\)/i.test(ua) && /Android/i.test(ua)) return 'app'
  if (/WhatsApp/i.test(ua)) return 'WhatsApp'
  return null
}

function inAppBrowserHtml(name: string): string {
  const label = name === 'app' ? 'este app' : name
  return `
    <strong>Abra este link no navegador de verdade.</strong>
    <br />Você está dentro do ${escapeHtml(label)}, que não libera microfone.
    <br />Toque nos <strong>três pontos (⋮)</strong> no canto superior → <em>Abrir no Chrome</em> (ou Safari, no iPhone).
    <br /><button type="button" class="arrival-cta arrival-cta--ghost" data-action="copy-link">Copiar link</button>
  `
}

function unsupportedBrowserHtml(): string {
  return `
    <strong>Este navegador não permite microfone.</strong>
    <br />Abra o link no <em>Chrome</em> (Android) ou <em>Safari</em> (iPhone) atualizado.
    <br /><button type="button" class="arrival-cta arrival-cta--ghost" data-action="copy-link">Copiar link</button>
  `
}

function wireOpenExternalButton(scope: HTMLElement): void {
  const btn = scope.querySelector<HTMLButtonElement>('[data-action="copy-link"]')
  if (!btn) return
  btn.addEventListener('click', async () => {
    const url = window.location.href
    try {
      await navigator.clipboard.writeText(url)
      btn.textContent = 'Link copiado — cole no Chrome'
    } catch {
      btn.textContent = url
    }
    btn.disabled = true
  })
}

async function checkMicPermissionDenied(): Promise<boolean> {
  const perms = (navigator as Navigator & { permissions?: Permissions }).permissions
  if (!perms?.query) return false
  try {
    const status = await perms.query({ name: 'microphone' as PermissionName })
    return status.state === 'denied'
  } catch {
    return false
  }
}

function describeJoinError(err: unknown): string {
  if (err && typeof err === 'object' && 'name' in err) {
    const name = String((err as { name?: unknown }).name ?? '')
    const message = String((err as { message?: unknown }).message ?? '')
    switch (name) {
      case 'NotAllowedError':
      case 'SecurityError':
        return MIC_BLOCKED_HTML
      case 'NotFoundError':
      case 'OverconstrainedError':
        return 'Nenhum microfone disponível neste aparelho.'
      case 'NotReadableError':
        return 'O microfone está ocupado por outro app. Feche apps de chamada e tente de novo.'
      case 'AbortError':
        return 'O pedido de microfone foi cancelado. Tente de novo.'
    }
    if (message) return `Erro: ${escapeHtml(message)}`
  }
  if (err instanceof Error && err.message) return `Erro: ${escapeHtml(err.message)}`
  return 'Não consegui iniciar a chamada. Tente de novo.'
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
