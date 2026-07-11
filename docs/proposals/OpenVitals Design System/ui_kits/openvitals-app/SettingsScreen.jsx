// SettingsScreen — settings list. Composes TopBar, SettingsListItem, SectionHeader.
const { TopBar, SettingsListItem, SectionHeader } = window.OpenVitalsDesignSystem_626946;

function SettingsScreen({ nav }) {
  const items = [
    ['tune', 'Display', 'Language, units, and theme'],
    ['directions_run', 'Activities', 'Activity week, favorite activity, recording, and offline maps'],
    ['bluetooth', 'Sensors & devices', 'Heart rate, cadence, and power sensors'],
    ['restaurant', 'Nutrition', 'Calories data and caffeine personalization'],
    ['favorite', 'Recovery', 'Sleep range and Body Energy calibration'],
    ['folder_open', 'Data Import', 'Import Apple Health export records with Health Connect equivalents'],
    ['health_and_safety', 'Health Connect', 'Sync, permissions, access, and app lock'],
    ['bug_report', 'Debug diagnostics', 'Save sanitized diagnostics logs for troubleshooting'],
  ];
  return (
    <div style={{ paddingBottom: 32 }}>
      <TopBar title="Settings" onBack={() => nav('dashboard')} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, padding: '8px 16px 0' }}>
        {items.map(([icon, title, sub], i) => (
          <SettingsListItem key={i} icon={icon} title={title} supportingText={sub}
            onClick={() => { if (title === 'Display') nav('display'); if (title === 'Recovery') nav('readiness'); if (title === 'Nutrition') nav('beverage'); }} />
        ))}
        <div style={{ marginTop: 8 }}>
          <SectionHeader text="Support" />
        </div>
        <SettingsListItem icon="volunteer_activism" title="Support OpenVitals"
          supportingText="Report bugs, join community support discussions, or help fund ongoing development." />
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, padding: '16px 0 4px' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 20, color: 'var(--ov-on-surface)' }}>open_in_new</span>
          <span style={{ font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1 var(--ov-font-sans)',
            color: 'var(--ov-on-surface)', textDecoration: 'underline' }}>Report an issue</span>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { SettingsScreen });
