function Sparkline({ data, width = 76, height = 22, color = 'var(--brand)', fill = true, dot = true }) {
  const max = Math.max(...data);
  const min = Math.min(...data);
  const range = max - min || 1;
  const pts = data.map((v, i) => {
    const x = (i / (data.length - 1)) * (width - 4) + 2;
    const y = height - ((v - min) / range) * (height - 6) - 3;
    return [x, y];
  });
  const poly = pts.map(p => p.join(',')).join(' ');
  const [lx, ly] = pts[pts.length - 1];
  return (
    <svg width={width} height={height} style={{ overflow: 'visible', display: 'block' }}>
      {fill && (
        <path
          d={`M${pts[0][0]},${height} L${poly.split(' ').join(' L')} L${lx},${height} Z`}
          fill={color} opacity="0.10"
        />
      )}
      <polyline points={poly} fill="none" stroke={color} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
      {dot && <circle cx={lx} cy={ly} r="2.4" fill={color}/>}
    </svg>
  );
}

function HBar({ value, max, color = 'var(--brand)', width = 140, height = 6 }) {
  const pct = Math.max(2, Math.min(100, (value / max) * 100));
  return (
    <div style={{ width, height, background: 'var(--surface-2)', borderRadius: 999, overflow: 'hidden' }}>
      <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 999 }}/>
    </div>
  );
}

function StatusDot({ color = 'var(--jade)', size = 6, pulsing = false }) {
  return (
    <span style={{
      width: size, height: size, borderRadius: '50%', background: color,
      display: 'inline-block', animation: pulsing ? 'tc-blink 1.8s ease-in-out infinite' : 'none',
    }}/>
  );
}

