// app.jsx — TrovataCast prototype assembled in a DesignCanvas

const PHONE_W = 380;
const PHONE_H = 780;

// ─── A wrapper that places content inside an IOSDevice ──────────
function Seller({ children, dark = false }) {
  return (
    <div className="tc" style={{ width: PHONE_W, height: PHONE_H, position: 'relative' }}>
      <IOSDevice width={PHONE_W} height={PHONE_H} dark={dark}>
        {children}
      </IOSDevice>
    </div>
  );
}

function Buyer({ children, dark = false }) {
  return (
    <div className="tc" style={{ width: PHONE_W, height: PHONE_H, position: 'relative' }}>
      <AndroidDevice width={PHONE_W} height={PHONE_H} dark={dark}>
        {children}
      </AndroidDevice>
    </div>
  );
}

// ─── Co-presence overlay — animated visual annotation ───────────
// Used in the live session row to make the sync between phones legible.
function CoPresenceCallout({ from = 0, to = 0, label, dotColor = 'var(--brand)', dir = 'right' }) {
  return (
    <div style={{
      position: 'absolute', left: from, right: 'auto', top: 0, zIndex: 5,
      pointerEvents: 'none',
    }}/>
  );
}

// ─── Top-of-canvas brand banner ─────────────────────────────────
function Banner() {
  return (
    <div style={{
      padding: '36px 60px 18px',
      display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 32,
    }}>
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14 }}>
          <TrovataMark size={34}/>
          <div>
            <div style={{ fontSize: 18, fontWeight: 600, letterSpacing: '-0.02em', color: 'var(--ink)' }}>TrovataCast</div>
            <div style={{ fontSize: 11.5, color: 'var(--ink-3)', textTransform: 'uppercase', letterSpacing: '0.10em', fontWeight: 500, marginTop: 2 }}>
              Protótipo conceitual · 7 cenas
            </div>
          </div>
        </div>
        <div style={{ fontSize: 38, fontWeight: 600, letterSpacing: '-0.03em', color: 'var(--ink)', lineHeight: 1.05, maxWidth: 720 }}>
          Vendedor e cliente vendo a mesma coisa, ao mesmo tempo, em telas diferentes.
        </div>
        <div style={{ marginTop: 14, fontSize: 15, color: 'var(--ink-3)', lineHeight: 1.45, maxWidth: 660 }}>
          Cada par de telas mostra <b style={{ color: 'var(--ink)' }}>iPhone do vendedor</b> à esquerda e
          {' '}<b style={{ color: 'var(--ink)' }}>navegador do cliente</b> à direita —
          a mesma sessão, dois pontos de vista.
        </div>
      </div>

      <div style={{
        display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'flex-start',
        background: 'var(--surface)', border: '1px solid var(--line)',
        borderRadius: 14, padding: 16, minWidth: 260, boxShadow: 'var(--sh-1)',
      }}>
        <div style={{ fontSize: 10.5, fontWeight: 600, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>Cenário</div>
        <div style={{ fontSize: 13, color: 'var(--ink-2)', lineHeight: 1.5 }}>
          <b>Camila Tavares</b>, representante da<br/>
          <b>Atelier Norte</b> — Coleção Outono 26<br/>
          mostra o catálogo para<br/>
          <b>Diego Albuquerque</b>, dono da<br/>
          <b>Trama Multimarcas</b> (BH).
        </div>
      </div>
    </div>
  );
}

// ─── Section: pair seller + buyer artboards side-by-side ────────
// DesignCanvas places artboards in the same DCSection on one row.
// Seller comes first, buyer next, separated visually by their label.

