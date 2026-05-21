// catalog.jsx — TrovataCast product catalog (fashion wholesale)
// Stylized garment illustrations rendered in SVG so we don't depend on stock photos.

// ─── Garment silhouettes (SVG, currentColor) ─────────────────────
const Garments = {
  // Each renders inside a 100×100 viewBox.
  shirt: (
    <g>
      <path d="M22 28 L34 18 L40 22 Q50 30 60 22 L66 18 L78 28 L72 38 L68 36 L68 82 L32 82 L32 36 L28 38 Z"
            fill="currentColor"/>
    </g>
  ),
  polo: (
    <g>
      <path d="M22 30 L36 20 L44 26 Q50 31 56 26 L64 20 L78 30 L72 40 L68 38 L68 82 L32 82 L32 38 L28 40 Z" fill="currentColor"/>
      <path d="M42 26 L50 34 L58 26 L56 22 L50 28 L44 22 Z" fill="rgba(0,0,0,.18)"/>
      <rect x="49" y="32" width="2" height="10" fill="rgba(0,0,0,.20)"/>
    </g>
  ),
  tee: (
    <g>
      <path d="M22 30 L36 20 L42 24 Q50 30 58 24 L64 20 L78 30 L72 40 L68 38 L68 82 L32 82 L32 38 L28 40 Z" fill="currentColor"/>
      <path d="M42 22 Q50 28 58 22" stroke="rgba(0,0,0,.20)" strokeWidth="1.5" fill="none"/>
    </g>
  ),
  dress: (
    <g>
      <path d="M36 22 L44 18 Q50 22 56 18 L64 22 L66 36 L72 82 L28 82 L34 36 Z" fill="currentColor"/>
      <path d="M44 18 Q50 22 56 18" stroke="rgba(0,0,0,.22)" strokeWidth="1.5" fill="none"/>
    </g>
  ),
  jacket: (
    <g>
      <path d="M22 28 L34 18 L40 22 L40 82 L60 82 L60 22 L66 18 L78 28 L72 38 L66 36 L66 82 L34 82 L34 36 L28 38 Z" fill="currentColor"/>
      <line x1="50" y1="24" x2="50" y2="82" stroke="rgba(0,0,0,.25)" strokeWidth="1.4"/>
      <circle cx="50" cy="38" r="1.2" fill="rgba(0,0,0,.3)"/>
      <circle cx="50" cy="52" r="1.2" fill="rgba(0,0,0,.3)"/>
      <circle cx="50" cy="66" r="1.2" fill="rgba(0,0,0,.3)"/>
    </g>
  ),
  pants: (
    <g>
      <path d="M32 18 L68 18 L70 50 L62 82 L52 82 L50 56 L48 56 L46 82 L38 82 L30 50 Z" fill="currentColor"/>
      <line x1="50" y1="22" x2="50" y2="56" stroke="rgba(0,0,0,.18)" strokeWidth="1"/>
    </g>
  ),
  shoe: (
    <g>
      <path d="M16 60 Q22 42 38 44 L62 50 Q78 54 84 62 L82 70 Q80 76 70 76 L20 76 Q14 76 14 70 Z" fill="currentColor"/>
      <path d="M38 44 L46 60 L58 56" stroke="rgba(0,0,0,.25)" strokeWidth="1.5" fill="none"/>
      <line x1="16" y1="70" x2="82" y2="70" stroke="rgba(0,0,0,.22)" strokeWidth="1"/>
    </g>
  ),
  sweater: (
    <g>
      <path d="M22 30 L34 20 L40 24 Q50 32 60 24 L66 20 L78 30 L72 42 L68 40 L68 82 L32 82 L32 40 L28 42 Z" fill="currentColor"/>
      <path d="M32 50 L68 50" stroke="rgba(0,0,0,.12)" strokeWidth="1"/>
      <path d="M32 60 L68 60" stroke="rgba(0,0,0,.12)" strokeWidth="1"/>
      <path d="M32 70 L68 70" stroke="rgba(0,0,0,.12)" strokeWidth="1"/>
    </g>
  ),
  skirt: (
    <g>
      <path d="M34 24 L66 24 L72 82 L28 82 Z" fill="currentColor"/>
      <line x1="34" y1="24" x2="66" y2="24" stroke="rgba(0,0,0,.25)" strokeWidth="1.4"/>
    </g>
  ),
};

