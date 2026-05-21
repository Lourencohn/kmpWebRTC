// ui.jsx — TrovataCast shared UI: icons, avatars, pills, video tile

// ─── Icons (24px stroke, currentColor) ───────────────────────────
const Icon = ({ d, size = 22, fill, stroke = 'currentColor', sw = 1.6 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={fill || 'none'} stroke={fill ? 'none' : stroke}
       strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
    {typeof d === 'string' ? <path d={d} /> : d}
  </svg>
);
const Icons = {
  plus:    'M12 5v14M5 12h14',
  search:  'M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16zm6-2l4 4',
  back:    'M15 6l-6 6 6 6',
  chev:    'M9 6l6 6-6 6',
  chevDown:'M6 9l6 6 6-6',
  more:    'M5 12h.01M12 12h.01M19 12h.01',
  bell:    'M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8M10 21a2 2 0 0 0 4 0',
  cart:    'M3 5h2l2 12h12l2-9H7M9 21a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zm10 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z',
  mic:     'M12 3a3 3 0 0 0-3 3v6a3 3 0 1 0 6 0V6a3 3 0 0 0-3-3zM5 11a7 7 0 0 0 14 0M12 18v3',
  micOff:  'M9 9V6a3 3 0 0 1 5.5-1.7M15 12V9M3 3l18 18M5 11a7 7 0 0 0 11.7 5.1M9.3 18.5A7 7 0 0 0 12 19v2',
  video:   'M3 7a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7zm13 3l5-3v10l-5-3',
  videoOff:'M3 3l18 18M16 16v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2m4 0h3a2 2 0 0 1 2 2v3l5-3v10',
  hangup:  'M3 11c5-5 13-5 18 0l-2 2-3-1-1-2c-2-1-4-1-6 0l-1 2-3 1-2-2z',
  pointer: 'M5 3l5 18 3-8 8-3z',
  check:   'M4 12l5 5L20 6',
  share:   'M4 12v7a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7M16 6l-4-4-4 4M12 2v14',
  copy:    'M9 9V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-4M5 9h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z',
  link:    'M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1',
  heart:   'M12 21s-7-4.5-9-9a5 5 0 0 1 9-3 5 5 0 0 1 9 3c-2 4.5-9 9-9 9z',
  send:    'M22 2L11 13M22 2l-7 20-4-9-9-4z',
  user:    'M20 21a8 8 0 1 0-16 0M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z',
  users:   'M16 21a6 6 0 1 0-12 0M10 14a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM22 21a6 6 0 0 0-6-6m-2-7a4 4 0 1 0 8 0 4 4 0 0 0-8 0',
  zap:     'M13 2L4 14h7l-1 8 9-12h-7l1-8z',
  filter:  'M3 5h18M6 12h12M10 19h4',
  grid:    'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  list:    'M4 6h16M4 12h16M4 18h16',
  trash:   'M4 7h16M9 7V4h6v3M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13',
  minus:   'M5 12h14',
  expand:  'M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5',
  globe:   'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM3 12h18M12 3a13 13 0 0 1 0 18M12 3a13 13 0 0 0 0 18',
  clock:   'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2',
  trend:   'M3 17l6-6 4 4 8-8M14 7h6v6',
  star:    'M12 2l3 6.5 7 .9-5 5 1.4 7L12 18l-6.4 3.4L7 14.4l-5-5 7-.9z',
  flame:   'M12 22c4 0 7-3 7-7 0-3-2-5-4-7 .5 3-1 5-3 5-2 0-3-2-2-4-3 1-5 4-5 7 0 4 3 6 7 6z',
  layers:  'M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 18l9 5 9-5',
  swatch:  'M3 3h8v8H3zM13 3h8v8h-8zM3 13h8v8H3zM13 13h8v8h-8z',
  eye:     'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zm10 3a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
  pin:     'M12 22s7-7 7-12a7 7 0 1 0-14 0c0 5 7 12 7 12zm0-9a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
  msg:     'M21 12a8 8 0 0 1-8 8H7l-4 3v-11a8 8 0 0 1 8-8h2a8 8 0 0 1 8 8z',
  lock:    'M6 11h12v9a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2v-9zm2 0V7a4 4 0 0 1 8 0v4',
  download:'M12 3v12m-5-5l5 5 5-5M5 21h14',
  sliders: 'M4 6h12M4 18h6M14 18h6M4 12h6M14 12h6M16 4v4M10 16v4M10 10v4',
  arrowRight: 'M5 12h14M13 5l7 7-7 7',
  arrowLeft:  'M19 12H5M11 5l-7 7 7 7',
  sparkle: 'M12 3v3M12 18v3M3 12h3M18 12h3M5.5 5.5l2 2M16.5 16.5l2 2M5.5 18.5l2-2M16.5 7.5l2-2',
  signal:  'M2 12h2M6 9v6M10 6v12M14 9v6M18 12h2',
};

// ─── Buttons / chips ─────────────────────────────────────────────
function Btn({ kind = 'primary', size = 'md', icon, children, style, ...p }) {
  const sizes = {
    sm: { h: 32, px: 12, fs: 13, r: 8 },
    md: { h: 44, px: 16, fs: 15, r: 12 },
    lg: { h: 52, px: 20, fs: 16, r: 14 },
  }[size];
  const kinds = {
    primary:  { bg: 'var(--brand)', c: '#fff', b: 'transparent', sh: '0 1px 0 rgba(0,0,0,.06) inset, 0 0 0 1px var(--brand-2)' },
    jade:     { bg: 'var(--jade)', c: '#fff', b: 'transparent', sh: '0 1px 0 rgba(0,0,0,.06) inset, 0 0 0 1px var(--jade-2)' },
    soft:     { bg: 'var(--brand-tint)', c: 'var(--brand-2)', b: 'transparent', sh: 'none' },
    ghost:    { bg: 'transparent', c: 'var(--ink)', b: 'var(--line)', sh: 'none' },
    surface:  { bg: 'var(--surface)', c: 'var(--ink)', b: 'var(--line)', sh: 'var(--sh-1)' },
    dark:     { bg: 'var(--ink)', c: '#fff', b: 'transparent', sh: 'none' },
    danger:   { bg: 'var(--live)', c: '#fff', b: 'transparent', sh: 'none' },
  }[kind];
  return (
    <button {...p} style={{
      height: sizes.h, padding: `0 ${sizes.px}px`, fontSize: sizes.fs, fontWeight: 500,
      borderRadius: sizes.r, background: kinds.bg, color: kinds.c,
      border: `1px solid ${kinds.b === 'transparent' ? 'transparent' : kinds.b}`,
      boxShadow: kinds.sh, display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      gap: 8, letterSpacing: '-0.005em', whiteSpace: 'nowrap', ...style,
    }}>
      {icon && <Icon d={Icons[icon]} size={sizes.fs + 3} sw={1.8} />}
      {children}
    </button>
  );
}

function Pill({ children, tone = 'neutral', icon, style }) {
  const tones = {
    neutral: { bg: 'var(--surface-2)', c: 'var(--ink-2)', b: 'var(--line)' },
    brand:   { bg: 'var(--brand-tint)', c: 'var(--brand-2)', b: 'transparent' },
    jade:    { bg: 'var(--jade-tint)', c: 'var(--jade-2)', b: 'transparent' },
    live:    { bg: 'rgba(229,72,77,.10)', c: 'var(--live)', b: 'transparent' },
    ghost:   { bg: 'transparent', c: 'var(--ink-3)', b: 'var(--line)' },
    dark:    { bg: 'rgba(14,17,22,.92)', c: '#fff', b: 'transparent' },
  }[tone];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 6, padding: '4px 10px',
      borderRadius: 999, fontSize: 11.5, fontWeight: 500, letterSpacing: '-0.005em',
      background: tones.bg, color: tones.c, border: `1px solid ${tones.b}`, ...style,
    }}>
      {icon && <Icon d={Icons[icon]} size={12} sw={2} />}
      {children}
    </span>
  );
}

