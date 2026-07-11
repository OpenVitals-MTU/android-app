// DashboardScreen — OpenVitals home. Composes SummaryRingCard, MetricStatCard,
// Button, TopBar, DateNavigator, SectionHeader from the design system bundle.
const { TopBar, DateNavigator, SummaryRingCard, MetricStatCard, Button, SectionHeader, AccentIconChip, SensorStatusCard } =
  window.OpenVitalsDesignSystem_626946;

function DashboardScreen({ nav }) {
  const dots = [0, 1, 2, 3, 4, 5];
  return (
    <div style={{ paddingBottom: 24 }}>
      <TopBar large title="OpenVitals" actions={[
        { icon: 'self_improvement', label: 'Mindfulness', onClick: () => {} },
        { icon: 'workspace_premium', label: 'Achievements', onClick: () => nav('achievements') },
        { icon: 'settings', label: 'Settings', onClick: () => nav('settings') },
      ]} />

      <div style={{ padding: '4px 16px 0' }}>
        <DateNavigator title="Sun, 28 Jun" subtitle="28 Jun 2026" canGoForward={false}
          onPrevious={() => {}} onNext={() => {}} onOpenCalendar={() => {}} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, padding: '16px 16px 0' }}>
        <SummaryRingCard title="Steps" value="19,576" subtitle="steps of 8,000"
          progress={1} accentColor="var(--ov-metric-steps)" size={150} style={{ aspectRatio: '1 / 1' }}
          onClick={() => nav('activity')} />
        <SummaryRingCard title="Weekly cardio" value="100%" subtitle="802 of 25"
          progress={1} accentColor="var(--ov-metric-workout)" size={150} style={{ aspectRatio: '1 / 1' }} />
      </div>

      <div style={{ display: 'flex', gap: 12, padding: '14px 16px 0', alignItems: 'center' }}>
        <Button variant="tonal" icon="add" fullWidth size="large" onClick={() => nav('beverage')}>Log</Button>
        <Button variant="filled" icon="directions_run" fullWidth size="large" onClick={() => nav('recording')}>Start</Button>
        <button aria-label="Edit dashboard" style={{ width: 44, height: 44, flex: '0 0 auto', borderRadius: 999,
          border: 'none', background: 'transparent', color: 'var(--ov-on-surface-variant)', cursor: 'pointer' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 20 }}>edit</span>
        </button>
      </div>

      <div style={{ height: 1, background: 'var(--ov-outline-variant)', opacity: 0.6, margin: '16px 16px 4px' }} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, padding: '12px 16px 0' }}>
        <MetricStatCard title="Distance" value="18.6" unit="km" icon="straighten"
          accentColor="var(--ov-metric-distance)" progress={1} onClick={() => nav('activity')} />
        <MetricStatCard title="Total calories" value="3,178" unit="kcal" icon="local_fire_department"
          accentColor="var(--ov-metric-calories)" progress={0.9} />
        <MetricStatCard title="Active calories" value="837" unit="kcal" icon="mode_heat"
          accentColor="var(--ov-metric-active-calories)" progress={0.7} />
        <MetricStatCard title="Elevation" value="84" unit="m" icon="landscape"
          accentColor="var(--ov-metric-elevation)" progress={0.5} />
        <MetricStatCard title="Sleep" value="5h 15m" subtitle="74 · Fair" icon="bedtime"
          accentColor="var(--ov-metric-sleep)" progress={0.5} onClick={() => nav('readiness')} />
        <MetricStatCard title="Beverages" value="0.00" unit="L" icon="local_drink"
          accentColor="var(--ov-metric-hydration)" progress={0} onClick={() => nav('beverage')} />
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', gap: 8, padding: '18px 0 4px' }}>
        {dots.map((d) => (
          <span key={d} style={{ width: 8, height: 8, borderRadius: 999,
            background: d === 0 ? 'var(--ov-primary)' : 'var(--ov-outline-variant)' }} />
        ))}
      </div>

      <div style={{ padding: '8px 16px 0' }}>
        {SensorStatusCard ? (
          <SensorStatusCard batteryPercent={64} activeCount={2} connectedCount={1} onClick={() => nav('settings')} />
        ) : null}
      </div>

      <div style={{ padding: '8px 16px 0' }}>
        <SectionHeader text="Activities" trailing="chevron_right" onTrailingClick={() => nav('activity')} />
        <div onClick={() => nav('activity')} style={{ background: 'var(--ov-surface-container)', borderRadius: 'var(--ov-radius-md)',
          padding: 16, cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <AccentIconChip icon="directions_walk" color="var(--ov-metric-workout)" />
              <span style={{ font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
                color: 'var(--ov-on-surface)' }}>Workout</span>
            </div>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8, padding: '6px 12px',
              borderRadius: 999, border: '1px solid var(--ov-outline-variant)' }}>
              <span style={{ width: 14, height: 14, borderRadius: '50%', background: 'var(--ov-outline)' }} />
              <span style={{ font: 'var(--ov-weight-medium) var(--ov-label-md-size)/1 var(--ov-font-sans)',
                color: 'var(--ov-on-surface-variant)' }}>Gadgetbridge</span>
            </div>
          </div>
          <div style={{ marginTop: 12, font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)' }}>Walking</div>
          <div style={{ font: 'var(--ov-weight-bold) var(--ov-headline-lg-size)/var(--ov-headline-lg-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface)' }}>2h 46m</div>
          <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)' }}>10:33 AM</div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { DashboardScreen });
