# :core

Shared foundation for every other module. `:core` depends on no other project module.

`:core` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.core`, and it contains nothing specific to Roadbook Navigator, so other apps can depend on it too.

## Responsibility

- **Logger:** the only logging entry point. Other modules never call `android.util.Log` directly.
- **Theme:** Material 3 theme, color, typography and dimension tokens.
- **Common UI components:** generic composables reused by several features.
- **Permissions management:** `PermissionGate`, which shows its content only while the required runtime permissions are granted. It is permission agnostic: callers describe each permission with a `RequiredPermission`.
- **Coroutine dispatchers:** `@IoDispatcher`, `@DefaultDispatcher`, … qualifiers and the application-level `CoroutineScope`.

Geographic and measurement value objects (`Distance`, `GeoPoint`, `Bearing`, …) are not part of `:core`. They live in the shared kernel, `:geo`.

## Package layout

`:core` holds unrelated capabilities, so each one has its own sub-package that follows the module rules of AGENTS.md: its root holds only its public API, and its layer packages hold `internal` code. A capability creates only the layer packages it needs. See the `:core` exception in AGENTS.md §5.1.

```
dev/giste/core/
├── logging/        Logger, DebugLogging   ← public API
│   ├── data/       TimberLogger, AutomaticTagTree
│   └── di/         LoggerModule
├── coroutines/     IoDispatcher, DefaultDispatcher, ApplicationScope   ← public API
│   └── di/         CoroutinesModule
├── permission/     RequiredPermission, PermissionGate   ← public API
│   ├── domain/     PermissionStatus, PermissionChecker
│   ├── data/       ActivityPermissionChecker, app settings
│   └── ui/         PermissionGateState, PermissionScreen
└── ui/
    ├── theme/      GisteTheme, Dimensions   ← public API
    │   └── ui/     BeerColorScheme, LocalDimensions, palette preview
    └── component/  AutoSizeText   ← public API
