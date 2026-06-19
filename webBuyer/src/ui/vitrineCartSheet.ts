import { formatBrl, type SnapshotProduct } from '../data/snapshot'
import type { CartEntry, CartSnapshotView } from '../cart/vitrineCart'

export type CartSheetActions = {
  onRemove: (productId: number, sizeId: number | null) => void
  onSend: () => void
  onDismiss: () => void
}

export type CartSheetView = {
  update(view: CartSnapshotView): void
  destroy(): void
}

export function mountCartSheet(
  host: HTMLElement,
  products: Map<number, SnapshotProduct>,
  initial: CartSnapshotView,
  actions: CartSheetActions,
): CartSheetView {
  const overlay = document.createElement('div')
  overlay.className = 'sheet-overlay'
  overlay.dataset.role = 'cart-overlay'

  const sheet = document.createElement('section')
  sheet.className = 'sheet sheet--cart'
  sheet.setAttribute('role', 'dialog')
  sheet.setAttribute('aria-label', 'Seu pedido')

  sheet.innerHTML = `
    <div class="sheet-grip" aria-hidden="true"></div>
    <header class="sheet-header">
      <div class="sheet-title-block">
        <span class="sheet-eyebrow">Pedido</span>
        <h2 class="sheet-title">Seu pedido</h2>
        <p class="sheet-meta" data-role="meta"></p>
      </div>
      <button type="button" class="sheet-close" data-action="dismiss" aria-label="Fechar">×</button>
    </header>
    <ul class="cart-lines" data-role="lines"></ul>
    <div class="cart-total" data-role="total" hidden></div>
    <button type="button" class="vitrine-send-btn" data-action="send" hidden>
      <span class="vitrine-send-icon" aria-hidden="true">↗</span>
      Enviar pedido pelo WhatsApp
    </button>
    <p class="vitrine-send-hint" data-role="send-hint" hidden>
      Você escolhe o vendedor no WhatsApp e envia. Ele recebe o pedido já montado.
    </p>
  `

  overlay.appendChild(sheet)
  host.appendChild(overlay)

  const linesEl = sheet.querySelector<HTMLElement>('[data-role="lines"]')!
  const totalEl = sheet.querySelector<HTMLElement>('[data-role="total"]')!
  const metaEl = sheet.querySelector<HTMLElement>('[data-role="meta"]')!
  const sendBtn = sheet.querySelector<HTMLButtonElement>('[data-action="send"]')!
  const sendHint = sheet.querySelector<HTMLElement>('[data-role="send-hint"]')!

  const render = (view: CartSnapshotView): void => {
    if (view.lines.length === 0) {
      linesEl.innerHTML = `
        <li class="cart-empty">
          <h3 class="cart-empty-title">Seu pedido está vazio</h3>
          <p class="cart-empty-text">Toque numa peça do catálogo para adicionar.</p>
        </li>
      `
      metaEl.textContent = ''
      totalEl.hidden = true
      sendBtn.hidden = true
      sendHint.hidden = true
      return
    }

    linesEl.innerHTML = view.lines.map((line) => lineRowHtml(line, products)).join('')
    metaEl.textContent = `${view.lines.length} ${view.lines.length === 1 ? 'linha' : 'linhas'} · ${view.totalUnits} un`
    totalEl.hidden = false
    totalEl.innerHTML = `
      <span class="cart-total-label">Subtotal</span>
      <span class="cart-total-value">${formatBrl(view.totalCents)}</span>
    `
    sendBtn.hidden = false
    sendHint.hidden = false

    linesEl.querySelectorAll<HTMLButtonElement>('[data-action="remove"]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const productId = Number(btn.dataset.productId)
        const sizeId = btn.dataset.sizeId === '-' ? null : Number(btn.dataset.sizeId)
        actions.onRemove(productId, sizeId)
      })
    })
  }

  render(initial)

  sheet
    .querySelector<HTMLButtonElement>('[data-action="dismiss"]')
    ?.addEventListener('click', () => actions.onDismiss())
  sendBtn.addEventListener('click', () => actions.onSend())
  overlay.addEventListener('click', (event) => {
    if (event.target === overlay) actions.onDismiss()
  })

  return {
    update(view) {
      render(view)
    },
    destroy() {
      overlay.remove()
    },
  }
}

function lineRowHtml(line: CartEntry, products: Map<number, SnapshotProduct>): string {
  const product = products.get(line.productId)
  const name = product?.name ?? `Produto ${line.productId}`
  const ref = product?.ref ?? ''
  const unitCents = product?.priceCents ?? 0
  const subtotal = unitCents * line.qty
  return `
    <li class="cart-line">
      <div class="cart-line-main">
        ${ref ? `<span class="cart-line-ref">${escapeHtml(ref)}</span>` : ''}
        <span class="cart-line-name">${escapeHtml(name)}</span>
        <span class="cart-line-meta">${escapeHtml(line.sizeLabel)} · ${line.qty} un</span>
      </div>
      <div class="cart-line-trail">
        <span class="cart-line-amount">${formatBrl(subtotal)}</span>
        <button
          type="button"
          class="cart-line-remove"
          data-action="remove"
          data-product-id="${line.productId}"
          data-size-id="${line.sizeId ?? '-'}"
          aria-label="Remover ${escapeHtml(name)} ${escapeHtml(line.sizeLabel)}">
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
