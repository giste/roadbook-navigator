# :settings

Stores the settings of every feature module and of the app, provides them to each module through its settings source (AGENTS.md §4.4), and shows the settings screen.

`:settings` is **app-specific**: its package is `dev.giste.roadbooknavigator.settings`. It may depend on `:core`, `:geo` and the feature modules, and no feature module depends on it (AGENTS.md §4).

## Responsibility

- Persist the settings of `:map` and `:roadbook`, each in its own DataStore file (AGENTS.md §7).
- Implement `LocationSettingsSource`, `MapSettingsSource`, `OdometerSettingsSource` and `RoadbookSettingsSource`, and bind them with Hilt, so each module gets its settings as a flow and reacts to every change. The `:location` and `:odometer` settings are fixed to their defaults (see Design decisions).
- Store the app's own settings (`AppSettings`: the theme mode and the screen orientation) and provide them as `AppSettingsSource`.
- Store the remote control settings (`RemoteSettings`: the remote model and the custom keys) and provide them as `RemoteSettingsSource`. `:app` acts on the keys while the dashboard is shown.
- Show the settings screen (`SettingsScreen`), where the rider changes the theme, the screen orientation, the remote control, the map view (zoom and tilt) and the roadbook settings.
- Never fail: a missing, invalid or unreadable value is the module's default.

It does **not** store runtime state, such as the odometer distances or the loaded roadbook: each module persists its own (AGENTS.md §7). It does **not** navigate: the app decides how the screen is reached and left.

## Package layout

```
dev/giste/roadbooknavigator/settings/
├── AppSettings, ThemeMode, ScreenOrientation, AppSettingsSource,
│   RemoteSettings, RemoteModel, RemoteKeys, RemoteKey, RemoteAction,
│   RemoteSettingsSource, SettingsScreen  ← public API
├── domain/         SettingsRepository, SettingsUpdateResult, ShortDistanceStep, MapViewStep,
│                   CustomKeyAssignment (RemoteKeys.withOnlyKey)
├── data/           DataStoreSettingsRepository, SettingsCodec (+ reading helpers),
│                   DataStoreAppSettingsRepository + AppSettingsCodec,
│                   DataStoreRemoteSettingsRepository + RemoteSettingsCodec,
│                   DataStoreMapSettingsRepository + MapSettingsCodec,
│                   DataStoreRoadbookSettingsRepository + RoadbookSettingsCodec,
│                   FixedLocationSettingsSource, FixedOdometerSettingsSource,
│                   SettingsQualifiers
├── ui/             SettingsViewModel, SettingsUiState (+ KeyMove), SettingsContent,
│                   KeyCaptureDialog (+ isCapturableKey), each with its previews
└── di/             SettingsModule
```

## Public API

Package `dev.giste.roadbooknavigator.settings`:

| Type | Purpose |
|---|---|
| `SettingsScreen(onBack, modifier)` | The settings screen, with its ViewModel from Hilt. Every change is stored at once, so the app reacts while the screen is open. `onBack` is called by its back button; the system back gesture belongs to the caller's navigation. |
| `AppSettings` | The app's own settings: `themeMode`, default `ThemeMode.SYSTEM`, and `orientation`, default `ScreenOrientation.FOLLOW_SYSTEM`. |
| `ThemeMode` | `SYSTEM`, `LIGHT` or `DARK`. |
| `ScreenOrientation` | `FOLLOW_SYSTEM`, `VERTICAL` or `HORIZONTAL`. The app decides how to apply it. |
| `RemoteSettings` | The remote control settings: `model`, default `RemoteModel.TERRA_PIRATA`, and `customKeys`, default `RemoteKeys.TERRA_PIRATA`. `keys` gives the keys in use: the model's fixed keys, or `customKeys` for `CUSTOM`. |
| `RemoteModel` | `DND2`, `TERRA_PIRATA` or `CUSTOM`. |
| `RemoteAction` | What a key does: `NEXT_WAYPOINT`, `PREVIOUS_WAYPOINT`, `INCREASE_PARTIAL`, `DECREASE_PARTIAL`, `RESET_PARTIAL`. |
| `RemoteKey` | An Android key code (`KeyEvent.KEYCODE_*`), always positive. |
| `RemoteKeys` | The keys of each action (none, one or several), with `keysFor(action)` and `actionFor(key)`. A key triggers at most one action. `RemoteKeys.DND2` and `RemoteKeys.TERRA_PIRATA` are the fixed keys of those remotes. |
| `RemoteSettingsSource` | Inject it to read the `RemoteSettings`: a cold flow of the stored settings, then every change, without repeats. It never throws. |
| `AppSettingsSource` | Inject it to read the `AppSettings`: a cold flow of the stored settings, then every change, without repeats. It never throws. |

Hilt makes every binding available as soon as `:app` depends on `:settings`.

## Inputs and outputs

