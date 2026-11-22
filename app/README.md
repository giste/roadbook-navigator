# :app

The composition root of Roadbook Navigator: the `Application`, the only activity, the dashboard screen, and the wiring and rules that connect the modules. `:app` may depend on every module, and no module depends on it (AGENTS.md §4).

## Responsibility

- Build the Hilt graph and bind what the modules need from the app: settings sources and adapters between their ports (AGENTS.md §4.2).
- Apply the **cross-module rules**, which decide how modules react to each other (AGENTS.md §4.3), and the **application rules**, this app's own policies on top of the modules' concepts.
- Show the dashboard: the odometer, the map and the roadbook, behind the precise-location permission gate.
- Translate the modules' neutral terms to rally terms: `:odometer`'s *trip* distance is the *partial*.

It does **not** hold rules about one module's concept, such as how distance is measured or when a vehicle counts as stopped: those belong in the module that owns the concept.

## Package layout

```
dev/giste/roadbooknavigator/
├── RoadbookNavigatorApplication, MainActivity
├── ui/
│   └── dashboard/  Dashboard + DashboardContent + DashboardTestTags, DashboardViewModel,
│                   DashboardUiState + OdometerUiState, SetPartialDialog, ResetAllDialog,
│                   PartialInput (parser and numpad keys), DistanceFormat
├── coordination/   RoadbookOdometerCoordinator
├── adapter/        LocationOdometerLocationSource
├── permission/     PreciseLocationPermission
├── settings/       DefaultLocationSettingsSource, DefaultOdometerSettingsSource,
│                   DefaultRoadbookSettingsSource   (temporary)
└── di/             AdapterModule, LocationSettingsModule, OdometerSettingsModule,
                    RoadbookSettingsModule
```

This is the `:app` layout of AGENTS.md §5.1. There is no `domain` package yet: the only application rule, offering imports only while stopped, is one line in `DashboardViewModel`. It will be created with the first rule that has real logic.

## Public API

None: nothing depends on `:app`. Everything is `internal` or `private`, except the `Application` and the activity, which the manifest names.

## Wiring

| Module needs | `:app` provides | Where |
|---|---|---|
| `:odometer`'s `OdometerLocationSource` | The fixes of `:location`'s `LocationSource.events` | `LocationOdometerLocationSource`, bound in `AdapterModule` |
| `:location`'s `LocationSettingsSource` | The default `LocationSettings` | `DefaultLocationSettingsSource`, bound in `LocationSettingsModule` |
| `:odometer`'s `OdometerSettingsSource` | The default `OdometerSettings` | `DefaultOdometerSettingsSource`, bound in `OdometerSettingsModule` |
| `:roadbook`'s `RoadbookSettingsSource` | The default `RoadbookSettings` | `DefaultRoadbookSettingsSource`, bound in `RoadbookSettingsModule` |

The three settings sources and their modules are **temporary**: each emits its module's defaults once. Delete them when `:settings` binds the real sources.

## Rules

| Rule | Kind | Where |
|---|---|---|
| A new roadbook resets the partial and the total. | Cross-module | `RoadbookOdometerCoordinator` |
| Long-pressing a waypoint's distances sets the partial to it. | Cross-module | `RoadbookOdometerCoordinator` |
| Importing a roadbook is only offered while stopped (`LocationSource.motion` is `STOPPED`). | Application | `DashboardViewModel` |

`RoadbookOdometerCoordinator` runs in the application scope and is started by `RoadbookNavigatorApplication`. It needs no location, so it runs from app start, outside the permission gate.

## Screens

- **`MainActivity`** shows everything inside `GisteTheme` and `:core`'s `PermissionGate` with `PreciseLocationPermission`: every screen needs precise location, so nothing runs before it is granted. Approximate location keeps the gate closed, because it cannot measure distances.
- **Dashboard** (`ui/dashboard/`): the total, the partial, the map (a placeholder until `:map` exists) and `:roadbook`'s `RoadbookViewer`.
  - The layout follows the aspect ratio of the space it gets (`BoxWithConstraints`), not a window size class: landscape puts the distances and the map in a side column; portrait stacks distances, map and roadbook. The shares are the `LANDSCAPE_*` and `PORTRAIT_*` constants.
  - The distances have no visible label and fill their boxes with `:core`'s `AutoSizeText`, sized for their largest value: 999.99 km for the partial and 9,999.9 km for the total. Larger values are shown capped; the odometer keeps the real value.
  - Long-pressing the partial opens `SetPartialDialog`: almost full screen, with a big-key numpad for gloves (digits, the locale's decimal separator and delete) and big cancel and set buttons. Its keys follow `pressNumpadKey`, which ignores any key that would make the partial something the dashboard cannot show exactly, so the dialog needs no error messages. Every key vibrates: a tick when it changes the partial, a rejection when it is ignored. Tapping outside the dialog does nothing, so a bump does not lose the typed partial. Long-pressing the total opens `ResetAllDialog`, which asks before resetting both distances. A tap does nothing.
  - Collecting `DashboardViewModel.uiState` collects `OdometerController.state`, which makes the odometer measure, and `LocationSource.motion`. Both only run while the dashboard is shown, behind the permission gate.

## Settings

None of its own. The settings sources above stand in for `:settings`.

## Persisted state

None. Each module persists its own state (AGENTS.md §7). The open dashboard dialog and the typed partial are kept with `rememberSaveable` across configuration changes.

## Design decisions

- **Rules stay small and on public APIs.** Coordinators and the ViewModel only call the modules' public APIs. A rule about one module's concept moved into that module: the stopped/moving detection is `:location`'s `LocationSource.motion`, and `:app` only decides what to do with it.
- **The permission is defined here,** not in `:location`, because its texts are specific to this app and a reusable module must not contain them (AGENTS.md §4.1).
- **No screen-reader semantics** (AGENTS.md §5.3). The readings have test tags (`DashboardTestTags`) instead of content descriptions.

## Testing notes

- **Unit tests** (`src/test`): `DashboardViewModelTest`, `PartialInputTest`, `DistanceFormatTest`, `RoadbookOdometerCoordinatorTest`, `LocationOdometerLocationSourceTest`, `PreciseLocationPermissionTest` and the `Default*SettingsSourceTest`s, with `FakeOdometerController`, `FakeLocationSource` and `FakeRoadbookController`. `MainDispatcherRule` replaces Main; create ViewModels inside each test, after the rule has run, because `viewModelScope` takes Main's dispatcher when the ViewModel is created.
- **UI tests** run on Robolectric in `src/test`: `DashboardContentTest` (layouts, readings, long presses, dialogs, state restoration), `SetPartialDialogTest` (keys, haptics, layouts, key sizes, Spanish separator) and `ResetAllDialogTest`. They find readings by `DashboardTestTags`, the typed partial and the delete key by `SetPartialTestTags`, the numpad keys by their label plus a click action (`NumpadTestSupport`), and everything else by visible text.
- **App integration tests** (`src/test/.../integration`) run the real Hilt graph on Robolectric with location granted: the permission gate, roadbook import resetting the odometer, a waypoint long press setting the partial, the set-partial and reset-all dialogs, and the import button hidden while moving. Only the GPS (Robolectric's location manager) and the file picker are replaced. `HiltTestApplication` replaces the app's `Application`, so the tests start the coordinator themselves.
- `:app` has no instrumented tests (`src/androidTest`).
