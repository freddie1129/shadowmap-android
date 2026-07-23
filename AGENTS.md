# Repository Guidelines

## Project Structure & Module Organization
This is a single-module Android app. App code lives in [`app/src/main/java/com/gooludou/shadowplanner`](./app/src/main/java/com/gooludou/shadowplanner), with Compose theme code under [`ui/theme`](./app/src/main/java/com/gooludou/shadowplanner/ui/theme). Resources are in [`app/src/main/res`](./app/src/main/res), and tests are split between [`app/src/test`](./app/src/test) for local JVM tests and [`app/src/androidTest`](./app/src/androidTest) for instrumented tests.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds the debug APK.
- `./gradlew test` runs local unit tests.
- `./gradlew connectedAndroidTest` runs instrumented tests on a device or emulator.
- `./gradlew lint` runs Android lint checks if available in the current Gradle setup.

## Coding Style & Naming Conventions
Use Kotlin and Jetpack Compose conventions: 4-space indentation, `PascalCase` for composables/classes, `camelCase` for functions, properties, and state variables. Keep composables small and state hoisted where practical. Follow the existing package naming rooted at `com.example.shadowmap`. Keep resource names lowercase with underscores, such as `mapbox_access_token.xml`.

## Compose UI Guidelines
New reusable Compose components should use `ShadowMapDesign.dimensions` for padding, spacing, margins, touch targets, and repeated layout sizes. Use `MaterialTheme.shapes`, `MaterialTheme.colorScheme`, and `MaterialTheme.typography` instead of hardcoded shapes, colors, and text styles. Use raw `dp` values only for component-specific drawing details or one-off visual geometry; add a named design token when a value is reused. Every new reusable composable should include a `@Preview`. Keep system inset handling at the screen or container level where possible, and avoid baking status or navigation bar padding into small reusable components.

For theme-sensitive Compose UI, add explicit light-mode and dark-mode previews. Preview the component inside the same themed `Surface`, sheet, or container that supplies its runtime background and content colors so the previews accurately represent the emulator or device.

## Testing Guidelines
Local tests use JUnit in `app/src/test`, and Android tests use `AndroidJUnit4` in `app/src/androidTest`. Name tests clearly after behavior, for example `MapScreen_showsControlsByDefault`. Prefer adding regression tests alongside feature changes when the behavior can be exercised without Mapbox or device-only dependencies.

Do not automatically install or launch the app on an emulator or physical device, and do not interact with emulator/device UI for verification. The developer performs runtime testing manually and will report the results. Use builds, local unit tests, lint, and static analysis for automated verification unless the developer explicitly requests an emulator or device test.

## Commit & Pull Request Guidelines
The current history uses short, descriptive imperative commits, such as `Initial commit: Compose app with Mapbox map + interactive shadow controls`. Keep commit subjects focused on one change. Pull requests should include a concise summary, the commands used to verify the change, and screenshots or screen recordings for UI work.

## Security & Configuration Tips
Do not commit Mapbox secrets. The public token file [`app/src/main/res/values/mapbox_access_token.xml`](./app/src/main/res/values/mapbox_access_token.xml) is listed in `.gitignore`; keep private download credentials in `~/.gradle/gradle.properties` as `MAPBOX_DOWNLOADS_TOKEN`.
