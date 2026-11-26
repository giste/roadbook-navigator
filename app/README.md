# :app

The composition root of Roadbook Navigator: the `Application`, the only activity, the dashboard screen, and the wiring and rules that connect the modules. `:app` may depend on every module, and no module depends on it (AGENTS.md §4).

## Responsibility

- Build the Hilt graph and bind what the modules need from the app: adapters between their ports (AGENTS.md §4.2). `:settings` binds the settings sources.
- Apply the **cross-module rules**, which decide how modules react to each other (AGENTS.md §4.3), and the **application rules**, this app's own policies on top of the modules' concepts.
- Show the dashboard: the odometer, the map and the roadbook, behind the precise-location permission gate, and navigate to the settings screen of `:settings` and to the map manager of `:map`, and from it to the map downloader.
- Show the About screen, opened from the settings: the app, its version and licence, the credits of the map data and the map renderer, and the licences of every open-source library the app ships.
- Apply the theme mode the rider chose in the settings.
- Act on the keys of a remote control while the dashboard is shown, following the remote settings of `:settings`.
- Translate the modules' neutral terms to rally terms: `:odometer`'s *trip* distance is the *partial*.

It does **not** hold rules about one module's concept, such as how distance is measured or when a vehicle counts as stopped: those belong in the module that owns the concept.

## Package layout

```
dev/giste/roadbooknavigator/
├── RoadbookNavigatorApplication, MainActivity
├── ui/             AppNavHost (+ DashboardRoute, SettingsRoute, AboutRoute, MapManagerRoute,
│                   MapDownloaderRoute), ThemeMode.isDarkTheme,
│                   ScreenOrientation.toActivityOrientation, RiderTypography,
│                   KeyEventInterception (KeyEventInterceptors, InterceptKeyEvents)
│   └── dashboard/  Dashboard + DashboardContent + DashboardTestTags, DashboardViewModel,
│                   DashboardUiState + OdometerUiState, SetPartialDialog, ResetAllDialog,
│                   PartialInput (parser and numpad keys), DistanceFormat,
│                   RemoteKeyEvents (KeyEvent.toKeyPress)
│   └── about/      AboutScreen + AboutContent + AboutTestTags, AboutViewModel + AboutUiState
├── domain/         RemoteKeyInterpreter (+ KeyPress, RemoteKeyOutcome),
│                   OpenSourceLibrary + LibraryLicence, OpenSourceLibraryRepository
│                   (+ OpenSourceLibrariesResult)
├── data/           AboutLibrariesOpenSourceLibraryRepository (+ readLibraryDefinitions,
│                   parseOpenSourceLibraries)
├── coordination/   RoadbookOdometerCoordinator
├── adapter/        LocationOdometerLocationSource, LocationMapLocationSource
├── permission/     PreciseLocationPermission
└── di/             AdapterModule, AboutModule
```

Outside the code, `aboutlibraries/` holds the AboutLibraries plugin's configuration (see Design decisions).

This is the `:app` layout of AGENTS.md §5.1. `domain` holds the application rules with real logic, in plain Kotlin (so far, how remote keys turn into actions), and the models and repository interface of the one thing `:app` reads itself: the About screen's library list. `data` reads that list.

## Public API

None: nothing depends on `:app`. Everything is `internal` or `private`, except the `Application` and the activity, which the manifest names.

## Wiring

| Module needs | `:app` provides | Where |
|---|---|---|
| `:odometer`'s `OdometerLocationSource` | The fixes of `:location`'s `LocationSource.events` | `LocationOdometerLocationSource`, bound in `AdapterModule` |
| `:map`'s `MapLocationSource` | The fixes of `:location`'s `LocationSource.events` (one shared GPS request with the odometer) | `LocationMapLocationSource`, bound in `AdapterModule` |

The settings sources of `:location`, `:map`, `:odometer` and `:roadbook` come from `:settings`, which `:app` depends on so that its Hilt bindings are part of the graph.

## Rules

| Rule | Kind | Where |
|---|---|---|
| A new roadbook resets the partial and the total. | Cross-module | `RoadbookOdometerCoordinator` |
| Long-pressing a waypoint's distances sets the partial to it. | Cross-module | `RoadbookOdometerCoordinator` |
| Importing a roadbook and opening the settings are only offered while stopped (`LocationSource.motion` is `STOPPED`). | Application | `DashboardViewModel` |
| On the dashboard, the remote's keys move between waypoints and change the partial by 10 m on every press and repeat; the reset key resets the partial once it has been held for 1 s. A key bound to an action is consumed even when nothing happens, so the system does not change the volume or skip music with it. | Application | `RemoteKeyInterpreter`, called by `DashboardViewModel.onKeyPress` |

