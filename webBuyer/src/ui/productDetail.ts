import {
  ProductSwatchPalette,
  SampleCatalog,
  tagLabel,
  tintFor,
  type Product,
} from '../data/catalog'
import { formatBrl, unitPriceCentsFor } from '../cart/store'

export type ProductDetailActions = {
  onAdd: (size: string, units: number) => void
  onDismiss: () => void
}

export type ProductDetailView = {
  setCurrentUnits(size: string, units: number): void
  destroy(): void
}

type SizeRow = {
  size: string
  stock: number
  units: number
  row: HTMLElement
  unitsBadge: HTMLElement
  qtyLabel: HTMLElement
  inc: HTMLButtonElement
  dec: HTMLButtonElement
}

const SWATCH_NAMES = ['Verde-musgo', 'Areia', 'Caramelo', 'Grafite']
const STOCK_SEEDS = [12, 8, 6, 4, 10]

export function mountProductDetail(
  host: HTMLElement,
  product: Product,
  initialLines: Array<{ size: string; units: number }>,
  actions: ProductDetailActions,
): ProductDetailView {
  const overlay = document.createElement('div')
  overlay.className = 'sheet-overlay sheet-overlay--page'
  overlay.dataset.role = 'product-detail-overlay'

  const sheet = document.createElement('section')
  sheet.className = 'sheet sheet--product-page'
  sheet.setAttribute('role', 'dialog')
  sheet.setAttribute('aria-label', `Detalhe do produto ${product.ref}`)

  const tint = tintFor(product.tintIndex)
  const sizesWithStock = sampleSizes(product)
  const swatches = sampleSwatches(product)

  sheet.innerHTML = `
    <header class="pd-topbar">
      <button type="button" class="pd-iconbtn" data-action="dismiss" aria-label="Voltar">←</button>
      <div class="pd-topbar-meta">
        <span class="pd-eyebrow">Voltar ao catálogo</span>
        <span class="pd-collection">${escapeHtml(SampleCatalog.collection)}</span>
      </div>
      <button type="button" class="pd-iconbtn" aria-label="Favoritar">♡</button>
      <button type="button" class="pd-iconbtn" aria-label="Compartilhar">↗</button>
    </header>

    <div class="pd-hero" style="--card-tint-bg:${tint.background};--card-tint-fg:${tint.foreground}">
      ${product.image
        ? `<img class="pd-hero-photo" src="${escapeHtml(product.image)}" alt="${escapeHtml(product.name)}" loading="lazy"/>`
        : `<svg class="pd-hero-silhouette" viewBox="0 0 100 120" aria-hidden="true">${heroSilhouette(product.garment)}</svg>`}
      ${product.tag ? `<span class="pd-tag" data-tag="${product.tag}">${escapeHtml(tagLabel(product.tag))}</span>` : ''}
      <span class="pd-layers">▦ 1 / 4</span>
      <div class="pd-dots">
        ${[0, 1, 2, 3].map((i) => `<span class="pd-dot${i === 0 ? ' pd-dot--active' : ''}"></span>`).join('')}
      </div>
    </div>

    <section class="pd-summary">
      <div class="pd-summary-main">
        <span class="pd-ref">${escapeHtml(product.ref)}</span>
        <h2 class="pd-title">${escapeHtml(product.name)}</h2>
        <p class="pd-subtitle">Linho 100% · feito em São Paulo</p>
      </div>
      <div class="pd-summary-price">
        <span class="pd-price-eyebrow">Atacado</span>
        <span class="pd-price">${escapeHtml(product.price)}</span>
        <span class="pd-moq">min ${product.moq}un</span>
      </div>
    </section>

    <section class="pd-block">
      <header class="pd-block-head">
        <h3>Cor</h3>
      </header>
      <div class="pd-colors">
        ${swatches.map((sw, i) => `
          <span class="pd-color${i === 0 ? ' pd-color--active' : ''}">
            <span class="pd-color-dot" style="background:${sw.hex}"></span>
            ${escapeHtml(sw.name)}
          </span>`).join('')}
      </div>
    </section>

    <section class="pd-block">
      <header class="pd-block-head">
        <h3>Grade · quantidade</h3>
        <span class="pd-block-action">Guia de tamanhos</span>
      </header>
      <div class="pd-sizes" data-role="sizes"></div>
    </section>

    <section class="pd-block">
      <header class="pd-block-head">
        <h3>Sobre o produto</h3>
      </header>
      <div class="pd-card">
        <p class="pd-about">
          Vestido midi de caimento solto, manga curta e cintura marcada por elástico interno.
          Tecido encorpado, com leve transparência. Perfeito para vitrine de início de outono.
        </p>
        <div class="pd-specs">
          <div><span class="pd-spec-label">Tecido</span><span class="pd-spec-value">Algodão 100%</span></div>
          <div><span class="pd-spec-label">Origem</span><span class="pd-spec-value">São Paulo · BR</span></div>
          <div><span class="pd-spec-label">Prazo</span><span class="pd-spec-value">12 a 18 dias</span></div>
          <div><span class="pd-spec-label">Frete</span><span class="pd-spec-value">Por sua conta</span></div>
        </div>
      </div>
    </section>

    <footer class="pd-footer">
      <div class="pd-footer-summary">
        <span class="pd-footer-label">Subtotal · <span data-role="summary-units">0 un</span></span>
        <span class="pd-footer-total" data-role="summary-total">${formatBrl(0)}</span>
      </div>
      <button type="button" class="pd-add-btn" data-action="continue">Continuar</button>
    </footer>
  `

  overlay.appendChild(sheet)
  host.appendChild(overlay)

  const sizesContainer = sheet.querySelector<HTMLElement>('[data-role="sizes"]')!
  const initialBySize = new Map<string, number>()
  initialLines.forEach((l) => initialBySize.set(l.size, l.units))

  const sizeRows = new Map<string, SizeRow>()
  sizesWithStock.forEach((entry) => {
    const initial = initialBySize.get(entry.size) ?? 0
    const row = renderSizeRow(entry, initial, (next) => {
      const prev = sizeRows.get(entry.size)
      if (!prev) return
      const clamped = Math.max(0, Math.min(next, entry.stock))
      prev.units = clamped
      prev.qtyLabel.textContent = String(clamped)
      prev.row.dataset.active = clamped > 0 ? 'true' : 'false'
      actions.onAdd(entry.size, clamped)
      refreshFooter()
    })
    sizeRows.set(entry.size, row)
    sizesContainer.appendChild(row.row)
  })

  const summaryUnitsEl = sheet.querySelector<HTMLElement>('[data-role="summary-units"]')!
  const summaryTotalEl = sheet.querySelector<HTMLElement>('[data-role="summary-total"]')!
  const unitPriceCents = unitPriceCentsFor(product.ref)

  const refreshFooter = (): void => {
    let units = 0
    sizeRows.forEach((r) => (units += r.units))
    summaryUnitsEl.textContent = `${units} un`
    summaryTotalEl.textContent = formatBrl(unitPriceCents * units)
  }
  refreshFooter()

  const dismiss = (): void => actions.onDismiss()
  sheet
    .querySelectorAll<HTMLButtonElement>('[data-action="dismiss"], [data-action="continue"]')
    .forEach((btn) => btn.addEventListener('click', dismiss))
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) dismiss()
  })

  return {
    setCurrentUnits(size, units) {
      const row = sizeRows.get(size)
      if (!row) return
      row.units = units
      row.qtyLabel.textContent = String(units)
      row.row.dataset.active = units > 0 ? 'true' : 'false'
      refreshFooter()
    },
    destroy() {
      overlay.remove()
    },
  }
}

