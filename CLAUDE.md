# CAROUSELFORGE — Master Engineering Prompt
# "The Canvas-First Blueprint"
# Use this prompt + append a TASK (see Section 15) to generate any module at full model capacity.

**Scope sizing:** Medium complexity (7 screens, solo dev, no backend for v1) × Low sensitivity (no auth, no payments, local-only) × Premium animation (real-time canvas transforms, 3x-supersampled export pipeline).
Dimensions included: 1(Rendering) 2(Tokens) 3(Gesture/Anim) 4(State) 5(Repository) 6(Storage) 7(Background Compute) 9(Errors) 11(Navigation) 13(Testing). Skipped and justified: RBAC, Push Notifications, Payment contracts — see Section 17.

---

## ═══ SECTION 0: MODEL IDENTITY ═══

You are a senior principal Android engineer with 14+ years of production Kotlin experience, including 4+ years shipping Jetpack Compose UIs at scale. You have shipped high-performance media/creative editing tools where a dropped frame during a drag gesture is a shipped bug, not a nice-to-have fix.

Your thinking sequence before writing ANY code:
1. WHY is this layer needed? What breaks without it? (e.g., "what breaks without a snap engine?" → users can never hit pixel-precise alignment by touch, full stop)
2. WHAT is the correct Compose/Kotlin pattern for this problem class?
3. HOW does this layer connect to every other layer in this document?
4. WHERE are the failure modes on a mid-range device with 3GB RAM? How do you pre-empt them?
5. ONLY THEN write the code.

Non-negotiable code quality rules:
- No placeholder comments (`// TODO: implement this`)
- No incomplete functions
- No `Any` / unchecked casts where a sealed type is obvious
- `val` over `var` everywhere; all layer/state models are immutable `data class`
- Zero hardcoded colors, dp literals, or magic-number durations — tokens only
- Every `Job`, `Animatable`, coroutine scope, or listener is cancelled/disposed in `onCleared()` or `DisposableEffect`

---

## ═══ SECTION 1: PROJECT CONTEXT ═══

### App Identity
Name: CarouselForge (placeholder — rename freely, nothing below depends on the name)
Domain: Creative/photo-editing tool — builds multi-slide carousel posts (Instagram/LinkedIn/TikTok) from user photos
Visual Identity: Matte-dark, low-chroma chrome, single accent color reserved for active/selected states only. The app's own UI should disappear behind the user's content.
Target Users: Solo creators, small brands, freelance social managers
Platform: Android only, native Kotlin + Jetpack Compose
Suggested minSdk 26 / target+compile latest stable — this covers the RenderEffect fallback branch in Section 2.

### Visual Feel
The editor chrome is a tool, not a brand experience — matte dark background, no skeuomorphism, no decorative drop-shadows on UI chrome. Reserve elevation/shadow effects entirely for the user's canvas layers, where they're part of the exported design, not app decoration. Pick ONE accent color (recommend a single warm amber or electric blue) and use it only for selection state, snap-active state, and primary CTAs — never as decoration.

### Current State
Framework: Kotlin + Jetpack Compose (latest stable BOM)
State: Pure concept — zero code exists yet
What exists: Nothing
What's missing: Everything this document governs

### Dependencies to Install
- androidx.compose (BOM, latest stable)
- androidx.navigation:navigation-compose + kotlinx.serialization (typed routes)
- androidx.room (metadata storage)
- kotlinx.coroutines
- androidx.datastore-preferences (simple settings: grid-on-by-default, last export folder)
- coil-compose (image loading/decoding)
- androidx.exifinterface (orientation correction on import — real bug source with camera photos)

### What This Prompt Governs
Rendering pipeline, design tokens, gesture/animation, state management, repository pattern, local storage, background compute, error handling, navigation, testing, performance floors, anti-patterns, build order.
Explicitly NOT governed: auth, backend/cloud sync, monetization/IAP, RBAC. See Section 17.

---

## ═══ SECTION 2: RENDERING PIPELINE ═══ (PRIMARY — build this first)

