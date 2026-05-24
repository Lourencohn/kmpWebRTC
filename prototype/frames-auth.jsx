function AuthWelcome() {
  return (
    <div style={{
      paddingTop: STATUS_PAD, minHeight: '100%',
      background: 'var(--bg)',
      display: 'flex', flexDirection: 'column',
      position: 'relative', overflow: 'hidden',
    }}>
      <div style={{
        position: 'absolute', top: '-10%', left: '-30%', width: '160%', height: '60%',
        background: 'radial-gradient(50% 50% at 50% 50%, rgba(0,87,184,.08) 0%, transparent 70%)',
        pointerEvents: 'none',
      }}/>
      <div style={{
        position: 'absolute', bottom: '15%', right: '-40%', width: '160%', height: '40%',
        background: 'radial-gradient(50% 50% at 50% 50%, rgba(27,67,50,.08) 0%, transparent 70%)',
        pointerEvents: 'none',
      }}/>

      <div style={{ padding: '8px 24px 0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <TrovataMark size={22}/>
          <span style={{ fontSize: 11.5, color: 'var(--ink-3)', fontWeight: 600, letterSpacing: '0.04em' }}>
            TrovataCast
          </span>
        </div>
        <span style={{ fontSize: 11, color: 'var(--ink-4)', fontFamily: 'var(--f-mono)' }}>v1.2.0</span>
      </div>

      <div style={{ flex: 1, padding: '36px 28px 0', position: 'relative', zIndex: 1 }}>
        <div style={{ fontSize: 10.5, color: 'var(--ink-3)', textTransform: 'uppercase', letterSpacing: '0.18em', fontWeight: 600 }}>
          Para representantes B2B
        </div>
        <div style={{
          marginTop: 16, fontSize: 38, fontWeight: 700, letterSpacing: '-0.035em',
          color: 'var(--ink)', lineHeight: 1.02,
        }}>
          A venda <br/>
          acontece <br/>
          <span style={{
            color: 'var(--brand)', position: 'relative', display: 'inline-block',
          }}>
            juntos
            <svg width="110%" height="14" viewBox="0 0 200 14" style={{ position: 'absolute', left: 0, bottom: -8 }}>
              <path d="M2 8 Q 50 -2 100 6 T 198 4" stroke="var(--jade)" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
            </svg>
          </span>.
        </div>
        <div style={{
          marginTop: 22, fontSize: 14.5, color: 'var(--ink-2)', lineHeight: 1.5, maxWidth: 300,
        }}>
          Você e seu cliente vendo o mesmo catálogo, ao vivo, em telas diferentes.<br/>
          <span style={{ color: 'var(--ink-3)' }}>Ele não precisa instalar nada.</span>
        </div>

        <div style={{
          marginTop: 36, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0,
          position: 'relative', height: 72,
        }}>
          <div style={{
            position: 'relative', width: 60, height: 60, borderRadius: '50%',
            background: `radial-gradient(120% 90% at 50% 40%, oklch(85% 0.10 28) 0%, oklch(55% 0.13 28) 55%, oklch(28% 0.10 28) 100%)`,
            boxShadow: 'var(--sh-2)', border: '2px solid var(--surface)',
          }}>
            <div style={{
              position: 'absolute', inset: -6, borderRadius: '50%',
              border: '2px solid var(--brand)', opacity: .4,
              animation: 'tc-ring 2.6s ease-out infinite',
            }}/>
          </div>
          <div style={{
            flex: 1, position: 'relative', height: 2,
            background: 'repeating-linear-gradient(to right, var(--ink-5) 0 4px, transparent 4px 8px)',
            margin: '0 -8px',
          }}>
            <div style={{
              position: 'absolute', left: '50%', top: -10, transform: 'translateX(-50%)',
              fontSize: 9, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.14em',
              color: 'var(--ink-3)', background: 'var(--bg)', padding: '2px 8px', borderRadius: 999,
              border: '1px solid var(--line)',
            }}>
              ao vivo
            </div>
          </div>
          <div style={{
            position: 'relative', width: 60, height: 60, borderRadius: '50%',
            background: `radial-gradient(120% 90% at 50% 40%, oklch(85% 0.10 280) 0%, oklch(55% 0.13 280) 55%, oklch(28% 0.10 280) 100%)`,
            boxShadow: 'var(--sh-2)', border: '2px solid var(--surface)',
          }}>
            <div style={{
              position: 'absolute', inset: -6, borderRadius: '50%',
              border: '2px solid var(--jade)', opacity: .4,
              animation: 'tc-ring 2.6s ease-out infinite', animationDelay: '0.6s',
            }}/>
          </div>
        </div>

        <div style={{
          marginTop: 24, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 18,
          fontSize: 10.5, color: 'var(--ink-4)', fontWeight: 500,
        }}>
          <span>Vendedor</span>
          <span style={{ color: 'var(--ink-5)' }}>App</span>
          <span style={{ color: 'var(--ink-5)' }}>↔</span>
          <span style={{ color: 'var(--ink-5)' }}>Navegador</span>
          <span>Cliente</span>
        </div>
      </div>

      <div style={{ padding: '0 24px 28px', position: 'relative', zIndex: 1 }}>
        <Btn kind="primary" size="lg" style={{ width: '100%' }}>
          Entrar pelo WhatsApp
        </Btn>
        <Btn kind="ghost" size="md" style={{ width: '100%', marginTop: 10 }}>
          Já tenho conta — entrar
        </Btn>
        <div style={{
          marginTop: 18, fontSize: 11, color: 'var(--ink-4)', textAlign: 'center', lineHeight: 1.5,
        }}>
          Ao continuar você aceita os <b style={{ color: 'var(--ink-2)' }}>termos</b> e a{' '}
          <b style={{ color: 'var(--ink-2)' }}>política de privacidade</b>.
        </div>
      </div>
    </div>
  );
}

function AuthLogin() {
  return (
    <div style={{
      paddingTop: STATUS_PAD, minHeight: '100%',
      background: 'var(--bg)', position: 'relative',
    }}>
      <div style={{ padding: '0 24px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Icon d={Icons.back} size={24}/>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <TrovataMark size={18}/>
          <span style={{ fontSize: 11, color: 'var(--ink-4)', fontWeight: 600, letterSpacing: '0.04em' }}>TrovataCast</span>
        </div>
        <div style={{ width: 24 }}/>
      </div>

      <div style={{ padding: '24px 28px 0' }}>
        <div style={{ fontSize: 11, color: 'var(--brand-2)', textTransform: 'uppercase', letterSpacing: '0.12em', fontWeight: 700 }}>
          Passo 1 de 2
        </div>
        <div style={{ marginTop: 8, fontSize: 30, fontWeight: 700, letterSpacing: '-0.03em', lineHeight: 1.1 }}>
          Entrar com <span style={{ color: 'var(--jade)' }}>WhatsApp</span>
        </div>
        <div style={{ marginTop: 8, fontSize: 13.5, color: 'var(--ink-3)', lineHeight: 1.5 }}>
          Vamos te chamar uma vez nesse número para confirmar quem é você. Não usamos o WhatsApp para mais nada.
        </div>
      </div>

      <div style={{ padding: '28px 24px 0' }}>
        <label style={{ fontSize: 10.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 700, paddingLeft: 4 }}>
          Seu número
        </label>
        <div style={{
          marginTop: 8, display: 'flex', alignItems: 'center',
          background: 'var(--surface)', border: '2px solid var(--brand)', borderRadius: 14,
          boxShadow: '0 0 0 4px var(--brand-ring)',
          overflow: 'hidden',
        }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 8, padding: '0 14px',
            borderRight: '1px solid var(--line)', height: 54,
          }}>
            <div style={{ width: 22, height: 16, borderRadius: 3, overflow: 'hidden', boxShadow: '0 0 0 1px rgba(0,0,0,.08)' }}>
              <div style={{ background: '#009C3B', width: '100%', height: '100%', position: 'relative' }}>
                <div style={{
                  position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  <div style={{ width: 12, height: 9, background: '#FFDF00', transform: 'rotate(45deg) scale(.85)' }}/>
                </div>
                <div style={{
                  position: 'absolute', left: '50%', top: '50%', width: 6, height: 6, borderRadius: '50%',
                  background: '#002776', transform: 'translate(-50%, -50%)',
                }}/>
              </div>
            </div>
            <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--ink)', fontFamily: 'var(--f-mono)' }}>+55</span>
            <Icon d={Icons.chevDown} size={14} stroke="var(--ink-4)"/>
          </div>
          <div style={{
            flex: 1, padding: '0 16px', display: 'flex', alignItems: 'center',
            fontSize: 18, fontWeight: 600, fontFamily: 'var(--f-mono)', letterSpacing: '0.04em',
            color: 'var(--ink)',
          }}>
            <span>(31) 99876-</span>
            <span style={{ display: 'inline-block', width: 8 }}/>
            <span style={{ color: 'var(--ink-5)' }}>____</span>
            <span style={{
              width: 2, height: 20, background: 'var(--brand)',
              marginLeft: -22, animation: 'tc-blink 1s ease-in-out infinite',
            }}/>
          </div>
        </div>

        <div style={{
          marginTop: 14, padding: '12px 14px',
          background: 'var(--jade-tint)', border: '1px solid var(--jade-tint)', borderRadius: 12,
          display: 'flex', alignItems: 'flex-start', gap: 10,
        }}>
          <div style={{
            width: 24, height: 24, borderRadius: 6, background: 'var(--jade)', color: '#fff',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <Icon d={Icons.msg} size={13} sw={2.2}/>
          </div>
          <div style={{ fontSize: 11.5, color: 'var(--jade-2)', lineHeight: 1.4 }}>
            Vamos enviar um <b>código de 6 dígitos</b> no WhatsApp.
            Em segundos você está dentro.
          </div>
        </div>
      </div>

      <div style={{ padding: '28px 24px 0' }}>
        <Btn kind="primary" size="lg" icon="arrowRight" style={{ width: '100%' }}>
          Continuar
        </Btn>
        <div style={{ marginTop: 18, display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ flex: 1, height: 1, background: 'var(--line)' }}/>
          <span style={{ fontSize: 10.5, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.12em', fontWeight: 600 }}>
            ou
          </span>
          <div style={{ flex: 1, height: 1, background: 'var(--line)' }}/>
        </div>
        <Btn kind="surface" size="md" icon="msg" style={{ width: '100%', marginTop: 14 }}>
          Entrar com email
        </Btn>
      </div>

      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        padding: '14px 24px 28px', textAlign: 'center',
        fontSize: 11.5, color: 'var(--ink-4)', lineHeight: 1.5,
      }}>
        Não tem conta?{' '}
        <span style={{ color: 'var(--brand)', fontWeight: 600 }}>Criar agora</span>
        {' '}— leva 2 minutos.
      </div>
    </div>
  );
}

function AuthVerify() {
  const code = ['8', '4', '3', '2', '', ''];
  return (
    <div style={{
      paddingTop: STATUS_PAD, minHeight: '100%',
      background: 'var(--bg)', position: 'relative',
    }}>
      <div style={{ padding: '0 24px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Icon d={Icons.back} size={24}/>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <TrovataMark size={18}/>
          <span style={{ fontSize: 11, color: 'var(--ink-4)', fontWeight: 600, letterSpacing: '0.04em' }}>TrovataCast</span>
        </div>
        <div style={{ width: 24 }}/>
      </div>

      <div style={{ padding: '24px 28px 0' }}>
        <div style={{ fontSize: 11, color: 'var(--brand-2)', textTransform: 'uppercase', letterSpacing: '0.12em', fontWeight: 700 }}>
          Passo 2 de 2
        </div>
        <div style={{ marginTop: 8, fontSize: 30, fontWeight: 700, letterSpacing: '-0.03em', lineHeight: 1.1 }}>
          Código no <span style={{ color: 'var(--jade)' }}>WhatsApp</span>
        </div>
        <div style={{ marginTop: 8, fontSize: 13.5, color: 'var(--ink-3)', lineHeight: 1.5 }}>
          Enviamos um código para <b style={{ fontFamily: 'var(--f-mono)', color: 'var(--ink-2)' }}>+55 (31) 99876-2104</b>.
        </div>
      </div>

      <div style={{ padding: '36px 18px 0', display: 'flex', justifyContent: 'center', gap: 8 }}>
        {code.map((d, i) => {
          const filled = d !== '';
          const active = i === 4;
          return (
            <div key={i} style={{
              width: 48, height: 60, borderRadius: 12,
              background: filled ? 'var(--surface)' : 'var(--surface-2)',
              border: `2px solid ${active ? 'var(--brand)' : filled ? 'var(--line-strong)' : 'var(--line)'}`,
              boxShadow: active ? '0 0 0 4px var(--brand-ring)' : (filled ? 'var(--sh-1)' : 'none'),
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 28, fontWeight: 700, fontFamily: 'var(--f-mono)', letterSpacing: '-0.02em',
              color: filled ? 'var(--ink)' : 'var(--ink-5)',
              position: 'relative',
            }}>
              {d}
              {active && (
                <span style={{
                  width: 2, height: 28, background: 'var(--brand)',
                  animation: 'tc-blink 1s ease-in-out infinite', position: 'absolute',
                }}/>
              )}
            </div>
          );
        })}
      </div>

      <div style={{ padding: '24px 28px 0', textAlign: 'center' }}>
        <div style={{ fontSize: 12, color: 'var(--ink-3)' }}>
          Não recebeu?{' '}
          <span style={{ color: 'var(--ink-4)' }}>Reenviar em </span>
          <b style={{ fontFamily: 'var(--f-mono)', color: 'var(--ink-2)' }}>00:42</b>
        </div>
      </div>

      <div style={{ padding: '28px 24px 0' }}>
        <Btn kind="primary" size="lg" icon="arrowRight" style={{ width: '100%' }}>
          Continuar
        </Btn>
        <Btn kind="ghost" size="md" style={{ width: '100%', marginTop: 10 }}>
          Mudar número
        </Btn>
      </div>

      <div style={{
        position: 'absolute', left: 24, right: 24, bottom: 28,
        padding: '12px 14px',
        background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 12,
        display: 'flex', alignItems: 'center', gap: 10,
      }}>
        <div style={{
          width: 28, height: 28, borderRadius: 8, background: 'var(--surface-2)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon d={Icons.lock} size={14} stroke="var(--ink-3)"/>
        </div>
        <div style={{ flex: 1, fontSize: 11.5, color: 'var(--ink-3)', lineHeight: 1.4 }}>
          O código é privado. Nunca peça para outra pessoa.
        </div>
      </div>
    </div>
  );
}

function AuthSetup() {
  const brands = [
    { name: 'Atelier Norte', tag: 'Moda atacado', skus: 47, colors: ['#3F4744', '#A5806A', '#C7B79B'], selected: true },
    { name: 'Casa Maré', tag: 'Decoração', skus: 122, colors: ['#DDE4E6', '#3A5260', '#E8E4DC'] },
    { name: 'Cobogó', tag: 'Calçados artesanais', skus: 28, colors: ['#7C6E58', '#534234', '#EEEAE0'] },
  ];
  return (
    <PhoneScroll>
      <div style={{ padding: '0 24px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Icon d={Icons.back} size={24}/>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <TrovataMark size={18}/>
          <span style={{ fontSize: 11, color: 'var(--ink-4)', fontWeight: 600, letterSpacing: '0.04em' }}>TrovataCast</span>
        </div>
        <span style={{ fontSize: 11, color: 'var(--ink-4)', fontWeight: 600 }}>3 / 3</span>
      </div>

      <div style={{ padding: '24px 24px 0' }}>
        <div style={{ fontSize: 11, color: 'var(--jade-2)', textTransform: 'uppercase', letterSpacing: '0.12em', fontWeight: 700 }}>
          Quase lá, Camila
        </div>
        <div style={{ marginTop: 8, fontSize: 28, fontWeight: 700, letterSpacing: '-0.03em', lineHeight: 1.1 }}>
          Qual marca você representa?
        </div>
        <div style={{ marginTop: 8, fontSize: 13.5, color: 'var(--ink-3)', lineHeight: 1.5 }}>
          Você pode representar mais de uma. A escolhida agora será a padrão.
        </div>
      </div>

      <div style={{ padding: '20px 16px 0', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {brands.map((b, i) => (
          <div key={i} style={{
            padding: 14, borderRadius: 16,
            background: b.selected ? 'var(--surface)' : 'var(--surface)',
            border: `2px solid ${b.selected ? 'var(--brand)' : 'var(--line)'}`,
            boxShadow: b.selected ? '0 0 0 4px var(--brand-ring), var(--sh-1)' : 'var(--sh-1)',
            display: 'flex', alignItems: 'center', gap: 14,
          }}>
            <div style={{
              width: 56, height: 56, borderRadius: 12, flexShrink: 0,
              background: `linear-gradient(135deg, ${b.colors[0]} 0%, ${b.colors[1]} 60%, ${b.colors[2]} 100%)`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#fff', fontSize: 20, fontWeight: 700, letterSpacing: '-0.02em',
              boxShadow: 'inset 0 -8px 16px rgba(0,0,0,.15)',
            }}>
              {b.name[0]}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14.5, fontWeight: 600, letterSpacing: '-0.01em' }}>{b.name}</div>
              <div style={{ fontSize: 11.5, color: 'var(--ink-3)', marginTop: 2 }}>{b.tag}</div>
              <div style={{ marginTop: 6, display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 10.5, fontFamily: 'var(--f-mono)', color: 'var(--ink-4)' }}>{b.skus} SKUs</span>
                <span style={{ width: 2, height: 2, background: 'var(--ink-5)', borderRadius: '50%' }}/>
                <div style={{ display: 'flex', gap: 3 }}>
                  {b.colors.map((c, j) => (
                    <span key={j} style={{ width: 9, height: 9, borderRadius: '50%', background: c, border: '1.5px solid var(--surface)' }}/>
                  ))}
                </div>
              </div>
            </div>
            <div style={{
              width: 24, height: 24, borderRadius: '50%',
              background: b.selected ? 'var(--brand)' : 'var(--surface)',
              border: b.selected ? 'none' : '1.5px solid var(--line-strong)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#fff', flexShrink: 0,
            }}>
              {b.selected && <Icon d={Icons.check} size={14} sw={3}/>}
            </div>
          </div>
        ))}
      </div>

      <div style={{ padding: '16px 16px 0' }}>
        <div style={{
          padding: 14, border: '1.5px dashed var(--line-strong)', borderRadius: 14,
          display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <div style={{
            width: 32, height: 32, borderRadius: 8, background: 'var(--surface-2)', color: 'var(--ink-3)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <Icon d={Icons.plus} size={16} sw={2}/>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 13, fontWeight: 600 }}>Outra marca</div>
            <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>Cadastre sua própria — leva 3 minutos</div>
          </div>
          <Icon d={Icons.chev} size={16} stroke="var(--ink-4)"/>
        </div>
      </div>

      <div style={{ padding: '20px 24px 16px' }}>
        <div style={{ fontSize: 11, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 700, paddingLeft: 4, marginBottom: 8 }}>
          Tabela padrão
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          {[
            { l: 'Atacado A', d: 'Min 6un', on: false },
            { l: 'Atacado B', d: 'Min 4un', on: true },
            { l: 'Mostruário', d: 'Avulso', on: false },
          ].map((t, i) => (
            <div key={i} style={{
              flex: 1, padding: '10px 8px', textAlign: 'center', borderRadius: 12,
              background: t.on ? 'var(--ink)' : 'var(--surface)',
              color: t.on ? '#fff' : 'var(--ink-2)',
              border: `1px solid ${t.on ? 'var(--ink)' : 'var(--line)'}`,
              boxShadow: t.on ? 'none' : 'var(--sh-1)',
            }}>
              <div style={{ fontSize: 12, fontWeight: 600 }}>{t.l}</div>
              <div style={{ fontSize: 10, color: t.on ? 'rgba(255,255,255,.6)' : 'var(--ink-4)', marginTop: 2, fontFamily: 'var(--f-mono)' }}>{t.d}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        padding: '14px 24px calc(22px + 12px)',
        background: 'rgba(250,250,246,.94)', backdropFilter: 'blur(20px)',
        borderTop: '1px solid var(--line)',
      }}>
        <Btn kind="primary" size="lg" icon="arrowRight" style={{ width: '100%' }}>
          Entrar no TrovataCast
        </Btn>
      </div>
    </PhoneScroll>
  );
}

Object.assign(window, { AuthWelcome, AuthLogin, AuthVerify, AuthSetup });
