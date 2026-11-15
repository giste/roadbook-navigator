# :roadbook

Imports a Rally Navigator roadbook definition file (`.rn2`) and turns its waypoints and moderators into a scrollable on-screen roadbook used to navigate the route. `:roadbook` depends only on `:core` and `:geo`.

`:roadbook` is an **app-specific** feature module (AGENTS.md §4): its package is `dev.giste.roadbooknavigator.roadbook`, and it uses rally terms (*roadbook, waypoint, moderator, partial, total, auto-advance, tulip, CAP/heading*).

> **Status:** skeleton only. The code is being migrated from the previous project step by step. Every section below is updated as each part lands.

## Responsibility

- Import `.rn2` files into a `Roadbook` aggregate that owns its waypoints and their moderators.
- Draw the waypoints and moderators as a scrollable roadbook.
- Emit `RoadbookEvent`s (e.g. *roadbook loaded*, *waypoint distance long-pressed*) for `:app` to react to.
- Auto-advance through the waypoints from a `Flow` of partial distances that `:app` feeds from the `:odometer` trip distance (AGENTS.md §4.2).

## Package layout

```
dev/giste/roadbooknavigator/roadbook/
├── *.kt            Public API (not defined yet)
├── domain/         Not created yet
├── data/           Not created yet
├── presentation/   Not created yet
└── di/             Not created yet
```

## Public API

Not defined yet.

## Inputs and outputs

Not defined yet.

## Settings

Not defined yet.

## Persisted state

Not defined yet.

## Design decisions

None yet.

## Testing notes

None yet.
