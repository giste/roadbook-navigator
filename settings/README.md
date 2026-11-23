# :settings

Stores the settings of every feature module and of the app, provides them to each module through its settings source (AGENTS.md §4.4), and shows the settings screen.

`:settings` is **app-specific**: its package is `dev.giste.roadbooknavigator.settings`. It may depend on `:core`, `:geo` and the feature modules, and no feature module depends on it (AGENTS.md §4).

## Responsibility

- Persist the settings of `:location`, `:odometer` and `:roadbook`, one DataStore file per module (AGENTS.md §7).
- Implement `LocationSettingsSource`, `OdometerSettingsSource` and `RoadbookSettingsSource`, and bind them with Hilt, so each module gets its stored settings as a flow and reacts to every change.
- Store the app's own settings (`AppSettings`: the theme mode and the screen orientation) and provide them as `AppSettingsSource`.
- Store the remote control settings (`RemoteSettings`: the remote model and the custom keys) and provide them as `RemoteSettingsSource`. `:app` acts on the keys while the dashboard is shown.
- Show the settings screen (`SettingsScreen`), where the rider changes the theme, the screen orientation, the remote control and the roadbook settings.
- Never fail: a missing, invalid or unreadable value is the module's default.

It does **not** store runtime state, such as the odometer distances or the loaded roadbook: each module persists its own (AGENTS.md §7). It does **not** navigate: the app decides how the screen is reached and left.

## Package layout

