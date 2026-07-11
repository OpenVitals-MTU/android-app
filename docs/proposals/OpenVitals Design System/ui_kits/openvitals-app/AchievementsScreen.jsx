// AchievementsScreen — badge progress list.
const { TopBar, Card, AchievementBadge } = window.OpenVitalsDesignSystem_626946;

function AchievementsScreen({ nav }) {
  const [filter, setFilter] = React.useState('All');
  const filters = ['All', 'Steps', 'Distance', 'Floors'];
  const badges = [
    { icon: 'directions_walk', name: 'High Tops', requirement: 'Walk 20,000 steps in a day', current: '19,576', target: '20,000', progress: 0.98, unlocked: false, status: 'Almost there', accentColor: 'var(--ov-metric-steps)', cat: 'Steps' },
    { icon: 'directions_walk', name: 'Sneakers', requirement: 'Walk 10,000 steps in a day', current: '19,576', target: '10,000', progress: 1, unlocked: true, status: 'Achieved 28 Jun', accentColor: 'var(--ov-metric-steps)', cat: 'Steps' },
    { icon: 'straighten', name: 'Marathon', requirement: 'Walk 26 mi in total', current: '41.2 mi', target: '26 mi', progress: 1, unlocked: true, status: 'Achieved 2 Jul', accentColor: 'var(--ov-metric-distance)', cat: 'Distance' },
    { icon: 'straighten', name: 'London Underground', requirement: 'Walk 250 mi in total', current: '41.2 mi', target: '250 mi', progress: 0.16, unlocked: false, status: 'Locked', accentColor: 'var(--ov-metric-distance)', cat: 'Distance' },
    { icon: 'stairs', name: 'Skyscraper', requirement: 'Climb 100 floors in a day', current: '62', target: '100', progress: 0.62, unlocked: false, status: 'Locked', accentColor: 'var(--ov-metric-floors)', cat: 'Floors' },
  ];
  const shown = badges.filter((b) => filter === 'All' || b.cat === filter);
  const unlocked = badges.filter((b) => b.unlocked).length;

  return (
    <div style={{ paddingBottom: 32 }}>
      <TopBar title="Achievements" onBack={() => nav('dashboard')} />
      <div style={{ padding: '4px 16px 0' }}>
        <Card padding={16}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ width: 48, height: 48, borderRadius: 999, flex: '0 0 auto',
              background: 'color-mix(in srgb, var(--ov-primary) 20%, transparent)',
              display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <span className="material-symbols-outlined" style={{ fontSize: 26, color: 'var(--ov-primary)' }}>workspace_premium</span>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-lg-size)/1.2 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>Achievements</div>
              <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.3 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>{unlocked} of {badges.length} unlocked</div>
            </div>
          </div>
          <div style={{ marginTop: 12, height: 8, borderRadius: 999, background: 'var(--ov-surface-container-highest)', overflow: 'hidden' }}>
            <div style={{ height: '100%', width: `${(unlocked / badges.length) * 100}%`, background: 'var(--ov-primary)' }} />
          </div>
        </Card>

        <div style={{ display: 'flex', gap: 8, margin: '14px 0 6px', flexWrap: 'wrap' }}>
          {filters.map((f) => {
            const sel = f === filter;
            return (
              <button key={f} onClick={() => setFilter(f)}
                style={{ padding: '7px 14px', borderRadius: 999, cursor: 'pointer',
                  border: `1px solid ${sel ? 'transparent' : 'var(--ov-outline-variant)'}`,
                  background: sel ? 'var(--ov-secondary-container)' : 'transparent',
                  color: sel ? 'var(--ov-on-secondary-container)' : 'var(--ov-on-surface-variant)',
                  font: 'var(--ov-weight-medium) var(--ov-label-lg-size)/1 var(--ov-font-sans)' }}>{f}</button>
            );
          })}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 8 }}>
          {shown.map((b, i) => <AchievementBadge key={i} {...b} />)}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { AchievementsScreen });
