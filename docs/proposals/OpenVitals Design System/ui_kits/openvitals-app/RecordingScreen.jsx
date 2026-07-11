// RecordingScreen — live activity recording (Stats tab). Composes TopBar.
const { TopBar } = window.OpenVitalsDesignSystem_626946;

function RecordingScreen({ nav }) {
  const tabs = ['Map', 'Stats', 'Intervals', 'By time', 'By distance'];
  const stats = [['—', 'CADENCE', 'rpm'], ['0.0', 'SPEED', 'mph'], ['0', 'DISTANCE', 'ft'], ['0:15', 'TOTAL TIME', '']];
  const accent = 'var(--ov-recording-light-accent)';
  return (
    <div style={{ minHeight: '100%', background: 'var(--ov-surface-container-lowest)' }}>
      <TopBar title="Recording activity" onBack={() => nav('dashboard')}
        actions={[{ icon: 'wb_sunny', label: 'Outdoor mode' }]} />

      <div style={{ display: 'flex', gap: 20, padding: '0 16px', borderBottom: '1px solid var(--ov-outline-variant)', overflowX: 'auto' }}>
        {tabs.map((t, i) => (
          <div key={t} style={{ padding: '12px 0', whiteSpace: 'nowrap',
            font: `${i === 1 ? 'var(--ov-weight-bold)' : 'var(--ov-weight-medium)'} var(--ov-title-md-size)/1 var(--ov-font-sans)`,
            color: accent, borderBottom: i === 1 ? `3px solid ${accent}` : '3px solid transparent' }}>{t}</div>
        ))}
      </div>

      <div style={{ padding: '28px 20px 8px' }}>
        <div style={{ width: 44, height: 5, borderRadius: 3, background: accent, marginBottom: 10 }} />
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
          <span style={{ font: 'var(--ov-weight-bold) var(--ov-title-sm-size)/1 var(--ov-font-sans)', letterSpacing: '.5px', color: accent }}>HEART RATE</span>
          <span style={{ font: 'var(--ov-weight-regular) var(--ov-title-lg-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>— bpm</span>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, padding: '24px 20px' }}>
        {stats.map(([v, l, u]) => (
          <div key={l}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
              <span style={{ font: 'var(--ov-weight-bold) var(--ov-headline-lg-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface)', fontFeatureSettings: "'tnum'" }}>{v}</span>
              {u ? <span style={{ font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>{u}</span> : null}
            </div>
            <div style={{ marginTop: 4, font: 'var(--ov-weight-medium) var(--ov-title-sm-size)/1 var(--ov-font-sans)', letterSpacing: '.5px', color: accent }}>{l}</div>
          </div>
        ))}
      </div>
      <div style={{ padding: '0 20px', font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>Last accuracy 13 ft</div>

      <div style={{ position: 'sticky', bottom: 0, marginTop: 32, padding: '16px 20px 20px', background: 'var(--ov-surface-container-lowest)',
        borderTop: '1px solid var(--ov-outline-variant)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          {[['pause', 'Pause'], ['crop_free', 'Focus'], ['check', 'Finish']].map(([ic, lb]) => (
            <button key={lb} onClick={() => { if (lb === 'Finish') nav('activity'); }}
              style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, border: 'none',
                background: 'transparent', color: 'var(--ov-on-surface)', cursor: 'pointer',
                font: 'var(--ov-weight-medium) var(--ov-title-md-size)/1 var(--ov-font-sans)' }}>
              <span className="material-symbols-outlined" style={{ fontSize: 22 }}>{ic}</span>{lb}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { RecordingScreen });
