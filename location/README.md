# :location

Provides the device location as a `Flow` to other modules. `:location` depends only on `:core`.

> **Status:** skeleton. The public API, data sources and tests are not implemented yet. Update every section below when they land.

## Responsibility

- Read location fixes from the platform and expose them as a `Flow` that other modules consume through `:app` (e.g. `:odometer` and `:map`).
- Map platform errors (permission revoked, location services disabled, provider unavailable) to sealed states. Never throw from the flow (AGENTS.md §6).

It does **not** request runtime permissions. `:core`'s `PermissionGate` does that.

## Package layout

```
dev/giste/roadbooknavigator/location/
├── *.kt            Public API only (to do)
├── domain/         Entities, value objects, repository interfaces (to do)
├── data/           Platform location data sources, mappers (to do)
├── presentation/   ViewModels, UI state, internal composables, if any (to do)
└── di/             Hilt modules (to do)
```

The `.gitkeep` files only keep the empty packages in Git. Delete each one when its package gets its first source file.

## Public API

To do.

## Inputs and outputs

- **Inputs:** `LocationSettings` through a `LocationSettingsSource` (to do, see AGENTS.md §4.3).
- **Outputs:** a location `Flow` (to do).

## Settings

To do.

## Persisted state

None yet.

## Design decisions

To do.

## Testing notes

To do. Use a `FakeXxx` for the platform location source so tests never depend on a real location (AGENTS.md §10.2).
