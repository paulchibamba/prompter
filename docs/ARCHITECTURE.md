# Architecture

This is the map for finding your way around, and the rule that keeps it navigable. Behaviour is specified in
[`docs/SPEC.md`](SPEC.md); this file only covers structure.

## Module

Single Gradle module, `:app`. No multi-module split — the app is small enough that module boundaries would
add build-graph overhead without buying anything. Package boundaries below do the same job.

Base package: `com.paulchibamba.teleprompter`.

> `docs/SPEC.md` uses `com.paul.prompter` as a placeholder package in its examples. The real package was
> fixed in Step 1 by the Android Studio project scaffold and is not changed to match — renaming it buys
> nothing and risks breaking `applicationId`-sensitive tooling. Read `com.paul.prompter` in the spec as
> illustrative; this file is the source of truth for the actual package layout.

## Packages and the dependency rule

```
com.paulchibamba.teleprompter
├── data
│   ├── db          Room: PrompterDatabase, ScriptDao, ScriptEntity
│   ├── prefs       DataStore: SettingsRepository, PresetRepository
│   └── io          ImportExport (SAF, charset detection)
├── domain
│   ├── model       Script, Marker, TypographySettings, LayoutSettings,
│   │               ScrollSettings, KeyBinding, PromptAction, Preset
│   ├── text        ScriptParser (markers, section breaks, word count)
│   └── scroll      ScrollEngine, WpmCalculator
├── input
│   ├── RemoteKeyRouter      dispatchKeyEvent → PromptAction
│   ├── KeyBindingStore      persistence + per-device profiles
│   └── KeyLearner           "press a button" capture flow
└── ui
    ├── library     LibraryScreen, LibraryViewModel
    ├── editor      EditorScreen, EditorViewModel
    ├── prompter    PrompterScreen, PrompterViewModel, PrompterSurface,
    │               ControlBar, QuickSettingsSheet
    ├── settings    SettingsScreen, RemoteMappingScreen, KeySnifferScreen
    ├── components  Sliders, ColorPicker, FontPicker, PreviewPane
    └── theme       Theme, Type, Color
```

**The dependency rule: `domain` imports nothing from `data`, `ui`, `input`, or `android.*`.**

`domain` is plain Kotlin — data classes and pure functions (marker parsing, WPM maths, scroll-position
calculation). It has no Android framework dependency, which is what makes it directly unit-testable without
Robolectric or an emulator, and is what Step 3 exists to establish before anything is built on top of it.

`data` and `input` depend on `domain` (they produce and consume its models) and on Android/Room/DataStore
APIs, but not on `ui`. `ui` depends on `domain` and reads/writes through `data` and `input`, via ViewModels.

Every PR checklist (`.github/PULL_REQUEST_TEMPLATE.md`) asks the author to confirm this rule wasn't crossed.
It is not yet enforced by a lint rule or build-time check — with one module, a dependency violation is a
Kotlin `import android...` or `import ...data...` inside a file under `domain/`, which is easy to catch by
eye at review time. If that stops being reliable, revisit with a real enforcement mechanism (e.g. an ArchUnit
or Konsist test) rather than trusting review alone.

## DI: none

No dependency-injection framework. A manual `AppContainer` (introduced in Step 4 alongside Room) constructs
and holds the app's singletons — database, repositories, use-cases — and is handed to `MainActivity`.
ViewModels take their dependencies as constructor parameters via a `ViewModelProvider.Factory` built from the
container. This is a deliberate choice, not a placeholder for Hilt later: the object graph here is a handful
of nodes, and a framework earns its keep on graphs much larger than this one.

## Single-activity, Compose navigation

One `MainActivity` hosts a Navigation Compose graph (Library → Editor → Prompter → Settings, plus the
remote-mapping and key-sniffer screens). This matters beyond convention: the RT02 remote arrives as ordinary
`KeyEvent`s (§9 of the spec), and `dispatchKeyEvent` needs exactly one place to live so the active screen —
not an arbitrary `Activity` — decides what a keypress means.

## Persistence

| Concern | Mechanism | Why |
|---|---|---|
| Scripts | Room (`data.db`) | Structured, queryable, grows large (10,000-word scripts, many of them) |
| Settings, presets, key bindings | DataStore Preferences (`data.prefs`) | Small, flat, read on every frame-loop start — Preferences DataStore avoids Room's per-query overhead for this shape of data |

Both are wrapped by repositories in `data`; nothing outside `data` talks to `RoomDatabase` or
`DataStore<Preferences>` directly.

## No permissions

Enforced by the `:app:verifyNoInternetPermission` Gradle task (registered in `app/build.gradle.kts`), which
inspects the *merged* manifest — the one produced after manifest merging pulls in every dependency's own
manifest — not just `app/src/main/AndroidManifest.xml`. That's the point of checking the merged output: a
transitively-added permission from a library would be invisible to a check that only reads the source file.
CI runs it on every PR (`.github/workflows/ci.yml`). See requirement P1 in `docs/SPEC.md` §0.

`android:allowBackup="false"` for the same reason in a different place: without it, script content would be
swept into Android's automatic cloud backup regardless of the permission story.

## Code style

- Kotlin, `official` code style (`gradle.properties`).
- Compose, Material 3.
- `minSdk` 26, `compileSdk`/`targetSdk` 36 — see `docs/SPEC.md` §1 for why.
