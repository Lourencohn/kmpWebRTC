// frames-buyer.jsx — Buyer-side screens (Android browser)
// The buyer never installs anything — everything happens in a mobile browser.

// ─── Simulated Chrome-on-Android URL bar ────────────────────────
function ChromeBar({ url = 'trovata.cast/d3e4-1f', secure = true }) {
  return (
    <div style={{
      height: 56, background: '#FFFFFF', borderBottom: '1px solid var(--line)',
      display: 'flex', alignItems: 'center', gap: 8, padding: '0 10px',
    }}>
      <div style={{
        flex: 1, height: 36, borderRadius: 100, background: '#F1F1ED',
        display: 'flex', alignItems: 'center', gap: 8, padding: '0 12px',
        fontSize: 12.5, color: 'var(--ink-2)', letterSpacing: '-0.005em',
      }}>
        <Icon d={secure ? Icons.lock : Icons.globe} size={13} stroke="var(--ink-4)" sw={2}/>
        <span style={{ flex: 1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontFamily: 'var(--f-mono)' }}>{url}</span>
        <Icon d={Icons.more} size={14} stroke="var(--ink-4)"/>
      </div>
      <div style={{
        width: 24, height: 24, borderRadius: '50%',
        background: 'var(--surface-3)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 11, fontWeight: 700, color: 'var(--ink-3)',
      }}>3</div>
    </div>
  );
}

// Buyer top header during a call (seller PiP + call status)
function BuyerCallHeader({ name = 'Camila', muted = false, time = '04:18' }) {
  return (
    <div style={{
      padding: '10px 14px', background: 'var(--surface)',
      borderBottom: '1px solid var(--line)',
      display: 'flex', alignItems: 'center', gap: 10,
    }}>
      <VideoTile name={name} hue={210} size={36} mini label={false} style={{ height: 36, width: 36, borderRadius: 8 }} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13.5, fontWeight: 600, letterSpacing: '-0.01em' }}>
          {name} · Atelier Norte
        </div>
        <div style={{ fontSize: 10.5, color: 'var(--ink-3)', display: 'flex', gap: 5, alignItems: 'center' }}>
          <span className="tc-live-dot"/> Mostrando · {time}
        </div>
      </div>
      <IconBtn icon="mic" kind={muted ? 'line' : 'jade'} size={32}/>
      <IconBtn icon="hangup" kind="danger" size={32}/>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// 1 · BuyerArrival — opens the seller's link in mobile browser
