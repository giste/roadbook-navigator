# :odometer

Measures the **trip** and **total** distance travelled from location fixes, and persists them. `:odometer` depends only on `:core` and `:geo`.

`:odometer` is a **reusable module** (AGENTS.md §4.1): its package is `dev.giste.odometer`, and it contains nothing specific to Roadbook Navigator, so other apps can depend on it too.

## Responsibility

- Add the distance between accepted location fixes to the trip and total distances, filtering out GPS noise (inaccurate fixes, jitter while stopped).
- Let the app reset, set, increase or decrease the trip distance, and reset both distances.
- Persist the distances so they survive process death and restarts (AGENTS.md §7).

It has **no UI**. It exposes the distances as a flow, and each app decides how to show them (numbers, a progress bar, …). It does **not** read the location itself: the app adapts its location provider (in Roadbook Navigator, `:location`) to `OdometerLocationSource` (AGENTS.md §4.2). It does **not** store its settings either: the app provides them through `OdometerSettingsSource` (AGENTS.md §4.4; in Roadbook Navigator, `:settings` will).

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
├── OdometerController, OdometerState, OdometerLocation, OdometerLocationSource,
│   OdometerSettings, OdometerSettingsSource                          ← public API
├── domain/         DefaultOdometerController, FixFilter, TravelledDistance,
│                   OdometerStateRepository
├── data/           DataStoreOdometerStateRepository, OdometerQualifiers
└── di/             OdometerModule
```

There is no `presentation` package, because `:odometer` has no UI.

## Public API

Package `dev.giste.odometer`:

| Type | Purpose |
|---|---|
| `OdometerController` | Inject it (Hilt binds it as a singleton). `state` is a hot `Flow<OdometerState>` shared by every collector. Measuring runs while it is collected, and stops 5 s after the last collector leaves. A new collector first gets the current state once it has been read from storage, never a placeholder. It never throws or completes. The suspend functions `resetTrip()`, `resetAll()`, `setTrip(distance)`, `increaseTrip(by)` and `decreaseTrip(by)` change the distances and save them at once. They are main-safe and applied in call order. `decreaseTrip` stops at zero. |
| `OdometerState` | `trip` and `total`, as `:geo` `Distance`s. No upper limit: the app decides how many digits it shows. `OdometerState.ZERO` has both at zero. |
| `OdometerLocation` | One fix: `position`, and the nullable `horizontalAccuracy`, `altitudeInMeters`, `verticalAccuracy` and `speed`. `null` means "unknown", which is not the same as zero. |
| `OdometerLocationSource` | Input port. The app implements it and binds it with Hilt. |
| `OdometerSettings` | `minSpeed`, `maxHorizontalAccuracy`, `minStepDistance`, `useAltitude`, `maxVerticalAccuracy`, with documented defaults (`DEFAULT_MIN_SPEED`, …). Any value is valid. |
| `OdometerSettingsSource` | Input port. The app implements it and binds it with Hilt. |

`:odometer` exposes `:geo` and `kotlinx-coroutines-core` as `api` dependencies, because their types appear in its public API.

## Inputs and outputs

- **Inputs:** an `OdometerLocationSource` and an `OdometerSettingsSource`, which the app must bind in `SingletonComponent`. Without them the Hilt graph does not compile as soon as something injects `OdometerController`. Commands through `OdometerController`, e.g. `:app` calls `resetTrip()` when `:roadbook` loads a new roadbook.
- **Outputs:** `OdometerController.state`. In Roadbook Navigator, `:app` shows it and feeds the trip distance to `:roadbook` for waypoint auto-advance.

## Settings

| Setting | Type | Default | Effect |
|---|---|---|---|
| `minSpeed` | `Speed` | 0.5 m/s | Fixes slower than this are treated as stopped and not counted. Fixes with an unknown speed are always counted. `Speed.ZERO` counts every fix. |
| `maxHorizontalAccuracy` | `Distance` | 20 m | Fixes less accurate than this, or with an unknown accuracy, are ignored. A value close to zero ignores nearly every real fix. |
| `minStepDistance` | `Distance` | 5 m | A fix closer than this to the anchor is not counted yet. The short steps are counted in one piece once the device is this far from the anchor. `Distance.ZERO` counts every step. |
| `useAltitude` | `Boolean` | `false` | Whether the altitude change is added to the distance. Turn it on only for altitude sources better than GPS, e.g. a barometer. |
| `maxVerticalAccuracy` | `Distance` | 3 m | With `useAltitude` on, the altitude change is only counted when both fixes have this vertical accuracy or better. No effect when `useAltitude` is off. |

`:odometer` imposes no ranges: `Speed` and `Distance` already reject negative and non-finite values, and every other value works. Which values the user may choose (e.g. the range of a slider) is up to each app, so Roadbook Navigator's `:settings` will define its own. Every new value applies to the next fix. The odometer does not measure until the settings source has emitted once. If the settings flow throws, `:odometer` logs it and uses the defaults.

## Persisted state

One Preferences DataStore file, `odometer_state`, owned by `DataStoreOdometerStateRepository`:

| Key | Type | Value |
|---|---|---|
| `trip_distance_meters` | `Double` | Trip distance in meters. |
| `total_distance_meters` | `Double` | Total distance in meters. |

A missing, negative or non-finite value is read as zero (and logged). A corrupted file is replaced with an empty one.

## Design decisions

- **Fix filter.** The odometer keeps an *anchor*, the last accepted fix, and measures the next distance from it. In order: an inaccurate fix is ignored; without an anchor the fix becomes the anchor; a stopped fix is ignored and the anchor stays, so jitter adds nothing and the real movement from the anchor is counted once the vehicle moves again; a fix closer to the anchor than `minStepDistance` is ignored and the anchor stays; otherwise the distance from the anchor is added and the fix becomes the anchor.
- **Minimum step, so any location source works.** GPS noise always makes a path longer, and consecutive fixes are a few meters apart from noise alone. Without a minimum step, an app that feeds raw fixes every second at walking speed would overcount badly. Holding short steps at the anchor loses nothing: they are counted in one piece later, only straightened.
- **Haversine distance, altitude off by default.** The spherical Earth model is off by at most about 0.5%, much less than GPS noise. GPS altitude is noisier than position, and the climb adds little (0.5% on a 10% slope), so including it usually adds more error than it removes. It can be turned on with `useAltitude` for better altitude sources, and is then only used when both vertical accuracies are good enough. The calculation lives in `:odometer` because it is the only user. Move it to `:geo` (e.g. `GeoPoint.distanceTo`) when a second module needs it.
- **Measuring runs only while `state` is collected.** The app decides when the odometer measures by collecting it, e.g. from an application-scoped coordinator. The anchor starts empty on every collection, so distance travelled while nobody collected is not counted.
- **No placeholder state.** `state` is a shared flow with replay instead of a `StateFlow`, because a `StateFlow` needs an initial value: emitting zero before storage is read would make consumers such as waypoint auto-advance jump back to the start.
- **Memory first, buffered saves.** The repository reads the file once and keeps the state in memory, because it is the file's only writer. Travelled distance is saved every 500 m or every minute, whichever comes first, to limit disk writes while still losing little on a process death. Explicit changes are saved at once. A mutex serializes every change.
- **Failures never escape.** Read failures start from zero, write failures keep the state in memory and are retried on the next change, and a failing location or settings flow is logged. Only `IOException` is caught, so cancellation always propagates.
- **Suspend commands.** Commands are suspend functions instead of fire-and-forget calls, so callers control their scope and commands are applied in order.
- **No coordinates in logs.** Only commands, ignored fix reasons and failures are logged.

## Testing notes

- `FixFilter` and `TravelledDistance` are pure functions with plain unit tests. Test fixes move due north from a fixed start (`testFix(metersNorth)`), so expected distances are exact.
- `DefaultOdometerController` is tested with `FakeOdometerStateRepository`, `FakeOdometerLocationSource`, `FakeOdometerSettingsSource` and `FakeLogger`, using Turbine and the virtual time of `runTest` (measuring while collected, stop timeout, restarts, failures, commands).
- `DataStoreOdometerStateRepositoryTest` uses `FakePreferencesDataStore`, which can fail reads and writes on demand, and `testScheduler.timeSource` for the save interval.
- `DataStoreOdometerStateRepositoryIntegrationTest` uses a real DataStore file in a `TemporaryFolder`, and closes and reopens it to check that the state survives. DataStore allows one open instance per file, so each instance runs in its own `Job`.
- `:core` does not share its test doubles, so `FakeLogger` lives in this module's test source set.