// Solid backgrounds for product cards — soft, fashion-magazine palette
const BgTints = [
  { bg: '#EEEAE0', fg: '#7C6E58' }, // sand
  { bg: '#E6EAE5', fg: '#5C6C5F' }, // sage
  { bg: '#E8E4DC', fg: '#534234' }, // walnut
  { bg: '#DDE4E6', fg: '#3A5260' }, // slate-blue
  { bg: '#E8DEDA', fg: '#8B4C44' }, // terracota
  { bg: '#E0DCD2', fg: '#3D3833' }, // graphite
  { bg: '#F0E7D6', fg: '#7A5A2C' }, // mustard
  { bg: '#D8DDD2', fg: '#3C4530' }, // moss
];

// ─── Catalog data ────────────────────────────────────────────────
// Coleção Outono · Atelier Norte — atacado de moda multimarca
const CATALOG = [
  { ref: 'AN-104', name: 'Blusa Tricot Canelado',     garment: 'sweater', tint: 0, price: 'R$ 89,90',  moq: 6, sizes: ['PP','P','M','G','GG'], colors: 3, tag: 'Novo'   },
  { ref: 'AN-217', name: 'Camisa Linho Manga Longa',  garment: 'shirt',   tint: 1, price: 'R$ 119,00', moq: 6, sizes: ['P','M','G','GG'], colors: 4, tag: null     },
  { ref: 'AN-088', name: 'Vestido Midi Algodão',      garment: 'dress',   tint: 4, price: 'R$ 159,00', moq: 4, sizes: ['P','M','G'], colors: 2, tag: 'Top venda' },
  { ref: 'AN-301', name: 'Polo Bordado Frente',       garment: 'polo',    tint: 3, price: 'R$  99,00', moq: 6, sizes: ['P','M','G','GG'], colors: 5, tag: null     },
  { ref: 'AN-156', name: 'Calça Wide Alfaiataria',    garment: 'pants',   tint: 5, price: 'R$ 169,00', moq: 4, sizes: ['36','38','40','42','44'], colors: 3, tag: null },
  { ref: 'AN-422', name: 'Jaqueta Sarja Oversized',   garment: 'jacket',  tint: 2, price: 'R$ 229,00', moq: 3, sizes: ['P','M','G'], colors: 2, tag: 'Pré-venda' },
  { ref: 'AN-512', name: 'Camiseta Algodão Pima',     garment: 'tee',     tint: 6, price: 'R$  59,90', moq: 8, sizes: ['PP','P','M','G','GG'], colors: 6, tag: null },
  { ref: 'AN-077', name: 'Tênis Couro Curado',        garment: 'shoe',    tint: 0, price: 'R$ 279,00', moq: 3, sizes: ['36','37','38','39','40','41','42','43'], colors: 2, tag: null },
  { ref: 'AN-621', name: 'Saia Plissada Midi',        garment: 'skirt',   tint: 7, price: 'R$ 139,00', moq: 4, sizes: ['P','M','G'], colors: 3, tag: null     },
];

