# :roadbook

Imports a Rally Navigator roadbook definition file (`.rn2`) and turns its waypoints and moderators into a scrollable on-screen roadbook used to navigate the route. `:roadbook` depends only on `:core` and `:geo`.

`:roadbook` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.roadbook`, and it contains nothing specific to Roadbook Navigator, so other rally apps can depend on it too. Its domain is the roadbook, so it uses rally terms (*roadbook, waypoint, moderator, partial, total, auto-advance, tulip, CAP/heading*). It must not get app-specific defaults or concepts from outside that domain: the app decides what to do with its events (e.g. how to change the odometer).

> **Status:** being migrated from the previous project step by step. The public API contract, the domain model and the `.rn2` parser are in place, but nothing implements the API yet: there is no import flow or persistence, `RoadbookController` is not bound with Hilt, and there is no screen. Every section below is updated as each part lands.

## Responsibility

- Import `.rn2` files into a `Roadbook` aggregate that owns its waypoints and their moderators.
- Draw the waypoints and moderators as a scrollable roadbook, and persist its position.
- Emit `RoadbookEvent`s for `:app` to react to.
- Let `:app` move the roadbook one waypoint forward or back (e.g. from a handlebar remote control).
- Later, once the whole project is migrated: auto-advance through the waypoints from a `Flow` of partial distances that `:app` feeds from the `:odometer` trip distance (AGENTS.md §4.2). This is new logic, not in the previous project.

## Package layout

```
dev/giste/roadbook/
├── RoadbookController, RoadbookEvent, RoadbookSettings,
│   RoadbookSettingsSource, ShortDistanceThreshold        ← public API
├── domain/
│   ├── roadbook/   Roadbook (aggregate root), Waypoint, WaypointNumber, DangerLevel,
│   │               RoutePoint + toWaypoints, RoadbookParser + ParseRoadbookResult
│   └── drawing/    Element, Track, Road, RoadType, Icon, IconType, Text, CanvasPoint,
│                   tulipExitPoint (drawn in the tulip and notes columns)
├── data/
│   └── rn2/        Rn2Dto (file DTOs), Rn2IconTypes, Rn2ElementMapper, Rn2RoadbookParser
├── presentation/   Not created yet
└── di/             RoadbookModule
```

## Public API

| Type | What it is |
|---|---|
| `RoadbookController` | Singleton facade for the app, independent of the screen. `events: SharedFlow<RoadbookEvent>` (hot, no replay, never completes), and `suspend` `moveToNextWaypoint()` / `moveToPreviousWaypoint()`. Moving does nothing at either end or without a roadbook, and persists the new position. |
| `RoadbookEvent` | Sealed. `Loaded`: a new roadbook was imported. `SetPartialRequested(partial: Distance)`: the user long-pressed a waypoint's distance to set the partial. |
| `RoadbookSettings` | Immutable settings: `shortDistanceThreshold`, default `ShortDistanceThreshold.DEFAULT`. |
| `RoadbookSettingsSource` | Input port for the settings flow, implemented by the app (in Roadbook Navigator, `:settings`). |
| `ShortDistanceThreshold` | Value object over a `:geo` `Distance`, always within `RANGE` (50 m – 1 km), default 250 m. `of()` requires a value in range, `coerce()` clamps one that may not be. |

The screen entry point is not defined yet. It is added with the screen.

## Inputs and outputs

- **Inputs:** `RoadbookSettingsSource` (settings), and `RoadbookController.moveToNextWaypoint()` / `moveToPreviousWaypoint()` (commands). The partial distance input for auto-advance comes later.
- **Outputs:** `RoadbookController.events`. In Roadbook Navigator, `:app` reacts to them:
  - `Loaded` → reset both the partial and the total (`OdometerController.resetAll()`), as the previous app did.
  - `SetPartialRequested` → set the partial to `partial` (`OdometerController.setTrip()`).

## Settings

| Setting | Type | Default | Range |
|---|---|---|---|
| `shortDistanceThreshold` | `ShortDistanceThreshold` | 250 m | 50 m – 1 km |

A waypoint whose distance from the previous one is shorter than the threshold is highlighted, to warn that it comes soon.

## Persisted state

Not defined yet.

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
- **Drawing elements describe the tulip and notes, not the file.** `Element` is a sealed interface, so the previous `elementType` field, which repeated the subtype, is gone. The `.rn2` numeric road type ids (4, 12, 15, …) belong to the parser, not to `RoadType`. Canvas positions are `CanvasPoint`s (previously `Point`), so they are never confused with a `GeoPoint`.
- **Icon sizes keep their fractions.** The previous `Icon` stored `width` and `height` as `Int`s, but `.rn2` files have fractional sizes (e.g. 35.67), so they are `Double`s now. Sizes must be positive and scales non-zero (a negative scale mirrors the symbol). Values the domain rejects must be handled by the parser.

### `.rn2` parsing

- **Format details stay in `data/rn2`, route logic in the domain.** The parser (`Rn2RoadbookParser`, bound as `RoadbookParser`) decodes the JSON with kotlinx.serialization and maps it to `RoutePoint`s: every point of the file, shown or not. The previous `WaypointProcessor` logic that turns a route into waypoints (partials, resets, hidden points, numbering) is `toWaypoints()` in the domain. Rally Navigator's symbol ids and names, road type ids and drawing defaults are only known to `data/rn2`.
- **Distances use `GeoPoint.distanceTo`** (WGS84, Vincenty), and the exit road of a tulip uses `GeoPoint.bearingTo`. The previous app used the spherical haversine formula, so distances differ by up to about 0.5 %. They are no longer rounded to whole meters: rounding is a matter of display.
- **Failures are typed, never thrown.** A file that is not valid JSON, lacks required fields (a point's `lat`, `lon` or `show`) or has an impossible position is `MALFORMED`. A file with no shown point is `NO_WAYPOINTS`. Decoding errors are caught at the data boundary, as kotlinx.serialization reports them all as `IllegalArgumentException`s.
- **One odd element does not reject a roadbook.** Elements of unknown type (e.g. from a newer Rally Navigator) and elements whose values cannot be drawn (e.g. an icon of zero width) are skipped with a warning. The previous app rejected the whole file for an unknown element type, and drew invalid values as they came.
- **One icon DTO.** The previous app had a DTO class per symbol (37 identical classes) to tell them apart. Now one `Rn2Icon` DTO is looked up by id, then by name, then falls back to `UNKNOWN`. Speed limits are matched by name only: the previous app's speed limit ids were placeholders that were never used.
- **Behavior kept from the previous app:**
  - A point resets the partial when its notes column has the reset symbol, even if it is not shown. A shown point's danger level is that of the first danger symbol in its notes.
  - A track's road out aims at the next point of the route (shown or not). It goes straight on (0, -55) when there is no next point, and in the notes column.
  - Roads of a track without a kind inherit it: the road in takes the kind the last shown tulip left on, and the road out takes the road in's.
  - Defaults: the road in ends at (0, 35) and the road out starts at the center, a text's line height is 1, and its box may grow to 180 by 100. A symbol is 50 units wide.
- **Small changes from the previous app:**
  - Icon angles are rounded instead of truncated.
  - A missing `ele` gives no altitude, instead of 0 m.
  - A text without a `fontSize` uses 18, instead of rejecting the file.
  - An icon with a scale on only one axis keeps its square size `w`, and both scales are 1. The drawn size is the same as before.

## Testing notes

- `ShortDistanceThreshold` and `RoadbookSettings` defaults are unit-tested in `src/test`.
- The domain model is unit-tested in `src/test`: every `Roadbook` invariant (accepted and broken), `Waypoint.partialToSet` with and without a reset, and the validation of `WaypointNumber`, `CanvasPoint`, `Icon` and `Text`. `testWaypoint()` in `domain/roadbook` builds valid waypoints for other tests. `Track` and `Road` have no logic of their own, so they are not tested.
- `toWaypoints()` is tested on routes along the equator, where each step has an exact expected length. Vincenty's iteration stops within about 6 µm of the exact geodesic, so distance assertions allow 10 µm per step measured.
- `tulipExitPoint()` is tested with the cardinal directions on the equator (straight on, right, left, U-turn) and the cases without a direction.
- `Rn2ElementMapper`, `Rn2IconTypes` and `Rn2RoadbookParser` are unit-tested with DTOs and inline JSON: every default, inheritance rule, skipped element and failure reason. `FakeLogger` records the warnings.
- `Rn2RoadbookParserIntegrationTest` parses `src/test/resources/route_example.rn2`, a real Rally Navigator export with 4 shown waypoints.
