# :settings

Stores the settings of every feature module and of the app, provides them to each module through its settings source (AGENTS.md §4.4), and shows the settings screen.

`:settings` is **app-specific**: its package is `dev.giste.roadbooknavigator.settings`. It may depend on `:core`, `:geo` and the feature modules, and no feature module depends on it (AGENTS.md §4).

## Responsibility

- Persist the settings of `:location`, `:odometer` and `:roadbook`, one DataStore file per module (AGENTS.md §7).
- Implement `LocationSettingsSource`, `OdometerSettingsSource` and `RoadbookSettingsSource`, and bind them with Hilt, so each module gets its stored settings as a flow and reacts to every change.
- Store the app's own settings (`AppSettings`: the theme mode) and provide them as `AppSettingsSource`.
- Show the settings screen (`SettingsScreen`), where the rider changes the theme and the roadbook settings.
- Never fail: a missing, invalid or unreadable value is the module's default.

It does **not** store runtime state, such as the odometer distances or the loaded roadbook: each module persists its own (AGENTS.md §7). It does **not** navigate: the app decides how the screen is reached and left.

## Package layout

```
dev/giste/roadbooknavigator/settings/
├── AppSettings, ThemeMode, AppSettingsSource, SettingsScreen    ← public API
├── domain/         SettingsRepository, SettingsUpdateResult, ShortDistanceStep
├── data/           DataStoreSettingsRepository, SettingsCodec (+ reading helpers),
│                   DataStoreAppSettingsRepository + AppSettingsCodec,
│                   DataStoreLocationSettingsRepository + LocationSettingsCodec,
│                   DataStoreOdometerSettingsRepository + OdometerSettingsCodec,
│                   DataStoreRoadbookSettingsRepository + RoadbookSettingsCodec,
│                   SettingsQualifiers
├── ui/             SettingsViewModel, SettingsUiState, SettingsContent (+ previews)
└── di/             SettingsModule
```

## Public API

Package `dev.giste.roadbooknavigator.settings`:

| Type | Purpose |
|---|---|
| `SettingsScreen(onBack, modifier)` | The settings screen, with its ViewModel from Hilt. Every change is stored at once, so the app reacts while the screen is open. `onBack` is called by its back button; the system back gesture belongs to the caller's navigation. |
| `AppSettings` | The app's own settings: `themeMode`, default `ThemeMode.SYSTEM`. |
| `ThemeMode` | `SYSTEM`, `LIGHT` or `DARK`. |
| `AppSettingsSource` | Inject it to read the `AppSettings`: a cold flow of the stored settings, then every change, without repeats. It never throws. |

Hilt makes every binding available as soon as `:app` depends on `:settings`.

## Inputs and outputs

- **Inputs:** the feature modules' settings models (`LocationSettings`, `OdometerSettings`, `RoadbookSettings`) and their defaults.
- **Outputs:** Hilt bindings in `SingletonComponent` of `LocationSettingsSource`, `OdometerSettingsSource`, `RoadbookSettingsSource` and `AppSettingsSource`, and the `SettingsScreen`. `:app` depends on `:settings` and must not bind those sources itself, or Hilt reports duplicate bindings.

## Settings

It stores every value of each module's settings model, and the `AppSettings`. Their meaning, defaults and validation are documented in each module's README.

The screen shows the settings the rider is expected to change; the others keep their stored values (the defaults unless a later phase exposes them):

| On screen | Setting | Control |
|---|---|---|
| Appearance › Theme | `AppSettings.themeMode` | System / Light / Dark |
| Roadbook › Roadbook colors | `RoadbookSettings.style` | App theme / FIA |
| Roadbook › Highlight close waypoints | `RoadbookSettings.shortDistanceThreshold` | − and + in steps of 50 m, within 50 m – 1 km |

## Persisted state

Four Preferences DataStore files, each owned by its repository:

