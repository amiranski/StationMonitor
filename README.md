# StationMonitor

Android app for monitoring fuel stations and the devices installed at them. It is built as a
test assignment: a list of stations, a detail screen with the station's devices, data from the
backend, and a local cache so the app also works offline.

**Features**

- Stations list: name, country, address, device count; pull to refresh; search by name or
  address (ignores case and diacritics: "zlin" finds "Zlín") and filter by country.
- Station detail (separate screen via Navigation 3): devices sorted problems first, status
  summary, device type, relative "last seen" time.
- Offline-first cache in Room: data from the last successful download is shown immediately and
  without internet, with a "Showing saved data · from 17:28" banner when a refresh fails.
- Every state handled: first load, network/server error without cache, empty list, refresh
  failure with cache (Snackbar + Retry), refreshing.
- Light and dark theme, English and Czech.

**Switching to Czech:** the app follows the phone language. To try Czech without changing the
whole phone (Android 13+): Settings → Apps → StationMonitor → Language → Čeština.

## Running the app

Requirements: an Android Studio version that supports Android Gradle plugin 9.5 (the stable
release from late 2026). An older Android Studio cannot sync the project: update it, or build from
the command line below, which only needs the Android SDK (Gradle downloads the JDK it needs by
itself, see `gradle/gradle-daemon-jvm.properties`). On the first sync Android Studio may ask to
install SDK Platform 37.2. Nothing else to configure: the API URL is public and there is no login.

1. Open the project in Android Studio and let Gradle sync.
2. Run the `app` configuration on an emulator or a device with Android 8.0 (API 26) or newer.

From the command line:

```bash
./gradlew :app:assembleDebug        # build the debug APK
./gradlew :app:testDebugUnitTest    # run the unit tests (JVM, no emulator needed)
```

## Architecture

MVVM with unidirectional data flow, offline-first data layer, one Gradle module split into
packages by layer:

```
com.amiran.stationmonitor
├── domain        models (Station, Device, DeviceStatus, DeviceType, DataError) and the StationRepository interface
├── data
│   ├── remote    Retrofit API, DTOs, retry policy, exception -> DataError mapping
│   ├── local     Room database, entities, DAOs
│   ├── mapper    DTO -> Entity -> domain model
│   └── repository OfflineFirstStationRepository
├── di            Hilt modules (network, database, repository)
└── ui
    ├── common    ListUiState, RefreshTracker, formatting
    ├── components shared composables (status pill, badges, banners, full-screen messages)
    ├── stations  stations screen + ViewModel, search and country filter
    ├── devices   station detail screen + ViewModel
    ├── navigation Navigation 3 setup
    └── theme     colors, typography (Nunito Sans), Material 3 theme
```

### Data flow

```
Compose screen ──refresh()──▶ ViewModel ──refreshStations()──▶ Repository ──GET + retry──▶ API
      ▲                          │  ▲                               │
      └──── StateFlow<UiState> ──┘  └──── Flow from Room ◀── replaceAll() in a transaction
```

- **Room is the single source of truth.** Screens only ever read the database. A refresh
  downloads data and writes it into Room; Room's Flows then emit the new data automatically.
  Offline mode is therefore not a separate code path: the cache is simply what is shown.
- **Refresh replaces the cache in one transaction.** Stations or devices removed on the server
  disappear from the cache as well (a plain upsert would keep them forever), and observers never
  see a half-written list.
- **The UI state is derived, not mutated.** Each ViewModel combines the cache Flow with the
  status of the last refresh into one `StateFlow`. `ListUiState` is a sealed type with exactly
  one of `Loading`, `Content(items, updatedAt, isRefreshing, isStale)`, `Empty` or `Error`.

### Handling the unreliable API

The mock API fails about 23 % of requests on purpose (500, 503, a 30 s silence followed by 504,
slow responses of 2–5 s).

| Situation | Behaviour |
| --- | --- |
| 500 / 503 / connection reset | retried up to 2 times with exponential backoff (0.5 s, 1 s) |
| Server silent (timeout) | read timeout 8 s, call timeout 10 s, retried once — every timeout already costs the user seconds |
| No internet, 4xx, invalid JSON | not retried, retrying cannot help |
| All attempts failed, cache exists | cached data stays on screen, "Showing saved data · from 17:28" banner, Snackbar with Retry |
| All attempts failed, no cache | full-screen error with Try again |
| Server returned an empty list | Empty state (different from "not loaded yet") |

