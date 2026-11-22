# :location

Provides the device location as a `Flow` to other modules. `:location` depends only on `:core` and `:geo`.

`:location` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.location`, and it contains nothing specific to Roadbook Navigator, so other apps can depend on it too.

## Responsibility

- Read location fixes from the platform GPS provider and expose them as one shared `Flow` of `LocationEvent`s, which other modules consume through `:app` (e.g. `:odometer` and `:map`).
- Restart the platform request whenever the request values of the location settings change.
- Tell whether the device is moving or stopped, from the speed of the fixes (`LocationSource.motion`), so apps can, e.g., allow some actions only while stopped.
- Report platform failures (permission missing, no GPS provider) and provider status changes as events. The flow never throws (AGENTS.md §6).

It does **not** request runtime permissions. `:core`'s `PermissionGate` does that. It does **not** store its settings either: the app provides them through `LocationSettingsSource`.

## Package layout

```
dev/giste/location/
├── LocationSource, LocationEvent, LocationError, MotionState,
│   LocationSettings, LocationSettingsSource                     ← public API
├── domain/         LocationRepository, DefaultLocationSource, MotionDetection
├── data/           GpsLocationRepository, LocationMapper
└── di/             LocationModule
```

There is no `presentation` package, because `:location` has no UI yet.

## Public API

Package `dev.giste.location`:

| Type | Purpose |
|---|---|
| `LocationSource` | Inject it (Hilt binds it as a singleton). `events` is a hot `Flow<LocationEvent>` shared by every collector. Updates start with the first collector and stop 5 s after the last one leaves. A new collector first gets the latest event while updates run. It never throws or completes. `motion` is a hot `Flow<MotionState>` derived from `events`, shared the same way: it starts `STOPPED`, emits only changes, and gives a new collector the current state while it runs. Collecting it also starts location updates. |
| `MotionState` | `STOPPED` or `MOVING`, as decided by `LocationSource.motion`. |
| `LocationEvent` | Sealed: `LocationUpdated(location)`, `ProviderEnabled`, `ProviderDisabled`, `Error(reason)`. `location` is a `:geo` `LocationFix`, the location type shared by every module, so consumers such as `:odometer` take it directly. |
| `LocationError` | Typed reason of `LocationEvent.Error`: `PERMISSION_DENIED`, `PROVIDER_UNAVAILABLE`, `UNKNOWN`. The UI maps it to a string. |
| `LocationSettings` | `updateInterval` (default 500 ms, must be positive and finite), `minUpdateDistance` (default 2 m), `movingSpeed` (default 10 km/h) and `stoppedSpeed` (default 5 km/h, not above `movingSpeed`). |
| `LocationSettingsSource` | Input port. The app implements it and binds it with Hilt. |

`:location` exposes `:geo` and `kotlinx-coroutines-core` as `api` dependencies, because their types appear in its public API.

## Inputs and outputs

- **Inputs:** `LocationSettings` through a `LocationSettingsSource` that the app must bind in `SingletonComponent` (in Roadbook Navigator, `:settings` does). Without it the Hilt graph does not compile.
- **Outputs:** `LocationSource.events` and `LocationSource.motion`. In Roadbook Navigator, `:app` forwards the fixes to `:odometer`, and offers to import a roadbook only while `motion` is `STOPPED`.
- **Manifest:** declares `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`, merged into the app manifest, and the optional `android.hardware.location.gps` feature. The app still has to ask for the permission at runtime, e.g. with `PermissionGate`.

## Settings

| Setting | Type | Default | Effect |
|---|---|---|---|
| `updateInterval` | `Duration` | 500 ms | Desired time between fixes. |
| `minUpdateDistance` | `Distance` | 2 m | Fixes closer than this to the previous one are not delivered. `Distance.ZERO` delivers every fix. |
| `movingSpeed` | `Speed` | 10 km/h | Above this speed, a stopped device starts moving. |
| `stoppedSpeed` | `Speed` | 5 km/h | Below this speed, a moving device is stopped again. Must not be above `movingSpeed`; an equal pair has no hysteresis. |

`updateInterval` and `minUpdateDistance` are hints to the platform, and a change in either restarts the request. A change in the motion speeds does not: it applies to the latest event at once, and to every event after it. The motion defaults suit motor vehicles; apps for slower movement, such as walking, should set their own. If the settings flow throws, `:location` logs it and uses the defaults.

## Persisted state

None.

## Design decisions

- **GPS provider only, high accuracy.** Distances along a route need GPS precision, so network or fused providers are not used. This also keeps the module free of Google Play services.
- **Errors are events.** If the request cannot start, the repository emits `Error` and completes normally, instead of closing the flow with the exception. `LocationSource` then waits for the next settings change, or for a new collection after it stopped, to try again.
- **Invalid fixes are dropped.** A platform fix that no `:geo` value object accepts (e.g. latitude out of range, `NaN` speed or altitude) is logged and skipped instead of crashing the flow.
- **The fix type lives in `:geo`.** `LocationFix` is a plain value, so it belongs to the shared kernel, while the provider stays here. Consumers declare an input port of `LocationFix`es instead of depending on `:location`, so they are not tied to the GPS provider, and the app can feed them recorded tracks.
- **Optional values are nullable.** `Location` returns `0` for missing values, so the mapper checks `hasSpeed()`, `hasBearing()`, … and uses `null` for "unknown". A real `0` speed stays `Speed.ZERO`.
- **Shared, short-lived replay.** `shareIn` with `WhileSubscribed(5 s, replayExpirationMillis = 0)`: a configuration change does not restart the request, and a stale fix is never replayed after updates stopped.
- **Callbacks on a direct executor.** Listener callbacks run on the platform thread and only call the thread-safe `trySend`, so no main-thread hop is needed.
- **Altitude is a `:geo` `Altitude`.** It can be negative, so it cannot be a `Distance`. The platform reports it above the WGS84 ellipsoid, which is the reference surface of every `LocationFix.altitude`.
- **Motion with hysteresis.** A single speed limit would flip between moving and stopped with GPS jitter around it, so there are two: a stopped device starts moving above `movingSpeed`, and a moving one stops below `stoppedSpeed`; in between, nothing changes. Without a known speed (a fix without speed, a disabled provider, an error) the device counts as stopped, so an app never locks the user out of a stopped-only action because a value is missing. The rule is the pure function `MotionState.after(event, settings)` in `domain`.
- **Motion speeds are settings.** The right speeds depend on the vehicle (a car or a walker), so the app chooses them through `LocationSettings`. They are not part of the platform request, so changing them never restarts it.
- **No coordinates in logs.** Only request changes, provider changes and failures are logged.

## Testing notes

- `DefaultLocationSource` is tested with `FakeLocationRepository`, `FakeLocationSettingsSource` and `FakeLogger`, using Turbine and the virtual time of `runTest` (sharing, restarts, stop timeout, replay, motion changes and settings changes). `MotionDetectionTest` covers every branch of the motion rule as a plain function, with fixes built by `fixAt(kilometersPerHour)`.
- `GpsLocationRepository` and the mappers run on Robolectric, whose `ShadowLocationManager` records requests and delivers simulated fixes. Robolectric drops fixes that arrive sooner than the request interval, so consecutive simulated fixes need increasing times.
- Robolectric never checks location permissions, so `FailingLocationManagerShadow` makes `requestLocationUpdates` throw to test the error events.
- Robolectric does not support `compileSdk` 37 yet, and SDK 36 fails on the current JDK, so `src/test/resources/robolectric.properties` pins tests to SDK 35.
- `:core` does not share its test doubles, so `FakeLogger` lives in this module's test source set.
