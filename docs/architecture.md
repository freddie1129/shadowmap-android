# ShadowMap Android architecture

ShadowMap is a single Android application module organised by dependency direction and feature
ownership. Package boundaries are enforced by `ArchitectureBoundaryTest` during local unit tests.

## Package layout

```text
com.gooludou.shadowplanner
├── app/navigation        Application navigation and destination wiring
├── core
│   ├── model             Shared domain models
│   ├── geometry          Pure geometry and matching logic
│   ├── solar             Solar calculations
│   ├── shadow            Shadow calculations
│   └── ui                Shared Compose theme and reusable components
├── feature
│   ├── shadowmap         Map state, screen orchestration, drawing, and scene UI
│   ├── locationsearch    Location search flow
│   ├── projects          Project list and save UI
│   └── settings          Settings UI
├── renderer
│   ├── mapbox            Active Mapbox rendering, queries, and building loading
│   └── filament          Retained legacy Filament scene implementation
├── location              Location services and search data access
├── project               Project persistence and serialization
└── di                    Dependency injection bindings
```

## Dependency direction

```text
app → feature → renderer → core
        │           │
        ├───────────┼──→ location
        └───────────┴──→ project
```

- `core` must not import application, feature, renderer, location, or project packages.
- Renderers may depend on `core`, Android, and their rendering SDK, but not on features or each
  other.
- Features own their ViewModels, screens, components, and feature-specific state transformations.
- `app/navigation` composes features and is the only application-level navigation owner.
- Package declarations must match their source directory.

## Renderer policy

Mapbox is the active map and 3D scene path. The Filament implementation is intentionally retained
under `renderer/filament` for possible future use. Shared behavior must not be implemented by
making the Mapbox and Filament packages depend on each other; move genuinely shared domain logic
into `core` instead.

## Adding code

1. Put domain-only calculations and models in the appropriate `core` package.
2. Put SDK-specific rendering code in its renderer package.
3. Put screens, ViewModels, and feature-specific UI state under the owning feature.
4. Add reusable Compose primitives to `core/ui` only when more than one feature needs them.
5. Run `./gradlew test assembleDebug lint` before committing.
