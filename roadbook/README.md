# :roadbook

Imports a Rally Navigator roadbook definition file (`.rn2`) and turns its waypoints and moderators into a scrollable on-screen roadbook used to navigate the route. `:roadbook` depends only on `:core` and `:geo`.

`:roadbook` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.roadbook`, and it contains nothing specific to Roadbook Navigator, so other rally apps can depend on it too. Its domain is the roadbook, so it uses rally terms (*roadbook, waypoint, moderator, partial, total, auto-advance, tulip, CAP/heading*). It must not get app-specific defaults or concepts from outside that domain: the app decides what to do with its events (e.g. how to change the odometer).

> **Status:** being migrated from the previous project step by step. The public API contract, the domain model, the `.rn2` parser and the import (reading the file, parsing it and saving the roadbook) and the persisted position are in place. Nothing implements `RoadbookController` yet, so it is not bound with Hilt, and there is no screen. Every section below is updated as each part lands.

## Responsibility

- Import `.rn2` files into a `Roadbook` aggregate that owns its waypoints and their moderators.
- Draw the waypoints and moderators as a scrollable roadbook, and persist its position.
- Emit `RoadbookEvent`s for `:app` to react to.
- Let `:app` move the roadbook one waypoint forward or back (e.g. from a handlebar remote control).
- Later, once the whole project is migrated: auto-advance through the waypoints from a `Flow` of partial distances that `:app` feeds from the `:odometer` trip distance (AGENTS.md §4.2). This is new logic, not in the previous project.

## Package layout

```
dev/giste/roadbook/
├── RoadbookController, RoadbookEvent, ImportRoadbookResult,
│   RoadbookSettings, RoadbookSettingsSource,
│   ShortDistanceThreshold                                ← public API
├── domain/
│   ├── roadbook/   Roadbook (aggregate root), Waypoint, WaypointNumber, DangerLevel,
│   │               RoutePoint + toWaypoints, RoadbookParser + ParseRoadbookResult,
│   │               RoadbookSource, RoadbookRepository + SavedRoadbook, ImportRoadbookUseCase,
│   │               RoadbookPositionRepository
│   └── drawing/    Element, Track, Road, RoadType, Icon, IconType, Text, CanvasPoint,
│                   tulipExitPoint (drawn in the tulip and notes columns)
├── data/
│   ├── rn2/        Rn2Dto (file DTOs), Rn2IconTypes, Rn2ElementMapper, Rn2RoadbookParser
│   ├── source/     ContentResolverRoadbookSource + its factory
│   └── persistence/ StoredRoadbookDto (saved format), StoredRoadbookMapper,
│                   RoadbookFileSerializer, DataStoreRoadbookRepository,
│                   RoadbookStateDataStore, DataStoreRoadbookPositionRepository
├── presentation/   Not created yet
└── di/             RoadbookModule
```

## Public API

| Type | What it is |
|---|---|
| `RoadbookController` | Singleton facade for the app, independent of the screen. `events: SharedFlow<RoadbookEvent>` (hot, no replay, never completes), `suspend` `importRoadbook(uri: Uri): ImportRoadbookResult`, and `suspend` `moveToNextWaypoint()` / `moveToPreviousWaypoint()`. Moving does nothing at either end or without a roadbook, and persists the new position. Not implemented yet. |
| `ImportRoadbookResult` | Sealed. `Success`, or `Failure(reason: ImportRoadbookFailure)`: `UNREADABLE` (the file cannot be opened or read), `MALFORMED`, `NO_WAYPOINTS`, or `NOT_SAVED` (valid, but it could not be written to storage). A failed import keeps the previous roadbook. |
| `RoadbookEvent` | Sealed. `Loaded`: a new roadbook was imported. `SetPartialRequested(partial: Distance)`: the user long-pressed a waypoint's distance to set the partial. |
| `RoadbookSettings` | Immutable settings: `shortDistanceThreshold`, default `ShortDistanceThreshold.DEFAULT`. |
| `RoadbookSettingsSource` | Input port for the settings flow, implemented by the app (in Roadbook Navigator, `:settings`). |
| `ShortDistanceThreshold` | Value object over a `:geo` `Distance`, always within `RANGE` (50 m – 1 km), default 250 m. `of()` requires a value in range, `coerce()` clamps one that may not be. |

The screen entry point is not defined yet. It is added with the screen.

## Inputs and outputs

- **Inputs:** `RoadbookSettingsSource` (settings), and `RoadbookController.importRoadbook()` (a `content://` or `file://` URI, usually from the system file picker) and `moveToNextWaypoint()` / `moveToPreviousWaypoint()` (commands). The partial distance input for auto-advance comes later.
- **Outputs:** `RoadbookController.events`. In Roadbook Navigator, `:app` reacts to them:
  - `Loaded` → reset both the partial and the total (`OdometerController.resetAll()`), as the previous app did.
  - `SetPartialRequested` → set the partial to `partial` (`OdometerController.setTrip()`).

