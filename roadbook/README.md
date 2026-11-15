# :roadbook

Imports a Rally Navigator roadbook definition file (`.rn2`) and turns its waypoints and moderators into a scrollable on-screen roadbook used to navigate the route. `:roadbook` depends only on `:core` and `:geo`.

`:roadbook` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.roadbook`, and it contains nothing specific to Roadbook Navigator, so other rally apps can depend on it too. Its domain is the roadbook, so it uses rally terms (*roadbook, waypoint, moderator, partial, total, auto-advance, tulip, CAP/heading*). It must not get app-specific defaults or concepts from outside that domain: the app decides what to do with its events (e.g. how to change the odometer).

> **Status:** being migrated from the previous project step by step. The public API contract and the domain model are defined, but nothing implements the API yet: there is no `.rn2` parsing, no persistence, `RoadbookController` is not bound with Hilt, and there is no screen. Every section below is updated as each part lands.

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
│   ├── roadbook/   Roadbook (aggregate root), Waypoint, WaypointNumber, DangerLevel
│   └── drawing/    Element, Track, Road, RoadType, Icon, IconType, Text, CanvasPoint
│                   (drawn in the tulip and notes columns)
├── data/           Not created yet
├── presentation/   Not created yet
└── di/             Not created yet
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

## Testing notes

- `ShortDistanceThreshold` and `RoadbookSettings` defaults are unit-tested in `src/test`.
- The domain model is unit-tested in `src/test`: every `Roadbook` invariant (accepted and broken), `Waypoint.partialToSet` with and without a reset, and the validation of `WaypointNumber`, `CanvasPoint`, `Icon` and `Text`. `testWaypoint()` in `domain/roadbook` builds valid waypoints for other tests. `Track` and `Road` have no logic of their own, so they are not tested.
