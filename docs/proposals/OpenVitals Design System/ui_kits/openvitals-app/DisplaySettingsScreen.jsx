// DisplaySettingsScreen — a Settings sub-screen showcasing the form controls.
const { TopBar, Select, RadioGroup, Switch, SectionHeader } = window.OpenVitalsDesignSystem_626946;

function Group({ children }) {
  return <div style={{ background: 'var(--ov-surface-container)', borderRadius: 'var(--ov-radius-md)', padding: 16 }}>{children}</div>;
}

function DisplaySettingsScreen({ nav }) {
  const [lang, setLang] = React.useState('System default');
  const [units, setUnits] = React.useState('Imperial');
  const [theme, setTheme] = React.useState('Dark');
  const [dynamic, setDynamic] = React.useState(true);
  const [amoled, setAmoled] = React.useState(false);

  return (
    <div style={{ paddingBottom: 32 }}>
      <TopBar title="Display" onBack={() => nav('settings')} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: '8px 16px 0' }}>
        <Group>
          <Select label="App language" value={lang} onChange={setLang}
            options={['System default', 'English', 'Español', 'Deutsch', 'Italiano']} />
          <div style={{ height: 14 }} />
          <Select label="Units" value={units} onChange={setUnits} options={['Metric', 'Imperial']} />
        </Group>

        <SectionHeader text="Theme" />
        <Group>
          <RadioGroup value={theme} onChange={setTheme} options={['System', 'Light', 'Dark', 'AMOLED']} />
        </Group>

        <SectionHeader text="Options" />
        <Group>
          <Switch label="Dynamic color (Material You)" checked={dynamic} onChange={setDynamic} />
          <div style={{ height: 4 }} />
          <div style={{ height: 1, background: 'var(--ov-outline-variant)', opacity: 0.5, margin: '10px 0' }} />
          <Switch label="Pure black in dark mode" checked={amoled} onChange={setAmoled} />
        </Group>
      </div>
    </div>
  );
}

Object.assign(window, { DisplaySettingsScreen });
