import type { SessionFetchErrorKind, SessionInfo } from '../api/sessions'

export type ArrivalActions = {
  onJoinAudio?: () => void | Promise<void>
  onRetry?: () => void
  onReopenLast?: (token: string) => void
}

function wordmark(): string {
  return `
    <div class="wordmark" aria-label="TrovataCast">
      <span class="wordmark-mark" aria-hidden="true"><span class="wordmark-dot"></span></span>
      <span class="wordmark-text">
        <span class="wordmark-text-a">Trovata</span><span class="wordmark-text-b">Cast</span>
      </span>
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
    </section>
  `
  const cta = root.querySelector<HTMLButtonElement>('[data-action="join"]')
  cta?.addEventListener('click', async () => {
    cta.disabled = true
    cta.textContent = 'Pedindo permissão…'
    try {
      await actions.onJoinAudio?.()
      cta.textContent = 'Microfone pronto — aguardando vendedor'
    } catch {
      cta.disabled = false
      cta.textContent = 'Tentar de novo'
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
}

export function renderLiveCall(
  root: HTMLElement,
  info: SessionInfo,
  actions: LiveCallActions = {},
): LiveCallView {
  const sellerInitial = (info.sellerName?.trim()?.[0] ?? 'V').toUpperCase()
  root.innerHTML = `
    <section class="live-call" data-view="live-call" data-status="connecting" data-local-muted="false" data-remote-muted="false">
      <div class="live-call-top">
        <span class="live-call-pill" data-role="status-pill">
          <span class="live-call-pill-dot"></span>
          <span data-role="status-pill-label">Conectando…</span>
        </span>
      </div>

      <div class="live-call-hero">
        <div class="live-call-avatar" aria-hidden="true">
          <span class="live-call-avatar-letter">${escapeHtml(sellerInitial)}</span>
        </div>
        <h1 class="live-call-title" data-role="status-title">Conectando ao vendedor…</h1>
        <p class="live-call-sub">${escapeHtml(info.sellerName)}${
          info.clientShop ? ` · ${escapeHtml(info.clientShop)}` : ''
        }</p>
        <p class="live-call-detail" data-role="status-detail">Token ${escapeHtml(info.token)}</p>

        <div class="live-call-remote-mute" data-role="remote-mute" hidden>
          <span class="live-call-remote-mute-icon" aria-hidden="true">🔇</span>
          <span>Vendedor sem áudio</span>
        </div>
      </div>

      <div class="live-call-actions">
        <button
          type="button"
          class="live-call-mic"
          data-action="toggle-mute"
          aria-pressed="false"
          aria-label="Silenciar microfone">
          <span class="live-call-mic-icon" data-role="mic-icon">🎙️</span>
        </button>
        <button type="button" class="live-call-hangup" data-action="hangup">
          Encerrar
        </button>
      </div>
    </section>
  `

  const section = root.querySelector<HTMLElement>('.live-call')!
  const statusPillLabel = section.querySelector<HTMLElement>('[data-role="status-pill-label"]')!
  const statusTitle = section.querySelector<HTMLElement>('[data-role="status-title"]')!
  const statusDetail = section.querySelector<HTMLElement>('[data-role="status-detail"]')!
  const remoteMute = section.querySelector<HTMLElement>('[data-role="remote-mute"]')!
  const micBtn = section.querySelector<HTMLButtonElement>('[data-action="toggle-mute"]')!
  const micIcon = section.querySelector<HTMLElement>('[data-role="mic-icon"]')!
  const hangupBtn = section.querySelector<HTMLButtonElement>('[data-action="hangup"]')!

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

  return {
    setStatus(status, detail) {
      section.dataset.status = status
      const labelByStatus: Record<LiveCallStatus, string> = {
        connecting: 'Conectando…',
        connected: 'Em chamada',
        failed: 'Conexão caiu',
        closed: 'Sessão encerrada',
      }
      const titleByStatus: Record<LiveCallStatus, string> = {
        connecting: 'Conectando ao vendedor…',
        connected: `Em chamada com ${info.sellerName}`,
        failed: 'Conexão caiu',
        closed: 'Sessão encerrada',
      }
      statusPillLabel.textContent = labelByStatus[status]
      statusTitle.textContent = titleByStatus[status]
      statusDetail.textContent = detail ?? `Token ${info.token}`
    },
    setRemoteMuted(muted) {
      section.dataset.remoteMuted = muted ? 'true' : 'false'
      remoteMute.hidden = !muted
    },
  }
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