`RoadbookNavigatorApplication` also gives WorkManager Hilt's worker factory (`Configuration.Provider`), so the modules' workers, such as `:map`'s downloads, are injected; WorkManager's default start-up is removed in the manifest, so it starts on first use with that configuration.

`RoadbookOdometerCoordinator` runs in the application scope and is started by `RoadbookNavigatorApplication`. It needs no location, so it runs from app start, outside the permission gate.

## Screens

- **`MainActivity`** shows everything inside `GisteTheme` and `:core`'s `PermissionGate` with `PreciseLocationPermission`: every screen needs precise location, so nothing runs before it is granted. Approximate location keeps the gate closed, because it cannot measure distances. The theme follows `AppSettingsSource`: dark or light as the rider chose, or the system's (also until the settings are read). So does the screen orientation (`ScreenOrientation.toActivityOrientation`): vertical and horizontal use the `USER_` orientations, so the app turns upside down with the device (mounted either way up) unless the rider locked the rotation; the system's follows the device. A change recreates the activity, as any rotation does. Its text uses `RiderTypography`.
- **Navigation** (`AppNavHost`, Navigation Compose with type-safe routes): the dashboard is the start screen, and the settings and `:map`'s `MapManager` open on top of it; the About screen opens on top of the settings, and `:map`'s `MapDownloader` on top of the map manager. Back, from a screen's back button or the system gesture, returns to the screen below (in the downloader, from its top folder); the dashboard then looks for the installed maps again.
- **Dashboard** (`ui/dashboard/`): the total, the partial, `:map`'s `MapViewer` (read-only, its default, so a touch while riding cannot move or zoom it) and `:roadbook`'s `RoadbookViewer`. The map follows the location only while the dashboard is shown, like the odometer. Previews draw a plain area in its place, as the OpenGL map cannot be drawn there.
  - The layout follows the aspect ratio of the space it gets (`BoxWithConstraints`), not a window size class: landscape puts the distances and the map in a side column; portrait stacks distances, map and roadbook. The shares are the `LANDSCAPE_*` and `PORTRAIT_*` constants.
  - The distances have no visible label and fill their boxes with `:core`'s `AutoSizeText`, sized for their largest value: 999.99 km for the partial and 9,999.9 km for the total. Each is centered vertically in its box, since the width usually limits the digits. Larger values are shown capped; the odometer keeps the real value.
  - The settings button (a gear, `DashboardTestTags.SETTINGS`) has a fixed place on the start side of the total, with the total's background: a plain gear in the primary color, with no container, as in the previous app. Its slot is a square as tall as the total's box (never narrower than the touch target), and the gear takes `SETTINGS_ICON_FRACTION` of it, so it grows with the space. It is shown only while the vehicle is stopped (`showStoppedActions`): leaving the dashboard stops the odometer, so a touch while riding must not open the settings. While moving the slot stays empty rather than going away, so the total keeps its size and place. It never takes the focus (`focusProperties { canFocus = false }`): it is meant to be touched, and otherwise, in key mode, an arrow or Tab key not bound to a remote action would move the focus onto it, drawing its focus circle, and an Enter or centre key would open the settings. The roadbook shows its import button, and the map its manage button (`MapViewer(showManageButton, onManageMaps)`), under the same condition; the map shows it anyway while it has no map to draw, since managing maps is then the only thing to do. Both belong to their modules, and the map's never takes the focus either.
  - Long-pressing the partial opens `SetPartialDialog`: almost full screen, with a big-key numpad for gloves (digits, the locale's decimal separator and delete) and big cancel and set buttons. Its keys follow `pressNumpadKey`, which ignores any key that would make the partial something the dashboard cannot show exactly, so the dialog needs no error messages. Every key vibrates: a tick when it changes the partial, a rejection when it is ignored. Tapping outside the dialog does nothing, so a bump does not lose the typed partial. Long-pressing the total opens `ResetAllDialog`, which asks before resetting both distances. A tap does nothing.
  - **Remote control.** While the dashboard is in the composition, `InterceptKeyEvents` passes it the activity's key events: `MainActivity.dispatchKeyEvent` offers them to its `KeyEventInterceptors` before the views and the window, which would otherwise change the volume or skip music. Compose's key modifiers are not used, because they need a focused node, and touching the screen takes the focus away. `KeyEvent.toKeyPress` turns each event into a `KeyPress`, and `DashboardViewModel.onKeyPress` acts on it (see Rules) with the keys of `RemoteSettingsSource`, read from the start and followed on every change. Until they are read, every key reaches the system. The settings screen and the dashboard dialogs (their own windows) get no remote actions.
  - The dashboard also takes the focus (a `focusable` root, requested whenever it is shown). With no focused view, after any touch Android would spend the next D-pad key (the DND2 remote's) on leaving touch mode, before the activity sees it, and that press would be lost.
  - The screen stays on while the dashboard is shown (`Modifier.keepScreenOn` on its root), because the rider reads it without touching it. The settings screen lets it sleep as usual, so the tablet can go off at a coffee stop.
  - Collecting `DashboardViewModel.uiState` collects `OdometerController.state`, which makes the odometer measure, and `LocationSource.motion`. Both only run while the dashboard is shown, behind the permission gate.

- **About** (`ui/about/`, opened from the settings' About button): one scrolling list with the app's name, version (`BuildConfig.VERSION_NAME`) and licence (GNU GPL v3 or later, with a link to its text); the map data credit, `© OpenStreetMap contributors` (fixed text, not translated) with a link to OpenStreetMap's copyright page, as its attribution guidelines ask; the map renderer credit, VTM from mapsforge (LGPL v3 or later), with a link to its website; and the open-source libraries, each with its version and licences. Tapping a library opens its licence texts in a dialog that scrolls, with a close icon; which library is open survives configuration changes. A licence without a text would show a link instead (none does, see Design decisions). Links open in the browser, and a snackbar says so when no app can open them. `AboutViewModel` reads the library list once the screen is shown: a spinner until then, and a message if it cannot be read, while the rest of the screen stays.

## Settings

None of its own yet. The modules' settings are stored and provided by `:settings`.

## Persisted state

None. Each module persists its own state (AGENTS.md §7). The open dashboard dialog and the typed partial are kept with `rememberSaveable` across configuration changes.

## Design decisions

- **Rules stay small and on public APIs.** Coordinators and the ViewModel only call the modules' public APIs. A rule about one module's concept moved into that module: the stopped/moving detection is `:location`'s `LocationSource.motion`, and `:app` only decides what to do with it.
- **Larger text, decided here.** The app is read at arm's length on a tablet on the handlebars, so `RiderTypography` scales every Material text style by `RIDER_TEXT_SCALE` (1.25: everyday text about 18–20 sp) and `MainActivity` passes it to `GisteTheme`. It lives in `:app`, not `:core`, because other apps reusing `:core` may not need it; `:core` only offers the `typography` parameter. Screens of `:settings` and `:roadbook` use it too, since they render inside the app's theme. Sizes stay in `sp`, so the system font size still scales them. The app's previews pass it as well, and `:settings` previews use `fontScale = 1.25`.
- **The permission is defined here,** not in `:location`, because its texts are specific to this app and a reusable module must not contain them (AGENTS.md §4.1).
- **The library list is generated, not written by hand.** The AboutLibraries Gradle plugin (`com.mikepenz.aboutlibraries.plugin.android`, applied to `:app` only, since `:app` sees every module's dependencies) writes every runtime dependency and its licences to `R.raw.aboutlibraries` at build time, for each variant (debug includes the debug-only tooling). `aboutlibraries-core` reads it in `AboutLibrariesOpenSourceLibraryRepository`, the only place that sees its types; its ready-made Compose UI is not used, so the screen follows the rider rules. A new dependency therefore appears on its own.
- **Builds never use the network for it.** `offlineMode` is on, so the plugin cannot download licence texts; their texts are committed in `aboutlibraries/licenses/` (Apache 2.0, BSD 3-Clause, LGPL 3.0 or later). `strictMode = FAIL` with those three as `allowedLicenses` fails the build when a dependency brings any other licence: add its text there (the plugin downloads it once with `offlineMode` off; copy it from the generated `app/build/generated/aboutLibraries/*/res/raw/aboutlibraries.json`) and allow it. VTM's POM names its licence without an SPDX id, so `aboutlibraries/licenses/lgpl_3_0_or_later.json` fills the entry the plugin makes for it (keyed by the hash the plugin gives it) with the LGPL text. `aboutlibraries/libraries/` gives VTM's Android and themes artifacts their own names, since all three call themselves "VTM".
- **No screen-reader semantics** (AGENTS.md §5.3). The readings have test tags (`DashboardTestTags`) instead of content descriptions.

## Testing notes

- **Unit tests** (`src/test`): `RemoteKeyInterpreterTest` (every action, repeats, the reset hold and its timing, missed first presses and releases), `DashboardViewModelTest` (including every remote action, the step, consumed and unbound keys, a model change and unread settings), `PartialInputTest`, `DistanceFormatTest`, `ScreenOrientationsTest`, `RoadbookOdometerCoordinatorTest`, `LocationOdometerLocationSourceTest`, `LocationMapLocationSourceTest`, and `PreciseLocationPermissionTest`, with `FakeOdometerController`, `FakeLocationSource`, `FakeRoadbookController` (which records waypoint moves) and `FakeRemoteSettingsSource`. `MainDispatcherRule` replaces Main; create ViewModels inside each test, after the rule has run, because `viewModelScope` takes Main's dispatcher when the ViewModel is created.
- **UI tests** run on Robolectric in `src/test`: `DashboardContentTest` (layouts, readings and their vertical centering, long presses, dialogs, state restoration, keeping the screen on, the settings button refusing the focus in key mode: Robolectric starts in touch mode, where no button takes it, so the test switches to `InputMode.Keyboard` first), `SetPartialDialogTest` (keys, haptics, layouts, key sizes, Spanish separator), `ResetAllDialogTest`, `KeyEventInterceptionTest` (order, consumption, removal, and `InterceptKeyEvents` entering and leaving the composition), `RemoteKeyEventsTest` (`KeyEvent.toKeyPress`) and `ThemeModesTest` (each theme mode on a light and a dark system). `RiderTypographyTest` checks that every style is scaled, and that weights and letter spacing are kept. They find readings by `DashboardTestTags`, the typed partial and the delete key by `SetPartialTestTags`, the numpad keys by their label plus a click action (`NumpadTestSupport`), and everything else by visible text.
- **App integration tests** (`src/test/.../integration`) run the real Hilt graph on Robolectric with location granted: the permission gate, the map viewer reporting that no maps are installed (drawing a map needs OpenGL, so it is checked on a device), opening the settings, leaving them with the system back gesture, opening the map manager from the map's manage button and leaving it the same way, opening the map downloader from the map manager and going back to it (with the download site bound to a closed local port, so no test uses the network), storing the chosen theme mode, storing and applying the chosen screen orientation, roadbook import resetting the odometer, a waypoint long press setting the partial, the set-partial and reset-all dialogs, and the import button hidden while moving. `RemoteKeysIntegrationTest` sends key events to `MainActivity`: the partial keys, the reset hold, an unbound key passed on, no action from the settings screen, a model picked in the settings, a custom key assigned in the settings and then used on the dashboard, and the dashboard holding the focus. The touch-mode behavior the focus guards against does not happen under Robolectric, so it was checked on the tablet with `adb shell input keyevent`; a 1 s hold cannot be injected that way on Android 13. Only the GPS (Robolectric's location manager) and the file picker are replaced. `HiltTestApplication` replaces the app's `Application`, so the tests start the coordinator themselves. `WorkerFactoryIntegrationTest` checks that the Hilt worker factory builds `:map`'s download worker (named by its class name, as it is internal to `:map`). `MapDownloadCancelIntegrationTest` sends the map download notification's cancel broadcast to `:map`'s receiver and checks that the download queue is rebuilt without that map.
- **About:** `AboutLibrariesOpenSourceLibraryRepositoryTest` (Robolectric, as AboutLibraries parses with Android's `org.json`) covers the parser on a small fixture (`src/test/resources/about/aboutlibraries_sample.json`: sorting, versions, several licences, blank values, invalid JSON) and the reader's failures (unreadable file, missing resource, invalid or empty list). `AboutLibrariesOpenSourceLibraryRepositoryIntegrationTest` reads the list generated for this build: the app's main libraries are in it, every licence has its text, names are unique, and VTM is under the LGPL. `AboutViewModelTest` uses `FakeOpenSourceLibraryRepository`, which answers when the test says so. `AboutContentTest` covers every state, the links (through a fake `UriHandler`, one that fails like Compose does without a browser), the licences dialog, its restoration, touch target sizes and the Spanish texts. `NavigationIntegrationTest` opens the About screen from the settings, finds a library of the real list, and goes back.
- `:app` has no instrumented tests (`src/androidTest`).