// ─── Avatar (initials on a soft tinted bg) ───────────────────────
function Avatar({ name = 'XX', size = 36, hue = 220, style }) {
  const initials = name.split(' ').slice(0,2).map(s => s[0]).join('').toUpperCase();
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%',
      background: `oklch(86% 0.07 ${hue})`, color: `oklch(34% 0.10 ${hue})`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontWeight: 600, fontSize: size * 0.40, letterSpacing: '-0.02em',
      flexShrink: 0, fontFamily: 'var(--f-sans)', ...style,
    }}>{initials}</div>
  );
}

// ─── Video PiP tile — simulated face via soft gradient + avatar ──
function VideoTile({ name, hue = 220, size = 76, label, mode = 'face', style, muted = false, mini = false }) {
  // Mode 'face' = a fake "headshot" using a radial gradient with hue.
  const bg = mode === 'face'
    ? `radial-gradient(120% 90% at 50% 40%, oklch(85% 0.10 ${hue}) 0%, oklch(55% 0.13 ${hue}) 55%, oklch(28% 0.10 ${hue}) 100%)`
    : 'linear-gradient(135deg, var(--ink-3), var(--ink-2))';
  const w = size, h = size * (mini ? 1.0 : 1.34);
  return (
    <div style={{
      width: w, height: h, borderRadius: 14, overflow: 'hidden',
      background: bg, position: 'relative', boxShadow: 'var(--sh-2)',
      border: '1px solid rgba(255,255,255,.6)', ...style,
    }}>
      {/* Subtle highlight to suggest 'a person' */}
      <div style={{
        position: 'absolute', left: '50%', top: '55%', transform: 'translate(-50%, -50%)',
        width: '46%', height: '46%', borderRadius: '50%',
        background: `radial-gradient(circle at 35% 30%, oklch(78% 0.06 ${hue}) 0%, oklch(48% 0.10 ${hue}) 80%)`,
        boxShadow: 'inset 0 -10px 14px rgba(0,0,0,.18)',
      }} />
      {/* shoulders */}
      <div style={{
        position: 'absolute', left: '-10%', right: '-10%', bottom: '-30%', height: '60%',
        borderRadius: '50%',
        background: `oklch(38% 0.05 ${hue + 20})`,
      }} />
      {/* name chip */}
      {label !== false && (
        <div style={{
          position: 'absolute', left: 6, bottom: 6, right: 6,
          display: 'flex', alignItems: 'center', gap: 5,
          padding: '3px 7px', borderRadius: 8,
          background: 'rgba(14,17,22,.55)', backdropFilter: 'blur(8px)',
          color: '#fff', fontSize: 10.5, fontWeight: 500, letterSpacing: '-0.005em',
        }}>
          <span style={{
            width: 6, height: 6, borderRadius: '50%',
            background: muted ? 'var(--ink-5)' : '#3DDB8A',
          }} />
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {label || name}
          </span>
        </div>
      )}
    </div>
  );
}

