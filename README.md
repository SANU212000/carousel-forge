# CarouselForge

Android app for building multi-slide carousel posts (Instagram, LinkedIn, TikTok) from your photos. Drag, scale, and rotate layers on a canvas, slice tall images into slides, snap to grid, preview safe zones, and export at 3× supersampled resolution.

**Package:** `com.sanu.carouselforge`  
**Min SDK:** 26 · **Target SDK:** 36  
**Stack:** Kotlin · Jetpack Compose · Room · Navigation Compose

---

## Features

| Area | Description |
|------|-------------|
| **Gallery** | Project list — create, open, and delete carousel projects |
| **Editor** | Multi-layer canvas with drag / scale / rotate gestures |
| **Snap engine** | Grid and sibling-edge alignment with spring settle animation |
| **Slice engine** | Split a tall image into equal square slides (e.g. 1080×4320 → 4×1080×1080) |
| **Safe-zone overlay** | Instagram profile-avatar crop preview on slide 1 |
| **Export** | Offscreen 3× supersampled PNG export to device gallery via MediaStore |
| **Autosave** | Debounced (400 ms) non-destructive save to Room on every edit |
| **Error handling** | Typed `AppError` hierarchy with user-facing messages per error type |

---

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- Android device or emulator (API 26+)

### Build & Run

```bash
./gradlew assembleDebug
./gradlew installDebug
```

### Run Tests

```bash
./gradlew test              # Unit tests (SnapEngine, SliceEngine, BitmapUtils)
./gradlew connectedAndroidTest   # Instrumented tests (Room, golden path)
```

---

## Project Structure

```
reading_app/                          # Repository root (Gradle project: carousel_forge)
├── app/                              # Android application module
│   ├── build.gradle.kts              # App-level dependencies and build config
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # App manifest, MainActivity launcher
│       │   ├── java/com/sanu/carouselforge/
│       │   │   ├── CarouselForgeApp.kt       # Application class, DI bootstrap
│       │   │   ├── MainActivity.kt           # Single-activity Compose host
│       │   │   ├── core/                     # Shared infrastructure
│       │   │   │   ├── di/
│       │   │   │   │   └── AppModule.kt      # Manual DI: Room, repository, file store
│       │   │   │   ├── error/
│       │   │   │   │   ├── AppError.kt       # Sealed error hierarchy
│       │   │   │   │   └── ErrorObserver.kt  # Logging / error observation hook
│       │   │   │   ├── theme/
│       │   │   │   │   ├── AppTheme.kt       # Composable theme entry point
│       │   │   │   │   ├── Color.kt          # Color primitives + semantic tokens
│       │   │   │   │   ├── Motion.kt         # Duration and animation curve tokens
│       │   │   │   │   ├── Spacing.kt        # 8pt spacing scale
│       │   │   │   │   └── Typography.kt     # Type scale
│       │   │   │   └── util/
│       │   │   │       └── BitmapUtils.kt    # inSampleSize calc, image downsampling
│       │   │   ├── data/                     # Persistence layer
│       │   │   │   ├── local/
│       │   │   │   │   ├── AppDatabase.kt    # Room database definition
│       │   │   │   │   ├── LayerEntity.kt    # Layer table entity
│       │   │   │   │   ├── ProjectDao.kt     # Room DAO (projects + layers)
│       │   │   │   │   └── ProjectEntity.kt  # Project table entity
│       │   │   │   └── repository/
│       │   │   │       ├── LocalProjectRepository.kt  # Room + file store impl
│       │   │   │       ├── ProjectFileStore.kt      # Internal image file storage
│       │   │   │       └── ProjectRepository.kt     # Repository interface
│       │   │   ├── features/                 # Feature modules (screens + logic)
│       │   │   │   ├── gallery/
│       │   │   │   │   ├── GalleryScreen.kt      # Project list home screen
│       │   │   │   │   └── GalleryViewModel.kt   # Gallery state management
│       │   │   │   ├── editor/
│       │   │   │   │   ├── EditorScreen.kt       # Main editor UI
│       │   │   │   │   ├── EditorViewModel.kt    # Editor state (sealed EditorState)
│       │   │   │   │   ├── components/
│       │   │   │   │   │   ├── CanvasViewport.kt # Canvas viewport wrapper
│       │   │   │   │   │   ├── EditorPreviews.kt # Compose @Preview composables
│       │   │   │   │   │   ├── EditorTopBar.kt   # Editor toolbar
│       │   │   │   │   │   ├── LayerToolDock.kt  # Layer add/remove/reorder controls
│       │   │   │   │   │   └── SplitControls.kt   # Slice/split UI controls
│       │   │   │   │   ├── overlay/
│       │   │   │   │   │   └── SafeZoneOverlay.kt  # IG avatar safe-zone overlay
│       │   │   │   │   ├── render/
│       │   │   │   │   │   ├── EditorCanvas.kt     # Per-layer Compose canvas
│       │   │   │   │   │   ├── LayerModel.kt       # Immutable layer data class
│       │   │   │   │   │   └── TransformGestures.kt # Combined drag/scale/rotate
│       │   │   │   │   ├── slice/
│       │   │   │   │   │   └── SliceEngine.kt      # Tall-image bisection math
│       │   │   │   │   └── snap/
│       │   │   │   │       └── SnapEngine.kt       # Grid + edge snap logic
│       │   │   │   └── export/
│       │   │   │       ├── ExportEngine.kt     # Offscreen 3× render + MediaStore save
│       │   │   │       ├── ExportScreen.kt     # Export progress / result UI
│       │   │   │       └── ExportViewModel.kt  # Export state management
│       │   │   ├── navigation/
│       │   │   │   ├── NavGraph.kt             # NavHost + screen wiring
│       │   │   │   └── NavRoutes.kt            # @Serializable typed routes
│       │   │   └── ui/
│       │   │       └── theme/                  # Legacy/default Compose theme stubs
│       │   └── res/                            # Android resources
│       │       ├── drawable/                   # Vector drawables (launcher icons)
│       │       ├── mipmap-*/                   # App launcher icons (all densities)
│       │       ├── values/
│       │       │   ├── strings.xml             # App name and string resources
│       │       │   └── themes.xml              # XML theme (splash / window)
│       │       └── xml/
│       │           ├── backup_rules.xml        # Auto-backup rules
│       │           └── data_extraction_rules.xml
│       ├── test/                               # JVM unit tests
│       │   └── java/com/sanu/carouselforge/
│       │       ├── core/util/
│       │       │   └── BitmapUtilsTest.kt
│       │       └── features/editor/
│       │           ├── slice/
│       │           │   └── SliceEngineTest.kt
│       │           └── snap/
│       │               └── SnapEngineTest.kt
│       └── androidTest/                        # Instrumented / UI tests
│           └── java/com/sanu/carouselforge/
│               ├── CarouselGoldenPathTest.kt   # Import → drag → export golden path
│               └── data/local/
│                   └── ProjectDaoInstrumentedTest.kt
├── gradle/
│   ├── libs.versions.toml                      # Version catalog (deps + plugins)
│   └── wrapper/
│       └── gradle-wrapper.properties           # Gradle wrapper version pin
├── planning/                                   # (empty) reserved for design docs
├── build.gradle.kts                            # Root Gradle build file
├── settings.gradle.kts                         # Module includes (app)
├── gradle.properties                           # Gradle JVM / Android flags
├── .gitignore                                  # Git ignore rules
└── CLAUDE.md                                   # Master engineering blueprint / architecture prompt
```