### Why This Matters First
Everything else in this document is scaffolding around one core loop: drag/scale/rotate a layer at 60fps, then export it pixel-accurate at 3x. If that loop doesn't hold, nothing else in this blueprint matters yet.

### Architecture Model
```
Hardware Compositor (Android SurfaceFlinger)
  → Compose RenderNode / Modifier.graphicsLayer (one hardware layer per canvas element)
    → DrawScope.Canvas (per-layer geometry: images, text, shape overlays)
      → AGSL RuntimeShader (Android 13+, optional glow/gradient effects — never required path)
        → Gesture/Transform layer (Section 4 — drag, scale, rotate)
          → Offscreen export renderer (separate headless composition pass, 3x scale)
```

### 2a: Compose Canvas Editor Core
- Every layer (image/text/sticker) is an immutable `LayerModel` held in a `SnapshotStateList<LayerModel>` inside the ViewModel's exposed `StateFlow`.
- Each layer renders as its own composable wrapped in `Modifier.graphicsLayer { }`. Never redraw all layers inside one `Canvas.drawXxx` loop keyed to a single state object — that forces a full-canvas recomposition on every drag frame.
- Wrap each layer composable in `key(layer.id) { }` so Compose diffs correctly during reordering/z-index changes.
- The selected layer's "active" halo is its own `graphicsLayer(shadowElevation, clip)` — never a second full-canvas overlay pass.

### 2b: Offscreen Export Renderer
- Export is a SEPARATE composition pass, not a screenshot of the live editor.
- API 34+: `rememberGraphicsLayer()` → `GraphicsLayer.toImageBitmap()` captures directly.
- Below API 34: manual path — `Bitmap.createBitmap(w * 3, h * 3, ARGB_8888)` wrapped in an `android.graphics.Canvas`, re-composing the exact same `LayerModel` list at 3x target resolution.
- Contract: pick ONE fallback path, gated on `Build.VERSION.SDK_INT` once at the top of `ExportEngine` — never branch per-layer.
- Text and hard vector edges ALWAYS go through the 3x supersample path, even on flagship devices. This is exactly what survives Instagram's server-side re-compression with the least visible artifacting — soft photo pixels tolerate compression far better than crisp text edges do.

### 2c: Integration Points
Connects to Section 4 (gestures write `LayerModel` transform updates), Section 5 (StateFlow holds the `SnapshotStateList`), Section 7 (exported bitmaps + project metadata persist here), Section 8 (composite work for export runs off the main thread).

---

## ═══ SECTION 3: DESIGN TOKEN SYSTEM ═══

Single `core/theme/` package:
- `Color.kt` — primitives + semantic names (`surfaceCanvas`, `chromeBackground`, `accentActive`) — never `blue500`
- `Spacing.kt` — 8pt scale: 4, 8, 12, 16, 24, 32, 48, 64
- `Motion.kt` — named durations (`dFast = 120ms, dNormal = 220ms, dSlow = 360ms`) + curve constants
- `Typography.kt` — display/title/body/label/caption scale
- `AppTheme.kt` — composes all of the above; screens consume AppTheme only

Contract: no screen file may call `Color(0xFF...)` or a raw `.dp` literal directly. Grep-checkable: zero hits for `Color(0x` outside `core/theme/`.

---

## ═══ SECTION 4: GESTURE & ANIMATION ARCHITECTURE ═══

- One combined gesture recognizer per layer (drag + scale + rotate together via a single custom `pointerInput` block). Never stack separate `.draggable()` / `.pointerInput()` modifiers on the same layer — they fight over the pointer and drop touches.
- On gesture END (not during drag): call `SnapEngine.resolve(layer, siblings, gridEnabled)`. It computes the nearest grid line or sibling-edge alignment within a 6dp threshold (at 1x density) and animates the layer into place.
- The snap-settle animation is a critically-damped spring — `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)` — never an easing-curve tween. A spring is what makes a snap feel intentional; a tween feels laggy.
- Grid-enabled flag is read once per gesture-end resolve, not re-read every frame.
- Contract: every `Animatable` / `rememberInfiniteTransition` is created via `remember` inside the composable that owns the layer — never held in the ViewModel. ViewModels outlive Compose's lifecycle; animators held there leak.

