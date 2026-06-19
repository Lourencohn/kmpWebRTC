import { formatBrl, type SnapshotProduct, type SnapshotSize } from '../data/snapshot'

export type DetailActions = {
  onSet: (size: SnapshotSize, qty: number) => void
  onDismiss: () => void
}

export type DetailView = {
  setQty(sizeId: number | null, qty: number): void
  destroy(): void
}

type Row = {
  size: SnapshotSize
  qty: number
  el: HTMLElement
  qtyLabel: HTMLElement
}

export function mountProductDetail(
  host: HTMLElement,
  product: SnapshotProduct,
  collectionLabel: string,
  initialQty: Map<string, number>,
  actions: DetailActions,
): DetailView {
  const overlay = document.createElement('div')
  overlay.className = 'sheet-overlay sheet-overlay--page'
  overlay.dataset.role = 'product-detail-overlay'

  const sheet = document.createElement('section')
  sheet.className = 'sheet sheet--product-page'
  sheet.setAttribute('role', 'dialog')
  sheet.setAttribute('aria-label', `Detalhe do produto ${product.name}`)

  const hero = product.imageUrl
    ? `<img class="pd-hero-photo" src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" loading="lazy" />`
    : `<div class="pd-hero-silhouette" aria-hidden="true"></div>`

  sheet.innerHTML = `
    <header class="pd-topbar">
      <button type="button" class="pd-iconbtn" data-action="dismiss" aria-label="Voltar">←</button>
      <div class="pd-topbar-meta">
        <span class="pd-eyebrow">Voltar ao catálogo</span>
        <span class="pd-collection">${escapeHtml(collectionLabel)}</span>
      </div>
    </header>

    <div class="pd-hero pd-hero--photo">
      ${hero}
    </div>

    <section class="pd-summary">
      <div class="pd-summary-main">
        ${product.ref ? `<span class="pd-ref">${escapeHtml(product.ref)}</span>` : ''}
        <h2 class="pd-title">${escapeHtml(product.name)}</h2>
        ${product.brand ? `<p class="pd-subtitle">${escapeHtml(product.brand)}</p>` : ''}
      </div>
      <div class="pd-summary-price">
        <span class="pd-price-eyebrow">Atacado</span>
        <span class="pd-price">${formatBrl(product.priceCents)}</span>
        ${
          product.available != null
            ? `<span class="pd-moq">${product.available} disp.</span>`
            : ''
        }
      </div>
    </section>

    <section class="pd-block">
      <header class="pd-block-head">
        <h3>Grade · quantidade</h3>
      </header>
      <div class="pd-sizes" data-role="sizes"></div>
    </section>

    <footer class="pd-footer">
      <div class="pd-footer-summary">
        <span class="pd-footer-label">Subtotal · <span data-role="units">0 un</span></span>
        <span class="pd-footer-total" data-role="total">${formatBrl(0)}</span>
      </div>
      <button type="button" class="pd-add-btn" data-action="continue">Pronto</button>
    </footer>
  `

  overlay.appendChild(sheet)
  host.appendChild(overlay)

  const sizesContainer = sheet.querySelector<HTMLElement>('[data-role="sizes"]')!
  const unitsEl = sheet.querySelector<HTMLElement>('[data-role="units"]')!
  const totalEl = sheet.querySelector<HTMLElement>('[data-role="total"]')!

  const rows = new Map<number, Row>()

  const refreshFooter = (): void => {
    let units = 0
    rows.forEach((r) => (units += r.qty))
    unitsEl.textContent = `${units} un`
    totalEl.textContent = formatBrl(product.priceCents * units)
  }

  product.sizes.forEach((size) => {
    const initial = initialQty.get(String(size.sizeId)) ?? 0
    const row = renderSizeRow(size, initial, (next) => {
      const max = size.available ?? Number.MAX_SAFE_INTEGER
      const clamped = Math.max(0, Math.min(next, max))
      const entry = rows.get(size.sizeId)
      if (!entry) return
      entry.qty = clamped
      entry.qtyLabel.textContent = String(clamped)
      entry.el.dataset.active = clamped > 0 ? 'true' : 'false'
      actions.onSet(size, clamped)
      refreshFooter()
    })
    rows.set(size.sizeId, { size, qty: initial, el: row.el, qtyLabel: row.qtyLabel })
    sizesContainer.appendChild(row.el)
  })

  refreshFooter()

  const dismiss = (): void => actions.onDismiss()
  sheet
    .querySelectorAll<HTMLButtonElement>('[data-action="dismiss"], [data-action="continue"]')
    .forEach((btn) => btn.addEventListener('click', dismiss))
  overlay.addEventListener('click', (event) => {
    if (event.target === overlay) dismiss()
  })

  return {
    setQty(sizeId, qty) {
      if (sizeId == null) return
      const entry = rows.get(sizeId)
      if (!entry) return
      entry.qty = qty
      entry.qtyLabel.textContent = String(qty)
      entry.el.dataset.active = qty > 0 ? 'true' : 'false'
      refreshFooter()
    },
    destroy() {
      overlay.remove()
    },
  }
}

function renderSizeRow(
  size: SnapshotSize,
  initialQty: number,
  onChange: (next: number) => void,
): { el: HTMLElement; qtyLabel: HTMLElement } {
  const row = document.createElement('div')
  row.className = 'pd-size'
  row.dataset.active = initialQty > 0 ? 'true' : 'false'
  const stockText = size.available != null ? `${size.available} em estoque` : 'disponível'
  row.innerHTML = `
    <div class="pd-size-head">
      <span class="pd-size-label">${escapeHtml(size.label)}</span>
      <span class="pd-size-stock">${escapeHtml(stockText)}</span>
    </div>
    <div class="pd-size-stepper">
      <button type="button" class="pd-step pd-step--dec" aria-label="Diminuir">−</button>
      <span class="pd-size-qty" data-role="qty">${initialQty}</span>
      <button type="button" class="pd-step pd-step--inc" aria-label="Aumentar">+</button>
    </div>
  `

  const inc = row.querySelector<HTMLButtonElement>('.pd-step--inc')!
  const dec = row.querySelector<HTMLButtonElement>('.pd-step--dec')!
  const qtyLabel = row.querySelector<HTMLElement>('[data-role="qty"]')!

  let current = initialQty
  inc.addEventListener('click', () => {
    current = current + 1
    onChange(current)
    current = Number(qtyLabel.textContent) || current
  })
  dec.addEventListener('click', () => {
    current = Math.max(0, current - 1)
    onChange(current)
    current = Number(qtyLabel.textContent) || current
  })

  return { el: row, qtyLabel }
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
