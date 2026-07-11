**DateNavigator** — the date header at the top of detail screens (Dashboard, Daily Readiness, Body Energy). Big day title + subtitle on the left, three 52px circle buttons (previous / next / calendar) on the right. Disable forward at "today".

```jsx
<DateNavigator title="Today" subtitle="4 Jul 2026" canGoForward={false}
  onPrevious={() => {}} onOpenCalendar={() => {}} />
```