// ────────────────────────────────────────────────────────────────
function BuyerArrival() {
  return (
    <div style={{ minHeight: '100%', background: 'var(--bg)' }}>
      <ChromeBar />
      <div style={{ padding: '0 18px 60px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', minHeight: 'calc(100% - 56px)' }}>
        {/* Seller's avatar with pulsing ring */}
        <div style={{ marginTop: 36, position: 'relative', width: 110, height: 110 }}>
          <div style={{
            position: 'absolute', inset: 0, borderRadius: '50%',
            border: '2px solid var(--brand)', animation: 'tc-ring 2.4s ease-out infinite',
          }}/>
          <div style={{
            position: 'absolute', inset: 6, borderRadius: '50%',
            border: '2px solid var(--brand)', animation: 'tc-ring 2.4s ease-out 0.8s infinite',
          }}/>
          <div style={{
            position: 'absolute', inset: 14, borderRadius: '50%',
            background: 'oklch(86% 0.07 210)', color: 'oklch(34% 0.10 210)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: 600, fontSize: 30,
          }}>CT</div>
        </div>

        <div style={{ marginTop: 26, fontSize: 12, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.10em', fontWeight: 600 }}>
          Convidando você
        </div>
        <div style={{ marginTop: 6, fontSize: 24, fontWeight: 600, letterSpacing: '-0.025em', lineHeight: 1.2 }}>
          Camila quer mostrar a<br/>
          coleção Outono 26
        </div>
        <div style={{ marginTop: 8, fontSize: 13.5, color: 'var(--ink-3)', lineHeight: 1.45, maxWidth: 280 }}>
          Vocês vão ver o catálogo juntos. Você pode marcar produtos enquanto conversam.
        </div>

        <div style={{ marginTop: 26, padding: '12px 14px', borderRadius: 14, background: 'var(--surface)', border: '1px solid var(--line)', width: '100%', boxShadow: 'var(--sh-1)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <TrovataMark size={28}/>
            <div style={{ textAlign: 'left' }}>
              <div style={{ fontSize: 12.5, fontWeight: 600 }}>TrovataCast</div>
              <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>Sem instalar. Áudio + catálogo ao vivo.</div>
            </div>
          </div>
          <div style={{ marginTop: 10, padding: '10px 12px', background: 'var(--surface-2)', borderRadius: 10, display: 'flex', alignItems: 'center', gap: 8, textAlign: 'left' }}>
            <Icon d={Icons.mic} size={16} stroke="var(--ink-3)"/>
            <span style={{ fontSize: 12, color: 'var(--ink-3)', flex: 1 }}>Vamos pedir microfone — câmera é opcional.</span>
          </div>
        </div>

        <div style={{ marginTop: 'auto', paddingTop: 32, width: '100%' }}>
          <Btn kind="primary" size="lg" icon="video" style={{ width: '100%' }}>
            Entrar com áudio
          </Btn>
          <Btn kind="ghost" size="md" style={{ width: '100%', marginTop: 8 }}>
            Só assistir, sem falar
          </Btn>
        </div>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// 2 · BuyerLive — live catalog, synced to seller, with PiP + pointer
// ────────────────────────────────────────────────────────────────
function BuyerLive({ pointed = 'AN-088', cart = { 'AN-217': 12 }, showSellerPointer = true }) {
  return (
    <div style={{ minHeight: '100%', background: 'var(--bg)', position: 'relative' }}>
      <ChromeBar />
      <BuyerCallHeader />

      {/* Sync banner */}
      <div style={{
        background: 'var(--brand-tint)', color: 'var(--brand-2)',
        padding: '7px 14px', display: 'flex', alignItems: 'center', gap: 8,
        fontSize: 11.5, fontWeight: 500,
      }}>
        <Icon d={Icons.signal} size={14} sw={2}/>
        Camila está te mostrando: <b style={{ fontWeight: 600 }}>Tricot & camisaria</b>
        <span style={{ flex: 1 }}/>
        <span style={{ opacity: .65, fontSize: 10.5 }}>4 produtos</span>
      </div>

      {/* Catalog grid */}
      <div style={{ padding: '12px 14px 12px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, position: 'relative' }}>
        {CATALOG.slice(0, 4).map(p => (
          <ProductCard
            key={p.ref}
            p={p}
            size="sm"
            highlight={p.ref === pointed}
            pointed={p.ref === pointed}
            inCart={cart[p.ref] || 0}
          />
        ))}

        {/* Seller's pointer (orange-ish) animating over the catalog */}
        {showSellerPointer && <RemotePointer name="Camila" hue={30} x={210} y={88} />}
      </div>

      {/* Co-presence ribbon */}
      <div style={{
        margin: '0 14px', padding: 12, borderRadius: 12,
        background: 'var(--surface)', border: '1px solid var(--line)',
        display: 'flex', alignItems: 'center', gap: 10,
      }}>
        <div style={{
          width: 28, height: 28, borderRadius: 6,
          background: 'oklch(94% 0.04 30)', color: 'oklch(50% 0.16 30)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon d={Icons.pointer} size={14} sw={2}/>
        </div>
        <div style={{ flex: 1, fontSize: 12.5, color: 'var(--ink-2)', lineHeight: 1.4 }}>
          Camila está apontando para o <b>Vestido Midi</b>
        </div>
      </div>

      <div style={{ padding: '12px 14px 0' }}>
        <SectionLabel>Você reagiu</SectionLabel>
        <div style={{ display: 'flex', gap: 8 }}>
          {[CATALOG[1], CATALOG[6]].map(p => (
            <div key={p.ref} style={{
              flex: 1, padding: '8px 10px', borderRadius: 10,
              background: 'var(--surface)', border: '1px solid var(--line)',
              display: 'flex', alignItems: 'center', gap: 8,
            }}>
              <div style={{
                width: 28, height: 28, borderRadius: 6,
                background: BgTints[p.tint].bg, color: BgTints[p.tint].fg,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <svg width="74%" height="74%" viewBox="0 0 100 100">{Garments[p.garment]}</svg>
              </div>
              <div style={{ fontSize: 11.5, fontWeight: 500, lineHeight: 1.2, flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.name}</div>
              <Icon d={Icons.heart} size={12} fill="var(--live)" stroke="none"/>
            </div>
          ))}
        </div>
      </div>

      {/* Seller PiP — top-right floating */}
      <div style={{ position: 'absolute', right: 12, top: 122, zIndex: 30 }}>
        <VideoTile name="Camila" hue={210} size={72} label="Camila"/>
      </div>

      {/* Cart docked tab */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, paddingBottom: 12,
        display: 'flex', justifyContent: 'center',
      }}>
        <div style={{
          background: 'var(--ink)', color: '#fff', borderRadius: 14,
          padding: '10px 14px', display: 'flex', alignItems: 'center', gap: 10,
          boxShadow: '0 12px 28px rgba(14,17,22,.18)',
        }}>
          <Icon d={Icons.cart} size={16} sw={2}/>
          <div style={{ fontSize: 12.5, fontWeight: 500 }}>1 item · 12 un</div>
          <span style={{ opacity: .45 }}>·</span>
          <div style={{ fontSize: 12.5, fontFamily: 'var(--f-mono)', fontWeight: 600 }}>R$ 1.428,00</div>
          <Icon d={Icons.chev} size={14} style={{ opacity: .65 }} sw={2}/>
        </div>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// 3 · BuyerProductDetail — synced product page, seller still in PiP
// ────────────────────────────────────────────────────────────────
function BuyerProductDetail() {
  const p = CATALOG[2]; // Vestido Midi
  const tint = BgTints[p.tint];
  return (
    <div style={{ minHeight: '100%', background: 'var(--bg)', position: 'relative' }}>
      <ChromeBar />
      <BuyerCallHeader />

      <div style={{
        background: 'var(--brand-tint)', color: 'var(--brand-2)',
        padding: '7px 14px', display: 'flex', alignItems: 'center', gap: 6,
        fontSize: 11.5, fontWeight: 500,
      }}>
        <Icon d={Icons.eye} size={13} sw={2}/>
        Camila te trouxe a este produto
      </div>

      {/* Image */}
      <div style={{ padding: '14px 14px 0' }}>
        <div style={{
          height: 220, borderRadius: 16, background: tint.bg, color: tint.fg,
          position: 'relative', overflow: 'hidden',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="62%" height="62%" viewBox="0 0 100 100">{Garments[p.garment]}</svg>
          <RemotePointer name="Camila" hue={30} x={200} y={120} />
          <div style={{
            position: 'absolute', bottom: 10, left: '50%', transform: 'translateX(-50%)',
            display: 'flex', gap: 5,
          }}>
            {[0,1,2,3].map(i => (
              <span key={i} style={{
                width: i === 1 ? 14 : 6, height: 6, borderRadius: 3,
                background: i === 1 ? 'rgba(0,0,0,.6)' : 'rgba(0,0,0,.20)',
              }}/>
            ))}
          </div>
        </div>
      </div>

      <div style={{ padding: '14px 16px 0' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 10 }}>
          <div>
            <div style={{ fontSize: 10.5, fontFamily: 'var(--f-mono)', color: 'var(--ink-4)', letterSpacing: '0.06em' }}>{p.ref}</div>
            <div style={{ fontSize: 18, fontWeight: 600, letterSpacing: '-0.015em', marginTop: 2 }}>{p.name}</div>
            <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 4 }}>Algodão 100% · feito em São Paulo</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 11, color: 'var(--ink-4)' }}>atacado</div>
            <div style={{ fontSize: 18, fontWeight: 700, fontFamily: 'var(--f-mono)' }}>{p.price}</div>
          </div>
        </div>

        <div style={{ marginTop: 14 }}>
          <div style={{ fontSize: 11, color: 'var(--ink-3)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600, marginBottom: 6 }}>Cor</div>
          <div style={{ display: 'flex', gap: 8 }}>
            {[['#3F4744','Verde-musgo'], ['#A5806A','Areia']].map(([c, n], i) => (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 6,
                padding: '6px 10px 6px 6px', borderRadius: 100,
                background: 'var(--surface)', border: `1px solid ${i === 0 ? 'var(--ink)' : 'var(--line)'}`,
                fontSize: 12, fontWeight: 500,
              }}>
                <span style={{ width: 18, height: 18, borderRadius: '50%', background: c }}/>{n}
              </div>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 16 }}>
          <div style={{ fontSize: 11, color: 'var(--ink-3)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600, marginBottom: 6 }}>Grade · quantidade</div>
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8,
            background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 12, padding: 10,
          }}>
            {[['P', 2], ['M', 2], ['G', 0]].map(([s, n], i) => (
              <div key={i} style={{
                display: 'flex', flexDirection: 'column', alignItems: 'center',
                padding: '6px 0', borderRadius: 8,
                background: n > 0 ? 'var(--brand-tint)' : 'var(--surface-2)',
              }}>
                <div style={{ fontSize: 11, color: 'var(--ink-3)', fontWeight: 600 }}>Tam {s}</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                  <Icon d={Icons.minus} size={14} sw={2.4} stroke="var(--ink-3)"/>
                  <span style={{ fontSize: 18, fontWeight: 700, minWidth: 18, textAlign: 'center', fontFamily: 'var(--f-mono)' }}>{n}</span>
                  <Icon d={Icons.plus}  size={14} sw={2.4} stroke="var(--ink-3)"/>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Seller PiP */}
      <div style={{ position: 'absolute', right: 12, top: 140, zIndex: 30 }}>
        <VideoTile name="Camila" hue={210} size={64} label="Camila"/>
      </div>

      {/* Add to cart bar */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, paddingBottom: 18,
        padding: '12px 14px 18px', background: 'var(--surface)',
        borderTop: '1px solid var(--line)',
        display: 'flex', gap: 8, alignItems: 'center',
      }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 11, color: 'var(--ink-4)' }}>Subtotal · 4 un</div>
          <div style={{ fontSize: 17, fontWeight: 700, fontFamily: 'var(--f-mono)' }}>R$ 636,00</div>
        </div>
        <Btn kind="jade" size="lg" icon="plus">Adicionar</Btn>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// 4 · BuyerConfirmation — call ended, order delivered
// ────────────────────────────────────────────────────────────────
function BuyerConfirmation() {
  return (
    <div style={{ minHeight: '100%', background: 'var(--bg)' }}>
      <ChromeBar />
      <div style={{ padding: '0 18px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', minHeight: 'calc(100% - 56px)' }}>

        <div style={{
          marginTop: 26, width: 64, height: 64, borderRadius: '50%',
          background: 'var(--jade-tint)', color: 'var(--jade-2)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon d={Icons.check} size={32} sw={2.4}/>
        </div>

        <div style={{ marginTop: 14, fontSize: 11.5, color: 'var(--jade-2)', textTransform: 'uppercase', letterSpacing: '0.10em', fontWeight: 600 }}>
          Pedido recebido
        </div>
        <div style={{ marginTop: 6, fontSize: 22, fontWeight: 600, letterSpacing: '-0.02em', lineHeight: 1.2 }}>
          #TC-8842 · Atelier Norte
        </div>
        <div style={{ marginTop: 6, fontSize: 13, color: 'var(--ink-3)', lineHeight: 1.45, maxWidth: 280 }}>
          Camila vai te ligar amanhã para confirmar grade e prazo. Você pode revisar até lá.
        </div>

        <Card pad={14} style={{ width: '100%', marginTop: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
            <span style={{ fontSize: 11.5, color: 'var(--ink-4)', fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.06em' }}>4 itens · 48 un</span>
            <span style={{ fontSize: 17, fontWeight: 700, fontFamily: 'var(--f-mono)' }}>R$ 5.276,80</span>
          </div>
          <div style={{ marginTop: 8 }}>
            {[
              { p: CATALOG[1], qty: 12 },
              { p: CATALOG[0], qty: 8 },
              { p: CATALOG[2], qty: 4 },
              { p: CATALOG[6], qty: 24 },
            ].map((it, i, arr) => (
              <ProductRow key={it.p.ref} p={it.p} qty={it.qty}
                style={{ borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none', padding: '8px 0' }}
              />
            ))}
          </div>
        </Card>

        <div style={{ width: '100%', marginTop: 14, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          <Btn kind="surface" icon="download">PDF</Btn>
          <Btn kind="surface" icon="copy">Compartilhar</Btn>
        </div>

        <div style={{
          width: '100%', marginTop: 16, padding: 12, borderRadius: 12,
          background: 'var(--surface)', border: '1px solid var(--line)',
          display: 'flex', alignItems: 'center', gap: 10, textAlign: 'left',
        }}>
          <div style={{
            width: 32, height: 32, borderRadius: 8,
            background: 'var(--brand-tint)', color: 'var(--brand-2)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon d={Icons.layers} size={16} sw={1.8}/>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 12.5, fontWeight: 600 }}>O catálogo continua aberto</div>
            <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>Adicione mais itens sozinho até amanhã 17h.</div>
          </div>
          <Icon d={Icons.chev} size={16} stroke="var(--ink-3)"/>
        </div>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// 5 · BuyerAsync — async catalog after the call
// ────────────────────────────────────────────────────────────────
function BuyerAsync() {
  return (
    <div style={{ minHeight: '100%', background: 'var(--bg)' }}>
      <ChromeBar url="trovata.cast/d3e4-1f/depois" />

      <div style={{ padding: '14px 16px 4px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <VideoTile name="Camila" hue={210} size={36} mini label={false} style={{ height: 36, width: 36, borderRadius: 8 }} muted/>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 12, color: 'var(--ink-4)', fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
              Pós-chamada · você no comando
            </div>
            <div style={{ fontSize: 15.5, fontWeight: 600, letterSpacing: '-0.015em' }}>
              Continuar com calma
            </div>
          </div>
        </div>
        <div style={{ marginTop: 8, fontSize: 12.5, color: 'var(--ink-3)', lineHeight: 1.45 }}>
          Esses produtos a Camila te apresentou na chamada de hoje. O carrinho está preservado — ajuste no seu tempo.
        </div>
      </div>

      <div style={{ padding: '12px 14px 0', display: 'flex', alignItems: 'center', gap: 8, overflowX: 'auto' }}>
        <Pill tone="dark">Apresentados (4)</Pill>
        <Pill tone="neutral">No carrinho (1)</Pill>
        <Pill tone="neutral">Que eu reagi (2)</Pill>
        <Pill tone="ghost">Coleção toda</Pill>
      </div>

      <div style={{ padding: '12px 14px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {CATALOG.slice(0, 4).map((p, i) => (
          <div key={p.ref} style={{ position: 'relative' }}>
            <ProductCard p={p} size="sm" inCart={i === 1 ? 12 : 0} />
            {/* "Mostrado às" stamp */}
            <div style={{
              position: 'absolute', top: 4, right: 4, padding: '2px 6px',
              background: 'rgba(14,17,22,.78)', color: '#fff', borderRadius: 6,
              fontSize: 9.5, fontWeight: 600, letterSpacing: '0.02em',
              backdropFilter: 'blur(8px)',
            }}>14:0{i+2}</div>
          </div>
        ))}
      </div>

      <div style={{ padding: '0 14px' }}>
        <Card pad={12} style={{ background: 'var(--brand-tint)', border: 'none' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              width: 32, height: 32, borderRadius: 8, background: 'var(--brand)', color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <Icon d={Icons.sparkle} size={16} sw={2}/>
            </div>
            <div style={{ flex: 1, fontSize: 12.5, color: 'var(--brand-2)', lineHeight: 1.4 }}>
              <b>Camila vai saber</b> se você mexer no carrinho — e pode te chamar de novo se quiser tirar uma dúvida.
            </div>
          </div>
        </Card>
      </div>

      <div style={{ padding: '14px 14px 30px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
        <Btn kind="surface" icon="msg">Falar com Camila</Btn>
        <Btn kind="jade" icon="check">Fechar pedido</Btn>
      </div>
    </div>
  );
}

function BuyerProductBrowse() {
  const p = CATALOG[2];
  const tint = BgTints[p.tint];
  const sizes = [
    { s: 'P', qty: 0, avail: true },
    { s: 'M', qty: 6, avail: true },
    { s: 'G', qty: 2, avail: true },
  ];
  const colors = [
    { hex: '#3F4744', name: 'Verde-musgo', on: true  },
    { hex: '#A5806A', name: 'Areia',       on: false },
  ];
  const related = [CATALOG[1], CATALOG[0], CATALOG[6]];

  return (
    <div style={{ minHeight: '100%', background: 'var(--bg)', position: 'relative' }}>
      <ChromeBar url={`trovata.cast/d3e4-1f/p/${p.ref.toLowerCase()}`}/>
      <BuyerCallHeader/>

      <div style={{
        padding: '10px 14px 0', display: 'flex', alignItems: 'center', gap: 8,
      }}>
        <IconBtn icon="back" kind="line" size={36}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 10.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>
            Voltar ao catálogo
          </div>
          <div style={{ fontSize: 13, fontWeight: 600, letterSpacing: '-0.01em', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            Outono 26 · Atelier Norte
          </div>
        </div>
        <IconBtn icon="heart" kind="line" size={36}/>
        <IconBtn icon="share" kind="line" size={36}/>
      </div>

      <div style={{ padding: '12px 14px 0' }}>
        <div style={{
          height: 240, borderRadius: 16, background: tint.bg, color: tint.fg,
          position: 'relative', overflow: 'hidden',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="60%" height="60%" viewBox="0 0 100 100">{Garments[p.garment]}</svg>

          {p.tag && (
            <span style={{
              position: 'absolute', top: 12, left: 12, padding: '4px 9px',
              background: p.tag === 'Top venda' ? 'var(--jade)' : 'var(--ink)',
              color: '#fff', fontSize: 10.5, fontWeight: 600, borderRadius: 6,
            }}>{p.tag}</span>
          )}

          <div style={{
            position: 'absolute', top: 12, right: 12, padding: '4px 9px',
            background: 'rgba(14,17,22,.74)', color: '#fff', borderRadius: 7,
            fontSize: 10, fontWeight: 600, letterSpacing: '0.04em',
            display: 'flex', alignItems: 'center', gap: 4, backdropFilter: 'blur(6px)',
          }}>
            <Icon d={Icons.layers} size={10} sw={2.2}/>2 / 4
          </div>

          <div style={{
            position: 'absolute', bottom: 12, left: '50%', transform: 'translateX(-50%)',
            display: 'flex', gap: 5,
          }}>
            {[0,1,2,3].map(i => (
              <span key={i} style={{
                width: i === 1 ? 14 : 6, height: 6, borderRadius: 3,
                background: i === 1 ? 'rgba(0,0,0,.6)' : 'rgba(0,0,0,.20)',
              }}/>
            ))}
          </div>
        </div>
      </div>

      <div style={{ padding: '14px 16px 0' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 10 }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 10.5, fontFamily: 'var(--f-mono)', color: 'var(--ink-4)', letterSpacing: '0.06em' }}>{p.ref}</div>
            <div style={{ fontSize: 18, fontWeight: 600, letterSpacing: '-0.015em', marginTop: 2, lineHeight: 1.2 }}>{p.name}</div>
            <div style={{ fontSize: 11.5, color: 'var(--ink-3)', marginTop: 4 }}>Algodão 100% · feito em São Paulo</div>
          </div>
          <div style={{ textAlign: 'right', flexShrink: 0 }}>
            <div style={{ fontSize: 10, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>atacado</div>
            <div style={{ fontSize: 18, fontWeight: 700, fontFamily: 'var(--f-mono)' }}>{p.price}</div>
            <div style={{ fontSize: 10, color: 'var(--ink-4)' }}>min {p.moq}un</div>
          </div>
        </div>

        <div style={{ marginTop: 14 }}>
          <div style={{ fontSize: 11, color: 'var(--ink-3)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600, marginBottom: 6 }}>Cor</div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {colors.map((c, i) => (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 7,
                padding: '6px 11px 6px 6px', borderRadius: 100,
                background: 'var(--surface)',
                border: `1px solid ${c.on ? 'var(--ink)' : 'var(--line)'}`,
                fontSize: 12.5, fontWeight: 500,
              }}>
                <span style={{ width: 20, height: 20, borderRadius: '50%', background: c.hex }}/>
                {c.name}
              </div>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
            <span style={{ fontSize: 11, color: 'var(--ink-3)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>Grade · quantidade</span>
            <span style={{ fontSize: 11, color: 'var(--brand)', fontWeight: 600 }}>Guia de tamanhos</span>
          </div>
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8,
            background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 12, padding: 10,
          }}>
            {sizes.map((s, i) => (
              <div key={i} style={{
                display: 'flex', flexDirection: 'column', alignItems: 'center',
                padding: '8px 0', borderRadius: 8,
                background: s.qty > 0 ? 'var(--brand-tint)' : 'var(--surface-2)',
              }}>
                <div style={{ fontSize: 11, color: 'var(--ink-3)', fontWeight: 600 }}>Tam {s.s}</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 6 }}>
                  <div style={{
                    width: 22, height: 22, borderRadius: 6, background: 'var(--surface)',
                    border: '1px solid var(--line)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <Icon d={Icons.minus} size={12} sw={2.4} stroke="var(--ink-3)"/>
                  </div>
                  <span style={{ fontSize: 18, fontWeight: 700, minWidth: 18, textAlign: 'center', fontFamily: 'var(--f-mono)' }}>{s.qty}</span>
                  <div style={{
                    width: 22, height: 22, borderRadius: 6,
                    background: s.qty > 0 ? 'var(--brand)' : 'var(--surface)',
                    border: s.qty > 0 ? 'none' : '1px solid var(--line)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <Icon d={Icons.plus}  size={12} sw={2.4} stroke={s.qty > 0 ? '#fff' : 'var(--ink-3)'}/>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 16 }}>
          <SectionLabel>Sobre o produto</SectionLabel>
          <Card pad={14}>
            <div style={{ fontSize: 13, color: 'var(--ink-2)', lineHeight: 1.55 }}>
              Vestido midi de caimento solto, manga curta e cintura marcada por elástico interno.
              Tecido encorpado, com leve transparência. Perfeito para vitrine de início de outono.
            </div>
            <div style={{
              marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--line)',
              display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10,
            }}>
              {[
                ['Tecido', 'Algodão 100%'],
                ['Origem', 'São Paulo · BR'],
                ['Prazo',  '12 a 18 dias'],
                ['Frete',  'Por sua conta'],
              ].map((r, i) => (
                <div key={i}>
                  <div style={{ fontSize: 10, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>{r[0]}</div>
                  <div style={{ fontSize: 12.5, fontWeight: 500, marginTop: 2 }}>{r[1]}</div>
                </div>
              ))}
            </div>
          </Card>
        </div>

        <div style={{ marginTop: 16, marginBottom: 140 }}>
          <SectionLabel action={<span style={{ fontSize: 11, color: 'var(--brand)', fontWeight: 600 }}>Ver coleção</span>}>
            Outros desta coleção
          </SectionLabel>
          <div style={{ display: 'flex', gap: 8, overflowX: 'auto', margin: '0 -16px', padding: '0 16px 4px' }}>
            {related.map(rel => (
              <div key={rel.ref} style={{ minWidth: 132, flexShrink: 0 }}>
                <ProductCard p={rel} size="sm"/>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div style={{ position: 'absolute', right: 12, top: 140, zIndex: 30 }}>
        <VideoTile name="Camila" hue={210} size={56} label="Camila"/>
      </div>

      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        padding: '12px 14px 18px', background: 'var(--surface)',
        borderTop: '1px solid var(--line)',
        display: 'flex', gap: 8, alignItems: 'center',
      }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 11, color: 'var(--ink-4)' }}>Subtotal · 8 un</div>
          <div style={{ fontSize: 17, fontWeight: 700, fontFamily: 'var(--f-mono)' }}>R$ 1.272,00</div>
        </div>
        <Btn kind="jade" size="lg" icon="cart">Adicionar</Btn>
      </div>
    </div>
  );
}

Object.assign(window, {
  ChromeBar, BuyerCallHeader,
  BuyerArrival, BuyerLive, BuyerProductDetail, BuyerProductBrowse,
  BuyerConfirmation, BuyerAsync,
});
