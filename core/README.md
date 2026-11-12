# :core

Shared foundation for every other module. `:core` depends on no other project module.

## Responsibility

- **Logger:** the only logging entry point. Other modules never call `android.util.Log` directly.
- **Theme:** Material 3 theme, colour, typography and dimension tokens.
- **Common UI components:** generic composables reused by several features.
- **Permissions management:** shared helpers for requesting and observing runtime permissions.
- **Coroutine dispatchers:** `@IoDispatcher`, `@DefaultDispatcher`, … qualifiers and the application-level `CoroutineScope`.
- **Shared kernel:** minimal, framework-free value objects that several features need (e.g. `Distance`, `GeoPoint`, `Bearing`). Keep it small. Feature-specific concepts stay in their feature.

## Package layout

`:core` holds unrelated capabilities, so each one has its own sub-package that follows the module rules of AGENTS.md: its root holds only its public API, and its layer packages hold `internal` code. A capability creates only the layer packages it needs. See the `:core` exception in AGENTS.md §5.1.

```
dev/giste/roadbooknavigator/core/
├── logging/        Logger, DebugLogging   ← public API
│   ├── data/       TimberLogger, AutomaticTagTree
│   └── di/         LoggerModule
├── coroutines/     IoDispatcher, DefaultDispatcher, ApplicationScope   ← public API
│   └── di/         CoroutinesModule
├── permission/     (planned) permission gate and states
├── ui/
│   ├── theme/      (planned) theme, colours, typography, dimensions
│   └── component/  (planned) common composables
└── model/          (planned) shared kernel: framework-free value objects
```

Capabilities use each other only through their public API. Kotlin's `internal` is module-wide, so this is enforced by review, not by the compiler.

## Public API

Package `dev.giste.roadbooknavigator.core.logging`:

| Type | Purpose |
|---|---|
| `Logger` | The only logging entry point. Inject it (Hilt binds it in `SingletonComponent`). Messages are `String.format` patterns. `withTag(tag)` returns a logger with a fixed tag. |
| `DebugLogging` | `DebugLogging.install()` turns on Logcat output. `:app` calls it from `Application.onCreate`, only in debuggable builds. Idempotent. |

Package `dev.giste.roadbooknavigator.core.coroutines`:

| Type | Purpose |
|---|---|
| `@IoDispatcher` | Qualifier for the `CoroutineDispatcher` used for blocking I/O (`Dispatchers.IO` in production). |
| `@DefaultDispatcher` | Qualifier for the `CoroutineDispatcher` used for CPU-bound work (`Dispatchers.Default` in production). |
| `@ApplicationScope` | Qualifier for the app-wide `CoroutineScope`: a `@Singleton`, never cancelled, `SupervisorJob` + `@DefaultDispatcher`. Use it instead of `GlobalScope`. |

`:core` exposes `kotlinx-coroutines-core` as an `api` dependency, because these types appear in its public API.

## Inputs and outputs

- **Outputs:** log lines written to Logcat through Timber, only after `DebugLogging.install()`.
- **Inputs:** none.

## Settings

None. `:core` has no settings.

## Persisted state

None.

## Resources

- `resourcePrefix` is `core_`.
- Only generic strings live here (e.g. `core_action_retry`), in `values` and `values-es`.

## Design decisions

### Logger

- **Timber stays internal.** `Logger` hides Timber, so no other module depends on it and domain code stays free of Android. `TimberLogger` (in `data/`) forwards every call to Timber.
- **Silent until installed.** `TimberLogger` writes nothing until a Timber tree is planted. Only `DebugLogging.install()` plants one, and `:app` calls it only in debuggable builds, so release builds log nothing and can't leak personal data such as locations.
- **Automatic tags.** `AutomaticTagTree` tags each line with the simple name of the class that called `Logger`, reduced to the outermost class (`PermissionViewModel$uiState$1` → `PermissionViewModel`). It skips frames of Timber, `TimberLogger` and itself, matching the exact class or its nested classes only, so similarly named classes (`TimberLoggerFactory`) are not skipped. It builds a stack trace per untagged line, which is acceptable in debug builds only.
- **Explicit tags per call.** Timber clears an explicit tag after each line, so the logger returned by `withTag` sets it before every call.
- **Unscoped binding.** `TimberLogger` is stateless, so `LoggerModule` binds it without `@Singleton`.

### Coroutines

- **Injected, not hardcoded.** Classes get dispatchers and the application scope through the qualifiers, so tests can pass a `StandardTestDispatcher` or a `TestScope` instead.
- **No `@MainDispatcher` yet.** ViewModels use `viewModelScope` (already on Main) and tests replace Main with a `MainDispatcherRule`. Add the qualifier when a non-ViewModel class needs Main.
- **Supervisor scope with no exception handler.** One failing child does not cancel the scope or its siblings. An uncaught exception still crashes the app on purpose: expected failures must be sealed states (AGENTS.md §6), so anything that reaches the scope is a bug.
- **Unscoped dispatchers, singleton scope.** The `Dispatchers` objects are already singletons. The scope must be one instance so the whole app shares its job.

## Testing notes

- **Logger:** tests plant a `RecordingTree` (in `src/test`) to capture what reaches Timber, and call `Timber.uprootAll()` in `@After` because Timber's forest is global.
- **AutomaticTagTree:** `callerTag` is tested with synthetic stack traces. The `stackTrace` constructor parameter lets tests drive `createStackElementTag` without depending on the real JUnit stack.
- **Coroutines:** `CoroutinesModuleTest` calls the `provide*` functions directly instead of going through Hilt. It builds the application scope on a `StandardTestDispatcher` and checks that a failing child leaves the scope and its siblings running.
