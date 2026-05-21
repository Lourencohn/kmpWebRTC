// frames-seller.jsx — Seller-side screens (iPhone)
// Status bar takes top ~60px of IOSDevice; content sits below.

const STATUS_PAD = 56;  // top safe area for our custom headers
const BOTTOM_PAD = 84;  // home indicator + tab bar

// Common scroll container that respects status bar / tab bar.
function PhoneScroll({ children, dark = false, style }) {
  return (
    <div style={{
      paddingTop: STATUS_PAD, paddingBottom: BOTTOM_PAD,
      minHeight: '100%', background: dark ? 'var(--ink)' : 'var(--bg)',
      color: dark ? '#fff' : 'var(--ink)',
      ...style,
    }}>{children}</div>
  );
}

// ────────────────────────────────────────────────────────────────
// 1 · SellerHome — "Sessões" hub
// ────────────────────────────────────────────────────────────────
function SellerHome() {
  return (
    <PhoneScroll>
      <ScreenHeader
        left={<div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <TrovataMark size={22} />
          <span style={{ fontSize: 11.5, color: 'var(--ink-4)', fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            Atelier Norte · Outono 26
          </span>
        </div>}
        title="Sessões"
        subtitle="3 agendadas para hoje · 2 clientes te chamando"
        right={<IconBtn icon="bell" kind="line" />}
      />

      <div style={{ padding: '0 16px' }}>
        <Btn kind="primary" size="lg" icon="plus" style={{ width: '100%', justifyContent: 'flex-start', paddingLeft: 18 }}>
          Iniciar nova sessão
        </Btn>
      </div>

      <div style={{ padding: '20px 16px 0' }}>
        <SectionLabel action={<Pill tone="live" icon="bell">Aguardando você</Pill>}>Agora</SectionLabel>
        <Card pad={0}>
          <div style={{ padding: 14, display: 'flex', alignItems: 'center', gap: 12 }}>
            <Avatar name="Diego Albuquerque" hue={28} size={42}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14.5, fontWeight: 600, letterSpacing: '-0.01em' }}>Diego — Trama Multimarcas</div>
              <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 1 }}>Loja em Belo Horizonte · 6 sessões anteriores</div>
            </div>
            <Btn kind="jade" size="sm">Atender</Btn>
          </div>
          <div style={{
            padding: '10px 14px 12px', borderTop: '1px solid var(--line)',
            display: 'flex', alignItems: 'center', gap: 8, fontSize: 11.5, color: 'var(--ink-3)',
          }}>
            <span className="tc-live-dot" />
            <span>Sala aberta há 1m 12s · Ele está vendo o catálogo Outono 26</span>
          </div>
        </Card>
      </div>

      <div style={{ padding: '20px 16px 0' }}>
        <SectionLabel>Próximas</SectionLabel>
        <Card pad={0}>
          {[
            { name: 'Renata Sá', shop: 'Concept Maré · Recife',     hue: 280, time: '14:30', n: '12 itens', tag: 'Reposição' },
            { name: 'Paulo Henrique', shop: 'Loja PH Atacadinho · Goiânia', hue: 200, time: '15:15', n: 'novo cliente', tag: 'Primeira sessão' },
            { name: 'Marcia Fontes',  shop: 'Studio Marcia · Curitiba',     hue: 340, time: '16:45', n: '8 itens', tag: null },
          ].map((c, i, arr) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px',
              borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
            }}>
              <Avatar name={c.name} hue={c.hue} size={36}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 500 }}>{c.name}</div>
                <div style={{ fontSize: 11.5, color: 'var(--ink-3)', display: 'flex', gap: 6, alignItems: 'center' }}>
                  {c.shop} <span>·</span> {c.n}
                  {c.tag && <Pill tone={c.tag === 'Primeira sessão' ? 'brand' : 'neutral'} style={{ marginLeft: 4 }}>{c.tag}</Pill>}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 14, fontFamily: 'var(--f-mono)', fontWeight: 500 }}>{c.time}</div>
                <div style={{ fontSize: 10.5, color: 'var(--ink-4)' }}>hoje</div>
              </div>
            </div>
          ))}
        </Card>
      </div>

      <div style={{ padding: '20px 16px 0' }}>
        <SectionLabel action={<span style={{ fontSize: 11.5, color: 'var(--brand)', fontWeight: 500 }}>Ver todas</span>}>
          Encerradas hoje
        </SectionLabel>
        <Card pad={0}>
          {[
            { name: 'Luciana Vidal', shop: 'Bossa Loja', total: 'R$ 3.840,00', items: 14, status: 'fechado' },
            { name: 'Eduardo Bastos', shop: 'EB Atacado', total: 'R$ 1.420,00', items: 6,  status: 'em revisão' },
          ].map((c, i, arr) => (
            <div key={i} style={{
              padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12,
              borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
            }}>
              <div style={{
                width: 36, height: 36, borderRadius: 8,
                background: c.status === 'fechado' ? 'var(--jade-tint)' : 'var(--surface-2)',
                color: c.status === 'fechado' ? 'var(--jade-2)' : 'var(--ink-3)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon d={Icons.check} size={18} sw={2.2} />
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13.5, fontWeight: 500 }}>{c.name} <span style={{ color: 'var(--ink-4)', fontWeight: 400 }}>· {c.shop}</span></div>
                <div style={{ fontSize: 11.5, color: 'var(--ink-3)' }}>{c.items} itens · {c.status}</div>
              </div>
              <div style={{ fontSize: 13.5, fontFamily: 'var(--f-mono)', fontWeight: 600 }}>{c.total}</div>
            </div>
          ))}
        </Card>
      </div>

      <TabBar active="sessoes" />
    </PhoneScroll>
  );
}