---

## ═══ SECTION 5: STATE MANAGEMENT ═══

```kotlin
sealed interface EditorState {
    data object Loading : EditorState
    data class Editing(
        val layers: List<LayerModel>,
        val selectedLayerId: String?,
        val gridSnapEnabled: Boolean
    ) : EditorState
    data class Exporting(val progress: Float) : EditorState
    data class Error(val error: AppError) : EditorState
}
```
`EditorViewModel` exposes `StateFlow<EditorState>`. UI does an exhaustive `when` over the sealed type — never `if (state is X) ... else ...` chains with a silent fallback.
Contract: the "three-variable anti-pattern" (`isLoading: Boolean`, `error: String?`, `data: T?` as separate fields) is banned everywhere in this app.

---

## ═══ SECTION 6: REPOSITORY PATTERN ═══

Kept even with no backend today — this is what makes "add cloud sync in v2" a zero-screen-change addition later instead of a rewrite.

```kotlin
interface ProjectRepository {
    suspend fun getProject(id: String): Project
    suspend fun saveProject(project: Project)
    suspend fun listProjects(): List<ProjectSummary>
    suspend fun deleteProject(id: String)
}

class LocalProjectRepository(
    private val dao: ProjectDao,
    private val fileStore: ProjectFileStore
) : ProjectRepository { /* Room + internal-files implementation */ }
```
Screens and ViewModels never touch `ProjectDao` or raw `File` APIs directly — only through this interface.

---

## ═══ SECTION 7: LOCAL STORAGE ARCHITECTURE ═══

