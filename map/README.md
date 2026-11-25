# :map

Shows an **offline map** that follows the device location, turned so the direction of travel points up, with a configurable zoom level and tilt. `:map` depends only on `:core` and `:geo`.

`:map` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.map`, and it contains nothing specific to Roadbook Navigator, so other apps can depend on it too.

## Responsibility

- Find the mapsforge `.map` files installed on the device, and check that each one can be read.
- Draw them with [VTM](https://github.com/mapsforge/vtm) (mapsforge's OpenGL vector map), centered on the location and turned to its bearing.
- Say so when no map is installed, or when none can be read.
- Manage the installed maps: list them with their size, mark the ones that cannot be read, and delete them.
- Browse the maps on a download site (by default, the mapsforge server), one folder at a time, marking the ones already installed.
- Download maps from that site in the background, one at a time, with a notification, resuming interrupted downloads, and install them only once they are complete and readable. Downloads wait for Wi-Fi unless the user agrees to use mobile data, and can be stopped from the screen or the notification.

It does **not** read the location itself: the app forwards the `LocationFix`es of its location provider (in Roadbook Navigator, `:location`) through `MapLocationSource` (AGENTS.md §4.2). It does **not** store its settings either: the app provides them through `MapSettingsSource` (AGENTS.md §4.4; in Roadbook Navigator, `:settings` does). Maps can also be copied into the folder by hand (see Testing notes).

## Package layout

```
dev/giste/map/
├── MapViewer, MapManager, MapDownloader, MapDownloadServer, MapLocationSource,
│   MapSettings, MapSettingsSource, MapZoomLevel, MapTilt      ← public API
├── domain/         InstalledMapFiles (+ InstalledMapFile, FileSize, toInstalledMaps, DeleteMapResult),
│                   InstalledMaps (+ MapFile), MapFileRepository, MapCenter (+ follow),
│                   RemoteMaps (RemoteFolderPath, isSafeRemoteName, RemoteMapEntry, RemoteListing,
│                   RemoteListingFailure, RemoteMapRepository), MapDownloads (RemoteMapPath,
│                   MapDownload, MapDownloadFailure, MapDownloadRepository, NetworkConnection)
├── data/           LocalMapFileRepository, MapFileChecker (+ VtmMapFileChecker), MapFolder,
│                   MapFolderChanges, HttpRemoteMapRepository, DirectoryListingParser, MapHttpClient,
│                   MapSite, HttpMapFileDownloader (+ MapFileDownloader, MapFileDownloadResult),
│                   FreeSpace (+ StorageManagerFreeSpace), MapDownloadWorker, MapDownloadNotifications,
│                   WorkManagerMapDownloadRepository (+ toDownloads), MapDownloadCancelReceiver,
│                   ConnectivityNetworkConnection
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
| `MapViewer(modifier, interactive = false, showManageButton = false, onManageMaps = null)` | The map, with its state from Hilt. Place it anywhere: a whole screen or part of one. Each time it is shown it looks for the installed maps, and again after each deletion or finished download, so it draws or drops them at once. While it is shown and has maps, it collects `MapLocationSource.locations`. Read-only by default: touches pass through it, and it always shows the settings' zoom and tilt. With `interactive = true` the user may pan, zoom, rotate and tilt it; the next fix centers it again, keeping the zoom and tilt. With `onManageMaps`, a manage button (a map icon) floats in the top end corner while `showManageButton` is set, and always when there is no map to draw; the app opens `MapManager` from it. |
| `MapManager(onBack, modifier, onDownloadMaps = null)` | The map manager screen, with its state from Hilt: a top bar with a back button, the installed map files with their sub-folder and size and, with `onDownloadMaps`, a floating add button at the bottom end (the list leaves room under its last row so the button never hides it). The list follows deletions and finished downloads. Unreadable files have a warning mark. Each has a delete button, which asks first. The app adds it as a destination of its own; its back button calls `onBack`, and the system back gesture is the app's. The app opens `MapDownloader` from `onDownloadMaps`. |
| `MapDownloader(onBack, modifier)` | The map downloader screen, with its state from Hilt: the download site's maps folder, then each folder the user opens. Folder rows open the folder; map rows show the name, the day it changed and the approximate size, and at their end a download button, a waiting mark, the download's progress, a mark if the map is installed, or, after a failed download, the reason under the name and a button to try again. A running download's progress ring has a ✕ inside; pressing it, or a waiting mark (a clock, which keeps saying the map waits), asks whether to stop that download. Off Wi-Fi, a download first asks whether to download now with mobile data (for the whole queue) or wait for Wi-Fi. While the app may not post notifications, a download button first asks for that permission, then downloads whatever the answer. If a folder cannot be read it says why, with a button to try again. In a sub-folder, its back button and the system back gesture go up one folder; in the maps folder, its back button calls `onBack`, and the system back gesture is the app's. The app adds it as a destination of its own. |
| `MapDownloadServer` | Where the maps are downloaded from: an `http(s)` folder address ending in `/`, served as plain directory listings. Defaults to `DEFAULT_URL`, the mapsforge server. An app may provide its own through Hilt (e.g. a mirror); `:map` uses it if bound (`@BindsOptionalOf`). |
| `MapLocationSource` | Input port: a `Flow` of `:geo` `LocationFix`es. The app implements it and binds it with Hilt. Only the position and the bearing are used. A fix without a bearing keeps the last one. |
| `MapSettings` | `zoomLevel` and `tilt`, with documented defaults. |
| `MapZoomLevel` | Web map zoom level in `2..20` (VTM's limits), default 18. `of` rejects values out of range, `coerce` clamps them. |
| `MapTilt` | Tilt in degrees in `0..65` (VTM's limits), default 60. `of` rejects values out of range and `NaN`, `coerce` clamps them (`NaN` becomes the default). |
| `MapSettingsSource` | Input port. The app implements it and binds it with Hilt. |

`:map` exposes `:geo` and `kotlinx-coroutines-core` as `api` dependencies, because their types appear in its public API.

## Inputs and outputs

- **Inputs:** a `MapLocationSource` and a `MapSettingsSource`, which the app must bind in `SingletonComponent`. Without them the Hilt graph does not compile as soon as `MapViewer` is used. Optionally, a `MapDownloadServer`. The map files, in the app's `map_files` folder (see Persisted state). The download site's folder listings and maps, over HTTP.
- **App set-up:** the app must give WorkManager Hilt's worker factory, so `:map`'s download worker is injected: make the `Application` a `Configuration.Provider` that returns `Configuration.Builder().setWorkerFactory(hiltWorkerFactory).build()`, and remove WorkManager's default start-up from the manifest (the `androidx.work.WorkManagerInitializer` meta-data of `androidx.startup.InitializationProvider`, with `tools:node="remove"`). Without it, downloads fail as WorkManager cannot build the worker.
- **Permissions** (declared by `:map`): `INTERNET`; `ACCESS_NETWORK_STATE`, to know whether the device is on Wi-Fi; `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC` for the download service, whose type is set on WorkManager's `SystemForegroundService`; `POST_NOTIFICATIONS` for the download notification, which `MapDownloader` asks for when a download button is pressed while it is not granted. Refusing it does not stop the download: the notification is just not shown. Android stops showing the request after the user refuses it twice.
- **Receiver** (declared by `:map`, not exported): `MapDownloadCancelReceiver`, the target of the notification's cancel action. It is a Hilt `@AndroidEntryPoint`, so it works in any Hilt app.
- **Outputs:** none besides the UI, the download notification, and the `onManageMaps` callback of `MapViewer` and `onDownloadMaps` of `MapManager`.

## Settings

| Setting | Type | Default | Effect |
|---|---|---|---|
| `zoomLevel` | `MapZoomLevel` | 18 | How close the map is shown. Each level shows the ground twice as large as the one before. |
| `tilt` | `MapTilt` | 60° | How much the map leans towards the horizon. 0° looks straight down. |

A read-only map always shows these values. An interactive map starts with them and goes back to them whenever they change; in between, the user's gestures are kept. If the settings flow throws, `:map` logs it and uses the defaults. In Roadbook Navigator they are fixed to the defaults for now (`:settings`' `FixedMapSettingsSource`).

## Persisted state

No DataStore. The map files live in `map_files` inside the app's **no-backup** folder (`Context.noBackupFilesDir`), e.g. `/data/user/0/<applicationId>/no_backup/map_files`. The folder is created when the map is first shown. Files with the `.map` extension (any case) in it and in its sub-folders are drawn; anything else is ignored.

Downloads keep the site's folders (`europe/italy/centro.map`). While a map downloads, it is a `.map.part` file next to where it goes, which the listings ignore; its modification time is the site's `Last-Modified`, so the next try can resume it. An installed map's modification time is also the site's `Last-Modified`. The queue of downloads, their progress and their failures are kept by WorkManager in its own database, so they survive the process being stopped and the device restarting.

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
- **The listings follow the folder.** `MapFolderChanges` counts the changes `:map` makes to the map folder (deletions and installed downloads), and `MapFileRepository.installedFiles` looks for the files again after each one, without completing. The viewer, the manager and the downloader's installed marks all follow it, so a map downloaded in the background is drawn at once, even on a dashboard that stays open. Files copied by hand are seen the next time a screen is shown.
- **A small listing parser, not an HTML library.** The download site's folders are Apache autoindex pages, generated by the server and regular in form, so `DirectoryListingParser` reads their table rows with regular expressions. Jsoup was considered and left out: it would not make the parser more robust to a change of layout, and it adds a dependency. Real listings saved from the site are its test fixtures. A page that is not a listing is `UNREADABLE_LISTING`, never a crash.
- **One folder at a time.** The site nests continents, countries and regions, so the downloader reads only the folder being shown, never the whole site. Folders come first, then maps, each in the site's order; other files are left out.
- **The site's names are untrusted.** They will become file and folder names on the device when maps are downloaded, so names that could leave the map folder (`..`, separators, control characters) are dropped by the parser and rejected by `RemoteFolderPath`.
- **Listed dates and sizes are approximate.** The site rounds sizes (`2.4M`, in powers of 1024) and lists dates in its own time zone (8 hours ahead of the files' HTTP `Last-Modified`), so the screen shows only the day. Exact sizes and times will come from the download itself.
- **Installed means the same folder and name.** Downloads will keep the site's folders (`europe/italy/centro.map`), so a map is marked as installed when a file of the same name is in the same folder of `map_files`. A map copied by hand to another folder is not marked.
- **WorkManager, one chain, one download at a time.** Downloads are `MapDownloadWorker`s in one unique work chain (`map_downloads`, `APPEND_OR_REPLACE`), so they run one at a time in the order they were started. Parallel downloads were considered and left out: they share the bandwidth, so every map is ready later; each would check the free space alone, so two could pass and both run out of it; and the mapsforge server is a free community service. WorkManager keeps the queue across process deaths and restarts and retries with an exponential backoff from 30 s. Each work is tagged with its map (`map_download:<path>`) and its place in the queue (`map_queue_order:<n>`), because `WorkInfo` does not show a work's input; starting a map already queued or running does nothing (besides the mobile data answer).
- **Mobile data: asked each time, for the whole queue.** Works wait for Wi-Fi (`NetworkType.UNMETERED`). Off Wi-Fi (`NET_CAPABILITY_NOT_METERED` missing on the active network, or no network), a download asks first, as the rider may be in a hurry. "Download now" allows mobile data (`NetworkType.CONNECTED`) for the new map **and every map already queued** (`WorkManager.updateWork`; a running download keeps its network until its next try): a map allowed mobile data behind one waiting for Wi-Fi would otherwise wait too, as the queue runs in order. "Wait for Wi-Fi" only affects the new map. The question is kept in the `SavedStateHandle`.
- **Cancelling rebuilds the queue.** WorkManager cannot take one work out of a chain: cancelling it cancels every work after it, and `APPEND_OR_REPLACE` on a chain whose end is cancelled deletes the whole chain, running work included (checked in WorkManager 2.12's `EnqueueRunnable`). So `cancel` cancels the whole queue, throws away the cancelled map's `.part` file, and queues the other maps again with their order and network. A download that was running before it stops for a moment and resumes from its `.part` file. `HttpMapFileDownloader` writes under a lock, so throwing a `.part` file away waits until a stopping download has closed it. Rebuilding the queue also drops the failures kept by WorkManager.
- **Stopping asks first on screen, not in the notification.** On screen, pressing a waiting mark or a progress opens a dialog (it throws away what was downloaded, AGENTS.md §5.3). The notification's cancel action, a labelled button of the system, stops it at once.
- **Asking for notifications, not gating on them.** The permission is optional, so it is asked with the standard `RequestPermission` contract when a download button is pressed, and the download starts after the answer, whatever it is. `:core`'s `PermissionGate` was not used: it hides its content until a permission is granted. A non-blocking request in `:core` can come when a second module needs one.
- **Failing for good still succeeds as work.** In a chain, a failed work cancels every work after it. So a download that fails for good (`MapDownloadFailure`) ends as a successful work with the reason in its output, and the rest of the queue runs. Failures that may go away (a lost connection, a busy server: `408`, `429`, `5xx`) are retried. WorkManager keeps finished work for about a day, so a map's last failure is shown until the user starts it again; a map that is installed shows as installed, whatever older failure is kept.
- **Resuming safely.** Maps are up to a few gigabytes, and the mapsforge server supports ranges, so a try resumes the `.part` file with `Range` and `If-Range`. The `.part` file's modification time holds the site's `Last-Modified` while it waits, which is what `If-Range` sends: if the map changed on the site, the site sends the whole new map, and two versions are never mixed. A site without `Last-Modified` cannot be resumed safely, so its `.part` file is deleted when a try stops. A partial answer from the wrong byte, or `416`, starts again from scratch.
- **Installed whole or not at all.** A complete `.part` file is checked with `VtmMapFileChecker` and only then moved to its `.map` name (an atomic move in the same folder, replacing an older map). A map that fails the check is deleted and reported as `UNREADABLE_MAP`, so a broken download never blanks the map, and an older map of the same name is kept.
- **Space checked first.** Before writing, the size left to download is compared with `StorageManager.getAllocatableBytes`, which also counts cached data the system would clear; if it does not fit, the download fails as `NOT_ENOUGH_SPACE` and its `.part` file is deleted.
- **Foreground, with a fallback.** The worker runs as a `dataSync` foreground service with a quiet notification (its own low-importance channel, the map's name, a progress bar and the sizes, opening the app when tapped), so Android does not stop a long download. Android may refuse to start a foreground service from the background (e.g. for a retry): the worker then downloads without it, and if it is stopped, the next try resumes. Progress is reported about each percent, to WorkManager and to the notification.
- **Network and storage failures are kept apart.** Both are `IOException`s; file-system failures are wrapped while writing, so they end as `STORAGE_ERROR` instead of being retried forever.
- **Its own HTTP client.** `:map` provides an `OkHttpClient` qualified with `MapHttpClient`, so an app that provides its own client does not clash with it. Requests run on OkHttp's threads, and cancelling the coroutine cancels the call.

## Testing notes

- `VtmMapFileCheckerTest` and `LocalMapFileRepositoryIntegrationTest` use real VTM on small map files written by `writeMapFile` (`TestMapFiles.kt`): a valid mapsforge header and an empty sub-file, so no real map has to be shipped. It also writes damaged variants (wrong magic bytes, unsupported version, truncated).
- `LocalMapFileRepositoryTest` uses a `TemporaryFolder` and a fake checker. The repository's dispatcher must share `runTest`'s scheduler (`StandardTestDispatcher(testScheduler)`), or `flowOn` never runs and the test hangs.
- `LocalMapFileRepositoryTest` also covers deleting: files in sub-folders, files already gone, paths outside the folder (direct or with `..`), files that are not maps, a folder the file system will not change (`setWritable(false)`), and a path with a NUL character, which `java.io.File` rejects with an `IOException`.
- `MapViewModelTest` uses `FakeMapFileRepository`, `FakeMapLocationSource` (which counts its collectors), `FakeMapSettingsSource`, `FakeLogger` and `MainDispatcherRule`.
- `MapCameraTest` runs VTM's pure-Java `MapPosition` on the JVM, and checks that `MapZoomLevel` and `MapTilt` match VTM's limits.
- `MapManagerViewModelTest` uses `FakeMapFileRepository`, which emits its `result` again each time it is set (as the real repository does after a change), and whose deletions answer a chosen `DeleteMapResult` and do not change the listed files.
- `MapManagerContentTest` runs on Robolectric. It finds rows by `MapManagerTestTags.file(file)`, and the icon-only buttons and marks by their tags inside a row; sizes are compared with `Formatter.formatShortFileSize` in the test's locale.
- `MapViewerContentTest` runs on Robolectric (SDK 35) with a placeholder in the map slot. `VtmMap` needs OpenGL and VTM's native library, which neither Robolectric nor previews have, so it is checked on a device: copy a map file with `adb push spain.map /data/local/tmp/` then `adb shell run-as dev.giste.roadbooknavigator sh -c 'mkdir -p no_backup/map_files && cp /data/local/tmp/spain.map no_backup/map_files/'`, and open the dashboard again. Maps are at https://download.mapsforge.org/maps/v5/.
- `DirectoryListingParserTest` reads `mapsforge_listing_root.html` and `mapsforge_listing_europe.html` (in `src/test/resources`), saved from the mapsforge server on 2026-11-24, and small hand-written listings for the edge cases (links elsewhere, other files, unsafe or encoded names, sizes and dates that cannot be read).
- `HttpRemoteMapRepositoryTest` uses OkHttp's `MockWebServer`, a local server, so no test uses the network: request paths, listings, error statuses, a page that is not a listing, an unreachable server, a connection lost while reading, and cancellation (seen through an `EventListener`).
- `MapDownloaderViewModelTest` uses `FakeRemoteMapRepository` (whose `gate` holds calls to check the loading state), `FakeMapFileRepository`, `FakeMapDownloadRepository` (which records the maps started, with their mobile data answer, and cancelled), a lambda `NetworkConnection`, and a `SavedStateHandle` to check that the folder and the mobile data question survive the process.
- `HttpMapFileDownloaderTest` uses `MockWebServer`, a `TemporaryFolder` and a fake checker. It checks the requests (`Range`, `If-Range`), resuming, a map changed on the site, partial answers from the wrong byte or without a range, `416`, answers that end early, a connection lost mid-body (`SocketEffect.CloseSocket`), an unreachable site, error statuses, free space, unreadable maps, replacing an older map, storage failures, the progress reports and cancellation. MockWebServer does not serve ranges itself: each test queues the answer the site would give. The cancellation test runs the download on a real thread, with a slow body (`throttleBody`), and waits for its request with a timeout so a broken download fails instead of hanging. `HttpMapFileDownloaderIntegrationTest` checks that VTM's own checker accepts a valid map while it still has its `.part` name.
- `MapDownloadWorkerTest` runs the worker with `work-testing`'s `TestListenableWorkerBuilder`, a fake downloader, and progress and foreground updaters that record what the worker sets (and can refuse the foreground). `MapDownloadNotificationsTest` reads the notification's extras on Robolectric.
- `WorkManagerMapDownloadRepositoryTest` uses `WorkManagerTestInitHelper`: the queued works never run, as the test WorkManager waits for Wi-Fi. It checks the network of each work (mobile data for the whole queue), the order of the queue (from the order tags, and `ENQUEUED` then `BLOCKED`), and cancelling at the start, in the middle and at the end, with a fake downloader that records the discarded maps. `toDownloads` is tested on `WorkInfo`s built directly. Its fallbacks for a failing WorkManager database are not tested: WorkManager offers no way to make it fail.
- `MapDownloaderContentTest` finds the waiting mark and the progress in the unmerged tree, as they sit inside the button that stops the download.
- The notification permission request lives in the stateful `MapDownloader`, so it is checked on a device: with notifications not allowed, press a download button, answer the system dialog, and the download starts.
- `:app`'s `MapDownloadCancelIntegrationTest` sends the notification's cancel broadcast to the real receiver, injected by the real Hilt graph, and checks the queue.
- `StorageManagerFreeSpaceTest` is an instrumented test (`src/androidTest`): Robolectric's storage manager has no volumes. Run it with `./gradlew :map:connectedDebugAndroidTest` on a device.
- Downloads were not run end to end in tests (no network): check them on a device from the map downloader, e.g. with `andorra.map` (2.4 MB) on Wi-Fi.
- `MapDownloaderContentTest` runs on Robolectric with a `ComponentActivity`, so it can press the system back button. Folder rows are clickable and merge their text, so their text is found in the unmerged tree.
- `:core` does not share its test doubles, so `FakeLogger` lives in this module's test source set.
