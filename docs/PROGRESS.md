# Progress

Tracks which build-plan steps are complete. See [`BUILD_PLAN.md`](BUILD_PLAN.md) for what each step delivers.

Tick a step only when it is merged to `main` with CI green. Record any deviation from the plan under
**Deviations** at the bottom — future work reads this file to know what actually happened.

## Phase A — Foundation

- [x] **Step 1** — Git, GitHub & open source scaffolding
- [x] **Step 2** — Build foundation & project hygiene
- [x] **Step 3** — Domain models & pure logic
- [x] **Step 4** — Script persistence
- [x] **Step 5** — Settings & preset persistence
- [x] **Step 6** — App shell: theme, navigation, DI wiring

## Phase B — Reading works end to end

- [x] **Step 7** — Library screen
- [ ] **Step 8** — Editor screen
- [ ] **Step 9** — Prompter surface (static)
- [ ] **Step 10** — Scroll engine & transport ← *reading works end to end*

## Phase C — Typography

- [ ] **Step 11** — Quick-settings sheet & type controls
- [ ] **Step 12** — Font system
- [ ] **Step 13** — Colour, contrast & marker styling

## Phase D — Layout for the glass

- [ ] **Step 14** — Margins, measure & safe-area preview
- [ ] **Step 15** — Reading line & edge fade
- [ ] **Step 16** — Mirroring & orientation
- [ ] **Step 17** — Scroll settings
- [ ] **Step 18** — Presets management
- [ ] **Step 19** — Prompter gestures

## Phase E — The remote

- [ ] **Step 20** — Key sniffer (diagnostic)
- [ ] **Step 21** — Remote routing, defaults & persistence
- [ ] **Step 22** — Learn-button flow & per-device profiles

## Phase F — Polish

- [ ] **Step 23** — Markers navigation & progress scrubber
- [ ] **Step 24** — Import / export
- [ ] **Step 25** — Resume, accessibility & performance

---

## Open questions

Answers go here as they are found. These block or reshape later steps.

- **RT02 remote keycodes** — unknown until the key sniffer (Step 20) is run with the physical remote.
  Record: how many buttons, what each sends, whether it auto-repeats when held, and whether it presents as
  an HID keyboard or a BLE GATT device. If GATT, Steps 21–22 need Bluetooth permissions and the
  no-permissions guarantee is lost.
- **Beam-splitter orientation** — does the RT02 need horizontal mirror, vertical, or both? Sets the default.
- **Glass aspect and crop** — measuring the visible area would let us ship a correct default margin preset
  rather than a guessed 10%.

## Deviations

Deviations from [`BUILD_PLAN.md`](BUILD_PLAN.md), with the reason.

- **Step 1** — `docs/SPEC.md` was moved out of `app/src/main/java/` during Step 1 rather than Step 2. The
  move is trivial and it fixes a real defect immediately (the spec was being packaged into the APK as a Java
  resource), and it makes the documentation links in `README.md` valid from the first commit.
- **Step 1** — `docs/PROGRESS.md` and `docs/BUILD_PLAN.md` were created in Step 1 rather than Step 2, since
  Step 1 needs somewhere to tick itself off and the README links to both. `docs/ARCHITECTURE.md` remains
  scheduled for Step 2.
- **Step 2** — `docs/ARCHITECTURE.md` was never written, and `CLAUDE.md` was not created until Step 6.
  `CLAUDE.md` now carries the layering rules, code style and git conventions; `README.md` and
  `CONTRIBUTING.md` carry the public-facing versions. A separate `docs/ARCHITECTURE.md` would only
  duplicate them, so it is dropped rather than deferred.
- **Step 6** — `material-icons-core` was added to the version catalog. Material3 1.4.0 no longer brings the
  icon set in transitively, and the app shell needs it for the navigation back button.
- **Step 6** — `TeleprompterTheme` was renamed `PrompterTheme`, matching the product name used everywhere
  else. Only `MainActivity` referenced it.
- **Step 7** — `lifecycle-runtime-compose` was added to the version catalog, for
  `collectAsStateWithLifecycle`.
- **Step 7** — Import and Export appear in the menus and empty state but are **disabled** until the
  import/export step wires them, so the menus do not change shape under the user later.
- **Step 7** — the reorder handle uses the hamburger icon. The proper drag-handle glyph lives in
  `material-icons-extended`, which is not worth its build cost for one icon.
- **Step 7** — reordering is drag-only, so it is unreachable via TalkBack. The accessibility pass owns
  fixing this; noted here so it is not forgotten.
- **Step 2** — `docs/SPEC.md` illustrates the package layout under `com.paul.prompter`. The real package,
  fixed by the Android Studio scaffold in Step 1, is `com.paulchibamba.teleprompter` and stays that way;
  renaming it now buys nothing and risks breaking `applicationId`-sensitive tooling. Documented in
  `docs/ARCHITECTURE.md`.
- **Step 2** — The manual `AppContainer` for DI is deferred to Step 4, when Room actually lands and there is
  something for it to hold. `docs/ARCHITECTURE.md` describes the intended shape now so later steps build
  against a documented plan rather than inventing one mid-step.
- **Step 2** — `AndroidManifest.xml` had `android:allowBackup="true"` plus `dataExtractionRules` and
  `fullBackupContent` from the Android Studio template, contradicting requirement P1 in `docs/SPEC.md` §0.
  Set to `allowBackup="false"` and removed the now-unused `xml/backup_rules.xml` and
  `xml/data_extraction_rules.xml` resources, matching the spec's manifest skeleton (§14).