- **Inputs:** the feature modules' settings models (`LocationSettings`, `MapSettings`, `OdometerSettings`, `RoadbookSettings`) and their defaults.
- **Outputs:** Hilt bindings in `SingletonComponent` of `LocationSettingsSource`, `MapSettingsSource`, `OdometerSettingsSource`, `RoadbookSettingsSource`, `AppSettingsSource` and `RemoteSettingsSource`, and the `SettingsScreen`. `:app` depends on `:settings` and must not bind those sources itself, or Hilt reports duplicate bindings.

## Settings

It stores every value of the `:map` and `:roadbook` settings, the `AppSettings` and the `RemoteSettings`. The `:location` and `:odometer` settings are always their modules' defaults, and are not stored. Their meaning, defaults and validation are documented in each module's README.

The screen shows every stored setting:

| On screen | Setting | Control |
|---|---|---|
| Appearance › Theme | `AppSettings.themeMode` | System / Light / Dark |
| Appearance › Screen orientation | `AppSettings.orientation` | System / Vertical / Horizontal |
| Remote control › Remote | `RemoteSettings.model` | DND2 / Terra Pirata / Custom |
| Remote control › Keys | `RemoteSettings.keys` | Each action with its keys on a button: disabled (grayed) for DND2 and Terra Pirata; for Custom, it opens `KeyCaptureDialog` |
| Map › Map zoom | `MapSettings.zoomLevel` | − and + in steps of 1, within 15 – 20 |
| Map › Map tilt | `MapSettings.tilt` | − and + in steps of 5°, within 0° – 60° |
| Roadbook › Roadbook colors | `RoadbookSettings.style` | App theme / FIA |
| Roadbook › Highlight close waypoints | `RoadbookSettings.shortDistanceThreshold` | − and + in steps of 50 m, within 50 m – 1 km |

## Persisted state

Four Preferences DataStore files, each owned by its repository:

| File | Key | Type | Value |
|---|---|---|---|
| `settings_app` | `theme_mode` | `String` | `AppSettings.themeMode`, by enum name |
| | `orientation` | `String` | `AppSettings.orientation`, by enum name |
| `settings_map` | `zoom_level` | `Int` | `MapSettings.zoomLevel` |
| | `tilt_degrees` | `Double` | `MapSettings.tilt`, in degrees |
| `settings_remote` | `model` | `String` | `RemoteSettings.model`, by enum name |
| | `custom_next_waypoint`, `custom_previous_waypoint`, `custom_increase_partial`, `custom_decrease_partial`, `custom_reset_partial` | `String` | The custom key codes of each action, comma-separated and sorted, e.g. `85,126`; empty for an action without keys |
| `settings_roadbook` | `short_distance_threshold_meters` | `Double` | `RoadbookSettings.shortDistanceThreshold` |
| | `style` | `String` | `RoadbookSettings.style`, by enum name |

A key is stored only while its value differs from the default.

## Design decisions