The worst case (the test station whose detail always times out) is about 16 s with a loading
indicator before the error is shown.

### Robustness against API changes

- `status` and `type` are parsed as Strings. Unknown values become `DeviceStatus.UNKNOWN` /
  `DeviceType.Unknown(rawCode)` instead of crashing; an unknown type still shows its code.
- Raw values are stored in the cache, so a future app version that knows a new status shows it
  correctly without downloading again.
- `lastSeen = null` is shown as "Never seen"; a malformed timestamp is treated the same way.
- Unknown JSON fields are ignored (`ignoreUnknownKeys`).

## Key decisions

| Decision | Why |
| --- | --- |
| Native Android + Jetpack Compose | The assignment allows Compose or CMP; Compose keeps the focus on the requested architecture. Sharing the data layer with iOS via KMP is the natural next step (see IMPROVEMENTS.md). |
| MVVM + UDF, not full MVI | Two screens with two actions (refresh, open station). One immutable UiState per screen gives the main MVI benefit without reducers and intent classes. |
| Hilt | Google's recommended DI for Android, dependency graph is checked at compile time. Assisted injection passes the station id into `DevicesViewModel`. |
| Navigation 3 | Stable since late 2025, built for Compose. The back stack is a plain list of typed, serializable keys, so there are no string routes. |
| Room | SQL checked at compile time, reactive `Flow` queries, schema exported for future migrations. |
| Retrofit + OkHttp + kotlinx.serialization | Standard, coroutine-friendly stack; kotlinx.serialization needs no reflection and works in KMP. |
| Separate DTO / Entity / domain models | API, database and UI change for different reasons; unknown values are handled in exactly one place (the mapper). |
| UI events as state | The refresh error for the Snackbar is part of the UI state and is cleared after it is shown, so it survives rotation and is not shown twice. |
| Devices sorted problems first | An operator opening a station wants to see what is broken without scrolling. |
| Brand look | Colors, outlined badges and typography follow the company's existing app; Material 3 components underneath. Light and dark theme. |
| English + Czech | UI strings are resources with an English default and a Czech translation (including Czech plural forms). English stays the fallback because the stations are in 6 countries; Czech phones get Czech automatically, and Android 13+ users can pick the app language in system settings (`generateLocaleConfig`). |
| Search in memory | The ViewModel filters the cached list (147 stations, well under a millisecond). SQLite `LIKE` cannot ignore diacritics, and the data layer stays untouched. The query is kept in `SavedStateHandle`, so it survives process death. |

## Tests

JVM unit tests (`app/src/test`), no emulator needed:

- `OfflineFirstStationRepositoryTest` — caching, server order, retries, failure keeps cache, removal of stale stations/devices
- `RetryPolicyTest` — retry budgets per failure type, backoff timing (virtual time), error mapping
- `ListUiStateTest` — every combination of cache state × refresh state
- `StationsViewModelTest`, `DevicesViewModelTest` — state transitions, no parallel refreshes, Snackbar shown once, filters and their restore after process death, sorting and summary
- `StationFilterTest` — search ignoring case and diacritics, multi-word queries, country filter, chip counts
- `MappersTest`, `DeviceModelTest` — parsing of unknown values, null/invalid timestamps, special characters

The tests use hand-written fakes (an in-memory API and DAOs extending the real abstract DAOs)
instead of a mocking library.

## Tech stack

Kotlin 2.4 · AGP 9.5 (built-in Kotlin) · Jetpack Compose (BOM 2026.11) + Material 3 ·
Navigation 3 · Lifecycle ViewModel · Hilt 2.60 · Room 2.8 · Retrofit 3 · OkHttp 5 ·
kotlinx.serialization · Coroutines/Flow · JUnit 4 + kotlinx-coroutines-test.
Versions are kept in `gradle/libs.versions.toml`.

## Licenses

Nunito Sans font: SIL Open Font License 1.1 (`licenses/NunitoSans-OFL.txt`).
Icons: Material Icons, Apache License 2.0.