// ─── Product card ────────────────────────────────────────────────
function ProductCard({ p, size = 'md', highlight = false, pointed = false, inCart = 0, onClick, style }) {
  const dims = {
    sm: { w: '100%', img: 96,  pad: 8,  fs: 11.5, ref: 9.5 },
    md: { w: '100%', img: 128, pad: 10, fs: 12.5, ref: 10 },
    lg: { w: '100%', img: 168, pad: 12, fs: 13.5, ref: 10.5 },
  }[size];
  const tint = BgTints[p.tint % BgTints.length];

  return (
    <div onClick={onClick} style={{
      position: 'relative',
      background: 'var(--surface)',
      border: `1px solid ${highlight ? 'var(--brand)' : 'var(--line)'}`,
      borderRadius: 14, overflow: 'hidden',
      boxShadow: highlight ? '0 0 0 3px var(--brand-ring), var(--sh-2)' : 'var(--sh-1)',
      transition: 'box-shadow .2s, border-color .2s',
      ...style,
    }}>
      {/* Image */}
      <div style={{
        height: dims.img, background: tint.bg, color: tint.fg,
        position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <svg width="74%" height="74%" viewBox="0 0 100 100">{Garments[p.garment]}</svg>

        {p.tag && (
          <span style={{
            position: 'absolute', top: 8, left: 8, padding: '3px 8px',
            background: p.tag === 'Pré-venda' ? 'var(--warn)' : (p.tag === 'Top venda' ? 'var(--jade)' : 'var(--ink)'),
            color: '#fff', fontSize: 10, fontWeight: 600, borderRadius: 6, letterSpacing: '-0.005em',
          }}>{p.tag}</span>
        )}

        {/* color dots */}
        <div style={{
          position: 'absolute', bottom: 8, left: 8, display: 'flex', gap: 3,
        }}>
          {Array.from({ length: Math.min(p.colors, 4) }).map((_, i) => (
            <span key={i} style={{
              width: 8, height: 8, borderRadius: '50%',
              background: ['#3F4744','#A5806A','#C7B79B','#5E5B57'][i],
              border: '1.5px solid #fff',
            }}/>
          ))}
          {p.colors > 4 && (
            <span style={{ fontSize: 9, color: tint.fg, marginLeft: 2, fontWeight: 600 }}>+{p.colors - 4}</span>
          )}
        </div>

        {inCart > 0 && (
          <div style={{
            position: 'absolute', top: 8, right: 8,
            background: 'var(--jade)', color: '#fff',
            fontSize: 10.5, fontWeight: 600, padding: '3px 7px', borderRadius: 999,
            display: 'flex', alignItems: 'center', gap: 4,
          }}>
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round">
              <path d="M4 12l5 5L20 6"/>
            </svg>
            {inCart}
          </div>
        )}

        {pointed && (
          <div style={{
            position: 'absolute', inset: -2, border: '2px solid oklch(60% 0.18 30)',
            borderRadius: 14, pointerEvents: 'none',
            boxShadow: '0 0 0 4px rgba(232,124,67,.18)',
          }}/>
        )}
      </div>

      {/* Body */}
      <div style={{ padding: dims.pad }}>
        <div style={{
          fontSize: dims.ref, fontFamily: 'var(--f-mono)',
          color: 'var(--ink-4)', letterSpacing: '0.04em',
        }}>{p.ref}</div>
        <div style={{
          fontSize: dims.fs, fontWeight: 500, color: 'var(--ink)',
          letterSpacing: '-0.01em', marginTop: 2, lineHeight: 1.25,
          overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
        }}>{p.name}</div>
        <div style={{
          display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
          marginTop: 6,
        }}>
          <span style={{ fontSize: dims.fs + 0.5, fontWeight: 600, color: 'var(--ink)' }}>{p.price}</span>
          <span style={{ fontSize: dims.ref, color: 'var(--ink-4)' }}>min {p.moq}un</span>
        </div>
      </div>
    </div>
  );
}

// ─── Compact list-row variant (for cart, history, etc) ───────────
function ProductRow({ p, qty, size = 'md', right, style }) {
  const tint = BgTints[p.tint % BgTints.length];
  const dims = { sm: 36, md: 44, lg: 52 }[size];
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0',
      borderBottom: '1px solid var(--line)', ...style,
    }}>
      <div style={{
        width: dims, height: dims, borderRadius: 8,
        background: tint.bg, color: tint.fg, flexShrink: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <svg width="74%" height="74%" viewBox="0 0 100 100">{Garments[p.garment]}</svg>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 9.5, fontFamily: 'var(--f-mono)', color: 'var(--ink-4)', letterSpacing: '0.04em' }}>{p.ref}</div>
        <div style={{
          fontSize: 13, fontWeight: 500, color: 'var(--ink)',
          letterSpacing: '-0.01em', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>{p.name}</div>
        {qty != null && (
          <div style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 2 }}>
            {qty} un · {p.price}
          </div>
        )}
      </div>
      {right}
    </div>
  );
}

Object.assign(window, { CATALOG, Garments, BgTints, ProductCard, ProductRow });
