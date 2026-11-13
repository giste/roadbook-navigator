# AGENTS.md

Guidance for AI coding agents (and humans) working on this repository. Read this file completely before making any change. If an instruction here conflicts with a request, stop and ask the developer.

---

## 1. Project overview

An Android app for rally navigation. It:

- Imports a **Rally Navigator roadbook definition file (`.rn2`)** and turns its waypoints and moderators into a scrollable on-screen roadbook used to navigate the route.
- Shows an **odometer** with **partial** and **total** distance (and possibly speed, bearing, etc.).
- Shows a **map** centered on the current location, with custom zoom and tilt.

Base package: `dev.giste.roadbooknavigator`. Module packages are `dev.giste.roadbooknavigator.<module>` (e.g. `dev.giste.roadbooknavigator.odometer`).

---

## 2. Hard rules (never break these)

1. **Do not modify `gradle/libs.versions.toml` without the developer's explicit permission.** If a task needs a library or plugin that is not in the catalog, stop and ask. Say which artifact you need, why, and the alternatives you considered. Once you have permission, change only what was approved: add the new entries using the latest stable version, and follow the file's existing naming and ordering. Developers update existing library versions by hand, so do not bump, rename or remove existing entries unless you are explicitly asked to.
2. **Never hardcode dependency versions** in `build.gradle.kts` files. Always use catalog aliases (`libs.*`).
3. **Never break the module dependency graph** described in [§4](#4-modules-and-dependency-rules).
4. **Use explicit API mode** in every library module. Everything that is not part of the module's public API must be `internal` or `private`.
5. **Never throw from a `Flow`.** Model failures as sealed states (see [§6](#6-error-handling)).
6. **All user-facing text is a string resource**, in English (default) and Spanish (see [§8](#8-strings-and-localization)).
7. **Everything is in English**: code, identifiers, comments, KDoc, commit messages, file names and logs.
8. **Document everything.** Public API gets full KDoc. Internal and private code gets KDoc or comments too (see [§9](#9-documentation)).
9. **Every change comes with tests.** It covers every functionality, branch, error path and screen state (see [§10](#10-testing)).
10. **Code copied from the previous project is not exempt.** Before you use it, make it follow every rule in this file: visibility, docs, strings, sealed states, tests, naming.
11. **Never commit without the developer's explicit approval of that specific commit.** The same applies to anything that changes or publishes history: push, merge, rebase, amend, tag (see [§12](#12-git-workflow)).

---

## 3. Tech stack

Use standard, current Android technologies and the official guidance ([Guide to app architecture](https://developer.android.com/topic/architecture), [Kotlin style guide](https://developer.android.com/kotlin/style-guide), [Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md)).

| Concern | Technology |
|---|---|
| Language | Kotlin only (no Java) |
| UI | Jetpack Compose, Material 3 |
| DI | Hilt (`hilt-android`, `hilt-navigation-compose`) |
| Async | Kotlin coroutines and `Flow` / `StateFlow` / `SharedFlow` |
| Persistence | Jetpack DataStore |
| Lifecycle | `androidx.lifecycle` ViewModel, `collectAsStateWithLifecycle` |
| Navigation | Navigation Compose with type-safe routes |
| Build | Gradle Kotlin DSL, version catalog, convention plugins (if present in `build-logic`) |
| Tests | JUnit 4, `kotlinx-coroutines-test`, Turbine, Compose UI test, Hilt testing, Robolectric where configured |

Use only libraries that are already in the version catalog, unless the developer approves a new one (see [§2](#2-hard-rules-never-break-these), rule 1).

---

## 4. Modules and dependency rules

```
                 ┌────────────────────────────────────────┐
                 │                 :app                   │  may depend on everything
                 └───────┬───────────────┬────────────────┘
                         │               │
                         ▼               ▼
                 ┌──────────────┐   ┌──────────────────────────────────────┐
                 │  :settings   │──▶│ :location :odometer :roadbook :map   │  feature modules
                 └──────┬───────┘   └──────────────────┬───────────────────┘
                        │                              │
                        ▼                              ▼
                 ┌────────────────────────────────────────┐
                 │                 :core                  │  depends on nothing
                 └────────────────────────────────────────┘
```

| Module | Responsibility | Allowed project dependencies |
|---|---|---|
| `:core` | Shared resources: logger, theme, common UI components, permissions management, coroutine dispatchers, shared-kernel value objects | **none** |
| `:location` | Provides a location `Flow` to other modules | `:core` |
| `:odometer` | Computes and keeps partial and total distance (and possibly speed, bearing, etc.) | `:core` |
| `:roadbook` | Imports `.rn2`, builds and draws waypoints and moderators, emits roadbook events, auto-advances from a partial-distance flow | `:core` |
| `:map` | Map centered on current location with custom zoom and tilt, map management | `:core` |
| `:settings` | Settings UI and persistence for all modules. Provides each module's settings flow | `:core` + feature modules |
| `:app` | Composition root: `Application`, `MainActivity`, navigation host, cross-module wiring and cross-module rules. No domain logic (see [§4.2](#42-cross-module-rules-in-app)) | all modules |

Rules:

- A feature module **never** depends on another feature module or on `:settings`.
- Feature modules communicate **only through `:app`** (or `:settings` for settings), using each module's public API.
- Do not add new modules without the developer's approval.

**Module-specific details** (public API, inputs and outputs, settings, persisted state, design decisions, algorithms and module testing notes) live in each module's `README.md`, not in this file. Read it before working in a module.

### 4.1 Cross-module communication: ports and adapters

Each feature module **declares the inputs it needs as interfaces or flows in its own public API** (input ports). `:app` connects them with small adapter classes and Hilt bindings. Examples:

- `:odometer` declares that it needs location fixes. `:app` adapts the `:location` flow to it.
- `:roadbook` emits `RoadbookEvent`s, for example *roadbook loaded*, or *waypoint distance long-pressed* to set or show the partial. `:app` collects them and calls the `:odometer` API, for example to reset the odometer when a new roadbook is loaded.
- `:roadbook` accepts a `Flow` of partial distances. `:app` feeds it from `:odometer` to drive waypoint auto-advance.

When several features need the **same primitive concept**, put a minimal, framework-free value object in `:core`'s **shared kernel**, for example `Distance`, `GeoPoint` or `Bearing`. Keep the shared kernel small. Feature-specific concepts stay in their feature.

### 4.2 Cross-module rules in `:app`

Some rules belong to no single feature, because they decide how features react to each other, and no feature may know about another. Resetting the odometer when a new roadbook is loaded is one example. These **cross-module rules** live in `:app`. **Domain logic** does not: anything about one concept (how distance is accumulated, when a waypoint is reached, how `.rn2` is parsed) belongs in the feature that owns that concept.

- Implement each rule as a small **coordinator** class in `:app`'s `coordination` package, named after what it coordinates, e.g. `RoadbookOdometerCoordinator`. It collects one module's public outputs (events, flows) and calls another module's public API. It runs in the injected application `CoroutineScope` and starts from the `Application`.
- Coordinators use only the modules' public APIs. They follow the same rules as any other code: KDoc, no exceptions escaping flows, injected dispatchers, and tests (see [§10.1](#101-test-types)).
- If a rule only involves one module's concepts, move it into that module.
- If a coordinator starts doing domain work, such as calculations, state machines or persistence, that logic belongs in a feature. Move it there, or ask the developer about a new module.

### 4.3 Settings contract

- Each feature module defines, in its public API:
  - an immutable settings model (`XxxSettings`, `data class` with documented defaults), and
  - a settings source interface that exposes it as a flow, e.g. `public interface OdometerSettingsSource { public val settings: Flow<OdometerSettings> }`.
- The feature **reacts dynamically** to every emission. It never reads settings once and caches them forever.
- `:settings` implements every settings source, persists the values with DataStore, provides the settings UI, and binds the implementations with Hilt.
- A feature module must work with its default settings when no value has been stored yet.

---

## 5. Architecture: Clean Architecture + DDD

Every feature module is internally layered. Dependencies point **inwards**: `presentation → domain ← data`. The domain layer has **no Android or framework dependencies**, except `kotlinx.coroutines` `Flow`.

### 5.1 Package layout per module

The module's **root package holds only its public API**. Implementation lives in **layer packages**, which every module has, even small ones. They mark the layer boundaries and keep modules consistent:

```
dev/giste/roadbooknavigator/<module>/
├── *.kt            Public API only: facade interfaces, public models, settings contract, events, entry-point composables / nav routes
├── domain/         Entities, value objects, aggregates, domain events, repository interfaces, use cases
├── data/           Repository implementations, data sources (DataStore, file system, Android services), mappers
├── presentation/   ViewModels, UI state, internal composables
└── di/             Hilt modules
```

- **Root package = public API only** (see the [`:core` exception](#core-exception-one-sub-package-per-capability)). Root-package files contain only `public` top-level declarations. They may also have `private` helpers used only by a public declaration in the same file. Do not put `internal` types or shared helpers there, even if they are small. Move them into the layer they belong to.
- **Layer packages contain no `public` declarations.** Everything in them is `internal` or `private`. If a type in a layer needs to become public, move it, or a public interface it implements, to the root package.
- Public types are imported from the module's root package, e.g. `dev.giste.roadbooknavigator.odometer.OdometerController`.

**Sub-packages inside a layer are optional. Add them only when the layer grows.**

- Keep a layer **flat** while it is small (roughly fewer than 8 files). Do not create sub-packages that hold only one or two files.
- When a layer grows, split it into sub-packages, but only for groups that are big enough to need one. Leave the remaining files at the layer root.
- **Group by concept rather than by type** when that reads better, e.g. `roadbook/domain/waypoint/` and `roadbook/domain/parsing/` instead of `model/` and `usecase/`. Use type-based names (`model/`, `repository/`, `usecase/`, `source/`, `mapper/`) when there is no clearer concept.
- Closely related declarations may share one Kotlin file, e.g. a sealed hierarchy and its subtypes, or a small value object used only by its owner. Name the file after the main declaration.
- When you restructure packages, move files without changing behavior, and do it in a separate change from functional work.

Example of a small module:

```
odometer/
├── OdometerController.kt, OdometerState.kt, OdometerSettings.kt, OdometerSettingsSource.kt   ← public API
├── domain/         Odometer.kt, DistanceFilter.kt, OdometerStateRepository.kt
├── data/           DataStoreOdometerStateRepository.kt
├── presentation/   OdometerViewModel.kt, OdometerUiState.kt, OdometerScreen.kt
└── di/             OdometerModule.kt
```

#### `:core` exception: one sub-package per capability

`:core` is a set of unrelated capabilities, not one feature, so its root package holds no code. Each capability lives in its own sub-package, and **each sub-package follows the module rules above**: its root holds only its public API, and its layer packages hold only `internal` or `private` code. A capability creates only the layer packages it needs.

```
core/
├── logging/        Logger.kt, DebugLogging.kt   ← public API
│   ├── data/       TimberLogger.kt, AutomaticTagTree.kt
│   └── di/         LoggerModule.kt
├── coroutines/     Dispatcher qualifiers, ApplicationScope
│   └── di/
├── permission/     PermissionGate.kt, AppPermission.kt
│   ├── domain/  data/  presentation/  di/
├── ui/
│   ├── theme/      Theme, colors, typography, dimensions
│   └── component/  Common composables
└── model/          Shared kernel: framework-free value objects (Distance, GeoPoint, …)
```

- Capabilities depend on each other only through their public API (e.g. `permission` injects `logging.Logger`, never `logging.data.TimberLogger`). Kotlin's `internal` is module-wide, so the compiler does not enforce this. Reviews must.
- Add a new capability sub-package instead of growing an existing one with unrelated code.

### 5.2 DDD building blocks

- **Value objects:** immutable and validated on creation. Use `@JvmInline value class` for single-value types such as `Distance` or `WaypointId`. Never pass raw `Double`s or `Int`s around for domain quantities. Store distance in meters internally and convert only for display.
- **Entities and aggregates:** have identity and enforce their own invariants. For example, `Roadbook` is the aggregate root that owns its `Waypoint`s and their moderators. All changes go through the aggregate root.
- **Domain events:** sealed hierarchies that describe something that happened (e.g. `RoadbookEvent.Loaded`).
- **Repositories:** interfaces are declared in `domain`. Implementations live in `data`. They expose `Flow`s and `suspend` functions and return sealed results.
- **Use cases:** one action per class, named `VerbNounUseCase`, with a single `operator fun invoke`. Add a use case only when it holds real logic or combines repositories. Do not add pass-through use cases.
- **Ubiquitous language:** use rally and roadbook terms consistently: *roadbook, waypoint, moderator, partial, total, odometer, auto-advance, tulip, CAP/heading*. Do not invent synonyms.

### 5.3 Presentation (Compose + UDF)

- Unidirectional data flow: the ViewModel exposes a single `StateFlow<XxxUiState>`. The UI sends user actions to the ViewModel through methods (or a sealed `XxxUiAction`).
- UI state is a sealed interface or an immutable data class. Model `Loading`, `Success`/content and `Error` explicitly.
- Expose state with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. Collect it with `collectAsStateWithLifecycle()`.
- ViewModels are `@HiltViewModel internal class`. They never hold a `Context`, `Activity` or `View`, and they **never expose text** (no strings and no resource IDs). UI state contains only typed data: domain values, enums and sealed reasons. **Composables resolve that state to text** with `stringResource`/`pluralStringResource`, typically through small `@Composable` mapping functions next to the screen, e.g. `ImportError.toMessage(): String`.
- Composables:
  - Use a stateful **`XxxRoute`** (gets the ViewModel and collects state) and a stateless **`XxxScreen`** (takes state and lambdas). Only the route and navigation entry points are public.
  - The first optional parameter is `modifier: Modifier = Modifier`, and it is applied to the root layout.
  - Hoist state and keep composables side-effect free. Use `LaunchedEffect` and similar APIs correctly.
  - Provide `@Preview`s for every screen state, in light and dark themes, using `:core` theme.
  - Use `:core` theme tokens and common components. Do not hardcode colors or dimensions.
- One-shot UI effects (snackbars, navigation) are modeled as state where possible, following Android guidance.

### 5.4 Coroutines and flows

- Inject dispatchers through the `:core` qualifiers (`@IoDispatcher`, `@DefaultDispatcher`, …). Never hardcode `Dispatchers.IO` in classes. This keeps them testable.
- Do not use `GlobalScope`. Scopes come from the lifecycle (`viewModelScope`) or from an injected application-level `CoroutineScope` provided by `:core`.
- `suspend` functions are main-safe.
- Use `StateFlow` for state and `SharedFlow` (no replay, or a buffer when needed) for events between modules.
- Prefer cold `Flow`s in data sources. Share them with `shareIn`/`stateIn` only where needed.

---

## 6. Error handling

- **Never let exceptions escape a `Flow` or reach the UI.** Catch them at the data boundary (I/O, parsing, Android services) and map them to sealed domain types such as `sealed interface ImportRoadbookResult { data class Success(...); sealed interface Failure ... }`.
- Use `Flow.catch { emit(XxxState.Error(...)) }` at the boundary. Do not use `try/catch` around `collect` in consumers.
- **Always rethrow `CancellationException`.** Never swallow it, and avoid bare `runCatching` in coroutines unless you rethrow cancellation.
- Error states carry a typed reason (an enum or sealed type), not a message string. The ViewModel passes the reason through in the UI state, and the composable maps it to a string resource.
- Use `require`/`check` only for programming errors (broken invariants), never for expected runtime failures.

---

## 7. State persistence

- **Every module persists its own state.** No module reads or writes another module's storage.
- Use DataStore, with **one DataStore instance per file**, provided as a singleton through Hilt. File names are prefixed with the module name (e.g. `odometer_state`, `roadbook_state`).
- Settings values are persisted by `:settings`. Runtime state is persisted by the feature module that owns it.
- Handle read errors: on `IOException` emit defaults or a sealed error state. Never crash.
- Persist only what is needed to restore state after process death or restart. Use `SavedStateHandle` for transient UI state.

---

## 8. Strings and localization

- All user-facing text lives in `res/values/strings.xml` (English, the default) **and** `res/values-es/strings.xml` (Spanish, used for every Spanish locale). Every key must exist in both files.
- Add a regional folder (e.g. `values-es-rMX`) only for the strings whose wording differs in that region. It overrides `values-es` and does not need every key.
- This includes content descriptions, error messages, units shown to users, notification text and labels. Use `plurals` and placeholders (`%1$s`, `%1$d`) instead of string concatenation.
- Format numbers and distances with the current locale.
- **Accepted exceptions** (no resource needed): log messages, exception or `require` messages meant for developers, DataStore keys, intent extras and other identifiers, MIME types, file extensions, test names and test data, `@Preview` sample data, and format patterns that are not user-visible.
- Each Android module sets `resourcePrefix` to its module name (e.g. `odometer_`), and every resource name uses it.
- Strings in `:core` are only generic ones (e.g. `core_action_retry`). Feature strings belong to their feature.

---

## 9. Documentation

- **Public API:** full KDoc on every public class, interface, function, property, parameter (`@param`), return value (`@return`), sealed subtype and enum entry. Explain behavior, threading or flow semantics (hot or cold, replay, when it emits) and error states.
- **Internal and private code:** KDoc for classes and non-trivial functions, and inline comments that explain *why*, not *what*. Document non-trivial algorithms in detail.
- Each module has a `README.md` describing its responsibility, public API, inputs and outputs, settings, persisted state, key design decisions and module-specific testing notes. It is the single place for module-specific guidance. Update it in the same change whenever any of these change.
- Do not leave commented-out code. `TODO`s must include context.

---

## 10. Testing

The test suite is exhaustive: **every functionality, branch, error path, sealed state and screen state is tested.** Do not test framework internals, such as DataStore storing data, Hilt injecting, Compose recomposing, generated `data class` members or trivial getters.

### 10.1 Test types

| Type | Source set | What |
|---|---|---|
| Unit | `src/test` | Value objects and invariants, aggregates, use cases, mappers, parsers (valid, invalid and edge-case fixtures), domain algorithms, ViewModels (every state transition and action), repositories with fake data sources, `:app` coordinators with fakes of the modules' public APIs |
| Integration (JVM) | `src/test` | Repositories with real DataStore in a temporary folder, parsers plus repositories on real fixture files, flow pipelines across layers |
| UI | `src/androidTest` (or `src/test` with Robolectric if configured) | Each stateless `XxxScreen` in every UI state, user interactions and callbacks, accessibility (content descriptions, semantics) |
| App integration | `:app` `src/androidTest` | Hilt-based tests of cross-module wiring and coordinators: roadbook load → odometer reset, odometer partial → roadbook auto-advance, settings changes propagating to features |

### 10.2 Conventions

- Prefer **fakes over mocks**. Fakes live in the test source set of the module that owns the interface and are named `FakeXxx`. Use a mocking library only when a fake is impractical.
- Test flows with Turbine. Use `runTest` and `StandardTestDispatcher`/`UnconfinedTestDispatcher` with injected dispatchers, and a `MainDispatcherRule` for ViewModels.
- Test names follow `subject_condition_expectedResult`, e.g. `addFix_lowAccuracy_isIgnored`. Test classes are named `XxxTest`, and integration tests `XxxIntegrationTest`.
- Each test follows Arrange / Act / Assert (Given / When / Then) and checks one behavior.
- Tests are deterministic: no real time, network, location or `Thread.sleep`. Inject clocks and time sources.
- Test fixtures (e.g. sample input files) go in `src/test/resources`.
- Bug fixes start with a failing test that reproduces the bug.

---

## 11. Kotlin, Gradle and naming conventions

- Follow the [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide) and the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html). Run any formatter or static analysis that is already configured in the project.
- Every library module applies `kotlin { explicitApi() }`. `:app` is exempt because nothing consumes it.
- Prefer immutability: `val`, read-only collections, immutable UI state.
- Log only through the `:core` `Logger`. Do not call `android.util.Log`, `println` or `printStackTrace` directly. Do not log precise locations or other personal data in release builds.

| Element | Convention | Example |
|---|---|---|
| Packages | lowercase, no underscores | `dev.giste.roadbooknavigator.roadbook.domain.model` |
| Classes / interfaces | PascalCase nouns | `Waypoint`, `RoadbookRepository` |
| Repository implementations | Descriptive prefix, not `Impl` | `DefaultRoadbookRepository`, `DataStoreOdometerStateRepository` |
| Use cases | `VerbNounUseCase` | `ImportRoadbookUseCase` |
| ViewModels / UI state / actions | `XxxViewModel`, `XxxUiState`, `XxxUiAction` | `OdometerUiState` |
| Composables | PascalCase nouns; stateful `XxxRoute`, stateless `XxxScreen` | `RoadbookRoute`, `WaypointItem` |
| Hilt modules | `XxxModule` in `di` | `OdometerDataModule` |
| Constants | `UPPER_SNAKE_CASE` | `DEFAULT_ZOOM_LEVEL` |
| Resources | `<module>_<type>_<name>` | `roadbook_error_invalid_file`, `odometer_ic_reset` |
| Test doubles | `FakeXxx` | `FakeLocationSource` |

---

## 12. Git workflow

### 12.1 Every commit needs the developer's approval

- **The developer must allow every commit.** An agent never runs `git commit` on its own initiative. Before each commit, show the developer:
  - the list of staged files,
  - a short summary of the changes, and
  - the proposed commit message,

  and wait for an explicit "yes". Approval covers **only that commit**. It does not carry over to later commits, to amended versions of the same commit, or to other sessions.
- The same approval is required, every time, for any operation that changes or publishes history: `push`, `merge`, `rebase`, `commit --amend`, `cherry-pick`, `revert`, `reset` of committed work, `tag`, branch deletion, and opening or merging a pull request.
- If the developer declines, leave the changes uncommitted in the working tree and ask how to proceed.

### 12.2 Branches

- `main` is always buildable, tested and releasable. **Never commit directly to `main`.**
- Do all work on a short-lived branch created from an up-to-date `main`, one branch per task.
- Name branches `<type>/<short-kebab-case-description>`, using the same types as commit messages (see [§12.3](#123-commits)), e.g. `feat/roadbook-rn2-import`, `fix/odometer-negative-partial`.
- Keep branches short-lived and in sync with `main`. Rebase your own unpublished branch on `main` rather than merging `main` into it.
- Delete the branch after it is merged.

### 12.3 Commits

- Follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <summary>`.
  - **Types:** `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`, `style`.
  - **Scope:** the module name (`core`, `location`, `odometer`, `roadbook`, `map`, `settings`, `app`). Omit it for changes that span the whole project.
  - **Summary:** imperative mood ("add", not "added"), lowercase, no trailing period, at most 72 characters in the whole subject line.
  - **Body** (optional, separated by a blank line, wrapped at 72 characters): explain *what* and *why*, not *how*.
  - **Footer:** `BREAKING CHANGE: <description>` for incompatible public API or persisted-format changes, and issue references such as `Refs: #12`.
  - Example: `feat(odometer): reset partial on long press`.
- **Atomic commits:** one logical change per commit. Code and its tests go in the same commit. Package moves and renames go in their own commit, separate from functional changes (see [§5.1](#51-package-layout-per-module)).
- **Every commit builds and passes the tests** listed in [§13](#13-agent-workflow), step 5. Do not commit work in progress to a shared branch.
- **Stage deliberately.** Add files by path and review the staged diff (`git diff --staged`) before asking for approval. Avoid `git add -A` / `git add .`, which can pick up unrelated or private files.
- **Never commit** build outputs, IDE files, `local.properties`, signing keys, keystores, API keys, `.env` files or other secrets. Keep `.gitignore` up to date instead.
- **Never bypass hooks** (`--no-verify`) or sign-off/signing requirements.

### 12.4 Pull requests and merging

- Every change reaches `main` through a pull request. Keep pull requests small and focused on one task.
- The description states what changed, why, how it was tested, and includes screenshots for UI changes. Go through the Definition of done in [§13](#13-agent-workflow) before requesting review.
- Merge only when the review is approved and all checks (CI, lint, tests) are green.
- Prefer **squash merge** with a Conventional Commits title, so `main` keeps one meaningful commit per change.
- **Write a proper squash commit message.** Squashing discards the branch commits, so this message is the only record of the change in the Git history. Replace the auto-generated list of branch commit messages ("wip", "fix typo", "address review") with:
  - **Title:** the pull request title, in Conventional Commits format.
  - **Body:** what changed, at the behavior level rather than file by file, and why. Add notable design decisions or trade-offs, and risks such as persisted-format changes or migrations.
  - **Footer:** `BREAKING CHANGE: <description>` when applicable, and issue references (`Refs: #12`, `Closes: #12`).

  Write the pull request description in the same shape so it can be reused as the squash message. Example:

  ```
  feat(roadbook): import .rn2 files

  Parse Rally Navigator .rn2 files into a Roadbook aggregate so routes
  can be loaded from the device storage.

  - Map waypoints and their moderators to domain types
  - Report malformed files as ImportRoadbookResult.Failure instead of
    throwing
  - Emit RoadbookEvent.Loaded so :app can reset the odometer

  Distances are stored in meters; .rn2 kilometer values are converted
  at the data boundary.

  Refs: #4
  ```

### 12.5 History safety

- **Never rewrite published history.** Do not force-push to `main` or to any branch someone else may be using.
- On your own feature branch, rewriting history (rebase, amend, force-push) is allowed only with the developer's approval, and force-pushes use `--force-with-lease`, never `--force`.
- Never use destructive commands that discard work (`git reset --hard`, `git clean -fd`, `git checkout -- <path>`, `git stash drop`) without the developer's explicit approval.

### 12.6 Releases

- Releases are tagged on `main` with annotated tags following [Semantic Versioning](https://semver.org/): `vMAJOR.MINOR.PATCH`, e.g. `v1.2.0`.
- Tags are created only by, or with the approval of, the developer.

---

## 13. Agent workflow

1. **Understand first.** Read the module's `README.md`, its public API and the existing tests before you change anything. If the README is missing or out of date, say so.
2. **Keep changes scoped** to the task. Do not refactor unrelated code or change public APIs unless asked.
3. **Ask before you:** add a dependency or modify `libs.versions.toml`, change the module graph, change a public API used by other modules, change a persisted data format (this may need a migration), delete user-facing functionality, or commit, push or otherwise change Git history (see [§12](#12-git-workflow)).
4. **When adapting code from the previous project,** bring it fully into compliance with this file and add the missing tests and documentation.
5. **Before finishing,** make sure all of these pass:
   - `./gradlew assembleDebug`
   - `./gradlew testDebugUnitTest`
   - `./gradlew lint` (no new warnings, no missing translations)
   - `./gradlew connectedDebugAndroidTest` when UI or instrumented tests changed and a device is available
   - any formatter or static-analysis task configured in the project
6. **Definition of done:**
   - [ ] Module boundaries and visibility rules are respected (explicit API, root package holds only public API, layer packages hold no public declarations).
   - [ ] No exceptions escape flows. Failures are sealed states.
   - [ ] All new user-facing strings exist in `values` and `values-es`.
   - [ ] Public API has full KDoc. Internal and private code is documented.
   - [ ] Unit and integration tests cover every branch and error path. UI tests cover every screen state.
   - [ ] The module `README.md` is updated if the public API, settings, persisted state or design decisions changed.
   - [ ] `libs.versions.toml` is untouched, or contains only the changes the developer approved.
   - [ ] Changes are on a task branch, not on `main`, and are committed only after the developer approved each commit (see [§12](#12-git-workflow)).
