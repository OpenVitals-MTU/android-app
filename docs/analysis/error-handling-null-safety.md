# Error Handling and Null Safety

## Current error model

Screen failures are represented as **optional strings** on `UiState`:

```kotlin
data class SleepUiState(
    // ...
    val error: String? = null,
)

data class DashboardUiState(
    // ...
    val errorMessage: String? = null,
)
```

ViewModels set errors inside `runCatching { }.onFailure { }`:

```kotlin
.onFailure {
    if (!isCurrent) return@load
    _uiState.value = _uiState.value.copy(
        isLoading = false,
        error = it.message,
    )
}
```

`MetricDetailScaffold` accepts `error: String?` and renders a shared error block when non-null.

## Strengths

- Simple mental model for feature developers
- Works with existing scaffold without extra mapping
- Failures do not crash the app; loading flag is always cleared in `onFailure`
- `LoadCoordinator` prevents stale errors from overwriting newer successful state

## Weaknesses

| Issue | Impact |
|-------|--------|
| Raw `Throwable.message` | Often null, not user-friendly, not localizable at source |
| No error taxonomy | Cannot show different UI for permission vs. not found vs. network |
| String duplication | Same message literals in multiple ViewModels |
| No retry semantics | Caller cannot distinguish transient vs. permanent failures |

## Recommended evolution

Introduce a small sealed hierarchy in `core/presentation` or per-feature if needed:

```kotlin
sealed interface ScreenError {
    data class Message(val text: String) : ScreenError
    data object NotFound : ScreenError
    data object PermissionDenied : ScreenError
    data object HealthConnectUnavailable : ScreenError
}
```

Map to user-facing copy in the UI layer:

```kotlin
@Composable
fun ScreenError.resolve(): String = when (this) {
    is ScreenError.Message -> text
    ScreenError.NotFound -> stringResource(R.string.error_not_found)
    ScreenError.PermissionDenied -> stringResource(R.string.error_permission)
    ScreenError.HealthConnectUnavailable -> stringResource(R.string.error_hc_unavailable)
}
```

Keep `UiState.error: ScreenError?` and convert to string only at the scaffold boundary, or extend `MetricDetailScaffold` to accept `ScreenError?`.

Do **not** adopt a heavy `Result<T>`/`Either` framework across every layer — map at ViewModel boundaries only.

## Validation and edge cases

Good patterns already in use:

### Blank route arguments

`SleepDetailViewModel` guards missing ID before loading:

```kotlin
if (sleepId.isBlank()) {
    _uiState.value = SleepDetailUiState(
        isLoading = false,
        error = "Missing sleep id.",
    )
    return
}
```

### Nullable repository results

```kotlin
error = if (session == null) "Sleep session not found." else null,
```

### Early returns on invalid user actions

`DashboardViewModel.deleteActivityEntry` returns when ID is blank, entry missing, or not an OpenVitals-owned entry.

Apply the same pattern to new detail screens with `SavedStateHandle` arguments.

## Null safety (Kotlin)

The codebase uses Kotlin null safety consistently:

- `?.` and `?:` for optional chains
- `.orEmpty()` for list defaults
- `coerceAtMost(LocalDate.now())` in period selection
- Avoidance of `!!` in reviewed production paths

### Nullable dependencies in ViewModels

Some ViewModels accept nullable collaborators for test or partial wiring:

```kotlin
private val heartRepository: HeartRepository? = null
// ...
crossDailyHrv = heartRepository?.loadDailyHRV(...).orEmpty()
```

Prefer non-null constructor parameters in production Hilt paths; use overloads or fakes in tests instead of nullable production dependencies when possible.

## Logging

Repositories use `android.util.Log` with tags (e.g. `SleepRepository`). For production:

- Log unexpected failures in repositories and `HealthConnectManager`
- Do not log PII (health values, identifiers) at info level
- ViewModels generally should not log — propagate structured errors upward

## Checklist for new features

- [ ] Clear `error` at start of load
- [ ] Set `isLoading = false` in both success and failure paths
- [ ] Guard blank nav args before repository calls
- [ ] Use `if (!isCurrent) return@load` inside `LoadCoordinator` blocks
- [ ] Prefer user-facing messages over raw exception text for known cases
- [ ] Consider mapping Health Connect / SecurityException to `PermissionDenied`
