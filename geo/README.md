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
├── Altitude.kt
├── Speed.kt
├── Bearing.kt
├── GeoPoint.kt
└── LocationFix.kt
```

## Public API

Package `dev.giste.geo`:

| Type | Purpose |
|---|---|
| `Distance` | Non-negative, finite length, stored in meters. Create with `Distance.meters(…)` / `Distance.kilometers(…)`; read `inMeters` / `inKilometers`. Supports `+`, `-`, comparison and `distance / duration` (a `Speed`). `Distance.ZERO`. |
| `Speed` | Non-negative, finite ground speed, stored in m/s. Create with `Speed.metersPerSecond(…)`, `Speed.kilometersPerHour(…)` or `distance / duration`; read `inMetersPerSecond` / `inKilometersPerHour`. Supports comparison and `speed * duration` (a `Distance`). `Speed.ZERO`. |
| `Bearing` | Direction clockwise from true north, in degrees in `[0, 360)`. Create with `Bearing.degrees(…)`, which wraps any finite angle into range. Not comparable, because directions are circular. `Bearing.NORTH`. |
| `GeoPoint` | WGS84 latitude `[-90, 90]` and longitude `[-180, 180]` in decimal degrees. No altitude. `distanceTo(other)` is the geodesic distance on the WGS84 ellipsoid (a `Distance`), and `bearingTo(other)` the initial bearing of that geodesic (a `Bearing`, or `null` for the same position). |
| `Altitude` | Finite height above a reference surface, stored in meters. Can be negative. Create with `Altitude.meters(…)`; read `inMeters`. Supports comparison and `verticalDistanceTo(other)` (a non-negative `Distance`). `Altitude.ZERO`. |
| `LocationFix` | One location fix: `position`, `time` (wall clock) and `elapsedRealtime` (monotonic, since boot) are always known. `horizontalAccuracy`, `altitude`, `verticalAccuracy`, `speed` and `bearing` are `null` when unknown. `elapsedRealtime` must be finite and non-negative. |

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
- **`LocationFix` is the shared location type.** Producers (`:location`) and consumers (`:odometer`, `:map`) all speak it, so a consumer declares an input port of `LocationFix`es instead of depending on a location provider, and the app connects them with almost no mapping. It is a plain value with no behavior, which is why it fits the shared kernel while the provider itself does not.
- **Unknown is `null`, not zero.** A platform may report a missing speed or accuracy as `0`. `LocationFix` uses `null` for "unknown", so a real `Speed.ZERO` (stopped) is never confused with "no speed".
- **`Altitude` is its own type, not a `Distance`.** An altitude is a position on the vertical axis, so it can be negative, while a `Distance` is a length and cannot. Mixing them would allow nonsense such as adding two altitudes, so `Altitude` has no `+` or `-`. The only operation is `verticalDistanceTo`, which returns the length between two altitudes as a `Distance`, e.g. the climb between two fixes. A feature that needs a signed climb (up or down) can compare the altitudes.
- **`Distance` cannot go negative.** `minus` requires the subtracted distance to be shorter or equal. A feature that needs signed differences (e.g. an odometer correction) should model them explicitly.
- **Distances and bearings use Vincenty's formula on the WGS84 ellipsoid**, not the spherical haversine formula. The sphere's error depends on latitude and direction, so it does not average out over a route: at 40° N it is +0.14 % north–south and −0.25 % east–west (+14 m and −25 m per 10 km), and at 15° N up to +0.49 % (+49 m per 10 km). That is about the precision a rally rider needs to find a turn. Vincenty's formula is accurate to well under a millimeter and costs about a microsecond per call. It is shared by every module that measures between points (`:odometer`, `:roadbook`), so the odometer and the roadbook's distances agree.
- **Vincenty's formula falls back to the sphere.** Its iteration does not converge for nearly antipodal points. Those never occur between consecutive fixes or waypoints, but the functions must still return a value, so they then use the haversine distance and the spherical initial bearing, with the mean Earth radius (IUGG R1, 6 371 008.8 m).
- **`bearingTo` is `null` for the same position.** No direction leads from a point to itself, and returning north (as `atan2(0, 0)` would) would be a silent, wrong answer.

## Testing notes

Plain JVM unit tests in `src/test`, one class per type. They cover every factory, every rejected value (negative, `NaN`, infinite, out of range, overflow), unit conversions (exact where the arithmetic allows), `-0.0` normalization, bearing wrap-around (including the tiny-negative edge case), arithmetic, distance/duration/speed operators with their rejected durations, and ordering.

`distanceTo` and `bearingTo` are checked against independent references, not against another implementation of the same formula: Vincenty's published reference line (Flinders Peak to Buninyong: 54 972.271 m, 306°52'05.37" forward and 127°10'25.07" back), the exact length of a degree along the equator (the equatorial radius times π/180), the WGS84 meridian quadrant (10 001 965.729 m), and a meridian arc integrated numerically from the radius of curvature. They also cover the same position, the antimeridian, the four cardinal directions and the antipodal fallback.
