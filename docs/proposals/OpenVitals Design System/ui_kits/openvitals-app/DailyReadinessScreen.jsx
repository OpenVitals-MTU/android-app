// DailyReadinessScreen — recovery detail. Composes TopBar, DateNavigator,
// ReadinessBanner, Card, AccentIconChip from the bundle.
const { TopBar, DateNavigator, ReadinessBanner, Card, AccentIconChip, CrossMetricInsightCard } =
  window.OpenVitalsDesignSystem_626946;

function SubTile({ icon, color, label, value, muted }) {
  return (
    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
      padding: '12px 14px', borderRadius: 'var(--ov-radius-sm)',
      background: muted ? 'var(--ov-surface-container-high)' : `color-mix(in srgb, ${color} 16%, var(--ov-surface-container))` }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
        <span className="material-symbols-outlined" style={{ fontSize: 22, color }}>{icon}</span>
        <div style={{ minWidth: 0 }}>
          <div style={{ font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)' }}>{label}</div>
          <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface)' }}>{value}</div>
        </div>
      </div>
      <span className="material-symbols-outlined" style={{ fontSize: 20, color: 'var(--ov-on-surface-variant)' }}>chevron_right</span>
    </div>
  );
}

function InsightBlock({ title, body }) {
  return (
    <div style={{ marginTop: 16 }}>
      <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface)' }}>{title}</div>
      <div style={{ marginTop: 2, font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface)' }}>{body}</div>
    </div>
  );
}

function RecommendRow({ icon, color, label, body }) {
  return (
    <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start', marginTop: 14 }}>
      <AccentIconChip icon={icon} color={color} size={36} />
      <div style={{ flex: 1 }}>
        <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface)' }}>{label}</div>
        <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface)' }}>{body}</div>
      </div>
    </div>
  );
}

function DailyReadinessScreen({ nav }) {
  return (
    <div style={{ paddingBottom: 32 }}>
      <TopBar title="Daily Readiness" onBack={() => nav('dashboard')} />
      <div style={{ padding: '4px 16px 0' }}>
        <DateNavigator title="Today" subtitle="4 Jul 2026" canGoForward={false}
          onPrevious={() => {}} onOpenCalendar={() => {}} />
      </div>

      <div style={{ padding: '16px 16px 0' }}>
        <ReadinessBanner
          score="65/100"
          scoreLabel="Readiness"
          confidence="Medium confidence · sleep data missing"
          headline="Train, but keep it controlled"
          body="Do moderate training today, but avoid maximal effort. Your signals suggest this mainly because hydration is 0% of today's goal and resting heart rate is 2 bpm below your usual baseline.">
          <div style={{ display: 'flex', gap: 12 }}>
            <SubTile icon="favorite" color="var(--ov-metric-heart)" label="Body Energy" value="63/100" />
            <SubTile icon="fitness_center" color="var(--ov-metric-workout)" label="Training Readiness" value="75/100" muted />
          </div>
          <InsightBlock title="HRV Status" body="Needs more HRV · HRV was not available for this day." />
          <InsightBlock title="Intensity Minutes" body="Goal met · 244/150 moderate-equivalent min this week; vigorous minutes count double." />
          <InsightBlock title="Stress Level" body="Low · 32/100 · Signals suggest low physiological stress." />
          <div style={{ height: 1, background: 'var(--ov-outline-variant)', opacity: 0.5, margin: '18px 0 4px' }} />
          <RecommendRow icon="directions_run" color="var(--ov-metric-heart)" label="Recommended"
            body="Zone 2 cardio, moderate strength, technique work, or an easy bike ride." />
          <RecommendRow icon="close" color="var(--ov-error)" label="Avoid"
            body="Max effort, HIIT, and very long sessions." />
          <RecommendRow icon="self_improvement" color="var(--ov-metric-mindfulness)" label="Alternative"
            body="If you feel tired, choose a 30 min easy walk or a mobility session." />
        </ReadinessBanner>
        {CrossMetricInsightCard ? (
          <div style={{ marginTop: 12 }}>
            <CrossMetricInsightCard title="Sleep and readiness" direction="positive" correlation={62}
              message="On nights you sleep longer, your next-day readiness tends to be higher."
              pairedDays={21} accentColor="var(--ov-metric-sleep)" />
          </div>
        ) : null}
      </div>
    </div>
  );
}

Object.assign(window, { DailyReadinessScreen });