- **One generic repository, one codec per module.** `DataStoreSettingsRepository<T>` holds the storage logic once (reading, error handling, updates). Each module only adds a `SettingsCodec` that maps its model to keys, and a small subclass that implements the module's settings source.
- **Defaults are not stored.** A codec writes a key only when its value differs from the default, and removes it otherwise. A setting the user never changed, or changed back, therefore follows the module's default, even if a later version changes it.
- **Invalid values fall back per field.** A negative or non-finite distance or an unknown enum name is replaced with that field's default, with a warning; the other fields keep their stored values. A short-distance threshold outside its range is brought into it with `ShortDistanceThreshold.coerce`, as that type asks for values read from storage.
- **Failures never escape.** A read failure emits the defaults, and a write failure keeps the settings as they were and returns `SettingsUpdateResult.NOT_SAVED`; both are logged. Only `IOException` is caught, so cancellation and programming errors propagate. A corrupted file is replaced with an empty one.
- **The screen shows what is stored.** Every change is stored at once and comes back through the repositories' flows, so the screen never shows a value that was not saved. A failed save shows a snackbar (state `saveFailed`, cleared once shown). The threshold buttons change the stored value (`update { stepped(±1) }`), so fast presses are never lost.
- **Segmented buttons without a check mark.** The selected button's color already marks it, so the label gets the whole button and long names ("Terra Pirata", "Personalizado") fit on a phone with the larger text.
- **Steppers and segmented buttons, not sliders.** Every control is at least `minTouchTarget` high, and none needs a precise drag, so the screen works with gloves (AGENTS.md §5.3). A slider would be hard to set to an exact value on a moving bike.
- **The `:location` and `:odometer` settings are fixed.** The app is for motor vehicles, and those modules' defaults are meant for them, so the rider never changes these settings and nothing is stored: `FixedLocationSettingsSource` and `FixedOdometerSettingsSource` always give the defaults, as a `StateFlow` that never completes. Their thresholds would mean little to a rider, and a wrong value would quietly spoil the odometer. If they are ever offered, they become stored settings like the others. Files left by earlier builds (`settings_location`, `settings_odometer`) are ignored.
- **The map view offers narrower ranges than `:map` allows.** The screen offers zoom levels 15 to 20 and tilts of 0° to 60° in 5° steps (`MapViewStep`). Below 15 the rider cannot see the next turn, and these steps are what a rider tells apart at a glance. The ranges are this app's rule, so they live here: `:map` keeps the wider limits of its renderer for other apps. A stored value outside the offered ranges, which only an older build or a damaged file can leave, is brought into them when it is read, so the screen and the map always agree. The defaults stay `MapSettings`' (zoom level 18, tilt 60°), the values the previous app used. Buttons to restore a default are planned for later.
- **The screen does not navigate.** It takes an `onBack` callback; `:app` owns the routes and the back stack.
- **The remote keys are their own settings group.** They are not part of `AppSettings`: only the dashboard needs them, and only `MainActivity` needs the theme and orientation, so each reads only what it uses, and each file falls back to its defaults on its own. One file per settings model, as for the modules.
- **Fixed remotes store only their name.** The keys of DND2 and Terra Pirata live in code (`RemoteKeys.DND2`, `RemoteKeys.TERRA_PIRATA`), so stored data can never disagree with them. Only the custom keys are stored, and they stay while another remote is selected, so they come back with Custom. Terra Pirata is the default remote, and the custom keys start as its keys.
- **A key triggers one action.** `RemoteKeys` rejects a key bound twice. Stored custom keys that do so, which only a damaged file can produce, fall back to the defaults as a whole, because there is no telling which binding was meant; an unreadable action falls back alone.
- **Custom keys are captured, not picked from a list.** Tapping an action's keys opens `KeyCaptureDialog`, and the first key the rider presses on the remote becomes that action's only key (`RemoteKeys.withOnlyKey`), so the rider never needs to know a key's name. Back and Escape cancel (so they cannot be assigned), as do the cancel button and a tap outside; modifier keys are ignored. The dialog consumes every other key, so a volume or media key changes nothing while it is captured.
- **A pressed key that another action has moves.** A key triggers one action, so assigning it takes it from the action that had it, which is left with no key, and a snackbar says so (state `keyMoved`, cleared once shown). The rider can give that action another key. The move is decided on the stored keys, inside the update, so it is always right even with fast taps. Assigning does nothing unless Custom is selected.
- **The capture dialog takes the focus itself.** It is opened with a tap, which leaves the app in touch mode, where a view that is not focusable in touch mode cannot take the focus, and without the focus the dialog gets no keys. So the dialog makes its window's view focusable in touch mode and requests the focus. Holding the focus also keeps Android from spending the first D-pad press on leaving touch mode.
- **Cold flows.** The repositories read DataStore's own flow, which already shares one copy of each file in memory, emits the current value first and then every change. `distinctUntilChanged` drops writes that change nothing.

## Testing notes

- The codecs are pure: `MapSettingsCodecTest`, `RoadbookSettingsCodecTest`, `AppSettingsCodecTest` and `RemoteSettingsCodecTest` round-trip settings, check that defaults store nothing, and cover every invalid value.
- `SettingsViewModelTest` uses `FakeSettingsRepository` (in memory, with failing updates on demand) and `MainDispatcherRule` for every state and action. `ShortDistanceStepTest` and `MapViewStepTest` cover the steps and their limits (and, for the map, bringing stored values into the offered ranges), and `CustomKeyAssignmentTest` the key assignment (new key, moved key, own key, several keys). `RemoteKeysTest` and `RemoteSettingsTest` cover the fixed keys, the lookups, a key bound twice, equality, and the keys in use for each model. `SettingsContentTest` runs on Robolectric (SDK 35, set in `src/test/resources/robolectric.properties`): every setting, selected choices (the theme's and the orientation's "System" share their label, so they are told apart by their order), the stepper limits, back, the save error, control sizes and the Spanish texts, plus the custom keys: buttons disabled for DND2 and Terra Pirata and enabled for Custom, opening the dialog, a key pressed in it after the tap that opened it (the touch-mode case), cancel, the open dialog surviving recreation, and the moved-key snackbar. `KeyCaptureDialogTest` covers the dialog on its own (texts, D-pad, volume, modifier and back keys, cancel) and `isCapturableKey`. Keys are sent with `performKeyInput`. It finds controls by their text plus a click action (scrolling to them first, since the screen is longer than the test window), the icon-only back button by `SettingsTestTags.BACK`, the steppers' buttons and values inside their stepper (`SettingsTestTags.THRESHOLD_STEPPER`, `MAP_ZOOM_STEPPER`, `MAP_TILT_STEPPER`), and a key name inside its action's row (`SettingsTestTags.keyBinding`). Under Robolectric `KeyEvent.keyCodeToString` gives the bare number, so the fallback key name is compared with what it returns.
- `DataStoreSettingsRepositoryTest` uses `FakePreferencesDataStore`, which can fail reads and writes on demand, for the shared behavior (through the roadbook repository) and checks that the remote and map repositories read their own keys. `FixedSettingsSourcesTest` checks that the fixed sources give the defaults and never complete.
- `DataStoreSettingsRepositoryIntegrationTest` uses a real DataStore file in a `TemporaryFolder`, through the roadbook repository, and closes and reopens it to check that the settings survive. DataStore allows one open instance per file, so each instance runs in its own `Job`.
- `:core` does not share its test doubles, so `FakeLogger` lives in this module's test source set.
