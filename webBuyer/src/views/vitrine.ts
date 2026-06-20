import { formatBrl, type Snapshot, type SnapshotProduct } from '../data/snapshot'
import type { CartSnapshotView } from '../cart/vitrineCart'

export type VitrineActions = {
  onOpenProduct?: (product: SnapshotProduct) => void
  onOpenCart?: () => void
}

export type VitrineView = {
  setCart(view: CartSnapshotView): void
  host(): HTMLElement
}

export function renderVitrine(
  root: HTMLElement,
  snapshot: Snapshot,
  actions: VitrineActions = {},
): VitrineView {
  const firstName = snapshot.customerName?.split(' ')[0]
  const greeting = firstName ? `Olá, ${firstName}` : 'Catálogo selecionado'
  const sellerLine = `${escapeHtml(snapshot.sellerName)} montou esta seleção pra você. Toque numa peça pra montar seu pedido.`

  root.innerHTML = `
    <section class="vitrine" data-view="vitrine" data-cart-empty="true">
      <header class="vitrine-header">
        <div class="vitrine-wordmark">
          <img class="vitrine-wordmark-img" src="/trovata-logo.png" alt="Trovata" />
        </div>
        <span class="vitrine-eyebrow">${escapeHtml(snapshot.collectionLabel)}</span>
        <h1 class="vitrine-title">${escapeHtml(greeting)}</h1>
        <p class="vitrine-sub">${sellerLine}</p>
        <div class="vitrine-meta">
          <span class="vitrine-chip">${snapshot.products.length} peças</span>
          <span class="vitrine-chip vitrine-chip--ghost">Preços já aplicados</span>
        </div>
      </header>

      <div class="vitrine-grid" data-role="grid"></div>

      <div class="vitrine-dock" data-role="cart-dock" hidden>
        <button type="button" class="vitrine-dock-btn" data-action="open-cart">
          <span class="vitrine-dock-text">
            <span class="vitrine-dock-units" data-role="cart-units">0 itens</span>
            <span class="vitrine-dock-subtotal" data-role="cart-subtotal">R$ 0,00</span>
          </span>
          <span class="vitrine-dock-cta">Ver pedido →</span>
        </button>
      </div>

      <div class="sheet-host" data-role="sheet-host"></div>
    </section>
  `

  const section = root.querySelector<HTMLElement>('.vitrine')!
  const grid = section.querySelector<HTMLElement>('[data-role="grid"]')!
  const dock = section.querySelector<HTMLElement>('[data-role="cart-dock"]')!
  const dockUnits = section.querySelector<HTMLElement>('[data-role="cart-units"]')!
  const dockSubtotal = section.querySelector<HTMLElement>('[data-role="cart-subtotal"]')!
  const dockBtn = section.querySelector<HTMLButtonElement>('[data-action="open-cart"]')!
  const sheetHost = section.querySelector<HTMLElement>('[data-role="sheet-host"]')!

  snapshot.products.forEach((product) => {
    const card = renderCard(product)
    card.addEventListener('click', () => actions.onOpenProduct?.(product))
    grid.appendChild(card)
  })

  dockBtn.addEventListener('click', () => actions.onOpenCart?.())

  return {
    setCart(view) {
      const empty = view.totalUnits === 0
      section.dataset.cartEmpty = empty ? 'true' : 'false'
      dock.hidden = empty
      dockUnits.textContent = `${view.totalUnits} ${view.totalUnits === 1 ? 'item' : 'itens'}`
      dockSubtotal.textContent = formatBrl(view.totalCents)
    },
    host() {
      return sheetHost
    },
  }
}

function renderCard(product: SnapshotProduct): HTMLElement {
  const card = document.createElement('article')
  card.className = 'vc-card'
  card.dataset.productId = String(product.productId)
  card.dataset.ref = product.ref
  card.setAttribute('role', 'button')
  card.setAttribute('tabindex', '0')
  card.setAttribute('aria-label', `${product.name}, ${formatBrl(product.priceCents)}`)

  const visual = product.imageUrl
    ? `<img class="vc-card-photo" src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" loading="lazy" />`
    : `<div class="vc-card-photo vc-card-photo--empty" aria-hidden="true"></div>`

  const stock = stockLabel(product)

  card.innerHTML = `
    <div class="vc-card-frame">
      ${visual}
      ${product.ref ? `<span class="vc-card-ref">${escapeHtml(product.ref)}</span>` : ''}
      ${stock ? `<span class="vc-card-badge" data-tone="${stock.tone}">${escapeHtml(stock.text)}</span>` : ''}
    </div>
    <div class="vc-card-body">
      <h3 class="vc-card-name">${escapeHtml(product.name)}</h3>
      <div class="vc-card-row">
        <span class="vc-card-price">${formatBrl(product.priceCents)}</span>
        ${product.brand ? `<span class="vc-card-brand">${escapeHtml(product.brand)}</span>` : ''}
      </div>
    </div>
  `

  card.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      card.click()
    }
  })

  return card
}

function stockLabel(product: SnapshotProduct): { text: string; tone: string } | null {
  const available = product.available
  if (available == null) return null
  if (available <= 0) return { text: 'Esgotado', tone: 'out' }
  if (available <= 6) return { text: `Últimas ${available}`, tone: 'low' }
  return { text: 'Pronta entrega', tone: 'ok' }
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
