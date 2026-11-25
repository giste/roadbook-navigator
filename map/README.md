# :map

Shows an **offline map** that follows the device location, turned so the direction of travel points up, with a configurable zoom level and tilt. `:map` depends only on `:core` and `:geo`.

`:map` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.map`, and it contains nothing specific to Roadbook Navigator, so other apps can depend on it too.

## Responsibility

- Find the mapsforge `.map` files installed on the device, and check that each one can be read.
- Draw them with [VTM](https://github.com/mapsforge/vtm) (mapsforge's OpenGL vector map), centered on the location and turned to its bearing.
- Say so when no map is installed, or when none can be read.
- Manage the installed maps: list them with their size, mark the ones that cannot be read, and delete them.
- Browse the maps on a download site (by default, the mapsforge server), one folder at a time, marking the ones already installed.

It does **not** read the location itself: the app forwards the `LocationFix`es of its location provider (in Roadbook Navigator, `:location`) through `MapLocationSource` (AGENTS.md §4.2). It does **not** store its settings either: the app provides them through `MapSettingsSource` (AGENTS.md §4.4; in Roadbook Navigator, `:settings` does). It does **not** download maps yet: the map downloader only browses the site so far, and downloading is the next step of map management. For now, maps are copied into the folder by hand (see Testing notes).

## Package layout

```
dev/giste/map/
├── MapViewer, MapManager, MapDownloader, MapDownloadServer, MapLocationSource,
│   MapSettings, MapSettingsSource, MapZoomLevel, MapTilt      ← public API
├── domain/         InstalledMapFiles (+ InstalledMapFile, FileSize, toInstalledMaps, DeleteMapResult),
│                   InstalledMaps (+ MapFile), MapFileRepository, MapCenter (+ follow),
│                   RemoteMaps (RemoteFolderPath, isSafeRemoteName, RemoteMapEntry, RemoteListing,
│                   RemoteListingFailure, RemoteMapRepository)
├── data/           LocalMapFileRepository, MapFileChecker (+ VtmMapFileChecker), MapFolder,
│                   HttpRemoteMapRepository, DirectoryListingParser, MapHttpClient
├── ui/             MapViewModel, MapUiState, MapViewerContent (+ MapViewerTestTags and previews),
│                   VtmMap, MapCamera, MapManagerViewModel, MapManagerUiState,
│                   MapManagerContent (+ MapManagerTestTags and previews), MapDownloaderViewModel,
│                   MapDownloaderUiState, MapDownloaderContent (+ MapDownloaderTestTags and previews)
└── di/             MapModule
```

## Public API

Package `dev.giste.map`:

| Type | Purpose |
|---|---|
| `MapViewer(modifier, interactive = false, showManageButton = false, onManageMaps = null)` | The map, with its state from Hilt. Place it anywhere: a whole screen or part of one. Each time it is shown it looks for the installed maps. While it is shown and has maps, it collects `MapLocationSource.locations`. Read-only by default: touches pass through it, and it always shows the settings' zoom and tilt. With `interactive = true` the user may pan, zoom, rotate and tilt it; the next fix centers it again, keeping the zoom and tilt. With `onManageMaps`, a manage button (a map icon) floats in the top end corner while `showManageButton` is set, and always when there is no map to draw; the app opens `MapManager` from it. |
| `MapManager(onBack, modifier, onDownloadMaps = null)` | The map manager screen, with its state from Hilt: a top bar with a back button, the installed map files and, with `onDownloadMaps`, a floating add button at the bottom end (the list leaves room under its last row so the button never hides it) with their sub-folder and size. Unreadable files have a warning mark. Each has a delete button, which asks first. The app adds it as a destination of its own; its back button calls `onBack`, and the system back gesture is the app's. The app opens `MapDownloader` from `onDownloadMaps`. |
| `MapDownloader(onBack, modifier)` | The map downloader screen, with its state from Hilt: the download site's maps folder, then each folder the user opens. Folder rows open the folder; map rows show the name, the day it changed, the approximate size and a mark if it is already installed. If a folder cannot be read it says why, with a button to try again. In a sub-folder, its back button and the system back gesture go up one folder; in the maps folder, its back button calls `onBack`, and the system back gesture is the app's. The app adds it as a destination of its own. |
| `MapDownloadServer` | Where the maps are downloaded from: an `http(s)` folder address ending in `/`, served as plain directory listings. Defaults to `DEFAULT_URL`, the mapsforge server. An app may provide its own through Hilt (e.g. a mirror); `:map` uses it if bound (`@BindsOptionalOf`). |
| `MapLocationSource` | Input port: a `Flow` of `:geo` `LocationFix`es. The app implements it and binds it with Hilt. Only the position and the bearing are used. A fix without a bearing keeps the last one. |
| `MapSettings` | `zoomLevel` and `tilt`, with documented defaults. |
| `MapZoomLevel` | Web map zoom level in `2..20` (VTM's limits), default 18. `of` rejects values out of range, `coerce` clamps them. |
| `MapTilt` | Tilt in degrees in `0..65` (VTM's limits), default 60. `of` rejects values out of range and `NaN`, `coerce` clamps them (`NaN` becomes the default). |
| `MapSettingsSource` | Input port. The app implements it and binds it with Hilt. |

`:map` exposes `:geo` and `kotlinx-coroutines-core` as `api` dependencies, because their types appear in its public API.

## Inputs and outputs

- **Inputs:** a `MapLocationSource` and a `MapSettingsSource`, which the app must bind in `SingletonComponent`. Without them the Hilt graph does not compile as soon as `MapViewer` is used. Optionally, a `MapDownloadServer`. The map files, in the app's `map_files` folder (see Persisted state). The download site's folder listings, over HTTP: `:map` declares the `INTERNET` permission.
- **Outputs:** none besides the UI, and the `onManageMaps` callback of `MapViewer` and `onDownloadMaps` of `MapManager`.

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
- **One look at the folder for both screens.** `MapFileRepository.installedFiles` lists every `.map` file, readable or not, with its size and sub-folder. The manager shows that list; the viewer sums it up with `toInstalledMaps()` (readable files only), so both agree on what is installed.
- **Unreadable files are listed.** The viewer leaves them out, so without the manager a damaged file could only be removed by hand. The manager marks them with a warning icon, so they can be deleted.
- **Deleting is guarded.** It always asks first, in a dialog (AGENTS.md §5.3). The repository deletes only `.map` files inside the map folder (checked on the canonical path), whatever path it is given. A file already gone counts as deleted. Failures are a `DeleteMapResult.Failed`, shown in a snackbar.
- **Icons for actions.** Manage, back, delete, confirm (a filled error-colored delete button), cancel (an outlined close button) and the unreadable mark are icons, not text: fewer strings to translate, and the same space in every language. File sizes come from Android's `Formatter.formatShortFileSize`, already localized.
- **The manage button.** It shows while the app says so (in Roadbook Navigator, only while stopped, so a touch while riding cannot leave the dashboard) and always when there is no map to draw, since managing maps is then the only way forward. It never takes the focus, so a remote control's arrow keys cannot land on it.
- **Changes reach the viewer by being shown again.** The manager is a separate destination, so the dashboard (and its viewer) leaves the composition while it is open, and the viewer looks for the maps again when it comes back. Maps downloaded in the background (next step) will need the viewer to notice them while it is shown.
- **A small listing parser, not an HTML library.** The download site's folders are Apache autoindex pages, generated by the server and regular in form, so `DirectoryListingParser` reads their table rows with regular expressions. Jsoup was considered and left out: it would not make the parser more robust to a change of layout, and it adds a dependency. Real listings saved from the site are its test fixtures. A page that is not a listing is `UNREADABLE_LISTING`, never a crash.
- **One folder at a time.** The site nests continents, countries and regions, so the downloader reads only the folder being shown, never the whole site. Folders come first, then maps, each in the site's order; other files are left out.
- **The site's names are untrusted.** They will become file and folder names on the device when maps are downloaded, so names that could leave the map folder (`..`, separators, control characters) are dropped by the parser and rejected by `RemoteFolderPath`.
- **Listed dates and sizes are approximate.** The site rounds sizes (`2.4M`, in powers of 1024) and lists dates in its own time zone (8 hours ahead of the files' HTTP `Last-Modified`), so the screen shows only the day. Exact sizes and times will come from the download itself.
- **Installed means the same folder and name.** Downloads will keep the site's folders (`europe/italy/centro.map`), so a map is marked as installed when a file of the same name is in the same folder of `map_files`. A map copied by hand to another folder is not marked.
- **Its own HTTP client.** `:map` provides an `OkHttpClient` qualified with `MapHttpClient`, so an app that provides its own client does not clash with it. Requests run on OkHttp's threads, and cancelling the coroutine cancels the call.

## Testing notes

- `VtmMapFileCheckerTest` and `LocalMapFileRepositoryIntegrationTest` use real VTM on small map files written by `writeMapFile` (`TestMapFiles.kt`): a valid mapsforge header and an empty sub-file, so no real map has to be shipped. It also writes damaged variants (wrong magic bytes, unsupported version, truncated).
- `LocalMapFileRepositoryTest` uses a `TemporaryFolder` and a fake checker. The repository's dispatcher must share `runTest`'s scheduler (`StandardTestDispatcher(testScheduler)`), or `flowOn` never runs and the test hangs.
- `LocalMapFileRepositoryTest` also covers deleting: files in sub-folders, files already gone, paths outside the folder (direct or with `..`), files that are not maps, a folder the file system will not change (`setWritable(false)`), and a path with a NUL character, which `java.io.File` rejects with an `IOException`.
- `MapViewModelTest` uses `FakeMapFileRepository`, `FakeMapLocationSource` (which counts its collectors), `FakeMapSettingsSource`, `FakeLogger` and `MainDispatcherRule`.
- `MapCameraTest` runs VTM's pure-Java `MapPosition` on the JVM, and checks that `MapZoomLevel` and `MapTilt` match VTM's limits.
- `MapManagerViewModelTest` uses `FakeMapFileRepository`, whose deletions answer a chosen `DeleteMapResult` and do not change the listed files.
- `MapManagerContentTest` runs on Robolectric. It finds rows by `MapManagerTestTags.file(file)`, and the icon-only buttons and marks by their tags inside a row; sizes are compared with `Formatter.formatShortFileSize` in the test's locale.
- `MapViewerContentTest` runs on Robolectric (SDK 35) with a placeholder in the map slot. `VtmMap` needs OpenGL and VTM's native library, which neither Robolectric nor previews have, so it is checked on a device: copy a map file with `adb push spain.map /data/local/tmp/` then `adb shell run-as dev.giste.roadbooknavigator sh -c 'mkdir -p no_backup/map_files && cp /data/local/tmp/spain.map no_backup/map_files/'`, and open the dashboard again. Maps are at https://download.mapsforge.org/maps/v5/.
- `DirectoryListingParserTest` reads `mapsforge_listing_root.html` and `mapsforge_listing_europe.html` (in `src/test/resources`), saved from the mapsforge server on 2026-11-24, and small hand-written listings for the edge cases (links elsewhere, other files, unsafe or encoded names, sizes and dates that cannot be read).
- `HttpRemoteMapRepositoryTest` uses OkHttp's `MockWebServer`, a local server, so no test uses the network: request paths, listings, error statuses, a page that is not a listing, an unreachable server, a connection lost while reading, and cancellation (seen through an `EventListener`).
- `MapDownloaderViewModelTest` uses `FakeRemoteMapRepository` (whose `gate` holds calls to check the loading state) and `FakeMapFileRepository`, and a `SavedStateHandle` to check that the folder survives the process.
- `MapDownloaderContentTest` runs on Robolectric with a `ComponentActivity`, so it can press the system back button. Folder rows are clickable and merge their text, so their text is found in the unmerged tree.
- `:core` does not share its test doubles, so `FakeLogger` lives in this module's test source set.
