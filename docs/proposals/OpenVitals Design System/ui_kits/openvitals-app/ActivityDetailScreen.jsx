// ActivityDetailScreen — a recorded walk. Composes TopBar, Card, DetailRow, AccentIconChip.
const { TopBar, Card, DetailRow, AccentIconChip } = window.OpenVitalsDesignSystem_626946;

function ActivityDetailScreen({ nav }) {
  const metrics = [
    ['Duration', '2h 46m'], ['Steps', '14,024'], ['Distance', '10.0 mi'],
    ['Average pace', '16:43 min/mi'], ['Average speed', '3.6 mph'], ['Recorded speed', '4.5 mph'],
    ['Average heart rate', '107 bpm'], ['Average power', 'Not available'], ['Step cadence', '79.3 rpm'],
    ['Total calories burned', '833 kcal'], ['Active calories', '562 kcal'], ['Floors climbed', '0'],
    ['Elevation gained', '276 ft'],
  ];
  return (
    <div style={{ paddingBottom: 32 }}>
      <TopBar title="Activity detail" onBack={() => nav('dashboard')} />

      <div style={{ padding: '4px 16px 0' }}>
        <Card padding={16}>
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <AccentIconChip icon="directions_walk" color="var(--ov-metric-workout)" size={40} />
              <div>
                <div style={{ font: 'var(--ov-weight-bold) var(--ov-headline-sm-size)/1.1 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>Walk</div>
                <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>Walking</div>
              </div>
            </div>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8, padding: '6px 12px',
              borderRadius: 999, border: '1px solid var(--ov-outline-variant)' }}>
              <span style={{ width: 14, height: 14, borderRadius: '50%', background: 'var(--ov-outline)' }} />
              <span style={{ font: 'var(--ov-weight-medium) var(--ov-label-md-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>Gadgetbridge</span>
            </div>
          </div>
          <div style={{ marginTop: 14, font: 'var(--ov-weight-bold) var(--ov-headline-lg-size)/1 var(--ov-font-sans)',
            color: 'var(--ov-metric-workout)', fontFeatureSettings: "'tnum'" }}>2h 46m</div>
          <div style={{ marginTop: 6, font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.4 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>
            Jun 28, 2026, 10:33 AM – 1:20 PM</div>
        </Card>

        <div style={{ marginTop: 12 }}>
          <Card padding={16}>
            <div style={{ font: 'var(--ov-weight-bold) var(--ov-title-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface)', marginBottom: 8 }}>Metrics</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {metrics.map(([l, v]) => <DetailRow key={l} label={l} value={v} />)}
            </div>
          </Card>
        </div>

        <div style={{ marginTop: 12 }}>
          <Card variant="accent" accentColor="var(--ov-metric-heart)" padding={16}>
            <div style={{ font: 'var(--ov-weight-bold) var(--ov-title-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface)', marginBottom: 12 }}>Heart rate</div>
            <div style={{ display: 'flex', gap: 12 }}>
              {[['108 bpm', 'Avg'], ['68–140 bpm', 'Range'], ['1,566', 'Samples']].map(([v, l]) => (
                <div key={l} style={{ flex: 1 }}>
                  <div style={{ font: 'var(--ov-weight-bold) var(--ov-title-lg-size)/1.1 var(--ov-font-sans)', color: 'var(--ov-metric-heart)' }}>{v}</div>
                  <div style={{ font: 'var(--ov-weight-medium) var(--ov-label-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>{l}</div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { ActivityDetailScreen });