function SellerCatalog() {
  const top = [CATALOG[2], CATALOG[1], CATALOG[5]];
  const grid = [CATALOG[0], CATALOG[3], CATALOG[4], CATALOG[6], CATALOG[7], CATALOG[8]];
  return (
    <PhoneScroll>
      <ScreenHeader
        title="Catálogo"
        subtitle="Atelier Norte · Outono 26"
        left={<div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <TrovataMark size={20}/>
          <span style={{ fontSize: 11, color: 'var(--ink-4)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            Coleção ativa
          </span>
        </div>}
        right={<IconBtn icon="plus" kind="line"/>}
      />

      <div style={{ padding: '0 16px 14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '11px 14px',
          background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 14, boxShadow: 'var(--sh-1)' }}>
          <Icon d={Icons.search} size={18} stroke="var(--ink-4)"/>
          <span style={{ flex: 1, fontSize: 13.5, color: 'var(--ink-4)' }}>Buscar por SKU, nome, cor...</span>
          <span style={{ fontSize: 10.5, fontFamily: 'var(--f-mono)', color: 'var(--ink-5)', padding: '2px 6px', border: '1px solid var(--line)', borderRadius: 5 }}>⌘K</span>
        </div>
      </div>

      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center', gap: 6, overflowX: 'auto', flexWrap: 'nowrap' }}>
        {[
          { l: 'Todos', t: 'dark' },
          { l: 'Outono 26', t: 'neutral' },
          { l: 'Inverno 25', t: 'neutral' },
          { l: 'Top venda', t: 'neutral' },
          { l: 'Pré-venda', t: 'neutral' },
          { l: 'Baixo estoque', t: 'neutral' },
        ].map((c, i) => <Pill key={i} tone={c.t}>{c.l}</Pill>)}
      </div>

      <div style={{ padding: '0 16px 14px' }}>
        <div style={{
          position: 'relative',
          borderRadius: 18, overflow: 'hidden',
          border: '1px solid var(--line)',
          background: 'linear-gradient(135deg, #2A3431 0%, #1B2422 70%)',
          color: '#fff',
          boxShadow: 'var(--sh-2)',
          padding: 18,
        }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 10.5, color: 'rgba(255,255,255,.55)', textTransform: 'uppercase', letterSpacing: '0.14em', fontWeight: 600 }}>
                Coleção em destaque
              </div>
              <div style={{ fontSize: 32, fontWeight: 700, letterSpacing: '-0.035em', lineHeight: 1.02, marginTop: 8, fontFamily: 'var(--f-sans)' }}>
                Outono <br/>
                <span style={{ fontStyle: 'italic', fontWeight: 500, color: '#E2D7B8' }}>vinte e seis</span>
              </div>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,.7)', marginTop: 10, lineHeight: 1.4 }}>
                Atelier Norte · 47 SKUs · 12 estreias<br/>
                Caimento solto, tons de musgo e linho cru.
              </div>
              <div style={{ marginTop: 14, display: 'flex', gap: 6 }}>
                <Btn kind="surface" size="sm" icon="arrowRight">Ver coleção</Btn>
                <Btn kind="dark" size="sm" style={{ background: 'rgba(255,255,255,.12)', border: '1px solid rgba(255,255,255,.18)' }}>Editar</Btn>
              </div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {[CATALOG[0], CATALOG[1], CATALOG[2]].map((p, i) => {
                const tint = BgTints[p.tint % BgTints.length];
                return (
                  <div key={i} style={{
                    width: 54, height: 54, borderRadius: 10,
                    background: tint.bg, color: tint.fg,
                    border: '1.5px solid rgba(255,255,255,.6)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    transform: `rotate(${i === 0 ? -4 : i === 1 ? 0 : 4}deg)`,
                  }}>
                    <svg width="70%" height="70%" viewBox="0 0 100 100">{Garments[p.garment]}</svg>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      <div style={{ padding: '0 16px 4px', display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 6, marginBottom: 16 }}>
        {[
          { lbl: 'SKUs', val: '47' },
          { lbl: 'Novos', val: '9', accent: 'brand' },
          { lbl: 'Pré-venda', val: '3', accent: 'warn' },
          { lbl: 'Baixo', val: '1', accent: 'live' },
        ].map((s, i) => (
          <div key={i} style={{
            padding: '8px 6px', textAlign: 'center', borderRadius: 10,
            background: 'var(--surface)', border: '1px solid var(--line)',
          }}>
            <div style={{ fontSize: 9.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>{s.lbl}</div>
            <div style={{
              fontSize: 18, fontWeight: 700, fontFamily: 'var(--f-mono)', letterSpacing: '-0.02em', marginTop: 2,
              color: s.accent === 'brand' ? 'var(--brand)' : s.accent === 'warn' ? 'var(--warn)' : s.accent === 'live' ? 'var(--live)' : 'var(--ink)',
            }}>{s.val}</div>
          </div>
        ))}
      </div>

      <div style={{ padding: '0 16px 14px' }}>
        <SectionLabel action={<span style={{ fontSize: 11, color: 'var(--brand)', fontWeight: 600 }}>Ver todos</span>}>
          Top da semana
        </SectionLabel>
        <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 4, margin: '0 -16px', padding: '0 16px 4px' }}>
          {top.map((p, i) => (
            <div key={p.ref} style={{ minWidth: 158, flexShrink: 0 }}>
              <ProductCard p={p} size="sm"/>
              <div style={{ marginTop: 6, display: 'flex', alignItems: 'center', gap: 5, fontSize: 10.5, color: 'var(--ink-3)' }}>
                <Icon d={Icons.trend} size={11} stroke="var(--jade-2)" sw={2.2}/>
                <span style={{ fontFamily: 'var(--f-mono)', fontWeight: 600, color: 'var(--jade-2)' }}>+{[42, 28, 12][i]}%</span>
                <span>· {['18 ped.', '14 ped.', '9 ped.'][i]}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ padding: '0 16px 100px' }}>
        <SectionLabel action={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Icon d={Icons.grid} size={14} stroke="var(--ink)" sw={2}/>
            <Icon d={Icons.list} size={14} stroke="var(--ink-4)"/>
          </div>
        }>
          Todos os produtos · 47
        </SectionLabel>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {grid.map(p => <ProductCard key={p.ref} p={p} size="sm"/>)}
        </div>
      </div>

      <div style={{
        position: 'absolute', right: 16, bottom: BOTTOM_PAD + 14, zIndex: 30,
      }}>
        <Btn kind="dark" size="md" icon="plus" style={{
          borderRadius: 999, padding: '0 18px', boxShadow: 'var(--sh-3)',
        }}>Adicionar</Btn>
      </div>

      <TabBar active="catalogo"/>
    </PhoneScroll>
  );
}

function SellerClients() {
  const ready = [
    { name: 'Luciana Vidal', shop: 'Bossa Loja · Porto Alegre', hue: 320, since: '41d', spark: [3, 4, 2, 5, 3, 4, 0], status: 'urgente', ltv: 'R$ 38.4k' },
    { name: 'Renata Sá', shop: 'Concept Maré · Recife', hue: 280, since: '23d', spark: [2, 3, 4, 3, 2, 1, 0], status: 'morno', ltv: 'R$ 22.1k' },
  ];
  const recent = [
    { name: 'Diego Albuquerque', shop: 'Trama Multimarcas · BH', hue: 28, last: 'agora', spark: [2, 3, 1, 4, 3, 5, 6], live: true, ltv: 'R$ 64.8k' },
    { name: 'Paulo Henrique', shop: 'PH Atacadinho · Goiânia', hue: 200, last: 'há 1h', spark: [0, 1, 2, 3, 4, 3, 5], ltv: 'R$ 8.2k', tag: 'novo' },
    { name: 'Marcia Fontes', shop: 'Studio Marcia · Curitiba', hue: 340, last: 'ontem', spark: [4, 3, 5, 4, 6, 5, 4], ltv: 'R$ 52.0k' },
  ];
  const month = [
    { name: 'Eduardo Bastos', shop: 'EB Atacado · Salvador', hue: 160, last: 'há 6d', spark: [3, 3, 2, 3, 2, 2, 3], ltv: 'R$ 19.3k' },
    { name: 'Tiago Brandão', shop: 'Brandão Multimar · São Luís', hue: 180, last: 'há 11d', spark: [2, 4, 3, 2, 1, 2, 2], ltv: 'R$ 14.7k' },
    { name: 'Camila Rocha', shop: 'Saudade Boutique · Niterói', hue: 14, last: 'há 18d', spark: [5, 3, 4, 2, 3, 2, 1], ltv: 'R$ 41.6k', tag: 'top' },
  ];

  const renderClient = (c, i, arr, dense = false) => (
    <div key={i} style={{
      display: 'flex', alignItems: 'center', gap: 11, padding: '12px 14px',
      borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
    }}>
      <div style={{ position: 'relative' }}>
        <Avatar name={c.name} hue={c.hue} size={40}/>
        {c.live && <span style={{
          position: 'absolute', right: -1, bottom: -1, width: 12, height: 12,
          background: 'var(--jade)', border: '2px solid var(--surface)', borderRadius: '50%',
          animation: 'tc-blink 1.8s ease-in-out infinite',
        }}/>}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <div style={{ fontSize: 13.5, fontWeight: 600, letterSpacing: '-0.01em' }}>{c.name}</div>
          {c.tag === 'novo' && <Pill tone="brand" style={{ padding: '2px 7px', fontSize: 10 }}>Novo</Pill>}
          {c.tag === 'top' && <Pill tone="jade" style={{ padding: '2px 7px', fontSize: 10 }}>Top</Pill>}
        </div>
        <div style={{ fontSize: 11.5, color: 'var(--ink-3)', marginTop: 1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {c.shop}
        </div>
        <div style={{ marginTop: 6, display: 'flex', alignItems: 'center', gap: 8 }}>
          <Sparkline data={c.spark} width={56} height={16} color={c.live ? 'var(--jade)' : 'var(--ink-4)'} fill={false} dot={false}/>
          <span style={{ fontSize: 10.5, color: 'var(--ink-4)', fontFamily: 'var(--f-mono)' }}>· {c.last}</span>
        </div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <div style={{ fontSize: 12.5, fontFamily: 'var(--f-mono)', fontWeight: 600, letterSpacing: '-0.01em' }}>{c.ltv}</div>
        <div style={{ fontSize: 9.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>LTV</div>
      </div>
    </div>
  );

  return (
    <PhoneScroll>
      <ScreenHeader
        title="Clientes"
        subtitle="47 ativos · 8 prontos para abordar"
        right={<IconBtn icon="filter" kind="line"/>}
      />

      <div style={{ padding: '0 16px 14px' }}>
        <Card pad={0} style={{
          background: 'linear-gradient(135deg, var(--brand-tint) 0%, var(--surface) 80%)',
          border: '1px solid var(--brand-tint)',
          overflow: 'hidden',
        }}>
          <div style={{ padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              width: 36, height: 36, borderRadius: 10, flexShrink: 0,
              background: 'var(--brand)', color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon d={Icons.sparkle} size={18} sw={2}/>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 12, color: 'var(--brand-2)', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                Sugestões TC · agora
              </div>
              <div style={{ fontSize: 13.5, fontWeight: 600, marginTop: 1, letterSpacing: '-0.01em' }}>
                5 clientes prontos para abordar
              </div>
            </div>
            <Icon d={Icons.chev} size={18} stroke="var(--brand-2)"/>
          </div>
          <div style={{ background: 'var(--surface)' }}>
            {ready.map((c, i, arr) => (
              <div key={i} style={{
                padding: '11px 14px', display: 'flex', alignItems: 'center', gap: 10,
                borderTop: '1px solid var(--line)',
              }}>
                <Avatar name={c.name} hue={c.hue} size={34}/>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 12.5, fontWeight: 600 }}>
                    {c.name} <span style={{ color: 'var(--ink-4)', fontWeight: 400 }}>· sem contato há {c.since}</span>
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 1 }}>{c.shop}</div>
                </div>
                <Btn kind={c.status === 'urgente' ? 'primary' : 'soft'} size="sm" icon="video">Convidar</Btn>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <div style={{ padding: '0 16px 12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px',
          background: 'var(--surface-2)', borderRadius: 12 }}>
          <Icon d={Icons.search} size={17} stroke="var(--ink-3)"/>
          <span style={{ flex: 1, fontSize: 13, color: 'var(--ink-4)' }}>Buscar cliente, cidade, loja...</span>
        </div>
      </div>

      <div style={{ padding: '0 16px 16px', display: 'flex', gap: 6 }}>
        {[
          { l: 'Recentes', n: 12, on: true },
          { l: 'Top venda', n: 8 },
          { l: 'Atenção', n: 5 },
          { l: 'Novos', n: 3 },
        ].map((s, i) => (
          <div key={i} style={{
            flex: 1, padding: '8px 6px', textAlign: 'center', borderRadius: 10,
            background: s.on ? 'var(--ink)' : 'var(--surface)',
            color: s.on ? '#fff' : 'var(--ink-2)',
            border: `1px solid ${s.on ? 'var(--ink)' : 'var(--line)'}`,
            fontSize: 12, fontWeight: 600, letterSpacing: '-0.01em',
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
          }}>
            <span>{s.l}</span>
            <span style={{ fontSize: 10.5, fontFamily: 'var(--f-mono)', opacity: s.on ? .7 : .55, fontWeight: 500 }}>{s.n}</span>
          </div>
        ))}
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SectionLabel action={<Pill tone="live" icon="signal">3 ao vivo</Pill>}>Esta semana</SectionLabel>
        <Card pad={0}>{recent.map((c, i) => renderClient(c, i, recent))}</Card>
      </div>

      <div style={{ padding: '0 16px 100px' }}>
        <SectionLabel>Este mês</SectionLabel>
        <Card pad={0}>{month.map((c, i) => renderClient(c, i, month))}</Card>
      </div>

      <div style={{
        position: 'absolute', right: 16, bottom: BOTTOM_PAD + 14, zIndex: 30,
      }}>
        <Btn kind="dark" size="md" icon="plus" style={{
          borderRadius: 999, padding: '0 18px', boxShadow: 'var(--sh-3)',
        }}>Cliente</Btn>
      </div>

      <TabBar active="clientes"/>
    </PhoneScroll>
  );
}

function SellerInsights() {
  const focus = [
    { name: 'Camisa Linho ML', t: '2m 14s', conv: 74, color: 'var(--jade)' },
    { name: 'Vestido Midi Algodão', t: '1m 48s', conv: 52, color: 'var(--brand)' },
    { name: 'Calça Wide Alfaiataria', t: '1m 22s', conv: 38, color: 'var(--brand)' },
    { name: 'Jaqueta Sarja', t: '3m 02s', conv: 12, color: 'var(--warn)' },
    { name: 'Tricot Canelado', t: '0m 54s', conv: 8, color: 'var(--ink-4)' },
  ];
  const funnel = [
    { l: 'Sessões agendadas', n: 95, pct: 100 },
    { l: 'Realizadas', n: 87, pct: 92 },
    { l: 'Com pedido', n: 63, pct: 66 },
    { l: 'Repetiram em ≤30d', n: 41, pct: 43 },
  ];
  const spark = [62, 78, 71, 88, 96, 84, 102, 118, 142];

  return (
    <PhoneScroll>
      <ScreenHeader
        title="Insights"
        subtitle="Atelier Norte · Outono 26 · 23 dias"
        right={<IconBtn icon="filter" kind="line"/>}
      />

      <div style={{ padding: '0 16px 14px', display: 'flex', gap: 6 }}>
        {['Semana', 'Mês', 'Trimestre', 'Coleção'].map((s, i) => (
          <div key={i} style={{
            flex: 1, padding: '7px 4px', textAlign: 'center', borderRadius: 8,
            background: i === 1 ? 'var(--surface)' : 'transparent',
            border: `1px solid ${i === 1 ? 'var(--line)' : 'transparent'}`,
            color: i === 1 ? 'var(--ink)' : 'var(--ink-4)',
            fontSize: 12, fontWeight: i === 1 ? 600 : 500,
            boxShadow: i === 1 ? 'var(--sh-1)' : 'none',
          }}>{s}</div>
        ))}
      </div>

      <div style={{ padding: '0 16px 12px' }}>
        <Card pad={0} style={{
          background: 'linear-gradient(180deg, #0A1429 0%, #050C1C 100%)',
          color: '#fff', border: 'none', overflow: 'hidden', position: 'relative',
        }}>
          <div style={{
            position: 'absolute', inset: 0, opacity: .4, pointerEvents: 'none',
            background: 'radial-gradient(120% 80% at 30% 0%, rgba(0,87,184,.5), transparent 60%)',
          }}/>
          <div style={{ padding: '16px 18px 14px', position: 'relative' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span className="tc-live-dot" style={{ background: '#3DDB8A' }}/>
              <div style={{ fontSize: 10.5, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.12em', color: 'rgba(255,255,255,.7)' }}>
                Vendas · este mês
              </div>
            </div>
            <div style={{ marginTop: 10, display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 }}>
              <div>
                <div style={{ fontSize: 36, fontWeight: 700, fontFamily: 'var(--f-mono)', letterSpacing: '-0.04em', lineHeight: 1 }}>
                  R$&nbsp;142,38<span style={{ fontSize: 22, color: 'rgba(255,255,255,.55)' }}>k</span>
                </div>
                <div style={{ marginTop: 6, display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
                  <span style={{ color: '#3DDB8A', fontWeight: 600, fontFamily: 'var(--f-mono)' }}>↑ +18,2%</span>
                  <span style={{ color: 'rgba(255,255,255,.5)' }}>vs ago/25 · R$ 120,4k</span>
                </div>
              </div>
              <div style={{ flexShrink: 0 }}>
                <Sparkline data={spark} width={110} height={44} color="#3DDB8A" fill={true} dot={true}/>
              </div>
            </div>
            <div style={{
              marginTop: 16, paddingTop: 12, borderTop: '1px solid rgba(255,255,255,.08)',
              display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8,
            }}>
              {[
                ['Meta mês', 'R$ 160k', '89%'],
                ['Faltam', '7 dias', null],
                ['Projeção', 'R$ 168k', '+5%'],
              ].map((m, i) => (
                <div key={i}>
                  <div style={{ fontSize: 9.5, color: 'rgba(255,255,255,.5)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>{m[0]}</div>
                  <div style={{ fontSize: 14, fontWeight: 600, fontFamily: 'var(--f-mono)', marginTop: 2 }}>{m[1]}</div>
                  {m[2] && <div style={{ fontSize: 10, color: 'rgba(255,255,255,.55)', fontFamily: 'var(--f-mono)' }}>{m[2]}</div>}
                </div>
              ))}
            </div>
          </div>
        </Card>
      </div>

      <div style={{ padding: '0 16px 16px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
        {[
          { l: 'Sessões', v: '87', d: '+12 vs mês anterior', spark: [4, 6, 5, 7, 6, 8, 9], color: 'var(--brand)' },
          { l: 'Conversão', v: '72%', d: '↑ 4pp · acima da meta', spark: [60, 64, 62, 68, 70, 72, 72], color: 'var(--jade)' },
          { l: 'Ticket médio', v: 'R$ 4,2k', d: '+R$ 280 vs ago', spark: [3.5, 3.6, 3.8, 4.0, 3.9, 4.1, 4.2], color: 'var(--brand)' },
          { l: 'Tempo em call', v: '14m', d: '−2m · mais objetivo', spark: [18, 17, 16, 15, 14, 14, 14], color: 'var(--ink-4)' },
        ].map((k, i) => (
          <Card key={i} pad={12}>
            <div style={{ fontSize: 10.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>{k.l}</div>
            <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginTop: 4 }}>
              <div style={{ fontSize: 20, fontWeight: 700, fontFamily: 'var(--f-mono)', letterSpacing: '-0.02em' }}>{k.v}</div>
              <Sparkline data={k.spark} width={50} height={20} color={k.color} fill={false} dot={false}/>
            </div>
            <div style={{ fontSize: 10.5, color: 'var(--ink-3)', marginTop: 4 }}>{k.d}</div>
          </Card>
        ))}
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SectionLabel action={<span style={{ fontSize: 11, color: 'var(--ink-4)' }}>Top 5 · este mês</span>}>
          Tempo em foco vs. conversão
        </SectionLabel>
        <Card pad={14}>
          {focus.map((f, i, arr) => (
            <div key={i} style={{
              padding: '8px 0',
              borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                <div style={{ fontSize: 12.5, fontWeight: 500 }}>{f.name}</div>
                <div style={{ fontSize: 11.5, color: 'var(--ink-3)', fontFamily: 'var(--f-mono)' }}>{f.t}</div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <HBar value={f.conv} max={100} color={f.color} width={180} height={6}/>
                <div style={{ fontSize: 11.5, fontWeight: 700, fontFamily: 'var(--f-mono)', color: f.color, minWidth: 32 }}>{f.conv}%</div>
              </div>
            </div>
          ))}
        </Card>
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SectionLabel>Funil da sessão</SectionLabel>
        <Card pad={14}>
          {funnel.map((f, i, arr) => (
            <div key={i} style={{
              padding: '10px 0',
              borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
            }}>
              <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 6 }}>
                <div style={{ fontSize: 12.5, fontWeight: 500 }}>{f.l}</div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
                  <span style={{ fontSize: 16, fontWeight: 700, fontFamily: 'var(--f-mono)', letterSpacing: '-0.02em' }}>{f.n}</span>
                  <span style={{ fontSize: 11, color: 'var(--ink-4)', fontFamily: 'var(--f-mono)' }}>{f.pct}%</span>
                </div>
              </div>
              <HBar value={f.pct} max={100} color={i === 0 ? 'var(--ink-3)' : i === arr.length - 1 ? 'var(--jade)' : 'var(--brand)'} width={332} height={5}/>
            </div>
          ))}
        </Card>
      </div>

      <div style={{ padding: '0 16px 100px' }}>
        <SectionLabel>Insight da semana</SectionLabel>
        <Card pad={16} style={{
          background: 'var(--jade-tint)',
          border: '1px solid var(--jade-tint)',
        }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
            <div style={{
              width: 28, height: 28, borderRadius: 8, background: 'var(--jade)', color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, marginTop: 2,
            }}>
              <Icon d={Icons.zap} size={15} sw={2.2}/>
            </div>
            <div style={{ flex: 1, fontSize: 13, color: 'var(--jade-2)', lineHeight: 1.5 }}>
              <b>Camisa Linho</b> está convertendo <b style={{ fontFamily: 'var(--f-mono)' }}>74%</b> das vezes que aparece.
              Em 9 das 11 últimas sessões, foi o primeiro produto que o cliente abriu sozinho.
              Considere abrir a próxima call por ela.
            </div>
          </div>
        </Card>
      </div>

      <TabBar active="insights"/>
    </PhoneScroll>
  );
}

function SellerAccount() {
  return (
    <PhoneScroll>
      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Icon d={Icons.back} size={24}/>
        <span style={{ fontSize: 14, fontWeight: 600 }}>Minha conta</span>
        <Icon d={Icons.share} size={22}/>
      </div>

      <div style={{ padding: '4px 16px 18px', textAlign: 'center' }}>
        <div style={{ position: 'relative', display: 'inline-block' }}>
          <Avatar name="Camila Tavares" hue={340} size={86}/>
          <div style={{
            position: 'absolute', right: -2, bottom: -2, width: 26, height: 26, borderRadius: '50%',
            background: 'var(--surface)', border: '1px solid var(--line)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: 'var(--sh-1)',
          }}>
            <Icon d={Icons.sliders} size={13} stroke="var(--ink-2)" sw={2}/>
          </div>
        </div>
        <div style={{ marginTop: 12, fontSize: 22, fontWeight: 700, letterSpacing: '-0.025em' }}>
          Camila Tavares
        </div>
        <div style={{ marginTop: 2, fontSize: 13, color: 'var(--ink-3)' }}>
          Representante · Atelier Norte
        </div>
        <div style={{ marginTop: 10, display: 'inline-flex', alignItems: 'center', gap: 12, padding: '6px 14px',
          background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 999, fontSize: 11.5, color: 'var(--ink-3)' }}>
          <span><b style={{ color: 'var(--ink)', fontFamily: 'var(--f-mono)' }}>47</b> clientes</span>
          <span style={{ color: 'var(--ink-5)' }}>·</span>
          <span><b style={{ color: 'var(--ink)', fontFamily: 'var(--f-mono)' }}>14</b> meses na rede</span>
          <span style={{ color: 'var(--ink-5)' }}>·</span>
          <span style={{ color: 'var(--jade-2)', fontWeight: 600 }}>★ Top 3%</span>
        </div>
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SectionLabel action={<Pill tone="jade" icon="check">Sincronizada</Pill>}>Marca representada</SectionLabel>
        <Card pad={0} style={{ overflow: 'hidden' }}>
          <div style={{
            padding: '16px 16px 14px',
            background: 'linear-gradient(135deg, #1F2521 0%, #161B17 100%)',
            color: '#fff', position: 'relative',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: 9.5, color: 'rgba(255,255,255,.55)', textTransform: 'uppercase', letterSpacing: '0.16em', fontWeight: 600 }}>
                  Atelier
                </div>
                <div style={{ fontSize: 24, fontWeight: 700, letterSpacing: '-0.03em', marginTop: 2, lineHeight: 1 }}>
                  Norte
                </div>
              </div>
              <div style={{ display: 'flex', gap: 4 }}>
                {['#3F4744', '#A5806A', '#C7B79B', '#5E5B57', '#E2D7B8'].map((c, i) => (
                  <span key={i} style={{
                    width: 18, height: 18, borderRadius: '50%', background: c,
                    border: '1.5px solid rgba(255,255,255,.7)',
                  }}/>
                ))}
              </div>
            </div>
            <div style={{ fontSize: 11.5, color: 'rgba(255,255,255,.6)', marginTop: 12, lineHeight: 1.4 }}>
              Moda atacado · Coleções Outono 26, Inverno 25<br/>
              47 SKUs ativos · tabela B (atacado)
            </div>
          </div>
          <div style={{ padding: '10px 14px', display: 'flex', gap: 8 }}>
            <Btn kind="surface" size="sm" icon="grid" style={{ flex: 1 }}>Catálogos</Btn>
            <Btn kind="surface" size="sm" icon="sliders" style={{ flex: 1 }}>Tabelas</Btn>
            <Btn kind="ghost" size="sm" icon="plus">Marca</Btn>
          </div>
        </Card>
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SectionLabel action={<span style={{ fontSize: 11, color: 'var(--ink-4)' }}>maio · em curso</span>}>
          Performance este mês
        </SectionLabel>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 6 }}>
          {[
            { l: 'Vendas', v: 'R$ 142k', d: '+18%', color: 'var(--jade-2)' },
            { l: 'Sessões', v: '87', d: '+12', color: 'var(--ink-3)' },
            { l: 'Conversão', v: '72%', d: '+4pp', color: 'var(--jade-2)' },
          ].map((k, i) => (
            <Card key={i} pad={10}>
              <div style={{ fontSize: 9.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600 }}>{k.l}</div>
              <div style={{ fontSize: 16, fontWeight: 700, fontFamily: 'var(--f-mono)', letterSpacing: '-0.02em', marginTop: 3 }}>{k.v}</div>
              <div style={{ fontSize: 10, color: k.color, fontFamily: 'var(--f-mono)', marginTop: 1, fontWeight: 600 }}>{k.d}</div>
            </Card>
          ))}
        </div>
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SectionLabel>Conta</SectionLabel>
        <Card pad={0}>
          {[
            { l: 'Perfil', v: 'Camila Tavares · camila@ateliernorte.com.br', icon: 'user' },
            { l: 'Marcas representadas', v: 'Atelier Norte', icon: 'layers', pill: '+ adicionar' },
            { l: 'Catálogos', v: 'Outono 26 · Inverno 25', icon: 'grid' },
            { l: 'Tabela de preços', v: 'Tabela B · atacado', icon: 'sliders' },
            { l: 'Sincronização ERP', v: 'Conectado · Bling', icon: 'signal', pill: 'ativo', tone: 'jade' },
            { l: 'Notificações', v: 'WhatsApp + push', icon: 'bell' },
          ].map((r, i, arr) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px',
              borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
            }}>
              <div style={{
                width: 32, height: 32, borderRadius: 8, background: 'var(--surface-2)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
              }}>
                <Icon d={Icons[r.icon]} size={16} stroke="var(--ink-2)" sw={1.7}/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13, fontWeight: 500 }}>{r.l}</div>
                <div style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {r.v}
                </div>
              </div>
              {r.pill && <Pill tone={r.tone || 'neutral'} icon={r.tone === 'jade' ? 'check' : null}>{r.pill}</Pill>}
              <Icon d={Icons.chev} size={15} stroke="var(--ink-4)"/>
            </div>
          ))}
        </Card>
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SectionLabel>Suporte e privacidade</SectionLabel>
        <Card pad={0}>
          {[
            { l: 'Central de ajuda', icon: 'msg' },
            { l: 'Termos de uso', icon: 'lock' },
            { l: 'Política de privacidade', icon: 'eye' },
            { l: 'Versão', icon: 'star', val: '1.2.0' },
          ].map((r, i, arr) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '11px 14px',
              borderBottom: i < arr.length - 1 ? '1px solid var(--line)' : 'none',
            }}>
              <Icon d={Icons[r.icon]} size={17} stroke="var(--ink-3)"/>
              <div style={{ flex: 1, fontSize: 13, fontWeight: 500 }}>{r.l}</div>
              {r.val && <div style={{ fontSize: 11.5, color: 'var(--ink-4)', fontFamily: 'var(--f-mono)' }}>{r.val}</div>}
              {!r.val && <Icon d={Icons.chev} size={14} stroke="var(--ink-4)"/>}
            </div>
          ))}
        </Card>
      </div>

      <div style={{ padding: '4px 16px 24px' }}>
        <div style={{
          textAlign: 'center', padding: '14px 14px',
          background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 14,
          fontSize: 13, color: 'var(--live)', fontWeight: 600, letterSpacing: '-0.01em',
        }}>
          Sair desta conta
        </div>
      </div>
    </PhoneScroll>
  );
}

Object.assign(window, { Sparkline, HBar, StatusDot, SellerCatalog, SellerClients, SellerInsights, SellerAccount });
