# :odometer

Computes and keeps the **trip** and **total** distance travelled (and possibly speed, bearing, etc.) from location fixes. `:odometer` depends only on `:core` and `:geo`.

`:odometer` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.odometer`, and it contains nothing specific to Roadbook Navigator, so other apps can depend on it too.

> **Status:** skeleton. The public API, domain, data sources and tests are not implemented yet. Update every section below when they land.

## Responsibility

- Accumulate trip and total distance from the location fixes it receives through its input port, filtering out fixes that would add noise.
- Let the app reset or set the trip distance and reset the total, on behalf of the user or of another module.
- Persist its own runtime state so the distances survive process death and restarts (AGENTS.md §7).

It has **no UI**. It exposes the odometer state as a flow, and each app decides how to show it (numbers, a progress bar, …). It does **not** read the location itself: the app adapts its location source (in Roadbook Navigator, `:location`) to the input port that `:odometer` declares (AGENTS.md §4.2). It does **not** store its settings either: the app provides them through an `OdometerSettingsSource` (AGENTS.md §4.4; in Roadbook Navigator, `:settings` does).

## Terminology

Rally concepts stay out of reusable modules (AGENTS.md §4.1), so `:odometer` uses neutral terms:

| `:odometer` | Roadbook Navigator |
|---|---|
| trip distance | partial |
| total distance | total |

`:app` translates between them, e.g. it feeds the trip distance to `:roadbook` as the partial.

## Package layout

```
dev/giste/odometer/
├── *.kt            Public API only (to do)
├── domain/         Entities, value objects, repository interfaces, use cases (to do)
├── data/           Repository implementations, DataStore data sources (to do)
└── di/             Hilt modules (to do)
```

There is no `presentation` package, because `:odometer` has no UI. The `.gitkeep` files only keep the empty packages in Git. Delete each one when its package gets its first source file.

## Public API

To do.

## Inputs and outputs

- **Inputs:** location fixes through an input port declared by this module (to do), and `OdometerSettings` through an `OdometerSettingsSource` (to do). Commands such as resetting the trip distance (to do). In Roadbook Navigator, `:app` calls them, for example when `:roadbook` loads a new roadbook.
- **Outputs:** an odometer state flow with the trip and total distance (to do). In Roadbook Navigator, `:app` shows it and feeds the trip distance to `:roadbook` for waypoint auto-advance.

## Settings

To do.

## Persisted state

To do. One DataStore file named `odometer_state` (AGENTS.md §7).

## Design decisions

To do.

## Testing notes

To do. Use a `FakeXxx` for the location input port and the state repository, and inject clocks, so tests never depend on a real location or real time (AGENTS.md §10.2).