// ─── Round icon button (toolbar) ─────────────────────────────────
function IconBtn({ icon, size = 38, kind = 'soft', active, style, ...p }) {
  const k = {
    soft:   { bg: 'rgba(255,255,255,.85)', c: 'var(--ink)', b: 'rgba(0,0,0,.06)' },
    dark:   { bg: 'rgba(14,17,22,.74)', c: '#fff', b: 'rgba(255,255,255,.1)' },
    danger: { bg: 'var(--live)', c: '#fff', b: 'transparent' },
    jade:   { bg: 'var(--jade)', c: '#fff', b: 'transparent' },
    brand:  { bg: 'var(--brand)', c: '#fff', b: 'transparent' },
    line:   { bg: 'var(--surface)', c: 'var(--ink)', b: 'var(--line)' },
  }[kind];
  return (
    <button {...p} style={{
      width: size, height: size, borderRadius: '50%',
      background: active ? 'var(--ink)' : k.bg, color: active ? '#fff' : k.c,
      border: `1px solid ${k.b}`, backdropFilter: 'blur(10px)',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      boxShadow: 'var(--sh-1)', flexShrink: 0, ...style,
    }}>
      <Icon d={Icons[icon]} size={size * 0.46} sw={1.8} />
    </button>
  );
}

// ─── Card frame with header / divider ────────────────────────────
function Card({ children, style, pad = 14 }) {
  return (
    <div style={{
      background: 'var(--surface)', borderRadius: 16,
      border: '1px solid var(--line)', boxShadow: 'var(--sh-1)',
      padding: pad, ...style,
    }}>{children}</div>
  );
}