| File | Key | Type | Value |
|---|---|---|---|
| `settings_app` | `theme_mode` | `String` | `AppSettings.themeMode`, by enum name |
| `settings_location` | `update_interval_millis` | `Long` | `LocationSettings.updateInterval` in milliseconds |
| | `min_update_distance_meters` | `Double` | `LocationSettings.minUpdateDistance` |
| | `moving_speed_mps` | `Double` | `LocationSettings.movingSpeed` in m/s |
| | `stopped_speed_mps` | `Double` | `LocationSettings.stoppedSpeed` in m/s |
| `settings_odometer` | `min_speed_mps` | `Double` | `OdometerSettings.minSpeed` in m/s |
| | `max_horizontal_accuracy_meters` | `Double` | `OdometerSettings.maxHorizontalAccuracy` |
| | `min_step_distance_meters` | `Double` | `OdometerSettings.minStepDistance` |
| | `use_altitude` | `Boolean` | `OdometerSettings.useAltitude` |
| | `max_vertical_accuracy_meters` | `Double` | `OdometerSettings.maxVerticalAccuracy` |
| `settings_roadbook` | `short_distance_threshold_meters` | `Double` | `RoadbookSettings.shortDistanceThreshold` |
| | `style` | `String` | `RoadbookSettings.style`, by enum name |

A key is stored only while its value differs from the default.

## Design decisions

- **One generic repository, one codec per module.** `DataStoreSettingsRepository<T>` holds the storage logic once (reading, error handling, updates). Each module only adds a `SettingsCodec` that maps its model to keys, and a small subclass that implements the module's settings source.
- **Defaults are not stored.** A codec writes a key only when its value differs from the default, and removes it otherwise. A setting the user never changed, or changed back, therefore follows the module's default, even if a later version changes it.
- **Invalid values fall back per field.** A negative or non-finite distance or speed, a non-positive interval or an unknown enum name is replaced with that field's default, with a warning; the other fields keep their stored values. Two fields that only make sense together are checked as a pair: a moving speed below the stopped speed resets both speeds. A short-distance threshold outside its range is brought into it with `ShortDistanceThreshold.coerce`, as that type asks for values read from storage.
- **Failures never escape.** A read failure emits the defaults, and a write failure keeps the settings as they were and returns `SettingsUpdateResult.NOT_SAVED`; both are logged. Only `IOException` is caught, so cancellation and programming errors propagate. A corrupted file is replaced with an empty one.
- **The screen shows what is stored.** Every change is stored at once and comes back through the repositories' flows, so the screen never shows a value that was not saved. A failed save shows a snackbar (state `saveFailed`, cleared once shown). The threshold buttons change the stored value (`update { stepped(±1) }`), so fast presses are never lost.
- **Steppers and segmented buttons, not sliders.** Every control is at least `minTouchTarget` high, and none needs a precise drag, so the screen works with gloves (AGENTS.md §5.3). A slider would be hard to set to an exact value on a moving bike.
- **The screen does not navigate.** It takes an `onBack` callback; `:app` owns the routes and the back stack.
- **Cold flows.** The repositories read DataStore's own flow, which already shares one copy of each file in memory, emits the current value first and then every change. `distinctUntilChanged` drops writes that change nothing.

## Testing notes

- The codecs are pure: `LocationSettingsCodecTest`, `OdometerSettingsCodecTest` and `RoadbookSettingsCodecTest` round-trip settings, check that defaults store nothing, and cover every invalid value.
- `SettingsViewModelTest` uses `FakeSettingsRepository` (in memory, with failing updates on demand) and `MainDispatcherRule` for every state and action. `ShortDistanceStepTest` covers the steps and their limits. `SettingsContentTest` runs on Robolectric (SDK 35, set in `src/test/resources/robolectric.properties`): every setting, selected choices, the stepper limits, back, the save error, control sizes and the Spanish texts. It finds controls by their text plus a click action, and the icon-only back button by `SettingsTestTags.BACK`.
- `DataStoreSettingsRepositoryTest` uses `FakePreferencesDataStore`, which can fail reads and writes on demand, for the shared behavior (through the roadbook repository) and checks that the location and odometer repositories read their own keys.
- `DataStoreSettingsRepositoryIntegrationTest` uses a real DataStore file in a `TemporaryFolder`, and closes and reopens it to check that the settings survive. DataStore allows one open instance per file, so each instance runs in its own `Job`.
- `:core` does not share its test doubles, so `FakeLogger` lives in this module's test source set.