```

Capabilities use each other only through their public API. Kotlin's `internal` is module-wide, so this is enforced by review, not by the compiler.

## Public API

Package `dev.giste.core.logging`:

| Type | Purpose |
|---|---|
| `Logger` | The only logging entry point. Inject it (Hilt binds it in `SingletonComponent`). Messages are `String.format` patterns. `withTag(tag)` returns a logger with a fixed tag. |
| `DebugLogging` | `DebugLogging.install()` turns on Logcat output. `:app` calls it from `Application.onCreate`, only in debuggable builds. Idempotent. |

Package `dev.giste.core.coroutines`:

| Type | Purpose |
|---|---|
| `@IoDispatcher` | Qualifier for the `CoroutineDispatcher` used for blocking I/O (`Dispatchers.IO` in production). |
| `@DefaultDispatcher` | Qualifier for the `CoroutineDispatcher` used for CPU-bound work (`Dispatchers.Default` in production). |
| `@ApplicationScope` | Qualifier for the app-wide `CoroutineScope`: a `@Singleton`, never cancelled, `SupervisorJob` + `@DefaultDispatcher`. Use it instead of `GlobalScope`. |

Package `dev.giste.core.ui.theme`:

| Type | Purpose |
|---|---|
| `GisteTheme(darkTheme, typography) { … }` | Material 3 theme with the beer palette. Wrap every screen and `@Preview` in it. `darkTheme` defaults to the system setting, and `typography` to the Material 3 one. |
| `GisteTheme.dimensions` | The current `Dimensions`. Read colors and typography through `MaterialTheme`. |
| `Dimensions` | Spacing tokens (`spacingExtraSmall` 4 dp … `spacingExtraLarge` 32 dp) and `minTouchTarget` (56 dp). Only the theme creates it. |

Package `dev.giste.core.ui.component`:

| Type | Purpose |
|---|---|
| `AutoSizeText(text, color, modifier, style, fontWeight, textAlign, widestText)` | One line of text that grows or shrinks (8 sp to 100 sp) to fill the box its `modifier` gives it, with tabular digits. For values that must be read at a glance, such as distances. Too long text is clipped, never wrapped. `textAlign` defaults to centered. With `widestText` (e.g. `999.99`), the size is computed for that text instead, so it stays the same for every value that is not wider. Like `Text`, it draws its line at the top of a taller box; `Modifier.fillMaxSize().wrapContentHeight()` centers it vertically, with the font size still fitted to the whole box. |

Package `dev.giste.core.permission`:

| Type | Purpose |
|---|---|
| `RequiredPermission` | A runtime permission described by the module that needs it: the manifest permissions it `required` (all must be granted), the ones `requested` together in one dialog (defaults to `required`), and the string resources of its title, first-time message, rationale, permanently-denied message and allow button. Throws `IllegalArgumentException` if `required` is empty or not contained in `requested`. |
| `PermissionGate(permissions, modifier) { … }` | Shows its content only while every `RequiredPermission` in the list is granted. Otherwise shows a full-screen explanation with the action that can grant the first missing one, in list order. Must be hosted in an `Activity`. |

`:core` exposes `kotlinx-coroutines-core` as an `api` dependency, because these types appear in its public API.

## Inputs and outputs

- **Outputs:** log lines written to Logcat through Timber, only after `DebugLogging.install()`.
- **Manifest:** `:core` declares no permission. The module that defines a `RequiredPermission` declares its manifest permissions.
- **Inputs:** none.

## Settings

None. `:core` has no settings.

## Persisted state

None.

## Resources

- `resourcePrefix` is `core_`.
- Only generic strings live here (e.g. `core_action_retry`), in `values` and `values-es`, including the generic buttons of the permission screen (`core_permission_action_*`). The texts of each permission belong to the module that defines it.

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

### Theme

- **Beer palette.** Built from four seeds: beer gold (primary, nudged toward amber), soft straw (secondary), roasted malt (tertiary) and warm foam/stout neutrals. Each seed gives a tonal palette, and every role uses its Material 3 baseline tone (e.g. primary is tone 80 in dark). The light `primary` is the one exception: amber copper (`#B25B00`) instead of the gold's tone 40, which looks khaki-brown. At tone 48 it is about the lightest color that still reaches 4.5:1 with white text and as text on the foam surface, with a little margin on contrast and on the distance from the error red (CIEDE2000 18.7, the test needs 15). The dark `inversePrimary` sits on the light inverse surface, so it uses the same hue darker (`#9E5000`). The secondary is a low-chroma straw, so tonal buttons, keypads and selections stay calm next to the primary; a hops green clashed with the beer colors, and a saturated golden lager turned khaki in its dark tones. The light scheme shows the bright gold as `primaryContainer`, because white text on bright gold is unreadable.
- **Every role is set.** Including `background`, the surface containers, the `inverse*` roles and the fixed roles (`primaryFixed` and the like, the same in both schemes), so no Material default purple leaks into `Scaffold`, cards, dialogs or bars.
- **Error stays Material red.** Warnings must never blend with the palette. Malt shares the reddish hue of the error colors, so its hue is shifted toward coffee brown to keep it clearly distinct.
- **No dynamic color.** The palette is the apps' identity, and fixed colors keep content equally readable on every device.
- **Accessible contrast.** Every content color reaches WCAG AA (4.5:1) on its background in both schemes, including primary drawn directly on surfaces (text buttons, links, icons).
- **Palette preview.** `ThemePreviews.kt` shows the whole palette in light and dark: every role with its hex value and the contrast of the color drawn on it, marked ✓ or ✗ against its minimum (4.5:1 for text, 3:1 for outlines), plus the buttons in their default colors. Open it in Android Studio to review a color change before running `BeerColorSchemeTest`.
- **Typography is the app's choice.** `GisteTheme` uses the Material 3 defaults with the system font unless the app passes its own `typography`. Text sizes depend on how each app is used (Roadbook Navigator scales them up for a tablet on the handlebars), so `:core` stays neutral and offers the parameter. Feature-specific styles (e.g. large odometer digits) belong to their feature until several features share them.
- **Glove-friendly touch targets.** `minTouchTarget` is 56 dp, above the Material 48 dp minimum, because the app is used while driving.
- **Layout.** The color schemes and `LocalDimensions` are `internal`, so they live in the `ui/theme/ui/` layer package. The root package only holds the public theme API.

### Common components

- **Shared only when reused.** A composable moves here when a second module needs it. `AutoSizeText` came from `:roadbook`, which uses it for waypoint distances, because the app's odometer needs the same behavior.
- **`AutoSizeText` clears the line height** of the given style. A fixed line height, as the Material styles have (44 sp for `displaySmall`), is taller than small boxes at any font size, so auto-size would always fall back to the smallest one.
- **Tabular digits.** Every digit has the same width, so a changing number keeps its width and does not jump.
- **Optional fixed size.** By default the size follows the text, so a number shrinks when it gains a digit. `widestText` sizes it for its widest value instead, so the digits keep their size as the value changes. Compose's `TextAutoSize` cannot do this, because it always measures the text it draws, so `AutoSizeText` measures `widestText` itself with a `TextMeasurer` (a binary search over the same 8 sp to 100 sp range and 0.25 sp steps) and draws the text at that fixed size. That text is only as tall as its line, as `Text` is, so callers align it with the usual modifiers (`wrapContentHeight()`).
- **Fixed limits.** 8 sp is the smallest size that stays legible, and 100 sp is enough for the largest boxes on a tablet. They are not parameters until a caller needs other values.

