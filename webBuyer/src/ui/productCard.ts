import { ProductSwatchPalette, tagLabel, tintFor, type Product } from '../data/catalog'

export type ProductCardOptions = {
  pointed?: boolean
  highlight?: boolean
}

export function renderProductCard(product: Product, opts: ProductCardOptions = {}): HTMLElement {
  const tint = tintFor(product.tintIndex)
  const card = document.createElement('article')
  card.className = 'product-card'
  card.dataset.productRef = product.ref
  card.dataset.pointed = opts.pointed ? 'true' : 'false'
  card.dataset.highlight = opts.highlight ? 'true' : 'false'
  card.style.setProperty('--card-tint-bg', tint.background)
  card.style.setProperty('--card-tint-fg', tint.foreground)

  const visual = product.image
    ? `<img class="product-card-photo" src="${escapeHtml(product.image)}" alt="${escapeHtml(product.name)}" loading="lazy"/>`
    : `<svg class="product-card-silhouette" viewBox="0 0 100 120" aria-hidden="true">${garmentSilhouette(product.garment)}</svg>`

  card.innerHTML = `
    <div class="product-card-frame">
      <span class="product-card-ref">${escapeHtml(product.ref)}</span>
      ${product.tag ? `<span class="product-card-tag" data-tag="${product.tag}">${escapeHtml(tagLabel(product.tag))}</span>` : ''}
      ${visual}
      <div class="product-card-halo" aria-hidden="true">
        <span class="product-card-halo-label">Apontando</span>
      </div>
    </div>
    <div class="product-card-meta">
      <h3 class="product-card-name">${escapeHtml(product.name)}</h3>
      <div class="product-card-row">
        <span class="product-card-price">${escapeHtml(product.price)}</span>
        <span class="product-card-moq">MOQ ${product.moq}</span>
      </div>
      <div class="product-card-swatches" aria-label="${product.colorCount} cores disponíveis">
        ${swatchRow(product.colorCount)}
      </div>
    </div>
  `
  return card
}

function swatchRow(count: number): string {
  const visible = Math.min(count, ProductSwatchPalette.length)
  const dots = Array.from({ length: visible })
    .map((_, i) => `<span class="product-card-swatch" style="background:${ProductSwatchPalette[i]}"></span>`)
    .join('')
  const extra = count > visible ? `<span class="product-card-swatch-extra">+${count - visible}</span>` : ''
  return dots + extra
}

function garmentSilhouette(kind: Product['garment']): string {
  switch (kind) {
    case 'shirt':
      return `<path d="M30 18 L20 30 L24 38 L32 36 L32 96 L68 96 L68 36 L76 38 L80 30 L70 18 L60 22 L50 26 L40 22 Z" fill="currentColor"/>`
    case 'polo':
      return `<path d="M30 18 L22 28 L26 36 L34 34 L34 96 L66 96 L66 34 L74 36 L78 28 L70 18 L60 22 L54 18 L46 18 L40 22 Z" fill="currentColor"/><rect x="48" y="26" width="4" height="14" fill="${maskColor()}"/>`
    case 'tee':
      return `<path d="M28 22 L18 32 L22 40 L30 38 L30 96 L70 96 L70 38 L78 40 L82 32 L72 22 L60 28 L50 30 L40 28 Z" fill="currentColor"/>`
    case 'dress':
      return `<path d="M34 18 L28 30 L34 36 L34 60 L24 96 L76 96 L66 60 L66 36 L72 30 L66 18 L58 22 L50 24 L42 22 Z" fill="currentColor"/>`
    case 'jacket':
      return `<path d="M30 18 L20 30 L24 40 L32 38 L32 100 L68 100 L68 38 L76 40 L80 30 L70 18 L60 24 L50 22 L40 24 Z" fill="currentColor"/><path d="M50 22 L50 100" stroke="${maskColor()}" stroke-width="1.5" fill="none"/>`
    case 'pants':
      return `<path d="M34 18 L32 50 L28 100 L44 100 L48 60 L52 60 L56 100 L72 100 L68 50 L66 18 Z" fill="currentColor"/>`
    case 'shoe':
      return `<path d="M16 70 L20 56 L42 56 L52 64 L78 70 L82 80 L80 88 L18 88 L14 80 Z" fill="currentColor"/>`
    case 'sweater':
      return `<path d="M28 22 L20 36 L26 44 L32 42 L32 96 L68 96 L68 42 L74 44 L80 36 L72 22 L60 28 L50 30 L40 28 Z" fill="currentColor"/><path d="M40 50 L40 90 M50 50 L50 90 M60 50 L60 90" stroke="${maskColor()}" stroke-width="0.8" fill="none"/>`
    case 'skirt':
      return `<path d="M34 28 L28 96 L72 96 L66 28 Z" fill="currentColor"/>`
  }
}

function maskColor(): string {
  return 'rgba(0,0,0,0.18)'
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
