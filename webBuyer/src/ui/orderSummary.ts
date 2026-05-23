import { formatBrl } from '../cart/store'
import { SampleCatalog } from '../data/catalog'
import type { OrderLine } from '../protocol/dataChannel'

export type OrderSummaryPayload = {
  orderId: string
  ts: number
  from: string
  lines: OrderLine[]
  totalCents: number
}

export type OrderSummaryActions = {
  onClose: () => void
}

export type OrderSummaryView = {
  destroy(): void
}

export function mountOrderSummary(
  host: HTMLElement,
  payload: OrderSummaryPayload,
  sellerName: string,
  actions: OrderSummaryActions,
): OrderSummaryView {
  const overlay = document.createElement('div')
  overlay.className = 'order-summary'
  overlay.dataset.role = 'order-summary'

  const totalUnits = payload.lines.reduce((sum, l) => sum + l.units, 0)

  overlay.innerHTML = `
    <header class="order-summary-header">
      <div class="order-summary-badge" aria-hidden="true">
        <span class="order-summary-check">✓</span>
      </div>
      <h1 class="order-summary-title">Pedido pronto</h1>
      <p class="order-summary-sub">${escapeHtml(sellerName)} confirmou o seu pedido.</p>
      <p class="order-summary-id">ID <code>${escapeHtml(payload.orderId)}</code></p>
    </header>
    <ul class="order-summary-lines"></ul>
    <footer class="order-summary-footer">
      <div class="order-summary-total">
        <span class="order-summary-total-label">Total</span>
        <span class="order-summary-total-meta">${totalUnits}un · ${payload.lines.length} ${payload.lines.length === 1 ? 'linha' : 'linhas'}</span>
        <span class="order-summary-total-value">${formatBrl(payload.totalCents)}</span>
      </div>
      <button type="button" class="order-summary-close" data-action="close">Fechar</button>
    </footer>
  `

  const linesEl = overlay.querySelector<HTMLElement>('.order-summary-lines')!
  payload.lines.forEach((line) => {
    linesEl.insertAdjacentHTML('beforeend', lineHtml(line))
  })

  overlay
    .querySelector<HTMLButtonElement>('[data-action="close"]')
    ?.addEventListener('click', () => actions.onClose())

  host.appendChild(overlay)

  return {
    destroy() {
      overlay.remove()
    },
  }
}

function lineHtml(line: OrderLine): string {
  const product = SampleCatalog.products.find((p) => p.ref === line.productId)
  const name = product?.name ?? line.productId
  const subtotal = line.unitPriceCents * line.units
  return `
    <li class="order-summary-line">
      <div class="order-summary-line-main">
        <span class="order-summary-line-ref">${escapeHtml(line.productId)}</span>
        <span class="order-summary-line-name">${escapeHtml(name)}</span>
        <span class="order-summary-line-meta">Tam ${escapeHtml(line.size)} · ${line.units}un</span>
      </div>
      <span class="order-summary-line-amount">${formatBrl(subtotal)}</span>
    </li>
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