// Wordmark for the seller app
function TrovataMark({ size = 22, color = 'var(--ink)' }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: size * 0.27,
      background: 'var(--ink)', color: '#fff',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      position: 'relative', flexShrink: 0,
    }}>
      <div style={{
        width: size * 0.5, height: size * 0.5, borderRadius: '50%',
        border: `${size * 0.10}px solid #fff`, position: 'relative',
      }}>
        <div style={{
          position: 'absolute', right: -size * 0.18, bottom: -size * 0.06,
          width: size * 0.22, height: size * 0.22, borderRadius: '50%',
          background: 'var(--jade)', border: `2px solid var(--ink)`,
        }}/>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// 2 · SellerPrep — choosing products for an upcoming session
// ────────────────────────────────────────────────────────────────
function SellerPrep() {
  const chosen = new Set(['AN-104', 'AN-088', 'AN-217', 'AN-512', 'AN-156']);
  return (
    <PhoneScroll>
      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Icon d={Icons.back} size={24} />
        <div style={{ textAlign: 'center', flex: 1 }}>
          <div style={{ fontSize: 12, color: 'var(--ink-4)', fontWeight: 500 }}>Preparando sessão</div>
          <div style={{ fontSize: 14, fontWeight: 600 }}>Diego · Trama Multimarcas</div>
        </div>
        <div style={{ width: 24 }} />
      </div>

      <div style={{ padding: '0 16px 12px' }}>
        <Card style={{ padding: 14 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
            <span style={{ fontSize: 12, fontWeight: 500, color: 'var(--ink-3)' }}>Catálogo sugerido</span>
            <Pill tone="brand" icon="sparkle">3 sugestões</Pill>
          </div>
          <div style={{ fontSize: 13, color: 'var(--ink-2)', lineHeight: 1.45 }}>
            Diego costuma comprar <b>tricot</b> e <b>vestidos</b> no início da estação.
            Sugerimos abrir com a coleção Outono e priorizar reposições da última compra.
          </div>
        </Card>
      </div>

      <div style={{ padding: '0 16px 8px', display: 'flex', alignItems: 'center', gap: 8, overflowX: 'auto' }}>
        {['Todos', 'Outono 26', 'Top venda', 'Pré-venda', 'Reposição'].map((t, i) => (
          <Pill key={i} tone={i === 1 ? 'dark' : 'neutral'}>{t}</Pill>
        ))}
      </div>

      <div style={{ padding: '0 16px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {CATALOG.slice(0, 8).map(p => (
          <div key={p.ref} style={{ position: 'relative' }}>
            <ProductCard p={p} size="sm" highlight={chosen.has(p.ref)} />
            <div style={{
              position: 'absolute', top: 8, right: 8, width: 22, height: 22, borderRadius: '50%',
              background: chosen.has(p.ref) ? 'var(--brand)' : 'rgba(255,255,255,.92)',
              border: chosen.has(p.ref) ? 'none' : '1px solid var(--line-strong)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#fff', boxShadow: 'var(--sh-1)',
            }}>
              {chosen.has(p.ref) && <Icon d={Icons.check} size={13} sw={3} />}
            </div>
          </div>
        ))}
      </div>

      {/* Sticky bottom bar */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        padding: '12px 16px calc(22px + 8px)',
        background: 'rgba(255,255,255,.94)', backdropFilter: 'blur(20px)',
        borderTop: '1px solid var(--line)',
        display: 'flex', alignItems: 'center', gap: 10,
      }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 12, color: 'var(--ink-4)', fontWeight: 500 }}>5 produtos · 32 SKUs</div>
          <div style={{ fontSize: 14, fontWeight: 600 }}>Pronto para enviar link</div>
        </div>
        <Btn kind="primary" size="md" icon="send">Gerar link</Btn>
      </div>
    </PhoneScroll>
  );
}

