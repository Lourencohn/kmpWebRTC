import { SampleCatalog } from '../data/catalog'
import {
  formatBrl,
  unitPriceCentsFor,
  type CartLine,
  type CartSnapshot,
} from '../cart/store'

export type CartSheetActions = {
  onRemove: (productId: string, size: string) => void
  onDismiss: () => void
}

export type CartSheetView = {
  update(snapshot: CartSnapshot): void
  destroy(): void
}

export function mountCartSheet(
  host: HTMLElement,
  initial: CartSnapshot,
  actions: CartSheetActions,
): CartSheetView {
  const overlay = document.createElement('div')
  overlay.className = 'sheet-overlay'
  overlay.dataset.role = 'cart-overlay'

  const sheet = document.createElement('section')
  sheet.className = 'sheet sheet--cart'
  sheet.setAttribute('role', 'dialog')
  sheet.setAttribute('aria-label', 'Pedido em construção')

  sheet.innerHTML = `
    <div class="sheet-grip" aria-hidden="true"></div>
    <header class="sheet-header">
      <div class="sheet-title-block">
        <span class="sheet-eyebrow">Pedido</span>
        <h2 class="sheet-title">Em construção</h2>
        <p class="sheet-meta" data-role="meta"></p>
      </div>
      <button type="button" class="sheet-close" data-action="dismiss" aria-label="Fechar">×</button>
    </header>
    <ul class="cart-lines" data-role="lines"></ul>
    <div class="cart-total" data-role="total"></div>
  `

  overlay.appendChild(sheet)
  host.appendChild(overlay)

  const linesEl = sheet.querySelector<HTMLElement>('[data-role="lines"]')!
  const totalEl = sheet.querySelector<HTMLElement>('[data-role="total"]')!
  const metaEl = sheet.querySelector<HTMLElement>('[data-role="meta"]')!

  const render = (snapshot: CartSnapshot): void => {
    if (snapshot.lines.length === 0) {
      linesEl.innerHTML = `
        <li class="cart-empty">
          <span class="cart-empty-icon" aria-hidden="true">🧺</span>
          <h3 class="cart-empty-title">Seu pedido está vazio</h3>
          <p class="cart-empty-text">Toque numa peça para adicionar.</p>
        </li>
      `
      metaEl.textContent = ''
      totalEl.innerHTML = ''
      return
    }
    linesEl.innerHTML = snapshot.lines.map(lineRowHtml).join('')
    metaEl.textContent = `${snapshot.lines.length} ${
      snapshot.lines.length === 1 ? 'linha' : 'linhas'
    } · ${snapshot.totalUnits}un`
    totalEl.innerHTML = `
      <span class="cart-total-label">Subtotal</span>
      <span class="cart-total-value">${formatBrl(snapshot.totalCents)}</span>
    `

    linesEl.querySelectorAll<HTMLButtonElement>('[data-action="remove"]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const productId = btn.dataset.productId
        const size = btn.dataset.size
        if (productId && size) actions.onRemove(productId, size)
      })
    })
  }

  render(initial)

  sheet
    .querySelector<HTMLButtonElement>('[data-action="dismiss"]')
    ?.addEventListener('click', () => actions.onDismiss())
  overlay.addEventListener('click', (event) => {
    if (event.target === overlay) actions.onDismiss()
  })

  return {
    update(snapshot) {
      render(snapshot)
    },
    destroy() {
      overlay.remove()
    },
  }
}

function lineRowHtml(line: CartLine): string {
  const product = SampleCatalog.products.find((p) => p.ref === line.productId)
  const name = product?.name ?? line.productId
  const unitCents = unitPriceCentsFor(line.productId)
  const subtotal = unitCents * line.units
  return `
    <li class="cart-line" data-product-ref="${escapeHtml(line.productId)}">
      <div class="cart-line-main">
        <span class="cart-line-ref">${escapeHtml(line.productId)}</span>
        <span class="cart-line-name">${escapeHtml(name)}</span>
        <span class="cart-line-meta">Tam ${escapeHtml(line.size)} · ${line.units}un</span>
      </div>
      <div class="cart-line-trail">
        <span class="cart-line-amount">${formatBrl(subtotal)}</span>
        <button
          type="button"
          class="cart-line-remove"
          data-action="remove"
          data-product-id="${escapeHtml(line.productId)}"
          data-size="${escapeHtml(line.size)}"
          aria-label="Remover ${escapeHtml(name)} tamanho ${escapeHtml(line.size)}">
          Remover
        </button>
      </div>
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