function renderSizeRow(
  entry: { size: string; stock: number },
  initialUnits: number,
  onChange: (next: number) => void,
): SizeRow {
  const row = document.createElement('div')
  row.className = 'pd-size'
  row.dataset.size = entry.size
  row.dataset.active = initialUnits > 0 ? 'true' : 'false'
  row.innerHTML = `
    <div class="pd-size-head">
      <span class="pd-size-label">Tam ${escapeHtml(entry.size)}</span>
      <span class="pd-size-stock">${entry.stock} em estoque</span>
    </div>
    <div class="pd-size-stepper">
      <button type="button" class="pd-step pd-step--dec" aria-label="Diminuir">−</button>
      <span class="pd-size-qty" data-role="qty">${initialUnits}</span>
      <button type="button" class="pd-step pd-step--inc" aria-label="Aumentar">+</button>
    </div>
    <span class="pd-size-badge" data-role="units">${initialUnits > 0 ? `${initialUnits}un no pedido` : ''}</span>
  `
  const inc = row.querySelector<HTMLButtonElement>('.pd-step--inc')!
  const dec = row.querySelector<HTMLButtonElement>('.pd-step--dec')!
  const qtyLabel = row.querySelector<HTMLElement>('[data-role="qty"]')!
  const unitsBadge = row.querySelector<HTMLElement>('[data-role="units"]')!

  let current = initialUnits
  inc.addEventListener('click', () => {
    current = Math.min(entry.stock, current + 1)
    qtyLabel.textContent = String(current)
    onChange(current)
    unitsBadge.textContent = current > 0 ? `${current}un no pedido` : ''
  })
  dec.addEventListener('click', () => {
    current = Math.max(0, current - 1)
    qtyLabel.textContent = String(current)
    onChange(current)
    unitsBadge.textContent = current > 0 ? `${current}un no pedido` : ''
  })

  return { size: entry.size, stock: entry.stock, units: initialUnits, row, unitsBadge, qtyLabel, inc, dec }
}