// ────────────────────────────────────────────────────────────────
// 3 · SellerCreateLink — share via WhatsApp / SMS / link
// ────────────────────────────────────────────────────────────────
function SellerCreateLink() {
  return (
    <PhoneScroll>
      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Icon d={Icons.back} size={24} />
        <span style={{ fontSize: 14, fontWeight: 600 }}>Convidar Diego</span>
        <div style={{ width: 24 }} />
      </div>

      <div style={{ padding: '0 16px' }}>
        <Card pad={20} style={{ textAlign: 'center' }}>
          <div style={{
            margin: '8px auto 12px', width: 56, height: 56, borderRadius: 14,
            background: 'var(--brand-tint)', color: 'var(--brand-2)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon d={Icons.link} size={28} sw={1.8} />
          </div>
          <div style={{ fontSize: 18, fontWeight: 600, letterSpacing: '-0.02em' }}>Link da sessão pronto</div>
          <div style={{ fontSize: 13, color: 'var(--ink-3)', marginTop: 4 }}>
            Diego abre no celular dele — sem instalar nada.
          </div>

          <div style={{
            margin: '16px 0 14px', padding: '12px 14px', borderRadius: 12,
            background: 'var(--surface-2)', border: '1px solid var(--line)',
            display: 'flex', alignItems: 'center', gap: 8, textAlign: 'left',
          }}>
            <Icon d={Icons.globe} size={18} stroke="var(--ink-3)" />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>
                Link
              </div>
              <div style={{ fontSize: 13, fontFamily: 'var(--f-mono)', color: 'var(--ink)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                trovata.cast/d3e4-1f-trama
              </div>
            </div>
            <Btn kind="ghost" size="sm" icon="copy">Copiar</Btn>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <Btn kind="surface" size="md" icon="msg">WhatsApp</Btn>
            <Btn kind="surface" size="md" icon="share">Mais...</Btn>
          </div>
        </Card>
      </div>

      <div style={{ padding: '16px 16px 0' }}>
        <SectionLabel>Configurações da sessão</SectionLabel>
        <Card pad={0}>
          {[
            { lbl: 'Catálogo',         val: 'Outono 26 · 5 produtos', icon: 'grid' },
            { lbl: 'Validade do link', val: '24 horas',                icon: 'clock' },
            { lbl: 'Visibilidade',     val: 'Só Diego',                icon: 'lock' },
            { lbl: 'Preços',           val: 'Tabela B (atacado)',      icon: 'sliders' },
          ].map((r, i, arr) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px',
              borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
            }}>
              <Icon d={Icons[r.icon]} size={18} stroke="var(--ink-3)" />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 11.5, color: 'var(--ink-4)', fontWeight: 500 }}>{r.lbl}</div>
                <div style={{ fontSize: 13.5, fontWeight: 500 }}>{r.val}</div>
              </div>
              <Icon d={Icons.chev} size={16} stroke="var(--ink-4)" />
            </div>
          ))}
        </Card>
      </div>

      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        padding: '12px 16px calc(22px + 8px)',
        background: 'rgba(255,255,255,.94)', backdropFilter: 'blur(20px)',
        borderTop: '1px solid var(--line)',
      }}>
        <Btn kind="jade" size="lg" icon="video" style={{ width: '100%' }}>
          Esperar Diego entrar
        </Btn>
      </div>
    </PhoneScroll>
  );
}