// ─── Section title (small caps) ──────────────────────────────────
function SectionLabel({ children, action, style }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 4px 8px', ...style,
    }}>
      <span style={{
        fontSize: 11, fontWeight: 600, textTransform: 'uppercase',
        letterSpacing: '0.08em', color: 'var(--ink-4)',
      }}>{children}</span>
      {action}
    </div>
  );
}

// ─── Big screen header (the seller's app) ────────────────────────
function ScreenHeader({ title, subtitle, left, right, style }) {
  return (
    <div style={{ padding: '0 16px 14px', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12, ...style }}>
      <div>
        {left}
        <div style={{ fontSize: 26, fontWeight: 600, letterSpacing: '-0.025em', color: 'var(--ink)', lineHeight: 1.15 }}>{title}</div>
        {subtitle && <div style={{ marginTop: 4, fontSize: 13.5, color: 'var(--ink-3)' }}>{subtitle}</div>}
      </div>
      {right}
    </div>
  );
}

// ─── Tab bar (seller bottom nav) ─────────────────────────────────
function TabBar({ active = 'sessoes', dark = false }) {
  const items = [
    { id: 'sessoes', label: 'Sessões',   icon: 'video' },
    { id: 'catalogo', label: 'Catálogo', icon: 'grid' },
    { id: 'clientes', label: 'Clientes', icon: 'users' },
    { id: 'insights', label: 'Insights', icon: 'trend' },
  ];
  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0, paddingBottom: 22,
      paddingTop: 8, background: dark ? 'rgba(14,17,22,.92)' : 'rgba(255,255,255,.94)',
      backdropFilter: 'blur(20px)', borderTop: `1px solid ${dark ? 'rgba(255,255,255,.08)' : 'var(--line)'}`,
      display: 'flex', justifyContent: 'space-around',
    }}>
      {items.map(it => {
        const on = it.id === active;
        const c = on ? (dark ? '#fff' : 'var(--brand)') : (dark ? 'rgba(255,255,255,.45)' : 'var(--ink-4)');
        return (
          <div key={it.id} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3, color: c }}>
            <Icon d={Icons[it.icon]} size={22} sw={on ? 2 : 1.6} />
            <span style={{ fontSize: 10.5, fontWeight: on ? 600 : 500 }}>{it.label}</span>
          </div>
        );
      })}
    </div>
  );
}

// ─── Notch / cutout sizes for status bar offset ──────────────────
const IOS_STATUSBAR_H = 60;     // ios-frame status bar
const ANDROID_STATUSBAR_H = 40;

// ─── Cursor/pointer that floats over a remote user's view ────────
function RemotePointer({ name = 'Camila', hue = 30, x = 60, y = 220, animated = false, style }) {
  return (
    <div style={{
      position: 'absolute', left: 0, top: 0, pointerEvents: 'none', zIndex: 30,
      transform: `translate(${x}px, ${y}px)`,
      animation: animated ? 'tc-cursor-glide 5.5s ease-in-out infinite' : 'none',
      ...style,
    }}>
      <svg width="24" height="24" viewBox="0 0 24 24" style={{ filter: 'drop-shadow(0 2px 4px rgba(0,0,0,.25))' }}>
        <path d="M4 3l5 18 3-8 8-3z"
              fill={`oklch(60% 0.18 ${hue})`} stroke="#fff" strokeWidth="1.2" strokeLinejoin="round" />
      </svg>
      <div style={{
        marginLeft: 14, marginTop: -2, display: 'inline-block',
        padding: '2px 7px', borderRadius: 6, fontSize: 10.5, fontWeight: 600,
        color: '#fff', background: `oklch(56% 0.18 ${hue})`,
        boxShadow: '0 2px 6px rgba(0,0,0,.18)', letterSpacing: '-0.005em',
      }}>{name}</div>
    </div>
  );
}

Object.assign(window, {
  Icon, Icons, Btn, Pill, Avatar, VideoTile, IconBtn, Card,
  SectionLabel, ScreenHeader, TabBar, RemotePointer,
  IOS_STATUSBAR_H, ANDROID_STATUSBAR_H,
});
