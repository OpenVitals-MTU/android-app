// BeverageScreen — hydration logging. Composes TopBar, Button, AccentIconChip.
const { TopBar, Button, AccentIconChip } = window.OpenVitalsDesignSystem_626946;

function BeverageScreen({ nav }) {
  const [total, setTotal] = React.useState(0);
  const goal = 2.0;
  const cats = [['Coffees', 83], ['Energy drinks', 91], ['Teas', 16], ['Chocolate drinks', 3], ['Carbonated soft drinks', 21]];

  return (
    <div style={{ paddingBottom: 32 }}>
      <TopBar title="Beverage entry" onBack={() => nav('dashboard')} />

      <div style={{ padding: '4px 16px 0' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <AccentIconChip icon="local_drink" color="var(--ov-metric-hydration)" size={40} />
          <div>
            <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface)' }}>Log beverage</div>
            <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface-variant)' }}>Saved directly to Health Connect</div>
          </div>
        </div>

        <div style={{ marginTop: 14, background: 'var(--ov-surface-container-high)', borderRadius: 'var(--ov-radius-md)', padding: 18 }}>
          <div style={{ textAlign: 'right', font: 'var(--ov-weight-semibold) var(--ov-headline-sm-size)/1 var(--ov-font-sans)',
            color: 'var(--ov-on-surface)', fontFeatureSettings: "'tnum'" }}>{total.toFixed(2)} L / {goal.toFixed(2)} L</div>
          <div style={{ marginTop: 14, height: 6, borderRadius: 999, background: 'var(--ov-outline-variant)', position: 'relative' }}>
            <div style={{ position: 'absolute', left: 0, top: 0, bottom: 0, borderRadius: 999,
              width: `${Math.min(1, total / goal) * 100}%`, background: 'var(--ov-secondary)' }} />
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 18 }}>
          <span style={{ font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>Drink catalog</span>
          <span className="material-symbols-outlined" style={{ fontSize: 22, color: 'var(--ov-on-surface-variant)' }}>edit</span>
        </div>

        <div style={{ marginTop: 12, border: '1px solid var(--ov-outline-variant)', borderRadius: 'var(--ov-radius-sm)',
          padding: '14px 16px', color: 'var(--ov-on-surface-variant)',
          font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/1 var(--ov-font-sans)' }}>Search drinks</div>

        <div style={{ marginTop: 16, font: 'var(--ov-weight-medium) var(--ov-title-sm-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>Saved drinks</div>
        <div onClick={() => setTotal((t) => Math.round((t + 0.35) * 100) / 100)}
          style={{ marginTop: 10, background: 'var(--ov-surface-container-lowest)', borderRadius: 'var(--ov-radius-sm)',
          padding: 16, display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer' }}>
          <AccentIconChip icon="local_drink" color="var(--ov-metric-hydration)" size={40} />
          <div>
            <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>water</div>
            <div style={{ font: 'var(--ov-weight-medium) var(--ov-body-md-size)/1.5 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>350 ml</div>
            <div style={{ font: 'var(--ov-weight-semibold) var(--ov-body-sm-size)/1.4 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>Liquid only</div>
          </div>
        </div>

        {cats.map(([name, n]) => (
          <div key={name} style={{ marginTop: 10, background: 'var(--ov-surface-container-high)', borderRadius: 'var(--ov-radius-sm)',
            padding: '16px 18px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>{name}</div>
              <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>{n} drinks</div>
            </div>
            <span className="material-symbols-outlined" style={{ fontSize: 24, color: 'var(--ov-on-surface)' }}>expand_more</span>
          </div>
        ))}

        <div style={{ marginTop: 18 }}>
          <Button variant="filled" icon="add" fullWidth size="large"
            style={{ background: 'var(--ov-secondary)', color: 'var(--ov-on-secondary)' }}>New drink</Button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { BeverageScreen });
