# :geo

The shared kernel: framework-free, immutable value objects for geographic positions and measurements. `:geo` depends on no other project module, on no Android API and on no DI framework.

`:geo` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.geo`, and it contains nothing specific to Roadbook Navigator, so other apps can depend on it too.

## Responsibility

- Give every module the same types for primitive geographic and measurement concepts, so no module passes raw `Double`s around for domain quantities (AGENTS.md §5.2).
- Validate every value on creation, so an instance is always valid.

It does **not** hold algorithms that belong to a feature (e.g. how the odometer accumulates distance), nor rally concepts (waypoint, CAP, partial, …).

## Package layout

All types are public API, so they live in the root package. There are no layer packages, no Hilt and no resources (see the `:geo` exception in AGENTS.md §5.1).

```
dev/giste/geo/
├── Distance.kt
├── Speed.kt
├── Bearing.kt
└── GeoPoint.kt
```

## Public API

Package `dev.giste.geo`:

| Type | Purpose |
|---|---|
| `Distance` | Non-negative, finite length, stored in meters. Create with `Distance.meters(…)` / `Distance.kilometers(…)`; read `inMeters` / `inKilometers`. Supports `+`, `-`, comparison and `distance / duration` (a `Speed`). `Distance.ZERO`. |
| `Speed` | Non-negative, finite ground speed, stored in m/s. Create with `Speed.metersPerSecond(…)`, `Speed.kilometersPerHour(…)` or `distance / duration`; read `inMetersPerSecond` / `inKilometersPerHour`. Supports comparison and `speed * duration` (a `Distance`). `Speed.ZERO`. |
| `Bearing` | Direction clockwise from true north, in degrees in `[0, 360)`. Create with `Bearing.degrees(…)`, which wraps any finite angle into range. Not comparable, because directions are circular. `Bearing.NORTH`. |
| `GeoPoint` | WGS84 latitude `[-90, 90]` and longitude `[-180, 180]` in decimal degrees. No altitude. |

Invalid values (`NaN`, infinite, negative, out of range) throw `IllegalArgumentException`. That is a programming error (AGENTS.md §6): data sources must check or map values at the boundary before creating these types.

## Inputs and outputs

None. `:geo` only defines types.

## Settings

None.

## Persisted state

None.

## Design decisions

- **Value classes with private constructors.** `Distance`, `Speed` and `Bearing` are `@JvmInline value class`es, so they cost no allocation in most cases. Their constructors are private, so every instance goes through a validating factory named after its unit, and the unit is always explicit at the call site (`Distance.kilometers(1.5)`).
- **One internal unit.** Each type stores one SI-based unit (meters, m/s, degrees) and converts only on creation or for display.
- **Each unit conversion has one owner.** `Distance` owns length factors (meters per kilometer) and `kotlin.time.Duration` owns time factors. `Speed` keeps no factors: it is defined as distance over time, so km/h is `Distance.kilometers(value) / 1.hours`, and its km/h reading is the distance covered in one hour. This also keeps common conversions exact (90 km/h is exactly 25 m/s), which multiplying by an inexact `3.6` would not.
- **Durations come from the standard library.** `kotlin.time.Duration` is framework-free, so `:geo` defines no time type. Dividing requires a finite, positive duration, and multiplying a finite, non-negative one. Anything else is a programming error.
- **`-0.0` is stored as `0.0`.** Value-class and data-class equality compare `Double`s bit by bit, so `-0.0` and `0.0` would be different values. The factories normalize it, so, for example, `Distance.meters(-0.0) == Distance.ZERO`. `GeoPoint` keeps coordinates as given.
- **`Bearing` wraps instead of rejecting.** Angles from sensors and calculations often fall outside `[0, 360)`. Wrapping is always correct for a direction, so only `NaN` and infinite values are rejected.
- **`Distance` cannot go negative.** `minus` requires the subtracted distance to be shorter or equal. A feature that needs signed differences (e.g. an odometer correction) should model them explicitly.
- **No distance or bearing between two points yet.** The formula is a precision decision: the spherical haversine formula can differ from the WGS84 ellipsoid by up to about 0.5 %, which is significant for a rally odometer. It will be added when the first consumer needs it, with the formula chosen for that consumer.

## Testing notes

Plain JVM unit tests in `src/test`, one class per type. They cover every factory, every rejected value (negative, `NaN`, infinite, out of range, overflow), unit conversions (exact where the arithmetic allows), `-0.0` normalization, bearing wrap-around (including the tiny-negative edge case), arithmetic, distance/duration/speed operators with their rejected durations, and ordering.
