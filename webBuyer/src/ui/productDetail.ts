import { SampleCatalog, tintFor, type Product } from '../data/catalog'
import { renderProductCard } from './productCard'

export type ProductDetailActions = {
  onAdd: (size: string, units: number) => void
  onDismiss: () => void
}

export type ProductDetailView = {
  setCurrentUnits(size: string, units: number): void
  destroy(): void
}

export function mountProductDetail(
  host: HTMLElement,
  product: Product,
  initialLines: Array<{ size: string; units: number }>,
  actions: ProductDetailActions,
): ProductDetailView {
  const overlay = document.createElement('div')
  overlay.className = 'sheet-overlay'
  overlay.dataset.role = 'product-detail-overlay'

  const sheet = document.createElement('section')
  sheet.className = 'sheet sheet--product'
  sheet.setAttribute('role', 'dialog')
  sheet.setAttribute('aria-label', `Detalhe do produto ${product.ref}`)

  const tint = tintFor(product.tintIndex)

  sheet.innerHTML = `
    <div class="sheet-grip" aria-hidden="true"></div>
    <header class="sheet-header">
      <div class="sheet-title-block">
        <span class="sheet-eyebrow">${escapeHtml(product.ref)}</span>
        <h2 class="sheet-title">${escapeHtml(product.name)}</h2>
        <p class="sheet-meta">${escapeHtml(product.price)} · MOQ ${product.moq}</p>
      </div>
      <button type="button" class="sheet-close" data-action="dismiss" aria-label="Fechar">×</button>
    </header>
    <div class="product-detail-preview" style="--card-tint-bg:${tint.background};--card-tint-fg:${tint.foreground}"></div>
    <div class="product-detail-sizes" data-role="sizes"></div>
    <div class="product-detail-stepper">
      <button type="button" class="product-detail-step" data-action="dec" aria-label="Diminuir">−</button>
      <span class="product-detail-qty" data-role="qty">${product.moq}</span>
      <button type="button" class="product-detail-step" data-action="inc" aria-label="Aumentar">+</button>
      <span class="product-detail-step-hint">MOQ ${product.moq}</span>
    </div>
    <button type="button" class="product-detail-add" data-action="add" disabled>
      <span data-role="add-label">Escolha um tamanho</span>
    </button>
  `

  overlay.appendChild(sheet)
  host.appendChild(overlay)

  const preview = sheet.querySelector<HTMLElement>('.product-detail-preview')!
  const previewCard = renderProductCard(product)
  preview.appendChild(previewCard)

  const sizesContainer = sheet.querySelector<HTMLElement>('[data-role="sizes"]')!
  const sizeButtons = new Map<string, HTMLButtonElement>()
  const currentUnitsBySize = new Map<string, number>()
  initialLines.forEach((line) => currentUnitsBySize.set(line.size, line.units))

  let selectedSize: string | null = null
  let qty = product.moq

  const qtyLabel = sheet.querySelector<HTMLElement>('[data-role="qty"]')!
  const addBtn = sheet.querySelector<HTMLButtonElement>('[data-action="add"]')!
  const addLabel = sheet.querySelector<HTMLElement>('[data-role="add-label"]')!

  const updateAddLabel = (): void => {
    if (!selectedSize) {
      addBtn.disabled = true
      addLabel.textContent = 'Escolha um tamanho'
      return
    }
    addBtn.disabled = false
    const current = currentUnitsBySize.get(selectedSize) ?? 0
    if (current === 0) {
      addLabel.textContent = `Adicionar ${qty}un tamanho ${selectedSize}`
    } else if (qty === current) {
      addLabel.textContent = `Já tem ${current}un de ${selectedSize}`
    } else if (qty > current) {
      addLabel.textContent = `Atualizar para ${qty}un (${selectedSize})`
    } else {
      addLabel.textContent = `Reduzir para ${qty}un (${selectedSize})`
    }
  }

  const setSelectedSize = (size: string): void => {
    selectedSize = size
    sizeButtons.forEach((btn, key) => {
      btn.dataset.selected = key === size ? 'true' : 'false'
    })
    const stored = currentUnitsBySize.get(size)
    qty = stored && stored > 0 ? stored : product.moq
    qtyLabel.textContent = String(qty)
    updateAddLabel()
  }

  product.sizes.forEach((size) => {
    const btn = document.createElement('button')
    btn.type = 'button'
    btn.className = 'product-detail-size'
    btn.dataset.size = size
    btn.dataset.selected = 'false'
    btn.dataset.hasUnits = (currentUnitsBySize.get(size) ?? 0) > 0 ? 'true' : 'false'
    btn.innerHTML = `
      <span class="product-detail-size-label">${escapeHtml(size)}</span>
      <span class="product-detail-size-units" data-role="size-units">${
        (currentUnitsBySize.get(size) ?? 0) > 0
          ? `${currentUnitsBySize.get(size)}un`
          : ''
      }</span>
    `
    btn.addEventListener('click', () => setSelectedSize(size))
    sizeButtons.set(size, btn)
    sizesContainer.appendChild(btn)
  })

  sheet.querySelector<HTMLButtonElement>('[data-action="inc"]')?.addEventListener('click', () => {
    qty = Math.min(qty + product.moq, 999)
    qtyLabel.textContent = String(qty)
    updateAddLabel()
  })
  sheet.querySelector<HTMLButtonElement>('[data-action="dec"]')?.addEventListener('click', () => {
    const next = qty - product.moq
    qty = Math.max(product.moq, next)
    qtyLabel.textContent = String(qty)
    updateAddLabel()
  })

  addBtn.addEventListener('click', () => {
    if (!selectedSize) return
    actions.onAdd(selectedSize, qty)
  })

  sheet
    .querySelector<HTMLButtonElement>('[data-action="dismiss"]')
    ?.addEventListener('click', () => actions.onDismiss())
  overlay.addEventListener('click', (event) => {
    if (event.target === overlay) actions.onDismiss()
  })

  return {
    setCurrentUnits(size, units) {
      currentUnitsBySize.set(size, units)
      const btn = sizeButtons.get(size)
      if (btn) {
        btn.dataset.hasUnits = units > 0 ? 'true' : 'false'
        const unitsEl = btn.querySelector<HTMLElement>('[data-role="size-units"]')
        if (unitsEl) unitsEl.textContent = units > 0 ? `${units}un` : ''
      }
      if (selectedSize === size) updateAddLabel()
    },
    destroy() {
      overlay.remove()
    },
  }
}

export function findProduct(productId: string): Product | null {
  return SampleCatalog.products.find((p) => p.ref === productId) ?? null
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