## Settings

| Setting | Type | Default | Range |
|---|---|---|---|
| `shortDistanceThreshold` | `ShortDistanceThreshold` | 250 m | 50 m – 1 km |

A waypoint whose distance from the previous one is shorter than the threshold is highlighted, to warn that it comes soon.

## Persisted state

| File | Content | Written by |
|---|---|---|
| `roadbook_route.json` (typed DataStore, in the app's `files/datastore/`) | The current roadbook, as versioned JSON (`{"version": 1, "roadbook": {…}}`). Empty or missing when no roadbook was imported. | Every successful import replaces it. |
| `roadbook_state` (Preferences DataStore) | `top_waypoint_number` (`Int`): the position, the number of the waypoint shown at the top. Missing means the first waypoint. | Every change of position, at once. |

## Design decisions

- **Events come from a singleton, not from the screen.** In the previous project they came from the ViewModel and were collected in the dashboard composable, so they only existed while the roadbook was shown. A singleton `RoadbookController` lets a coordinator in `:app` collect them in the application scope (AGENTS.md §4.3).
- **Events say what was asked, not which gesture asked it.** `SetPartialRequested` (previously `DistanceSectionLongPressed`) keeps `:app` independent of the UI. Which partial a waypoint gives (its distance, or zero at a waypoint that resets the partial) is decided in `:roadbook`.
- **`Loaded` is only emitted after an import**, not when the persisted roadbook is restored after a restart, so the odometer is not reset at every app start.
- **The threshold range lives in `:roadbook`.** Unlike `:odometer`, which leaves ranges to each app, 50 m – 1 km is a property of roadbooks, so `ShortDistanceThreshold` enforces it and `:settings` uses its `RANGE` for its slider. `coerce()` keeps out-of-range stored values from crashing.
- **Distances are `:geo` `Distance`s**, not raw `Double`/`Long` meters as in the previous project.
- **No module-specific logger:** `:roadbook` uses the `:core` `Logger` instead of the previous `RoadbookLogger`.
- **Colors and dimensions are not public API.** The previous `RoadbookColors` and `RoadbookDimensions` were public but never customized by the app. They become internal and use `:core` theme tokens when the UI is migrated.
- **The roadbook name is not exposed** until `:app` needs it.

### Domain model

- **`Roadbook` is the aggregate root** (previously `Route`, renamed to the ubiquitous language). It is immutable and checks its invariants on creation: at least one waypoint, numbered 1…n in order with no gaps, and the first one has no distance from a previous one. The distances are not checked against each other, because waypoints that are not shown still add distance.
- **An empty roadbook is invalid.** A file without waypoints is reported as an import failure instead of producing an empty roadbook.
- **`Waypoint.partial` is the partial**, the distance since the last waypoint that resets it (or since the start). The previous `distance` field held the same value, but its documentation called it the total from the start. `distanceFromPrevious` counts from the previous waypoint shown, including waypoints that are not shown.
- **`partialToSet` is domain logic.** The partial the odometer should show at a waypoint (`partial`, or zero when it `resetsPartial`) used to be computed in the long-press handler of a composable. It now lives in the model, so the UI only emits it.
- **Typed values instead of raw ones:** `WaypointNumber` (at least 1), `GeoPoint` and `Altitude` from `:geo` (previously a local `Coordinates` with `elevation`), and `Distance` from `:geo` (previously a local `Distance` in whole meters). The altitude is `null` when the file does not give one.
- **Drawing elements describe the tulip and notes, not the file.** `Element` is a sealed interface, so the previous `elementType` field, which repeated the subtype, is gone. Each column has its own element type: a tulip holds `TulipElement`s (tracks, roads, icons and texts) and a notes column `NoteElement`s (only icons and texts), so a road in the notes cannot be represented. The previous app used one element list for both. The `.rn2` numeric road type ids (4, 12, 15, …) belong to the parser, not to `RoadType`. Canvas positions are `CanvasPoint`s (previously `Point`), so they are never confused with a `GeoPoint`.
- **Icon sizes keep their fractions.** The previous `Icon` stored `width` and `height` as `Int`s, but `.rn2` files have fractional sizes (e.g. 35.67), so they are `Double`s now. Sizes must be positive and scales non-zero (a negative scale mirrors the symbol). Values the domain rejects must be handled by the parser.

### `.rn2` parsing

- **Format details stay in `data/rn2`, route logic in the domain.** The parser (`Rn2RoadbookParser`, bound as `RoadbookParser`) decodes the JSON with kotlinx.serialization and maps it to `RoutePoint`s: every point of the file, shown or not. The previous `WaypointProcessor` logic that turns a route into waypoints (partials, resets, hidden points, numbering) is `toWaypoints()` in the domain. Rally Navigator's symbol ids and names, road type ids and drawing defaults are only known to `data/rn2`.
- **Distances use `GeoPoint.distanceTo`** (WGS84, Vincenty), and the exit road of a tulip uses `GeoPoint.bearingTo`. The previous app used the spherical haversine formula, so distances differ by up to about 0.5 %. They are no longer rounded to whole meters: rounding is a matter of display.
- **Failures are typed, never thrown.** A file that is not valid JSON, lacks required fields (a point's `lat`, `lon` or `show`) or has an impossible position is `MALFORMED`. A file with no shown point is `NO_WAYPOINTS`. Decoding errors are caught at the data boundary, as kotlinx.serialization reports them all as `IllegalArgumentException`s.
- **One odd element does not reject a roadbook.** Elements of unknown type (e.g. from a newer Rally Navigator) and elements whose values cannot be drawn (e.g. an icon of zero width) are skipped with a warning. The previous app rejected the whole file for an unknown element type, and drew invalid values as they came.
- **One icon DTO.** The previous app had a DTO class per symbol (37 identical classes) to tell them apart. Now one `Rn2Icon` DTO is looked up by id, then by name, then falls back to `UNKNOWN`. Speed limits are matched by name only: the previous app's speed limit ids were placeholders that were never used.
- **Behavior kept from the previous app:**
  - A point resets the partial when its notes column has the reset symbol, even if it is not shown. A shown point's danger level is that of the first danger symbol in its notes.
  - A track's road out aims at the next point of the route (shown or not). It goes straight on (0, -55) when there is no next point.
  - Roads of a track without a kind inherit it: the road in takes the kind the last shown tulip left on, and the road out takes the road in's.
  - Defaults: the road in ends at (0, 35) and the road out starts at the center, a text's line height is 1, and its box may grow to 180 by 100. A symbol is 50 units wide.
- **Small changes from the previous app:**
  - Icon angles are rounded instead of truncated.
  - Roads and tracks in a notes column are skipped with a warning, instead of being drawn there.
  - A missing `ele` gives no altitude, instead of 0 m.
  - A text without a `fontSize` uses 18, instead of rejecting the file.
  - An icon with a scale on only one axis keeps its square size `w`, and both scales are 1. The drawn size is the same as before.

### Import and persistence

- **The public API takes a `Uri`, the domain a `RoadbookSource`.** The app gets a `Uri` from the file picker anyway, and opening it is where most failures happen (a file that is gone, a lost permission). Taking the `Uri` keeps those failures inside `:roadbook`, at the data boundary, with the parsing failures: the app gets one `ImportRoadbookResult`. The domain does not see the `Uri`: `ImportRoadbookUseCase` reads a `RoadbookSource`, a `fun interface` that returns the file's text, so it has no Android types and tests pass a lambda. `ContentResolverRoadbookSource` implements it in `data`. Kotlin has no URI type, and `java.net.URI` rejects some content URIs, so no URI type is converted.
- **The roadbook is saved, not the file.** As in the previous app, the parsed roadbook is saved as JSON and read back at start, instead of saving the `.rn2` text and parsing it again. Reading it is faster (only the shown waypoints, already measured), and above all the restored roadbook is always the one the user was navigating: a later version that maps `.rn2` files differently cannot renumber the waypoints or change the partials in the middle of a rally. The cost is that a fix in the `.rn2` mapping only applies after importing the file again.
- **The saved format is separate from the domain.** `StoredRoadbookDto` mirrors the domain, so the domain has no serialization annotations and a domain refactor does not change the format by accident. Every field is written, null included, and required on read. Enum values are stored by the name of their domain entry. `RoadbookFileSerializerTest` compares the written JSON with `src/test/resources/stored_roadbook_v1.json`, so a change of format fails a test.
- **The format is versioned.** A file of a version the serializer does not know, damaged JSON, or values that break a domain invariant are read as `SavedRoadbook.Unreadable`, never thrown: the serializer does not throw `CorruptionException`, which would make DataStore silently replace the file. The file is kept until the next import replaces it. A change of format must raise `CURRENT_VERSION` and either migrate the previous version or let it be read as unreadable.
- **A roadbook that cannot be saved is not imported** (`NOT_SAVED`), instead of being kept only in memory, so the user knows it would be lost on the next start.
- **The position is the waypoint at the top, nothing more.** The previous app saved the index of the first visible list item and its scroll offset in pixels. The offset depends on the screen (size, density, font size, orientation), so it is transient UI state, not roadbook state (AGENTS.md §7): after a restart, the roadbook shows the saved waypoint at the top. The screen may keep its exact scroll in `SavedStateHandle`.
- **The position has its own file.** It changes often, so it is a small Preferences DataStore (`roadbook_state`) instead of a field of `roadbook_route.json`, which would be rewritten on every move. Both files are not written atomically together, so the position may not belong to the roadbook (e.g. a crash between saving an imported roadbook and resetting the position): whoever uses the position keeps it within the roadbook's waypoints.
- **The position repository does not know the roadbook.** It only stores a valid `WaypointNumber`: a stored value below 1 is read as the first waypoint with a warning. Keeping it within the waypoints of the current roadbook is not its job.
- **Losing a position is not worth failing for.** `DataStoreRoadbookPositionRepository` keeps the position in memory, so a failed write is logged and the next change writes it again, and a failed read starts from the first waypoint. A corrupted file is replaced with an empty one. It is a singleton, because it holds the position.
- **The typed DataStore comes with `androidx-datastore-preferences`,** which depends on it. The catalog has no separate entry for it.

## Testing notes

- `ShortDistanceThreshold` and `RoadbookSettings` defaults are unit-tested in `src/test`.
- The domain model is unit-tested in `src/test`: every `Roadbook` invariant (accepted and broken), `Waypoint.partialToSet` with and without a reset, and the validation of `WaypointNumber`, `CanvasPoint`, `Icon` and `Text`. `testWaypoint()` in `domain/roadbook` builds valid waypoints for other tests. `Track` and `Road` have no logic of their own, so they are not tested.
- `toWaypoints()` is tested on routes along the equator, where each step has an exact expected length. Vincenty's iteration stops within about 6 µm of the exact geodesic, so distance assertions allow 10 µm per step measured.
- `tulipExitPoint()` is tested with the cardinal directions on the equator (straight on, right, left, U-turn) and the cases without a direction.
- `Rn2ElementMapper`, `Rn2IconTypes` and `Rn2RoadbookParser` are unit-tested with DTOs and inline JSON: every default, inheritance rule, skipped element and failure reason. `FakeLogger` records the warnings.
- `Rn2RoadbookParserIntegrationTest` parses `src/test/resources/route_example.rn2`, a real Rally Navigator export with 4 shown waypoints.
- `ImportRoadbookUseCaseTest` covers every step that can fail with `FakeRoadbookParser`, `FakeRoadbookRepository` and lambda `RoadbookSource`s. `ImportRoadbookIntegrationTest` imports `route_example.rn2` with the real parser and a real DataStore file, and reads it back after reopening the file.
- `ContentResolverRoadbookSourceTest` runs on Robolectric (SDK 35, set in `src/test/resources/robolectric.properties`, because Robolectric does not support compileSdk 37 yet). It reads real `file://` URIs from a temporary folder, and registers what `content://` URIs return to simulate failures. Robolectric only returns a `null` stream through a registered provider, so that case uses a test `ContentProvider`.
- `RoadbookFileSerializerTest` round-trips `FULL_ROADBOOK` (every field and kind of element), checks the version 1 fixture both ways, and every way a file is unreadable. `DataStoreRoadbookRepositoryTest` uses `FakeDataStore` for read and write failures. `DataStoreRoadbookRepositoryIntegrationTest` uses a real DataStore file in a temporary folder.
- `DataStoreRoadbookPositionRepositoryTest` covers stored, missing and invalid positions, read and write failures, and that a saved position is never overwritten by a later first read, with `FakeDataStore`. `DataStoreRoadbookPositionRepositoryIntegrationTest` reopens a real file.