- **Step 2** — The CI check for zero permissions previously grepped the *source* manifest
  (`app/src/main/AndroidManifest.xml`). It's replaced by the `:app:verifyNoInternetPermission` Gradle task,
  which inspects the *merged* manifest so a permission pulled in transitively by a dependency is caught too.
- **Step 3** — Settings validation is done with a `coerced()` method on each settings block rather than
  `require` in an `init`. Values arrive from DataStore, imported presets and remote key presses, none of which
  are guaranteed in range; a stale or corrupt stored value should degrade to the nearest sane one, never crash
  the prompter mid-read. Ranges live as named constants on each companion so the UI sliders in Steps 11–17
  bind to the same numbers the tests assert.
- **Step 3** — A `domain.model.Script` was added, which `docs/SPEC.md` §3.1 only shows as a Room entity. The
  entity lands in Step 4 and maps onto this; keeping a domain type means nothing outside `data.db` depends on
  Room, and the pure logic here stays JVM-testable.
- **Step 3** — `ScriptParser.wordCount` excludes `---` section-break lines (nobody reads them aloud) and counts
  a marker heading's label but not its hashes. The spec does not say which way to count; this is the reading
  that makes the library's duration estimate honest.
- **Step 3** — `nextAfter`/`previousBefore` marker navigation helpers landed here rather than in Step 23. They
  are two pure list functions over the markers this step already parses, and Step 21's remote actions need
  them before the scrubber exists.
- **Step 4** — Room is **2.7.2**, not the 2.6.1 pinned in `docs/SPEC.md` §14. Room 2.6.1's annotation
  processor fails under KSP2 (`unexpected jvm signature V`) with Kotlin 2.2, and KSP2 is the default in the
  KSP version this project already uses. Upgrading Room is the fix; downgrading KSP would trade a supported
  toolchain for a pinned number in the spec.
- **Step 4** — The `ScriptRepository` *interface* lives in `domain/repository`, with `RoomScriptRepository`
  implementing it in `data/db`. `docs/SPEC.md` §2 lists only a repository under `data`; splitting it is what
  lets the use-cases stay inside the dependency rule (`domain` importing nothing from `data`) and be tested
  against an in-memory fake instead of an emulator.
- **Step 4** — DAO tests run on the JVM under **Robolectric** against an in-memory SQLite database, rather
  than as instrumented tests. CI has no emulator, and untested SQL is exactly the kind of thing that is only
  found to be wrong after it ships. `@Config(sdk = [34])` pins a Robolectric-supported API level.
- **Step 4** — Room schemas are exported to `app/schemas/` and committed, and an `AppContainer` +
  `PrompterApplication` were added at the root package (the spec's §2 layout does not name them). `MainActivity`
  is untouched — wiring ViewModels to the container is Step 6's job.
- **Step 5** — Presets are a **Room** table, not DataStore. `docs/SPEC.md` §3.3 specifies a `PresetEntity`,
  but `docs/ARCHITECTURE.md` listed presets under DataStore alongside settings; the spec wins, and it is also
  the right call — presets are a list the user adds to and deletes from, and a script's `presetId` is a
  reference to a row. `docs/ARCHITECTURE.md`'s persistence table is corrected to match. Settings themselves are
  DataStore as planned.
- **Step 5** — kotlinx.serialization was added, and the three settings models are `@Serializable`. Both
  storage sites hold the same JSON (three DataStore keys for the global defaults, three columns for a preset),
  so applying a preset and saving the defaults back as one are exact inverses. Annotating the domain models
  does not breach the dependency rule — kotlinx.serialization is pure Kotlin, no Android — and the alternative
  of mirror DTOs in `data` would be forty hand-maintained fields, where one forgotten line silently drops a
  user's setting.
- **Step 5** — A settings block is one JSON string rather than one DataStore key per field. Blocks are always
  read and written whole, so per-field keys would only add thirty-odd names to keep in sync with the model.
  The cost — one bad character reverts a whole block — is bounded by `SettingsCodec` never throwing: a corrupt,
  older or out-of-range value degrades to the model default.
- **Step 5** — The built-in presets carry **fixed ids** (1, 2, 3) and are seeded by
  `RoomPresetRepository.ensureBuiltIns()` on first read or write, not by a `RoomDatabase.Callback` or by seed
  SQL inside the migration. A script's `presetId` refers to a preset by id, so a built-in's id has to be the
  same number on every install; and seeding on access is one implementation that covers a fresh install, this
  migration, and a later step that adds a fourth built-in. Built-ins are read-only: deleting one is refused and
  saving over one stores a copy, so the app can never end up with no presets.
- **Step 5** — `PrompterDatabase` is version 2 with `MIGRATION_1_2` adding the `presets` table.
  `PrompterDatabaseMigrationTest` builds a real v1 file by hand and opens it at v2 under Robolectric, rather
  than using Room's `MigrationTestHelper`, which reads its schemas from instrumentation assets and would need
  an emulator. Room validates the schema when it opens, so the test fails loudly if the migration's SQL ever
  drifts from the exported schema.
- **Step 5** — `ApplyPreset` and `SaveCurrentSettingsAsPreset` landed here rather than with presets management
  in Step 18. They are the two operations that read settings and presets together, and writing them now is what
  forced `SettingsRepository.setAll` to be a single atomic write — three separate writes would let the prompter
  render a new type size against the old margins for a frame. The management *UI* remains Step 18's.
