# :map

Shows an **offline map** that follows the device location, turned so the direction of travel points up, with a configurable zoom level and tilt. `:map` depends only on `:core` and `:geo`.

`:map` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.map`, and it contains nothing specific to Roadbook Navigator, so other apps can depend on it too.

## Responsibility

- Find the mapsforge `.map` files installed on the device, and check that each one can be read.
- Draw them with [VTM](https://github.com/mapsforge/vtm) (mapsforge's OpenGL vector map), centered on the location and turned to its bearing.
- Say so when no map is installed, or when none can be read.

It does **not** read the location itself: the app forwards the `LocationFix`es of its location provider (in Roadbook Navigator, `:location`) through `MapLocationSource` (AGENTS.md §4.2). It does **not** store its settings either: the app provides them through `MapSettingsSource` (AGENTS.md §4.4; in Roadbook Navigator, `:settings` does). It does **not** install maps yet: map management (getting, listing and deleting map files) is planned as a next step.

## Package layout

```
dev/giste/map/
├── MapViewer, MapLocationSource, MapSettings, MapSettingsSource,
│   MapZoomLevel, MapTilt                                      ← public API
├── domain/         InstalledMaps (+ MapFile), MapFileRepository, MapCenter (+ follow)
├── data/           LocalMapFileRepository, MapFileChecker (+ VtmMapFileChecker), MapFolder
├── ui/             MapViewModel, MapUiState, MapViewerContent (+ MapViewerTestTags and previews),
│                   VtmMap, MapCamera
└── di/             MapModule
```

## Public API

Package `dev.giste.map`:

| Type | Purpose |
|---|---|
| `MapViewer(modifier, interactive = false)` | The map, with its state from Hilt. Place it anywhere: a whole screen or part of one. Each time it is shown it looks for the installed maps. While it is shown and has maps, it collects `MapLocationSource.locations`. Read-only by default: touches pass through it, and it always shows the settings' zoom and tilt. With `interactive = true` the user may pan, zoom, rotate and tilt it; the next fix centers it again, keeping the zoom and tilt. |
| `MapLocationSource` | Input port: a `Flow` of `:geo` `LocationFix`es. The app implements it and binds it with Hilt. Only the position and the bearing are used. A fix without a bearing keeps the last one. |
| `MapSettings` | `zoomLevel` and `tilt`, with documented defaults. |
| `MapZoomLevel` | Web map zoom level in `2..20` (VTM's limits), default 18. `of` rejects values out of range, `coerce` clamps them. |
| `MapTilt` | Tilt in degrees in `0..65` (VTM's limits), default 60. `of` rejects values out of range and `NaN`, `coerce` clamps them (`NaN` becomes the default). |
| `MapSettingsSource` | Input port. The app implements it and binds it with Hilt. |

`:map` exposes `:geo` and `kotlinx-coroutines-core` as `api` dependencies, because their types appear in its public API.

## Inputs and outputs

- **Inputs:** a `MapLocationSource` and a `MapSettingsSource`, which the app must bind in `SingletonComponent`. Without them the Hilt graph does not compile as soon as `MapViewer` is used. The map files, in the app's `map_files` folder (see Persisted state).
- **Outputs:** none besides the UI.

## Settings

| Setting | Type | Default | Effect |
|---|---|---|---|
| `zoomLevel` | `MapZoomLevel` | 18 | How close the map is shown. Each level shows the ground twice as large as the one before. |
| `tilt` | `MapTilt` | 60° | How much the map leans towards the horizon. 0° looks straight down. |

A read-only map always shows these values. An interactive map starts with them and goes back to them whenever they change; in between, the user's gestures are kept. If the settings flow throws, `:map` logs it and uses the defaults. In Roadbook Navigator they are fixed to the defaults for now (`:settings`' `FixedMapSettingsSource`).

## Persisted state

No DataStore. The map files live in `map_files` inside the app's **no-backup** folder (`Context.noBackupFilesDir`), e.g. `/data/user/0/<applicationId>/no_backup/map_files`. The folder is created when the map is first shown. Files with the `.map` extension (any case) in it and in its sub-folders are drawn; anything else is ignored.

## Design decisions

- **VTM from Maven Central.** `org.mapsforge:vtm`, `vtm-android` and `vtm-themes` 0.25.0, with the native renderer for arm64-v8a, armeabi-v7a and x86_64 (the emulator). The previous project took the same library from JitPack; Maven Central needs no extra repository.
- **Every file is checked first.** VTM draws several files as one source, and that source fails as a whole if any file fails to open: the map stays blank. `VtmMapFileChecker` opens each file with VTM's own header reader before it is drawn, and unreadable files are left out (and logged by name). Only when every file is unreadable does the viewer say so. A damaged file that makes VTM's parser throw is reported the same way, never crashing the app.
- **No-backup folder.** Map files are hundreds of megabytes. Auto Backup would try to upload them from the normal files folder and fail over its size limit, so they live where Auto Backup does not look.
- **Location only while there are maps.** The ViewModel collects the settings and the location only when maps are available, so a viewer that has nothing to draw does not keep the GPS busy.
- **Heading up, bearing kept.** VTM's bearing is the map's rotation, so a heading of `h` turns the map by `-h`. Devices report no bearing when stopped, so a fix without one keeps the last bearing instead of snapping back to north.
- **Location below the middle.** The location is drawn at VTM's view center 0.6 (below the middle of the view), as in the previous project, so more of the road ahead is visible.
- **Starting point.** Before the first fix, the map is centered on the middle of the installed maps, rather than on a fixed place.
- **Attribution not translated.** The map data's license (ODbL) requires crediting OpenStreetMap, and the OpenStreetMap Foundation's [attribution guidelines](https://osmfoundation.org/wiki/Licence/Attribution_Guidelines) name "© OpenStreetMap" as an accepted form, without covering translations. So `map_attribution` is that exact text, declared once with `translatable="false"` (AGENTS.md §8). The guidelines also ask for a way to reach the data's origin and license (e.g. a link to openstreetmap.org/copyright); the app does not offer one yet.
- **Corners.** The scale bar sits at the bottom left and the OpenStreetMap attribution at the bottom right, leaving the top corners free for floating controls.
- **Read-only by default.** A map read in glances while riding must not move under a glove or a bump, so `MapViewer` ignores touches unless the app asks for `interactive = true`. VTM's `MapView` handles no touch while it is not clickable, so read-only sets `isClickable` (and the focus flags) to `false`: touches reach what is under the map, and the map cannot take the focus. VTM can also turn single gestures off (`map().eventLayer.enableMove`/`enableZoom`/`enableRotation`/`enableTilt`); that is not offered, as no app needs it yet.
- **View lifecycle.** The `MapView` pauses and resumes with the lifecycle, and is destroyed when it leaves the composition, not only when the lifecycle ends: navigating to another screen disposes the dashboard, and otherwise every return would leak a view and its threads. A different set of files creates a new view, since VTM sets its tile source once.
- **Default theme.** The map uses VTM's `DEFAULT` render theme in both light and dark app themes, as the previous project did. A dark map theme could follow later.
- **No coordinates in logs.** Only file names and failures are logged.

## Testing notes

- `VtmMapFileCheckerTest` and `LocalMapFileRepositoryIntegrationTest` use real VTM on small map files written by `writeMapFile` (`TestMapFiles.kt`): a valid mapsforge header and an empty sub-file, so no real map has to be shipped. It also writes damaged variants (wrong magic bytes, unsupported version, truncated).
- `LocalMapFileRepositoryTest` uses a `TemporaryFolder` and a fake checker. The repository's dispatcher must share `runTest`'s scheduler (`StandardTestDispatcher(testScheduler)`), or `flowOn` never runs and the test hangs.
- `MapViewModelTest` uses `FakeMapFileRepository`, `FakeMapLocationSource` (which counts its collectors), `FakeMapSettingsSource`, `FakeLogger` and `MainDispatcherRule`.
- `MapCameraTest` runs VTM's pure-Java `MapPosition` on the JVM, and checks that `MapZoomLevel` and `MapTilt` match VTM's limits.
- `MapViewerContentTest` runs on Robolectric (SDK 35) with a placeholder in the map slot. `VtmMap` needs OpenGL and VTM's native library, which neither Robolectric nor previews have, so it is checked on a device: copy a map file with `adb push spain.map /data/local/tmp/` then `adb shell run-as dev.giste.roadbooknavigator sh -c 'mkdir -p no_backup/map_files && cp /data/local/tmp/spain.map no_backup/map_files/'`, and open the dashboard again. Maps are at https://download.mapsforge.org/maps/v5/.
- `:core` does not share its test doubles, so `FakeLogger` lives in this module's test source set.