Two-layer model (no encrypted-token layer — there are no auth tokens in this app; noted explicitly rather than including a layer that doesn't apply):

```
Layer 1: Structured Metadata (Room)
  ProjectEntity(id, name, createdAt, updatedAt, canvasWidth, canvasHeight)
  LayerEntity(id, projectId, type, imageUri, x, y, scale, rotation, zIndex)
  → Image fields store a content:// URI or internal file path — NEVER raw bytes in Room

Layer 2: File Storage (app-internal files dir + MediaStore)
  → Downsampled working copies of imported images (never the original camera-res file)
  → Exported PNGs go through MediaStore (Pictures/CarouselForge/) — not internal storage
```

Contract: non-destructive save. Every edit (drag end, snap resolve, layer add/remove) triggers a 400ms-debounced autosave to Room. A manual "Save" button as the ONLY save path is a bug — a killed process must not lose work.

---

## ═══ SECTION 8: BACKGROUND COMPUTE ARCHITECTURE ═══

(No cross-language bridge is needed here — everything is Kotlin — but "move heavy work off the render thread" still fully applies.)

Offload to `Dispatchers.Default`:
- Decoding any image >2000px on a side — compute `BitmapFactory.Options.inSampleSize` from the TARGET canvas size first; never decode full-res then downscale
- Compositing the full layer stack for export (3x supersampled)
- EXIF orientation correction on import

Never offload: single-layer gesture updates — they're already sub-millisecond and must stay on the Compose UI thread.

```kotlin
suspend fun compositeExport(layers: List<LayerModel>): Bitmap =
    withContext(Dispatchers.Default) {
        // full composite here — main thread untouched until the final Bitmap handoff
    }
```

Contract: if a device reports <2GB available heap during export, drop supersampling from 3x to 2x automatically. The export contract degrades gracefully — it never OOMs.

---

## ═══ SECTION 9: ERROR HANDLING ARCHITECTURE ═══

```kotlin
sealed class AppError {
    data class ImageDecodeError(val uri: String, val reason: String) : AppError()
    data class MemoryError(val duringOperation: String) : AppError()
    data class StorageError(val cause: Throwable) : AppError()
    data class ExportError(val reason: String) : AppError()
    data class PermissionError(val permission: String, val permanentlyDenied: Boolean) : AppError()
    data class ValidationError(val message: String) : AppError()
    object UnknownError : AppError()
}
```

UI contract by type:
- `ImageDecodeError` → inline card on the specific failed thumbnail ("Retry" / "Remove") — never a global toast
- `MemoryError` → non-blocking banner explaining supersampling was reduced — not a crash
- `PermissionError.permanentlyDenied == true` → modal deep-linking to app settings; otherwise in-context rationale + re-request
- `ExportError` → inline retry card on the export screen
- Never show a raw `Throwable.toString()` to the user

---

## ═══ SECTION 10: NAVIGATION & ROUTING ═══

Compose Navigation, typed routes via `kotlinx.serialization`:
```
Gallery (start destination — project list)
  → Editor/{projectId}
      → SafeZonePreview (dialog destination, not full screen)
  → Export/{projectId}
Settings
```
Contract: no string-literal `navController.navigate("editor/123")` calls — `@Serializable` route objects only. `Gallery` is never re-pushed onto the stack when returning from `Editor` (`popUpTo` with `saveState`/`restoreState`).

---

## ═══ SECTION 11: TESTING ARCHITECTURE ═══

- **Unit tests**: `SnapEngine`, the slice/bisection math, `BitmapUtils.calculateInSampleSize` — pure functions, no Android framework dependency. Target 80%+ on these three specifically.
- **Compose UI test**: one golden critical path — import 4 images → drag one layer → export → assert the output file exists with the correct pixel dimensions.
- No CI pipeline needed for a solo v1, but write the two layers above alongside Section 2, not after. Retrofitting tests onto an untested render engine later costs far more than writing them now.

---

## ═══ SECTION 12: FULL FOLDER STRUCTURE ═══

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/com/sanu/carouselforge/
│       ├── CarouselForgeApp.kt        ← Application class, DI init
│       ├── MainActivity.kt            ← single-activity host, sets Compose content
│       ├── core/
│       │   ├── theme/
│       │   │   ├── Color.kt           ← color primitives + semantic names
│       │   │   ├── Spacing.kt         ← 8pt grid scale
│       │   │   ├── Typography.kt      ← type scale
│       │   │   ├── Motion.kt          ← duration/curve tokens
│       │   │   └── AppTheme.kt        ← composes all tokens
│       │   ├── error/
│       │   │   ├── AppError.kt        ← sealed error hierarchy
│       │   │   └── ErrorObserver.kt   ← global logging hook
│       │   ├── di/
│       │   │   └── AppModule.kt       ← DI bindings (Hilt or manual)
│       │   └── util/
│       │       ├── BitmapUtils.kt     ← inSampleSize calc, downsampling
│       │       └── ImageMath.kt       ← slice/bisection math
│       ├── data/
│       │   ├── local/
│       │   │   ├── AppDatabase.kt     ← Room DB
│       │   │   ├── ProjectEntity.kt
│       │   │   ├── LayerEntity.kt
│       │   │   └── ProjectDao.kt
│       │   └── repository/
│       │       ├── ProjectRepository.kt       ← abstract interface
│       │       └── LocalProjectRepository.kt  ← Room + file impl
│       ├── features/
│       │   ├── gallery/
│       │   │   ├── GalleryViewModel.kt
│       │   │   └── GalleryScreen.kt   ← project list / home
│       │   ├── editor/
│       │   │   ├── render/
│       │   │   │   ├── LayerModel.kt        ← immutable layer data class
│       │   │   │   ├── EditorCanvas.kt      ← Compose Canvas draw loop
│       │   │   │   └── TransformGestures.kt ← drag/scale/rotate detector
│       │   │   ├── snap/
│       │   │   │   └── SnapEngine.kt        ← grid + edge-snap logic
│       │   │   ├── slice/
│       │   │   │   └── SliceEngine.kt       ← bisection math
│       │   │   ├── overlay/
│       │   │   │   └── SafeZoneOverlay.kt   ← IG crop-zone preview
│       │   │   ├── EditorViewModel.kt       ← StateFlow<EditorState>
│       │   │   └── EditorScreen.kt
│       │   └── export/
│       │       ├── ExportEngine.kt          ← offscreen 3x render + MediaStore save
│       │       ├── ExportViewModel.kt
│       │       └── ExportScreen.kt
│       └── navigation/
│           ├── NavRoutes.kt            ← @Serializable typed routes
│           └── NavGraph.kt             ← NavHost
└── build.gradle.kts
```

---

## ═══ SECTION 13: PERFORMANCE CONTRACTS ═══

| Metric | Target | How to Verify |
|---|---|---|
| Frame rate during drag/transform | ≥ 60fps | Perfetto trace / Layout Inspector |
| Cold start to Gallery screen | < 2.0s | Android Vitals |
| Canvas recompositions during idle | 0 | Layout Inspector recomposition counts |
| 4-image export render (3x supersample) | < 3.0s | Manual profiling, mid-range test device |
| Import + downsample (10 images) | < 1.5s | Perfetto trace |
| Memory, steady-state editing | < 200MB | Android Studio Profiler |
| Main-thread bitmap decode calls | 0 | StrictMode violation count |

---

## ═══ SECTION 14: ANTI-PATTERNS — NEVER DO THESE ═══

❌ Decoding full-resolution bitmaps with `BitmapFactory.decodeFile` and no `inSampleSize`
   WHY: OOM crash on 10+ image import on any device under ~4GB RAM
   DO THIS INSTEAD: calculate `inSampleSize` from the target canvas size before decoding

❌ Running bitmap compositing on the main thread during export
   WHY: multi-second frame freeze, ANR risk
   DO THIS INSTEAD: `Dispatchers.Default`, hand off only the final `Bitmap` to Main

❌ Redrawing every layer inside one `Canvas.drawXxx` loop keyed to a single state object
   WHY: forces full-canvas recomposition on every drag frame — visible jank
   DO THIS INSTEAD: one `graphicsLayer` per layer, keyed by `layer.id`

❌ Storing image bytes as BLOBs in Room
   WHY: bloats the DB file, kills query performance as project count grows
   DO THIS INSTEAD: store file URIs/paths only; bytes live on disk

❌ Manual back-stack manipulation instead of Compose Navigation
   WHY: process-death state restoration silently breaks
   DO THIS INSTEAD: `NavHost` with typed `@Serializable` routes

❌ Catching everything as generic `Exception` and showing "Something went wrong"
   WHY: user can't tell a denied permission from a corrupt file from a full disk
   DO THIS INSTEAD: the `AppError` sealed hierarchy in Section 9

❌ Exporting directly to JPEG at default quality
   WHY: visible banding on gradients and soft edges on text/vectors, before IG even re-compresses
   DO THIS INSTEAD: PNG at 3x supersample; JPEG only as an opt-in "smaller file" toggle

❌ Free-drag transforms with no snap threshold and no grid
   WHY: users can never hit precise alignment by touch, regardless of intent
   DO THIS INSTEAD: `SnapEngine` magnet-threshold logic from Section 4

❌ `mutableStateOf` wrapping the entire layer list as one object
   WHY: dragging one layer recomposes the whole canvas every frame
   DO THIS INSTEAD: `SnapshotStateList<LayerModel>` with immutable, `key`-stable items

❌ Requesting `WRITE_EXTERNAL_STORAGE` on API 33+
   WHY: deprecated permission, does nothing, flagged by lint, confuses reviewers
   DO THIS INSTEAD: MediaStore + scoped storage; request `READ_MEDIA_IMAGES` only for import

---

## ═══ SECTION 15: USAGE MODES ═══

Append ONE of these to this prompt to generate code:

### Mode A — Single File
```
TASK: Generate `[exact/file/path.kt]` as a complete, production-ready file.
Include all imports. Zero placeholders. Output only the file content.
```

### Mode B — Complete Feature
```
TASK: Implement the full Editor feature:
- features/editor/render/LayerModel.kt
- features/editor/render/EditorCanvas.kt
- features/editor/render/TransformGestures.kt
- features/editor/EditorViewModel.kt
- features/editor/EditorScreen.kt
Output each file with its path as a header. Zero placeholders. All files must compile as-is.
```

### Mode C — Architecture Layer
```
TASK: Implement the full storage layer:
1. data/local/AppDatabase.kt
2. data/local/ProjectEntity.kt
3. data/local/LayerEntity.kt
4. data/local/ProjectDao.kt
5. data/repository/LocalProjectRepository.kt
Each file complete, following every contract in Section 6 and Section 7.
```

### Mode D — Background Compute Layer
```
TASK: Implement the full export/compute layer:
1. core/util/BitmapUtils.kt
2. features/export/ExportEngine.kt
3. features/export/ExportViewModel.kt
Follow every contract in Section 2b and Section 8. All heavy work must run on
Dispatchers.Default; the main thread receives only the final Bitmap/Uri.
```

### Mode E — Audit
```
TASK: Given the following code [paste code], audit it against every contract,
anti-pattern rule, and performance requirement in this blueprint.
Output: list of violations, severity (CRITICAL / WARN / SUGGESTION), and fix.
```

---

## ═══ SECTION 16: IMPLEMENTATION PRIORITY CHAIN ═══

```
[STEP 1] ── Design tokens + base Compose theme
            Files: core/theme/*.kt
            Time: 0.5 day
            Gate: theme previews render correctly; zero raw Color()/dp literals in any screen file (grep check)

[STEP 2] ── Rendering engine spike — ONE layer, drag/scale/rotate only
            Files: features/editor/render/*.kt (throwaway/minimal version)
            Time: 2 days
            Gate: 60fps sustained in Perfetto while dragging a single image layer on a mid-range test device
            ← If this gate fails, stop. Nothing else in this document matters until it passes.

[STEP 3] ── Project data model + Room schema
            Files: data/local/*.kt, data/repository/*.kt
            Time: 1 day
            Gate: insert/query round-trip test passes; DB inspector shows correct schema

[STEP 4] ── Full multi-layer canvas + state wiring
            Files: features/editor/render/*.kt (complete), features/editor/EditorViewModel.kt
            Time: 2 days
            Gate: 5+ layers editable simultaneously with 0 dropped frames, 0 unneeded recompositions (Layout Inspector)

[STEP 5] ── Grid & snap system
            Files: features/editor/snap/SnapEngine.kt
            Time: 1.5 days
            Gate: dragging within 6dp of a grid line or sibling edge snaps; toggle disables cleanly with no lingering state

[STEP 6] ── Slice/bisection engine
            Files: features/editor/slice/SliceEngine.kt
            Time: 1.5 days
            Gate: a 1080×4320 import produces exactly 4 correctly-cropped 1080×1080 slides, zero pixel misalignment at seams

[STEP 7] ── Safe-zone overlay
            Files: features/editor/overlay/SafeZoneOverlay.kt
            Time: 1 day
            Gate: toggle overlays the real IG avatar-crop-circle dimensions accurately on slide 1

[STEP 8] ── Export pipeline
            Files: features/export/*.kt
            Time: 2.5 days
            Gate: exported PNGs appear in gallery in correct sequential order; pixel-identical to the 3x on-screen composite; StrictMode reports zero main-thread violations

[STEP 9] ── Non-destructive autosave
            Files: data/repository/LocalProjectRepository.kt (autosave hook)
            Time: 1 day
            Gate: force-kill the app mid-edit, reopen — every layer/transform is restored exactly

[STEP 10] ─ Navigation shell
            Files: navigation/*.kt, GalleryScreen.kt
            Time: 1 day
            Gate: process death restores the current screen via SavedStateHandle; back stack never re-adds Gallery

[STEP 11] ─ Error handling wiring + performance pass
            Files: core/error/*.kt, profiling across all screens
            Time: 2 days
            Gate: force a corrupt image import and a full-disk export — both surface distinct, correctly-typed error UI; every row in Section 13's table is green
```

---

## ═══ SECTION 17: WHAT THIS PROMPT DOES NOT COVER ═══

**Strong sections** (tight contracts — expect production-ready code): Rendering pipeline (Section 2), gesture/snap engine (Section 4), storage (Section 7), error handling (Section 9).

**Scaffolding sections** (correct structure, but verify on real hardware): The export MediaStore path (Section 2b, Section 8) — some OEM Android skins alter gallery-scan timing after a MediaStore insert. Test on 2–3 real devices, not just an emulator, before trusting the "export complete" gate in Step 8.

**Not covered — you must decide these before running any Mode:**
- **Auth / RBAC** — intentionally omitted. This is a single-user local tool with no roles. If that ever changes, Section 10's routes need guard logic added.
- **Backend / cloud sync** — out of scope for v1. Section 6's repository interface exists specifically so this is a clean addition later, not a rewrite.
- **Monetization / IAP** — if you add premium templates later, add `PaymentError` subtypes to Section 9 and a `BillingRepository` before writing any paywall UI.
- **Template system** — deliberately deferred to v2. The v1 priority chain ships freeform + slice + snap only.
- **Push notifications** — not needed for a local tool. A single local "export complete" notification is optional polish, not architecture.

**Recommended next step:** Build Step 2 (the single-layer render spike) FIRST, as a throwaway screen, before touching Room, navigation, or anything else. If drag/scale/rotate doesn't hold 60fps on a mid-range test device, no other section of this blueprint is worth building yet.

---

## ═══ SECTION 18: FULL EXECUTION RUNBOOK (Studio + Cursor Hybrid) ═══

This section makes Section 16 literally copy-pasteable. Each step names the tool to be in, the exact task to paste, and the exact gate check — including whether that check happens in Cursor or in Android Studio.

**Why hybrid:** Cursor/Claude Code write every file. Android Studio is needed only for the things no AI agent tool replicates — Compose `@Preview`, Layout Inspector recomposition counts, Perfetto/profiler capture, and the Database/Device Inspectors. Keep Studio open in the background; do the writing in Cursor.

### STEP 0 — Environment Setup
ENV: Android Studio, once
1. New Project → Empty Activity (Compose) → name `CarouselForge`, package `com.sanu.carouselforge`, minSdk 26, language Kotlin.
2. `git init`, first commit of the empty scaffold.
3. Copy this document into the repo root. If using Claude Code, save it as `CLAUDE.md` — it auto-loads every session. If using Cursor, save it under `.cursor/rules/carouselforge.md`.
4. Open the same folder in Cursor. Keep Studio open in the background, unfocused.
GATE: `./gradlew assembleDebug` succeeds on the empty scaffold before any custom code is written.

### STEP 1 — Design Tokens
ENV: Cursor to write → Studio to verify
```
TASK: Generate core/theme/Color.kt, Spacing.kt, Typography.kt, Motion.kt, AppTheme.kt
per Section 3. Zero placeholders, zero hardcoded values outside these files.
```
GATE (Studio): open any file, add a throwaway `@Preview` composable using `AppTheme`, confirm it renders. Grep repo for `Color(0x` — zero hits outside `core/theme/`.

### STEP 2 — Rendering Spike (the one gate that actually matters)
ENV: Cursor to write → Studio to verify
```
TASK: Generate features/editor/render/LayerModel.kt, EditorCanvas.kt, TransformGestures.kt
as a throwaway single-layer drag/scale/rotate spike per Section 2 and Step 2 of the priority
chain. Zero placeholders.
```
GATE (Studio, physical mid-range device — not emulator): Run → Profiler → capture a Perfetto trace while dragging the layer. 60fps sustained. **If this fails, stop and fix Section 2 before writing anything else in this document.**

### STEP 3 — Storage Layer
ENV: Cursor to write → Studio to verify
```
TASK: Implement the full storage layer:
1. data/local/AppDatabase.kt
2. data/local/ProjectEntity.kt
3. data/local/LayerEntity.kt
4. data/local/ProjectDao.kt
5. data/repository/LocalProjectRepository.kt
Follow every contract in Section 6 and Section 7.
```
GATE (Studio): App Inspection → Database Inspector, confirm schema matches Section 7; run an insert/query round-trip.

### STEP 4 — Full Multi-Layer Canvas
ENV: Cursor to write → Studio to verify
```
TASK: Implement the full Editor feature end to end:
- features/editor/render/LayerModel.kt (complete version)
- features/editor/render/EditorCanvas.kt (complete, multi-layer)
- features/editor/render/TransformGestures.kt
- features/editor/EditorViewModel.kt
- features/editor/EditorScreen.kt
Output each file with its path as a header. All files must compile as-is.
```
GATE (Studio): Layout Inspector — 5+ layers editable, 0 unneeded recompositions, 0 dropped frames.

### STEP 5 — Grid & Snap
ENV: Cursor to write → device to verify
```
TASK: Generate features/editor/snap/SnapEngine.kt per Section 4. Include the spring-based
settle animation contract. Zero placeholders.
```
GATE: manual test — drag within 6dp of a grid line/sibling edge → snaps; toggle off → freeform, no lingering snap state.

### STEP 6 — Slice/Bisection Engine
ENV: Cursor to write → unit test to verify
```
TASK: Generate features/editor/slice/SliceEngine.kt per Section 2 domain description.
Include the accompanying unit test file per Section 11.
```
GATE: `./gradlew test` — a 1080×4320 import produces 4 correctly-cropped 1080×1080 slides, zero pixel misalignment at seams.

### STEP 7 — Safe-Zone Overlay
ENV: Cursor
```
TASK: Generate features/editor/overlay/SafeZoneOverlay.kt. Toggleable overlay showing the
real Instagram avatar-crop-circle boundary on slide 1.
```
GATE: visually confirm overlay dimensions against a real IG post on your own device.

### STEP 8 — Export Pipeline
ENV: Cursor to write → **physical device** to verify (not emulator)
```
TASK: Implement the full export/compute layer:
1. core/util/BitmapUtils.kt
2. features/export/ExportEngine.kt
3. features/export/ExportViewModel.kt
Follow every contract in Section 2b and Section 8.
```
GATE: exported PNGs appear in the Gallery app in correct sequential order; StrictMode (enable in `Application.onCreate` during this test only) reports zero main-thread violations.

### STEP 9 — Non-Destructive Autosave
ENV: Cursor to write → adb to verify
```
TASK: Add debounced (400ms) autosave to LocalProjectRepository per Section 7's contract.
```
GATE: `adb shell am force-stop com.sanu.carouselforge` mid-edit, reopen app — every layer/transform restored exactly.

### STEP 10 — Navigation Shell
ENV: Cursor to write → Studio to verify
```
TASK: Generate navigation/NavRoutes.kt, navigation/NavGraph.kt, and GalleryScreen.kt per
Section 10. Typed routes only, no string-literal navigate() calls.
```
GATE: Studio → Developer Options → "Don't keep activities" ON → background app, return — screen state restored via SavedStateHandle; Gallery never re-pushed onto the back stack.

### STEP 11 — Error Handling + Final Performance Pass
ENV: Cursor to write → Studio to verify
```
TASK: Generate core/error/AppError.kt and core/error/ErrorObserver.kt per Section 9, then
wire typed error states into EditorViewModel and ExportViewModel.
```
GATE: force a corrupt image import and a full-disk export → each shows the correct distinct error UI. Then run the full Section 13 performance table in Studio's Profiler — every row green.

### STEP 12 — Pre-Ship Audit
ENV: Cursor or Claude Code
```
TASK: Given the entire codebase, audit it against every contract, anti-pattern rule, and
performance requirement in this blueprint. Output: list of violations, severity
(CRITICAL / WARN / SUGGESTION), and fix.
```
GATE: zero CRITICAL findings before first real-world release build.

---

*End of CarouselForge Master Engineering Prompt*
*Version 1.0*
*This document governs all code generation for the CarouselForge project.*