// ────────────────────────────────────────────────────────────────
// 4 · SellerLive — main co-presence view (catalog + buyer PiP)
// ────────────────────────────────────────────────────────────────
function SellerLive({ pointed = 'AN-088', cart = { 'AN-217': 12, 'AN-088': 0 } }) {
  return (
    <div style={{
      paddingTop: STATUS_PAD, minHeight: '100%',
      background: 'var(--bg)', position: 'relative',
    }}>
      {/* Live header */}
      <div style={{
        padding: '0 14px 12px', display: 'flex', alignItems: 'center', gap: 10,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Avatar name="Diego Albuquerque" hue={28} size={34}/>
          <div>
            <div style={{ fontSize: 13.5, fontWeight: 600, lineHeight: 1.2 }}>Diego · Trama</div>
            <div style={{ fontSize: 10.5, color: 'var(--ink-3)', display: 'flex', alignItems: 'center', gap: 5 }}>
              <span className="tc-live-dot" /> Ao vivo · 4m 18s
            </div>
          </div>
        </div>
        <div style={{ flex: 1 }} />
        <IconBtn icon="users" kind="line" size={34}/>
        <IconBtn icon="more"  kind="line" size={34}/>
      </div>

      {/* Catalog header / page indicator */}
      <div style={{ padding: '0 14px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <div style={{ fontSize: 11, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>
            Atelier Norte · Outono 26
          </div>
          <div style={{ fontSize: 17, fontWeight: 600, letterSpacing: '-0.02em', marginTop: 2 }}>
            Tricot & camisaria
          </div>
        </div>
        <Pill tone="dark" icon="pointer">Apontando</Pill>
      </div>

      {/* Catalog grid */}
      <div style={{ padding: '0 14px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
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
      </div>

      <div style={{ padding: '12px 14px 0' }}>
        <Card pad={12} style={{
          background: 'linear-gradient(180deg, var(--jade-tint), var(--surface))',
          border: '1px solid var(--jade-tint)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              width: 32, height: 32, borderRadius: 8,
              background: 'var(--jade)', color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon d={Icons.cart} size={18} sw={2}/>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 12.5, color: 'var(--jade-2)', fontWeight: 600 }}>
                Diego adicionou 12un · Camisa Linho
              </div>
              <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>há 8s · Tam P/M/G · 4 cores</div>
            </div>
            <Btn kind="surface" size="sm">Ver carrinho</Btn>
          </div>
        </Card>
      </div>

      {/* Buyer PiP — sits over the bottom-right */}
      <div style={{
        position: 'absolute', right: 12, bottom: BOTTOM_PAD + 14, zIndex: 30,
      }}>
        <VideoTile name="Diego" hue={28} size={76} label="Diego" />
      </div>

      {/* Live action bar */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        padding: '10px 16px calc(22px + 8px)',
        background: 'rgba(14,17,22,.92)', backdropFilter: 'blur(20px)',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
      }}>
        <IconBtn icon="mic"   kind="dark"   size={42} />
        <IconBtn icon="video" kind="dark"   size={42} />
        <IconBtn icon="pointer" kind="brand" size={42} active />
        <IconBtn icon="layers"  kind="dark"   size={42} />
        <IconBtn icon="hangup"  kind="danger" size={42} />
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// 5 · SellerProductDetail — sees what buyer is doing on a product
// ────────────────────────────────────────────────────────────────
function SellerProductDetail() {
  const p = CATALOG[2]; // Vestido Midi
  const tint = BgTints[p.tint];
  return (
    <div style={{ paddingTop: STATUS_PAD, minHeight: '100%', background: 'var(--bg)', position: 'relative' }}>
      <div style={{ padding: '0 14px 10px', display: 'flex', alignItems: 'center', gap: 10 }}>
        <Icon d={Icons.back} size={22} />
        <div style={{ flex: 1, textAlign: 'center' }}>
          <div style={{ fontSize: 11, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>Vendo junto</div>
          <div style={{ fontSize: 13.5, fontWeight: 600 }}>{p.name}</div>
        </div>
        <Icon d={Icons.more} size={22} />
      </div>

      {/* product hero */}
      <div style={{
        margin: '0 14px', height: 220, borderRadius: 16, position: 'relative',
        background: tint.bg, color: tint.fg, overflow: 'hidden',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <svg width="58%" height="58%" viewBox="0 0 100 100">{Garments[p.garment]}</svg>

        {/* Buyer's pointer hovering over a detail */}
        <RemotePointer name="Diego" hue={28} x={186} y={114} />

        {/* image dots */}
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

        <Pill tone="dark" icon="eye" style={{ position: 'absolute', top: 12, left: 12 }}>
          Diego está vendo
        </Pill>
      </div>

      <div style={{ padding: '14px 16px 0' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontSize: 10.5, fontFamily: 'var(--f-mono)', color: 'var(--ink-4)', letterSpacing: '0.06em' }}>{p.ref}</div>
            <div style={{ fontSize: 17, fontWeight: 600, letterSpacing: '-0.015em', marginTop: 2 }}>{p.name}</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 17, fontWeight: 600, fontFamily: 'var(--f-mono)' }}>{p.price}</div>
            <div style={{ fontSize: 11, color: 'var(--ink-4)' }}>min {p.moq} un</div>
          </div>
        </div>

        <div style={{ marginTop: 14, display: 'flex', gap: 5, flexWrap: 'wrap' }}>
          <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--ink-3)', letterSpacing: '0.05em', textTransform: 'uppercase', marginRight: 4, alignSelf: 'center' }}>Cores</span>
          {['#3F4744','#A5806A'].map((c, i) => (
            <span key={i} style={{
              width: 26, height: 26, borderRadius: '50%', background: c,
              border: i === 0 ? '2px solid var(--ink)' : '2px solid var(--surface)',
              outline: i === 0 ? '1px solid var(--surface)' : 'none',
            }}/>
          ))}
        </div>

        <div style={{
          marginTop: 14, padding: 12, borderRadius: 12,
          border: '1px solid var(--line)', background: 'var(--surface)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
            <Icon d={Icons.eye} size={14} stroke="var(--brand)" sw={2}/>
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--brand-2)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
              Em tempo real
            </span>
          </div>
          <div style={{ fontSize: 13.5, lineHeight: 1.45, color: 'var(--ink-2)' }}>
            Diego abriu esta página há <b>32 segundos</b>. Está olhando a cor <b>Verde-musgo</b> e ampliou a foto 2× para ver o caimento da barra.
          </div>
        </div>

        <div style={{ marginTop: 12, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          <Btn kind="surface" icon="zap">Sugerir oferta</Btn>
          <Btn kind="primary" icon="pin">Fixar para Diego</Btn>
        </div>
      </div>

      {/* Mini buyer PiP */}
      <div style={{ position: 'absolute', right: 12, bottom: 24, zIndex: 30 }}>
        <VideoTile name="Diego" hue={28} size={64} label="Diego"/>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────
// 6 · SellerSummary — end-of-call recap with order ready
// ────────────────────────────────────────────────────────────────
function SellerSummary() {
  const items = [
    { p: CATALOG[1], qty: 12 }, // camisa linho
    { p: CATALOG[0], qty: 8 },  // tricot
    { p: CATALOG[2], qty: 4 },  // vestido midi
    { p: CATALOG[6], qty: 24 }, // tee
  ];
  const total = 'R$ 5.276,80';
  return (
    <PhoneScroll>
      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Icon d={Icons.back} size={24} />
        <span style={{ fontSize: 14, fontWeight: 600 }}>Resumo da sessão</span>
        <Icon d={Icons.share} size={22} />
      </div>

      <div style={{ padding: '0 16px 14px' }}>
        <Card pad={14}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <Avatar name="Diego Albuquerque" hue={28} size={40}/>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>Diego · Trama Multimarcas</div>
              <div style={{ fontSize: 11.5, color: 'var(--ink-3)' }}>Encerrada às 14h08 · duração 12m 04s</div>
            </div>
            <Pill tone="jade" icon="check">Pedido pronto</Pill>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 1,
                        marginTop: 14, background: 'var(--line)', borderRadius: 10, overflow: 'hidden' }}>
            {[
              ['Itens', '4', '48un'],
              ['Tempo', '12m', 'em catálogo'],
              ['Total', 'R$ 5.276', '+8% vs média'],
            ].map((m, i) => (
              <div key={i} style={{ background: 'var(--surface)', padding: '10px 8px', textAlign: 'center' }}>
                <div style={{ fontSize: 10.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>{m[0]}</div>
                <div style={{ fontSize: 16, fontWeight: 600, letterSpacing: '-0.02em', marginTop: 2 }}>{m[1]}</div>
                <div style={{ fontSize: 10, color: 'var(--ink-3)', marginTop: 1 }}>{m[2]}</div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <div style={{ padding: '0 16px 14px' }}>
        <SectionLabel action={<span style={{ fontSize: 11.5, color: 'var(--ink-4)' }}>4 itens · 48 un</span>}>
          Pedido fechado em chamada
        </SectionLabel>
        <Card pad={14}>
          {items.map((it, i) => (
            <ProductRow key={it.p.ref} p={it.p} qty={it.qty}
              right={<div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 13, fontWeight: 600, fontFamily: 'var(--f-mono)' }}>{it.p.price}</div>
              </div>}
              style={{ borderBottom: i < items.length - 1 ? '1px solid var(--line)' : 'none' }}
            />
          ))}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', padding: '10px 0 0', marginTop: 4, borderTop: '1px solid var(--line)' }}>
            <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--ink-3)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Total</span>
            <span style={{ fontSize: 20, fontWeight: 700, letterSpacing: '-0.02em', fontFamily: 'var(--f-mono)' }}>{total}</span>
          </div>
        </Card>
      </div>

      <div style={{ padding: '0 16px 14px' }}>
        <SectionLabel>Mostrados mas não pedidos</SectionLabel>
        <Card pad={0}>
          {[
            { p: CATALOG[5], reason: '47s em foco · "Achei a sarja firme demais"' },
            { p: CATALOG[4], reason: '22s em foco · sem comentários' },
          ].map((it, i, arr) => (
            <ProductRow key={it.p.ref} p={it.p}
              right={<Btn kind="surface" size="sm">Reenviar</Btn>}
              style={{
                padding: '10px 14px',
                borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
              }}
            />
          ))}
        </Card>
      </div>

      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        padding: '12px 16px calc(22px + 8px)',
        background: 'rgba(255,255,255,.94)', backdropFilter: 'blur(20px)',
        borderTop: '1px solid var(--line)',
        display: 'flex', gap: 10,
      }}>
        <Btn kind="surface" size="md" icon="download" style={{ flex: 1 }}>PDF</Btn>
        <Btn kind="primary" size="md" icon="send"     style={{ flex: 2 }}>Enviar ao ERP</Btn>
      </div>
    </PhoneScroll>
  );
}

// ────────────────────────────────────────────────────────────────
// 7 · SellerPipeline — intelligence / "Insights" tab
// ────────────────────────────────────────────────────────────────
function SellerPipeline() {
  return (
    <PhoneScroll>
      <ScreenHeader
        title="Pipeline"
        subtitle="22 clientes ativos · 4 prontos para abordar"
        right={<IconBtn icon="filter" kind="line"/>}
      />

      <div style={{ padding: '0 16px 16px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          <Card pad={12} style={{ background: 'var(--brand)', color: '#fff', border: 'none' }}>
            <div style={{ fontSize: 10.5, opacity: .8, textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>Este mês</div>
            <div style={{ fontSize: 24, fontWeight: 700, marginTop: 4, letterSpacing: '-0.02em', fontFamily: 'var(--f-mono)' }}>R$ 142k</div>
            <div style={{ fontSize: 11, opacity: .8 }}>+18% vs ago/25</div>
          </Card>
          <Card pad={12}>
            <div style={{ fontSize: 10.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>Sessões</div>
            <div style={{ fontSize: 24, fontWeight: 700, marginTop: 4, letterSpacing: '-0.02em', fontFamily: 'var(--f-mono)' }}>87</div>
            <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>72% converteram</div>
          </Card>
        </div>
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SectionLabel action={<Pill tone="brand" icon="sparkle">Sugestões TC</Pill>}>
          Prontos para abordar
        </SectionLabel>
        <Card pad={0}>
          {[
            { name: 'Luciana Vidal',     shop: 'Bossa Loja',       hue: 320, why: 'Costuma repor a cada 38 dias · 41 dias desde a última', urg: 'agora' },
            { name: 'Renata Sá',          shop: 'Concept Maré',     hue: 280, why: 'Viu catálogo Outono 2× sem comprar · interesse em tricot', urg: 'hoje' },
            { name: 'Tiago Brandão',      shop: 'Brandão Multimar', hue: 180, why: 'Aniversário da loja em 9 dias · perfil de coleção nova',  urg: 'esta semana' },
          ].map((c, i, arr) => (
            <div key={i} style={{
              padding: '12px 14px', display: 'flex', gap: 10, alignItems: 'flex-start',
              borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
            }}>
              <Avatar name={c.name} hue={c.hue} size={38}/>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ fontSize: 13.5, fontWeight: 600 }}>{c.name}</div>
                  <Pill tone={c.urg === 'agora' ? 'live' : (c.urg === 'hoje' ? 'brand' : 'neutral')}>{c.urg}</Pill>
                </div>
                <div style={{ fontSize: 11, color: 'var(--ink-4)', marginTop: 1 }}>{c.shop}</div>
                <div style={{ fontSize: 12, color: 'var(--ink-2)', marginTop: 6, lineHeight: 1.4 }}>{c.why}</div>
                <div style={{ marginTop: 8, display: 'flex', gap: 6 }}>
                  <Btn kind="primary" size="sm" icon="video">Convidar</Btn>
                  <Btn kind="ghost"   size="sm">Mais tarde</Btn>
                </div>
              </div>
            </div>
          ))}
        </Card>
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SectionLabel>O que mais ficou em foco</SectionLabel>
        <Card pad={14}>
          {[
            { name: 'Camisa Linho ML', s: '2m 14s', conv: '74%', trend: 'up' },
            { name: 'Vestido Midi',    s: '1m 48s', conv: '52%', trend: 'flat' },
            { name: 'Jaqueta Sarja',   s: '3m 02s', conv: '12%', trend: 'down' },
          ].map((r, i, arr) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '8px 0',
              borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
            }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 500 }}>{r.name}</div>
                <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>Tempo médio em foco: {r.s}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 13, fontWeight: 600 }}>{r.conv}</div>
                <div style={{ fontSize: 10.5, color: r.trend === 'up' ? 'var(--jade-2)' : (r.trend === 'down' ? 'var(--live)' : 'var(--ink-4)') }}>
                  {r.trend === 'up' ? '↑ subindo' : r.trend === 'down' ? '↓ caindo' : '→ estável'}
                </div>
              </div>
            </div>
          ))}
        </Card>
      </div>

      <TabBar active="insights" />
    </PhoneScroll>
  );
}

Object.assign(window, {
  SellerHome, SellerPrep, SellerCreateLink,
  SellerLive, SellerProductDetail, SellerSummary, SellerPipeline,
  TrovataMark, PhoneScroll, STATUS_PAD, BOTTOM_PAD,
});