function App() {
  return (
    <>
      <Banner/>
      <DesignCanvas>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="prep" title="1 · Antes da chamada"
                   subtitle="Camila monta a sessão no iPhone e manda o link para o Diego pelo WhatsApp.">
          <DCArtboard id="seller-home" label="Vendedor · Sessões" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerHome/></Seller>
          </DCArtboard>
          <DCArtboard id="seller-prep" label="Vendedor · Selecionando produtos" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerPrep/></Seller>
          </DCArtboard>
          <DCArtboard id="seller-link" label="Vendedor · Enviando o link" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerCreateLink/></Seller>
          </DCArtboard>
        </DCSection>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="arrival" title="2 · Cliente entra pelo link"
                   subtitle="Sem instalar nada. O link abre no navegador do celular do Diego.">
          <DCArtboard id="buyer-arrival" label="Cliente · Convite no navegador" width={PHONE_W} height={PHONE_H}>
            <Buyer><BuyerArrival/></Buyer>
          </DCArtboard>
        </DCSection>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="live" title="3 · Sessão ao vivo · co-presença"
                   subtitle="A premissa central: scroll sincronizado, ponteiro compartilhado, carrinho em tempo real. As duas telas existem no mesmo instante.">
          <DCArtboard id="seller-live" label="Vendedor · Apontando · vê o carrinho" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerLive/></Seller>
          </DCArtboard>
          <DCArtboard id="buyer-live" label="Cliente · Vê o ponteiro da Camila" width={PHONE_W} height={PHONE_H}>
            <Buyer><BuyerLive/></Buyer>
          </DCArtboard>
        </DCSection>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="detail" title="4 · Foco em um produto"
                   subtitle="Quando o cliente toca em um item, o vendedor vê — com tempo de leitura, qual cor está olhando, e pode sugerir oferta sem interromper.">
          <DCArtboard id="seller-detail" label="Vendedor · Vê o que Diego está fazendo" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerProductDetail/></Seller>
          </DCArtboard>
          <DCArtboard id="buyer-detail" label="Cliente · Detalhe do produto" width={PHONE_W} height={PHONE_H}>
            <Buyer><BuyerProductDetail/></Buyer>
          </DCArtboard>
        </DCSection>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="closing" title="5 · Encerramento — pedido pronto"
                   subtitle="Sem digitação posterior. O pedido nasce na chamada, formatado e completo.">
          <DCArtboard id="seller-summary" label="Vendedor · Resumo automático" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerSummary/></Seller>
          </DCArtboard>
          <DCArtboard id="buyer-confirm" label="Cliente · Pedido confirmado" width={PHONE_W} height={PHONE_H}>
            <Buyer><BuyerConfirmation/></Buyer>
          </DCArtboard>
        </DCSection>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="async" title="6 · O catálogo continua aberto"
                   subtitle="O cliente decide com calma; o carrinho está preservado; o vendedor é notificado de qualquer mexida.">
          <DCArtboard id="buyer-async" label="Cliente · Catálogo assíncrono" width={PHONE_W} height={PHONE_H}>
            <Buyer><BuyerAsync/></Buyer>
          </DCArtboard>
        </DCSection>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="pipeline" title="7 · Inteligência sobre a relação"
                   subtitle="Cada sessão alimenta o pipeline. O TrovataCast vira a memória comercial da carteira.">
          <DCArtboard id="seller-pipeline" label="Vendedor · Pipeline (legado)" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerPipeline/></Seller>
          </DCArtboard>
          <DCArtboard id="seller-insights" label="Vendedor · Insights (novo)" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerInsights/></Seller>
          </DCArtboard>
        </DCSection>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="tabs" title="8 · As outras abas do app"
                   subtitle="Catálogo, Clientes e Conta — o vendedor entre as chamadas.">
          <DCArtboard id="seller-catalog" label="Vendedor · Catálogo" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerCatalog/></Seller>
          </DCArtboard>
          <DCArtboard id="seller-clients" label="Vendedor · Clientes" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerClients/></Seller>
          </DCArtboard>
          <DCArtboard id="seller-account" label="Vendedor · Minha conta" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerAccount/></Seller>
          </DCArtboard>
        </DCSection>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="product-detail" title="10 · Detalhes do produto · navegação livre"
                   subtitle="Quando se toca em um card — no Catálogo do vendedor ou no navegador do cliente — abre a ficha completa, com botão de voltar.">
          <DCArtboard id="seller-product-detail" label="Vendedor · Ficha do produto" width={PHONE_W} height={PHONE_H}>
            <Seller><SellerCatalogProductDetail/></Seller>
          </DCArtboard>
          <DCArtboard id="buyer-product-browse" label="Cliente · Ficha do produto" width={PHONE_W} height={PHONE_H}>
            <Buyer><BuyerProductBrowse/></Buyer>
          </DCArtboard>
        </DCSection>

        {/* ───────────────────────────────────────────────── */}
        <DCSection id="auth" title="9 · Entrar no app"
                   subtitle="Fluxo de login pelo WhatsApp — sem senha, sem fricção. O primeiro acesso configura a marca representada.">
          <DCArtboard id="auth-welcome" label="Boas-vindas" width={PHONE_W} height={PHONE_H}>
            <Seller><AuthWelcome/></Seller>
          </DCArtboard>
          <DCArtboard id="auth-login" label="Número de WhatsApp" width={PHONE_W} height={PHONE_H}>
            <Seller><AuthLogin/></Seller>
          </DCArtboard>
          <DCArtboard id="auth-verify" label="Código de 6 dígitos" width={PHONE_W} height={PHONE_H}>
            <Seller><AuthVerify/></Seller>
          </DCArtboard>
          <DCArtboard id="auth-setup" label="Primeiro acesso · marca" width={PHONE_W} height={PHONE_H}>
            <Seller><AuthSetup/></Seller>
          </DCArtboard>
        </DCSection>

      </DesignCanvas>
    </>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
