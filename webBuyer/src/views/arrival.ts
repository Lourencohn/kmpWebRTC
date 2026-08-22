function wordmark(): string {
  return `
    <div class="wordmark" aria-label="Trovata">
      <img class="wordmark-img" src="/trovata-logo.png" alt="Trovata" />
    </div>
  `
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

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
