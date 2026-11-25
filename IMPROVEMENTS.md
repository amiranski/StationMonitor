# Improvements

What I would add next if this app were developed further, roughly in order of value.

## 1. UI and integration tests

**What:** Compose UI tests for both screens (loading, error, empty, stale states, navigation to
the detail), an instrumented Room DAO test with an in-memory database, and screenshot tests of
the `@Preview`s (Compose Preview Screenshot Testing or Roborazzi).

**Why:** The unit tests cover the logic (repository, retry, state derivation, ViewModels). What
is not covered yet is that the SQL queries really do what the fakes do, and that each state
renders correctly. Screenshot tests also catch visual regressions in the brand styling and in
long texts (the API contains a deliberately very long station name).

## 2. CI/CD

**What:** A GitHub Actions workflow running `./gradlew lint testDebugUnitTest assembleDebug`
on every pull request, plus building a signed release on tags.

**Why:** Every change is verified the same way, independent of anyone's machine. Lint and tests
become a merge requirement instead of a habit.

## 3. Crash and performance reporting

**What:** Firebase Crashlytics (or Sentry) and Firebase Performance Monitoring for release
builds.

**Why:** Unknown values from the API are already handled defensively, but in production the
only way to learn about the crashes that were not foreseen is to report them. Network timing
data would also show how often users actually hit the slow/timeout paths.

## 4. Release hardening with R8

**What:** Verify the release build with R8 (already enabled in `app/build.gradle.kts`): keep
rules for kotlinx.serialization and Retrofit where the libraries' own rules are not enough, and
a smoke test of the minified APK. Add baseline profiles for faster startup and scrolling.

**Why:** R8 shrinks and obfuscates the app, but reflection-like code (serializers, Retrofit
proxies) can break silently only in release builds.

## 5. Search and filtering

**What:** Search by station name or address, filter by country, and a "stations with problems
only" filter on the list.

**Why:** The API returns 147 stations in 6 countries. An operator usually looks for one station
or for the ones that need attention. Filtering in Room with a query is cheap and also works
offline.

## 6. Problems overview on the stations list

**What:** Show the number of devices in ERROR/UNKNOWN state directly on each station row and
sort or highlight stations with problems.

**Why:** Today the list shows only the device count. The station endpoint does not return
statuses, so this needs either a backend change (aggregated status in the list response) or
background prefetching of devices — I would discuss the backend option first.

## 7. Background refresh and staleness policy

**What:** WorkManager periodic sync while the app is in the background, and a "data older than
N minutes" rule that triggers a refresh when the screen opens.

**Why:** Device status is operational data; showing it fresh without waiting for a pull to
refresh is a clear UX win. A staleness policy also avoids refetching on every screen visit.

## 8. Retry improvements

**What:** Add jitter to the exponential backoff, respect `Retry-After` headers, and observe
connectivity (`ConnectivityManager`) to refresh automatically when the network comes back and
to show "You're offline" instead of a generic error.

**Why:** Jitter prevents many clients retrying in lockstep against a struggling server;
connectivity awareness makes offline mode feel intentional.

## 9. Kotlin Multiplatform / Compose Multiplatform

**What:** Move `domain` and `data` into a shared KMP module (Ktor instead of Retrofit, Room KMP
or SQLDelight, Koin or a KMP-friendly DI) and consume it from the existing Android UI and from an
iOS app (SwiftUI or Compose Multiplatform).

**Why:** The data layer — caching, retry rules, unknown-value handling — is exactly the logic
that should behave identically on both platforms. The current architecture already separates it
cleanly, and kotlinx.serialization and Room 2.8 are KMP-ready, so the move is incremental.

## 10. Modularization

**What:** Split into `:core:data`, `:core:domain`, `:core:ui`, `:feature:stations`,
`:feature:devices` Gradle modules with convention plugins.

**Why:** Not worth it for two screens (it would only add build configuration), but it becomes
valuable as features and the team grow: faster incremental builds and enforced layer boundaries.

## 11. Smaller polish

- Tablet / foldable layout: list-detail side by side (Navigation 3 scenes support this directly).
- Proper database migrations instead of the destructive fallback once the cache holds anything
  the user cannot re-download.
- Accessibility pass with TalkBack and large font sizes; content descriptions are already in
  place for icon buttons.
- More languages (Slovak, Polish, German, Hungarian — the countries in the data).