---

## Architecture

```
UI (Compose screens)
  └── ViewModels (StateFlow<sealed State>)
        └── ProjectRepository (interface)
              ├── Room (ProjectEntity, LayerEntity, ProjectDao)
              └── ProjectFileStore (internal image files)

Editor rendering
  └── SnapshotStateList<LayerModel>
        └── per-layer graphicsLayer + TransformGestures
              └── SnapEngine (on gesture end)

Export
  └── ExportEngine (Dispatchers.Default)
        └── 3× supersampled composite → MediaStore PNG
```

### Navigation Routes

| Route | Type | Screen |
|-------|------|--------|
| `GalleryRoute` | object | Project list (start destination) |
| `EditorRoute(projectId)` | data class | Canvas editor |
| `SafeZonePreviewRoute(projectId)` | data class | Safe-zone dialog |
| `ExportRoute(projectId)` | data class | Export screen |
| `SettingsRoute` | object | Settings |

### Key Dependencies

| Library | Purpose |
|---------|---------|
| Jetpack Compose BOM | UI framework |
| Navigation Compose + kotlinx.serialization | Typed navigation |
| Room + KSP | Local metadata storage |
| DataStore Preferences | App settings |
| Coil | Image loading / decoding |
| ExifInterface | Camera photo orientation correction |

---

## Design Principles

Defined in detail in [`CLAUDE.md`](CLAUDE.md):

- **Canvas-first** — one `graphicsLayer` per layer; no full-canvas redraw on drag
- **Token-only styling** — no raw `Color(0x…)` or `.dp` literals outside `core/theme/`
- **Sealed state** — no `isLoading` / `error` / `data` triple-boolean anti-pattern
- **Repository boundary** — screens never touch Room or `File` APIs directly
- **Background compute** — bitmap decode and export compositing on `Dispatchers.Default`
- **Non-destructive autosave** — 400 ms debounce; killed process must not lose work

---

## License

Private project — no license specified.
