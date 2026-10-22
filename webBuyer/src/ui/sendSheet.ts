import type { CartState } from '../cart/storefrontCart'
import { formatBrl } from '../data/formatBrl'
import type { SnapshotProduct } from '../data/snapshot'
import { escapeHtml } from './escapeHtml'

type SendSheetActions = {
  onConfirm: () => void
  onCopy: () => Promise<boolean>
  onDismiss: () => void
}

export type SendSheetView = {
  destroy(): void
}

export function mountSendSheet(
  host: HTMLElement,
  products: Map<number, SnapshotProduct>,
  cart: CartState,
  sellerName: string,
  actions: SendSheetActions,
): SendSheetView {
  const overlay = document.createElement('div')
  overlay.className = 'sheet-overlay'
  overlay.dataset.role = 'send-overlay'

  const sheet = document.createElement('section')
  sheet.className = 'sheet sheet--send'
  sheet.setAttribute('role', 'dialog')
  sheet.setAttribute('aria-label', 'Enviar pedido')

  const lines = cart.lines
    .map((line) => {
      const product = products.get(line.productId)
      const name = product?.name ?? `Produto ${line.productId}`
      return `
        <li class="send-line">
          <span class="send-line-name">${escapeHtml(name)}</span>
          <span class="send-line-meta">${escapeHtml(line.sizeLabel)} · ${line.qty} un</span>
        </li>
      `
    })
    .join('')

  sheet.innerHTML = `
    <div class="sheet-grip" aria-hidden="true"></div>
    <header class="sheet-header">
      <div class="sheet-title-block">
        <span class="sheet-eyebrow">Quase lá</span>
        <h2 class="sheet-title">Enviar para ${escapeHtml(sellerName)}</h2>
        <p class="sheet-meta">${cart.totalUnits} un · ${formatBrl(cart.totalCents)}</p>
      </div>
      <button type="button" class="sheet-close" data-action="dismiss" aria-label="Fechar">×</button>
    </header>

    <ul class="send-lines">${lines}</ul>

    <p class="send-explainer">
      Vamos abrir o WhatsApp com o resumo e o pedido já montado. Escolha o seu vendedor e toque em enviar.
    </p>

    <button type="button" class="vitrine-send-btn" data-action="confirm">
      <span class="vitrine-send-icon" aria-hidden="true">↗</span>
      Abrir o WhatsApp
    </button>
    <button type="button" class="vitrine-copy-btn" data-action="copy">Copiar resumo do pedido</button>
  `

  overlay.appendChild(sheet)
  host.appendChild(overlay)

  sheet
    .querySelector<HTMLButtonElement>('[data-action="dismiss"]')
    ?.addEventListener('click', () => actions.onDismiss())
  sheet
    .querySelector<HTMLButtonElement>('[data-action="confirm"]')
    ?.addEventListener('click', () => actions.onConfirm())

  const copyBtn = sheet.querySelector<HTMLButtonElement>('[data-action="copy"]')
  copyBtn?.addEventListener('click', async () => {
    const ok = await actions.onCopy()
    copyBtn.textContent = ok ? 'Resumo copiado ✓' : 'Não consegui copiar'
    copyBtn.disabled = true
  })

  overlay.addEventListener('click', (event) => {
    if (event.target === overlay) actions.onDismiss()
  })

  return {
    destroy() {
      overlay.remove()
    },
  }
}