export function findProduct(productId: string): Product | null {
  return SampleCatalog.products.find((p) => p.ref === productId) ?? null
}

function sampleSizes(product: Product): Array<{ size: string; stock: number }> {
  if (product.sizes.length === 1 && product.sizes[0]!.toLowerCase() === 'único') {
    return [{ size: 'Único', stock: 24 }]
  }
  return product.sizes.map((s, i) => ({ size: s, stock: STOCK_SEEDS[i % STOCK_SEEDS.length]! }))
}

function sampleSwatches(product: Product): Array<{ name: string; hex: string }> {
  const count = Math.max(1, Math.min(product.colorCount, ProductSwatchPalette.length))
  return Array.from({ length: count }, (_, i) => ({
    name: SWATCH_NAMES[i % SWATCH_NAMES.length]!,
    hex: ProductSwatchPalette[i % ProductSwatchPalette.length]!,
  }))
}

function heroSilhouette(kind: Product['garment']): string {
  switch (kind) {
    case 'dress':
      return `<path d="M34 18 L28 30 L34 36 L34 60 L24 96 L76 96 L66 60 L66 36 L72 30 L66 18 L58 22 L50 24 L42 22 Z" fill="currentColor"/>`
    case 'jacket':
      return `<path d="M30 18 L20 30 L24 40 L32 38 L32 100 L68 100 L68 38 L76 40 L80 30 L70 18 L60 24 L50 22 L40 24 Z" fill="currentColor"/>`
    case 'pants':
      return `<path d="M34 18 L32 50 L28 100 L44 100 L48 60 L52 60 L56 100 L72 100 L68 50 L66 18 Z" fill="currentColor"/>`
    case 'shoe':
      return `<path d="M16 70 L20 56 L42 56 L52 64 L78 70 L82 80 L80 88 L18 88 L14 80 Z" fill="currentColor"/>`
    case 'skirt':
      return `<path d="M34 28 L28 96 L72 96 L66 28 Z" fill="currentColor"/>`
    case 'sweater':
    case 'polo':
    case 'tee':
    case 'shirt':
    default:
      return `<path d="M28 22 L18 32 L22 40 L30 38 L30 96 L70 96 L70 38 L78 40 L82 32 L72 22 L60 28 L50 30 L40 28 Z" fill="currentColor"/>`
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

