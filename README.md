# Calendar

Calendar is a personal calendar and task app for Android, written in Kotlin with Jetpack Compose. This repository holds the app only: events, recurrence, tasks, categories and reminders are stored by a separate backend (the private `manage-api`), which the app reaches over HTTP.

The app has three tabs (tasks, calendar and settings) plus an agenda and a categories screen. It shows the month as a swipeable grid with the selected day's agenda, creates and edits events with recurrence rules, completes and removes occurrences, lists tasks by today, upcoming, overdue and done, and can attach an action to a task that opens a link or another installed app. Reminders ring through local alarms kept in sync with the backend by a WorkManager job, and an optional daily digest notification sums up the day. A home screen widget (Glance) shows the day's tasks and is configured per instance. When the backend gates a write, the app asks for an 8-digit access code on a keypad of shuffled digit pairs and sends it with the following requests.

| Part | What it is |
| --- | --- |
| [`app/src/main/java/.../core`](app/src/main/java/com/archieapps/calendar/core) | HTTP client and DTOs (`net`), local alarms, digest and notifications (`alarm`), reminder sync worker (`sync`), local stores and avatar loading. |
| [`app/src/main/java/.../feature`](app/src/main/java/com/archieapps/calendar/feature) | Screens and view models: `auth`, `calendar`, `agenda`, `tasks`, `categories`, `settings` and the `widget`. |
| [`app/src/main/java/.../design`](app/src/main/java/com/archieapps/calendar/design) | Theme, palette, typography, spacing and the shared Compose components. |
| [`docs`](docs) | Design notes and plans written while building the event details sheet. |

## Requirements

- JDK 11+ (the build targets Java 11)
- Android SDK with compile SDK 37; the app runs on Android 8.0 (API 26) and up
- Gradle 9.5 (through the wrapper) and Android Gradle Plugin 9.3
- A running instance of the backend

## Configuration

The backend address is read at build time from `env.properties` in the repository root, which is ignored by git. Copy the example and set `BASE_URL`:

```bash
cp env.properties.example env.properties
```

Without the file the build uses `http://10.0.2.2:8001/`, which is the host machine as seen from the Android emulator. A missing trailing slash is added by the build.

## Build and run

Build a debug APK and install it on a connected device or emulator:

```bash
./gradlew installDebug
```

Build the release APK (R8 enabled, signed with the debug key):

```bash
./gradlew assembleRelease
```

## Tests

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

They cover the pure models: recurrence rules and presets, event drafts, calendar bounds, occurrence projection, task actions and the task model, agenda filters and paging, settings, the digest text, the task snapshot store and the widget configuration.