### Permissions

- **Permission agnostic.** `:core` knows no permission in particular. Each `RequiredPermission` carries its manifest permissions and its texts, and the module that needs it defines it and declares the manifest permissions. Precise location, for example, is defined in `:app` (until `:location` exists) as `required = {FINE}`, `requested = {FINE, COARSE}`, so approximate location alone keeps the gate closed.
- **Required versus requested.** A permission is granted when every `required` manifest permission is. `requested` can add ones Android needs in the same dialog. The rationale flag is only read for the missing `required` ones, and a request result for any `requested` one counts as having requested the permission, because they are always requested together.
- **No ViewModel.** `shouldShowRequestPermissionRationale` needs the current `Activity`, so a ViewModel could not compute the status on its own. `PermissionGateState` is a plain Compose state holder, and the decision logic is a pure function (`PermissionStatus.resolve`) tested on the JVM.
- **Detecting "permanently denied".** Android has no flag for it. After one denial the rationale flag turns on; after a second it turns off and the dialog is no longer shown. So "not granted, no rationale" means *never asked* before any request in this state and *permanently denied* after one. The requested permissions are saved with `rememberSaveable`, so this survives rotation and process death. A dismissed dialog looks the same, so the permanently-denied screen offers "Try again" next to "Open settings".
- **Re-checked on resume.** The gate refreshes on every `ON_RESUME`, which picks up permissions granted in the settings or revoked while the app was in the background.
- **One permission at a time.** The gate explains and requests the first missing permission in list order, so each screen can explain one permission clearly.
- **Texts come from the caller.** `PermissionGateContent` resolves the `RequiredPermission` string resources, picking the message for the current status, and `PermissionScreen` only receives resolved text. Adding a permission needs no change in `:core`.

## Testing notes

- **Logger:** tests plant a `RecordingTree` (in `src/test`) to capture what reaches Timber, and call `Timber.uprootAll()` in `@After` because Timber's forest is global.
- **AutomaticTagTree:** `callerTag` is tested with synthetic stack traces. The `stackTrace` constructor parameter lets tests drive `createStackElementTag` without depending on the real JUnit stack.
- **Coroutines:** `CoroutinesModuleTest` calls the `provide*` functions directly instead of going through Hilt. It builds the application scope on a `StandardTestDispatcher` and checks that a failing child leaves the scope and its siblings running.
- **Theme:** `BeerColorSchemeTest` (JVM) checks scheme selection, the WCAG AA contrast of every content/background pair, a minimum CIELAB distance between each brand color and its error counterpart, and that no role is left at its Material default, in both schemes. A palette change that hurts readability or reintroduces a lookalike or a leaking default fails the build. `GisteThemeTest` (`androidTest`) checks that the composable provides the right scheme and the dimensions. It needs a device or emulator.
- **Common components:** `AutoSizeTextTest` runs on the JVM with Robolectric and `@GraphicsMode(NATIVE)`. The default graphics mode returns the same text size for any font size, so auto-size could not be observed. Auto-size does not report the font size it picks, so the tests compare the line height of the shown text with a `TextMeasurer` reference at 8 sp and 100 sp. Robolectric runs on SDK 35 (`src/test/resources/robolectric.properties`), since it does not support compileSdk 37 yet.
- **Permissions:** `PermissionStatusTest`, `RequiredPermissionTest`, `PendingPermissionTest` and `PermissionGateStateTest` (JVM) cover the resolver, the `RequiredPermission` invariants, the message per status and every state transition, using `FakePermissionChecker` and the sample permissions in `TestPermissions` (in `src/test`). `PermissionScreenTest` and `PermissionGateContentTest` (`androidTest`) cover every screen state, every button, the text resolution and the content switch. The instrumented tests use test-only strings (`src/androidTest/res`) and declare the location permissions in `src/androidTest/AndroidManifest.xml`, since `:core` itself declares none. `PermissionGateTest` and `ActivityPermissionCheckerTest` (`androidTest`) run the real gate and checker with location granted through `UiAutomation`. Revoking a runtime permission kills the app process and would stop the instrumentation, so the denied paths are covered by the stateless and JVM tests. Note that AGP installs connected-test APKs with every runtime permission granted.
