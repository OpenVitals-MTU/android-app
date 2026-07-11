**TextField** — Material 3 outlined input for search / custom amounts / manual entry. Supports `leadingIcon` and a `suffix` unit.

```jsx
<TextField value={q} onChange={setQ} placeholder="Search drinks" leadingIcon="search" />
<TextField value={amt} onChange={setAmt} label="Amount" suffix="ml" type="number" />
```
