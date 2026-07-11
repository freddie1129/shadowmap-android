# Repository Guidelines

## Project Structure & Module Organization
This is a single-module Android app. App code lives in [`app/src/main/java/com/example/shadowmap`](./app/src/main/java/com/example/shadowmap), with Compose theme code under [`ui/theme`](./app/src/main/java/com/example/shadowmap/ui/theme). Resources are in [`app/src/main/res`](./app/src/main/res), and tests are split between [`app/src/test`](./app/src/test) for local JVM tests and [`app/src/androidTest`](./app/src/androidTest) for instrumented tests.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds the debug APK.
- `./gradlew test` runs local unit tests.
- `./gradlew connectedAndroidTest` runs instrumented tests on a device or emulator.
- `./gradlew lint` runs Android lint checks if available in the current Gradle setup.

## Coding Style & Naming Conventions
Use Kotlin and Jetpack Compose conventions: 4-space indentation, `PascalCase` for composables/classes, `camelCase` for functions, properties, and state variables. Keep composables small and state hoisted where practical. Follow the existing package naming rooted at `com.example.shadowmap`. Keep resource names lowercase with underscores, such as `mapbox_access_token.xml`.

## Testing Guidelines
Local tests use JUnit in `app/src/test`, and Android tests use `AndroidJUnit4` in `app/src/androidTest`. Name tests clearly after behavior, for example `MapScreen_showsControlsByDefault`. Prefer adding regression tests alongside feature changes when the behavior can be exercised without Mapbox or device-only dependencies.

## Commit & Pull Request Guidelines
The current history uses short, descriptive imperative commits, such as `Initial commit: Compose app with Mapbox map + interactive shadow controls`. Keep commit subjects focused on one change. Pull requests should include a concise summary, the commands used to verify the change, and screenshots or screen recordings for UI work.

## Security & Configuration Tips
Do not commit Mapbox secrets. The public token file [`app/src/main/res/values/mapbox_access_token.xml`](./app/src/main/res/values/mapbox_access_token.xml) is listed in `.gitignore`; keep private download credentials in `~/.gradle/gradle.properties` as `MAPBOX_DOWNLOADS_TOKEN`.
