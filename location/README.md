# :location

Provides the device location as a `Flow` to other modules. `:location` depends only on `:core` and `:geo`.

`:location` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.location`, and it contains nothing specific to Roadbook Navigator, so other apps can depend on it too.

## Responsibility

- Read location fixes from the platform GPS provider and expose them as one shared `Flow` of `LocationEvent`s, which other modules consume through `:app` (e.g. `:odometer` and `:map`).
- Restart the platform request whenever the location settings change.
- Report platform failures (permission missing, no GPS provider) and provider status changes as events. The flow never throws (AGENTS.md §6).

It does **not** request runtime permissions. `:core`'s `PermissionGate` does that. It does **not** store its settings either: the app provides them through `LocationSettingsSource`.

## Package layout

```
dev/giste/location/
├── LocationSource, LocationEvent, LocationError, UserLocation,
│   LocationSettings, LocationSettingsSource                     ← public API
├── domain/         LocationRepository, DefaultLocationSource
├── data/           GpsLocationRepository, LocationMapper
└── di/             LocationModule
```

There is no `presentation` package, because `:location` has no UI yet.

## Public API

Package `dev.giste.location`:

| Type | Purpose |
|---|---|
| `LocationSource` | Inject it (Hilt binds it as a singleton). `events` is a hot `Flow<LocationEvent>` shared by every collector. Updates start with the first collector and stop 5 s after the last one leaves. A new collector first gets the latest event while updates run. It never throws or completes. |
| `LocationEvent` | Sealed: `LocationUpdated(location)`, `ProviderEnabled`, `ProviderDisabled`, `Error(reason)`. |
| `LocationError` | Typed reason of `LocationEvent.Error`: `PERMISSION_DENIED`, `PROVIDER_UNAVAILABLE`, `UNKNOWN`. The UI maps it to a string. |
| `UserLocation` | One fix: `position` (`GeoPoint`), `time` (`Instant`, wall clock) and `elapsedRealtime` (`Duration` since boot, monotonic: use it for intervals). `horizontalAccuracy`, `altitudeInMeters`, `verticalAccuracy`, `speed` and `bearing` are `null` when the platform did not provide them. |
| `LocationSettings` | `updateInterval` (default 500 ms, must be positive and finite) and `minUpdateDistance` (default 2 m). |
| `LocationSettingsSource` | Input port. The app implements it and binds it with Hilt. |

`:location` exposes `:geo` and `kotlinx-coroutines-core` as `api` dependencies, because their types appear in its public API.

## Inputs and outputs

- **Inputs:** `LocationSettings` through a `LocationSettingsSource` that the app must bind in `SingletonComponent` (in Roadbook Navigator, `:settings` does). Without it the Hilt graph does not compile.
- **Outputs:** `LocationSource.events`.
- **Manifest:** declares `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`, merged into the app manifest, and the optional `android.hardware.location.gps` feature. The app still has to ask for the permission at runtime, e.g. with `PermissionGate`.

## Settings

| Setting | Type | Default | Effect |
|---|---|---|---|
| `updateInterval` | `Duration` | 500 ms | Desired time between fixes. |
| `minUpdateDistance` | `Distance` | 2 m | Fixes closer than this to the previous one are not delivered. `Distance.ZERO` delivers every fix. |

Both are hints to the platform. Every distinct emission restarts the request. If the settings flow throws, `:location` logs it and uses the defaults.

## Persisted state

None.

## Design decisions

- **GPS provider only, high accuracy.** Distances along a route need GPS precision, so network or fused providers are not used. This also keeps the module free of Google Play services.
- **Errors are events.** If the request cannot start, the repository emits `Error` and completes normally, instead of closing the flow with the exception. `LocationSource` then waits for the next settings change, or for a new collection after it stopped, to try again.
- **Invalid fixes are dropped.** A platform fix that no `:geo` value object accepts (e.g. latitude out of range, `NaN` speed) is logged and skipped instead of crashing the flow.
- **Optional values are nullable.** `Location` returns `0` for missing values, so the mapper checks `hasSpeed()`, `hasBearing()`, … and uses `null` for "unknown". A real `0` speed stays `Speed.ZERO`.
- **Shared, short-lived replay.** `shareIn` with `WhileSubscribed(5 s, replayExpirationMillis = 0)`: a configuration change does not restart the request, and a stale fix is never replayed after updates stopped.
- **Callbacks on a direct executor.** Listener callbacks run on the platform thread and only call the thread-safe `trySend`, so no main-thread hop is needed.
- **Altitude is a raw `Double`.** It can be negative, so it cannot be a `Distance`, and `:geo` has no altitude type yet. Add one to `:geo` when a module needs to compute with altitude.
- **No coordinates in logs.** Only request changes, provider changes and failures are logged.

## Testing notes

- `DefaultLocationSource` is tested with `FakeLocationRepository`, `FakeLocationSettingsSource` and `FakeLogger`, using Turbine and the virtual time of `runTest` (sharing, restarts, stop timeout, replay).
- `GpsLocationRepository` and the mappers run on Robolectric, whose `ShadowLocationManager` records requests and delivers simulated fixes. Robolectric drops fixes that arrive sooner than the request interval, so consecutive simulated fixes need increasing times.
- Robolectric never checks location permissions, so `FailingLocationManagerShadow` makes `requestLocationUpdates` throw to test the error events.
- Robolectric does not support `compileSdk` 37 yet, and SDK 36 fails on the current JDK, so `src/test/resources/robolectric.properties` pins tests to SDK 35.
- `:core` does not share its test doubles, so `FakeLogger` lives in this module's test source set.
