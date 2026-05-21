export type SessionInfo = {
  token: string
  sellerName: string
  collectionLabel: string
  clientName: string | null
  clientShop: string | null
  scheduledFor: string | null
  productCount: number
  createdAtMs: number
  expiresAtMs: number
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

export function renderLanding(root: HTMLElement): void {
  root.innerHTML = `
    <section class="arrival arrival--landing">
      ${wordmark()}
      <p class="arrival-text">
        Abra o link enviado pelo seu vendedor para entrar na sessão.
      </p>
    </section>
  `
}

export function renderLoading(root: HTMLElement, token: string): void {
  root.innerHTML = `
    <section class="arrival arrival--loading" data-token="${escapeHtml(token)}">
      ${wordmark()}
      <p class="arrival-text">Carregando sessão…</p>
      <div class="arrival-spinner" aria-hidden="true"></div>
    </section>
  `
}

export function renderError(root: HTMLElement, message: string, token: string): void {
  root.innerHTML = `
    <section class="arrival arrival--error">
      ${wordmark()}
      <h1 class="arrival-title">Não consegui abrir essa sessão</h1>
      <p class="arrival-text">${escapeHtml(message)}</p>
      <p class="arrival-hint">Token: <code>${escapeHtml(token)}</code></p>
      <button class="arrival-cta" type="button" onclick="window.location.reload()">Tentar novamente</button>
    </section>
  `
}

export function renderArrival(root: HTMLElement, info: SessionInfo): void {
  const greeting = info.clientName ? `Oi, ${info.clientName.split(' ')[0]}` : 'Tudo pronto'
  const sellerLine = info.clientShop
    ? `${info.sellerName} chamou você de <strong>${escapeHtml(info.clientShop)}</strong>`
    : `${info.sellerName} está te esperando`
  root.innerHTML = `
    <section class="arrival">
      ${wordmark()}
      <span class="arrival-eyebrow">${escapeHtml(info.collectionLabel)}</span>
      <h1 class="arrival-title">${escapeHtml(greeting)}</h1>
      <p class="arrival-text">${sellerLine}.</p>
      <ul class="arrival-meta">
        <li><span>Sessão</span><code>${escapeHtml(info.token)}</code></li>
        <li><span>Catálogo</span>${info.productCount} produtos</li>
        ${info.scheduledFor ? `<li><span>Horário</span>${escapeHtml(info.scheduledFor)}</li>` : ''}
      </ul>
      <button class="arrival-cta" type="button" id="join-cta">Entrar com áudio</button>
      <p class="arrival-hint">Você vai precisar liberar o microfone. Nada é gravado.</p>
    </section>
  `
  const cta = document.getElementById('join-cta') as HTMLButtonElement | null
  cta?.addEventListener('click', () => {
    cta.disabled = true
    cta.textContent = 'Pedindo permissão…'
    navigator.mediaDevices
      ?.getUserMedia({ audio: true })
      .then(() => {
        cta.textContent = 'Microfone pronto — aguardando vendedor'
      })
      .catch(() => {
        cta.disabled = false
        cta.textContent = 'Tentar de novo'
      })
  })
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
