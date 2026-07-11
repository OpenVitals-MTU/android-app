// BodyEnergyScreen — recovery detail with the line chart + data confidence.
const { TopBar, DateNavigator, Card, MetricLineChart, DataConfidenceCard } =
  window.OpenVitalsDesignSystem_626946;

function Stat({ label, value }) {
  return (
    <div>
      <div style={{ font: 'var(--ov-weight-medium) var(--ov-label-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>{label}</div>
      <div style={{ font: 'var(--ov-weight-bold) var(--ov-title-lg-size)/1.1 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>{value}</div>
    </div>
  );
}

function BodyEnergyScreen({ nav }) {
  const accent = 'var(--ov-metric-workout)';
  return (
    <div style={{ paddingBottom: 32 }}>
      <TopBar title="Body Energy" onBack={() => nav('dashboard')} />
      <div style={{ padding: '4px 16px 0' }}>
        <DateNavigator title="Today" subtitle="Sat 4 Jul" canGoForward={false}
          onPrevious={() => {}} onOpenCalendar={() => {}} />
      </div>

      <div style={{ padding: '16px 16px 0' }}>
        <Card variant="accent" accentColor={accent} padding={18}>
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span className="material-symbols-outlined" style={{ fontSize: 28, color: accent }}>battery_charging_full</span>
              <div>
                <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1.2 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>Body Energy</div>
                <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>Estimated by OpenVitals</div>
              </div>
            </div>
            <div style={{ font: 'var(--ov-weight-bold) var(--ov-headline-lg-size)/1 var(--ov-font-sans)', color: accent }}>83</div>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 18 }}>
            <Stat label="Start" value="81" />
            <Stat label="Charged" value="+2" />
            <Stat label="Drained" value="-0" />
          </div>
        </Card>

        <div style={{ marginTop: 12 }}>
          <Card padding={16}>
            <div style={{ font: 'var(--ov-weight-bold) var(--ov-title-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface)', marginBottom: 12 }}>Daily timeline</div>
            <MetricLineChart data={[81, 81, 82, 82, 83, 83, 83, 84]} yTicks={[0, 50, 100]}
              accentColor={accent} height={180} />
          </Card>
        </div>

        <div style={{ marginTop: 12 }}>
          <DataConfidenceCard level="low" coverage="Some timeline buckets have sparse Health Connect data"
            samples="Estimated where calibration is incomplete" accentColor={accent}
            warnings={["Charge and drain rates are approximate today"]} />
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { BodyEnergyScreen });