```
dev/giste/roadbooknavigator/settings/
├── AppSettings, ThemeMode, ScreenOrientation, AppSettingsSource,
│   RemoteSettings, RemoteModel, RemoteKeys, RemoteKey, RemoteAction,
│   RemoteSettingsSource, SettingsScreen  ← public API
├── domain/         SettingsRepository, SettingsUpdateResult, ShortDistanceStep,
│                   CustomKeyAssignment (RemoteKeys.withOnlyKey)
├── data/           DataStoreSettingsRepository, SettingsCodec (+ reading helpers),
│                   DataStoreAppSettingsRepository + AppSettingsCodec,
│                   DataStoreRemoteSettingsRepository + RemoteSettingsCodec,
│                   DataStoreLocationSettingsRepository + LocationSettingsCodec,
│                   DataStoreOdometerSettingsRepository + OdometerSettingsCodec,
│                   DataStoreRoadbookSettingsRepository + RoadbookSettingsCodec,
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
| `RemoteSettings` | The remote control settings: `model`, default `RemoteModel.DND2`, and `customKeys`, default `RemoteKeys.DND2`. `keys` gives the keys in use: the model's fixed keys, or `customKeys` for `CUSTOM`. |
| `RemoteModel` | `DND2`, `TERRA_PIRATA` or `CUSTOM`. |
| `RemoteAction` | What a key does: `NEXT_WAYPOINT`, `PREVIOUS_WAYPOINT`, `INCREASE_PARTIAL`, `DECREASE_PARTIAL`, `RESET_PARTIAL`. |
| `RemoteKey` | An Android key code (`KeyEvent.KEYCODE_*`), always positive. |
| `RemoteKeys` | The keys of each action (none, one or several), with `keysFor(action)` and `actionFor(key)`. A key triggers at most one action. `RemoteKeys.DND2` and `RemoteKeys.TERRA_PIRATA` are the fixed keys of those remotes. |
| `RemoteSettingsSource` | Inject it to read the `RemoteSettings`: a cold flow of the stored settings, then every change, without repeats. It never throws. |
| `AppSettingsSource` | Inject it to read the `AppSettings`: a cold flow of the stored settings, then every change, without repeats. It never throws. |

Hilt makes every binding available as soon as `:app` depends on `:settings`.

## Inputs and outputs

- **Inputs:** the feature modules' settings models (`LocationSettings`, `OdometerSettings`, `RoadbookSettings`) and their defaults.
- **Outputs:** Hilt bindings in `SingletonComponent` of `LocationSettingsSource`, `OdometerSettingsSource`, `RoadbookSettingsSource`, `AppSettingsSource` and `RemoteSettingsSource`, and the `SettingsScreen`. `:app` depends on `:settings` and must not bind those sources itself, or Hilt reports duplicate bindings.

## Settings

It stores every value of each module's settings model, and the `AppSettings`. Their meaning, defaults and validation are documented in each module's README.

The screen shows the settings the rider is expected to change; the others keep their stored values (the defaults unless a later phase exposes them):

| On screen | Setting | Control |
|---|---|---|
| Appearance › Theme | `AppSettings.themeMode` | System / Light / Dark |
| Appearance › Screen orientation | `AppSettings.orientation` | System / Vertical / Horizontal |
| Remote control › Remote | `RemoteSettings.model` | DND2 / Terra Pirata / Custom |
| Remote control › Keys | `RemoteSettings.keys` | Each action with its keys: read-only for DND2 and Terra Pirata; for Custom, a button per action that opens `KeyCaptureDialog` |
| Roadbook › Roadbook colors | `RoadbookSettings.style` | App theme / FIA |
| Roadbook › Highlight close waypoints | `RoadbookSettings.shortDistanceThreshold` | − and + in steps of 50 m, within 50 m – 1 km |

## Persisted state

Five Preferences DataStore files, each owned by its repository:

| File | Key | Type | Value |
|---|---|---|---|
| `settings_app` | `theme_mode` | `String` | `AppSettings.themeMode`, by enum name |
| | `orientation` | `String` | `AppSettings.orientation`, by enum name |
| `settings_remote` | `model` | `String` | `RemoteSettings.model`, by enum name |
| | `custom_next_waypoint`, `custom_previous_waypoint`, `custom_increase_partial`, `custom_decrease_partial`, `custom_reset_partial` | `String` | The custom key codes of each action, comma-separated and sorted, e.g. `85,126`; empty for an action without keys |
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
- **Segmented buttons without a check mark.** The selected button's color already marks it, so the label gets the whole button and long names ("Terra Pirata", "Personalizado") fit on a phone with the larger text.
- **Steppers and segmented buttons, not sliders.** Every control is at least `minTouchTarget` high, and none needs a precise drag, so the screen works with gloves (AGENTS.md §5.3). A slider would be hard to set to an exact value on a moving bike.
- **The screen does not navigate.** It takes an `onBack` callback; `:app` owns the routes and the back stack.
- **The remote keys are their own settings group.** They are not part of `AppSettings`: only the dashboard needs them, and only `MainActivity` needs the theme and orientation, so each reads only what it uses, and each file falls back to its defaults on its own. One file per settings model, as for the modules.
- **Fixed remotes store only their name.** The keys of DND2 and Terra Pirata live in code (`RemoteKeys.DND2`, `RemoteKeys.TERRA_PIRATA`), so stored data can never disagree with them. Only the custom keys are stored, and they stay while another remote is selected, so they come back with Custom. They start as the DND2 keys.
- **A key triggers one action.** `RemoteKeys` rejects a key bound twice. Stored custom keys that do so, which only a damaged file can produce, fall back to the defaults as a whole, because there is no telling which binding was meant; an unreadable action falls back alone.
- **Custom keys are captured, not picked from a list.** Tapping an action's keys opens `KeyCaptureDialog`, and the first key the rider presses on the remote becomes that action's only key (`RemoteKeys.withOnlyKey`), so the rider never needs to know a key's name. Back and Escape cancel (so they cannot be assigned), as do the cancel button and a tap outside; modifier keys are ignored. The dialog consumes every other key, so a volume or media key changes nothing while it is captured.
- **A pressed key that another action has moves.** A key triggers one action, so assigning it takes it from the action that had it, which is left with no key, and a snackbar says so (state `keyMoved`, cleared once shown). The rider can give that action another key. The move is decided on the stored keys, inside the update, so it is always right even with fast taps. Assigning does nothing unless Custom is selected.
- **The capture dialog takes the focus itself.** It is opened with a tap, which leaves the app in touch mode, where a view that is not focusable in touch mode cannot take the focus, and without the focus the dialog gets no keys. So the dialog makes its window's view focusable in touch mode and requests the focus. Holding the focus also keeps Android from spending the first D-pad press on leaving touch mode.
- **Cold flows.** The repositories read DataStore's own flow, which already shares one copy of each file in memory, emits the current value first and then every change. `distinctUntilChanged` drops writes that change nothing.

## Testing notes

- The codecs are pure: `LocationSettingsCodecTest`, `OdometerSettingsCodecTest`, `RoadbookSettingsCodecTest`, `AppSettingsCodecTest` and `RemoteSettingsCodecTest` round-trip settings, check that defaults store nothing, and cover every invalid value.
- `SettingsViewModelTest` uses `FakeSettingsRepository` (in memory, with failing updates on demand) and `MainDispatcherRule` for every state and action. `ShortDistanceStepTest` covers the steps and their limits, and `CustomKeyAssignmentTest` the key assignment (new key, moved key, own key, several keys). `RemoteKeysTest` and `RemoteSettingsTest` cover the fixed keys, the lookups, a key bound twice, equality, and the keys in use for each model. `SettingsContentTest` runs on Robolectric (SDK 35, set in `src/test/resources/robolectric.properties`): every setting, selected choices (the theme's and the orientation's "System" share their label, so they are told apart by their order), the stepper limits, back, the save error, control sizes and the Spanish texts, plus the custom keys: buttons only for Custom, opening the dialog, a key pressed in it after the tap that opened it (the touch-mode case), cancel, the open dialog surviving recreation, and the moved-key snackbar. `KeyCaptureDialogTest` covers the dialog on its own (texts, D-pad, volume, modifier and back keys, cancel) and `isCapturableKey`. Keys are sent with `performKeyInput`. It finds controls by their text plus a click action (scrolling to them first, since the screen is longer than the test window), the icon-only back button by `SettingsTestTags.BACK`, and a key name inside its action's row (`SettingsTestTags.keyBinding`). Under Robolectric `KeyEvent.keyCodeToString` gives the bare number, so the fallback key name is compared with what it returns.
- `DataStoreSettingsRepositoryTest` uses `FakePreferencesDataStore`, which can fail reads and writes on demand, for the shared behavior (through the roadbook repository) and checks that the location and odometer repositories read their own keys.
- `DataStoreSettingsRepositoryIntegrationTest` uses a real DataStore file in a `TemporaryFolder`, and closes and reopens it to check that the settings survive. DataStore allows one open instance per file, so each instance runs in its own `Job`.
- `:core` does not share its test doubles, so `FakeLogger` lives in this module's test source set.
