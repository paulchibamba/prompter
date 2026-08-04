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
- [x] **Step 8** — Editor screen
- [x] **Step 9** — Prompter surface (static)
- [x] **Step 10** — Scroll engine & transport ← *reading works end to end*

## Phase C — Typography

- [x] **Step 11** — Quick-settings sheet & type controls
- [x] **Step 12** — Font system
- [x] **Step 13** — Colour, contrast & marker styling

## Phase D — Layout for the glass

- [x] **Step 14** — Margins, measure & safe-area preview
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
- **Step 8** — marker insertion lives in `domain/text/MarkerInsertion.kt` rather than in the editor, so it
  is unit-testable. It is a pure text edit, and it now has 11 tests.
- **Step 8** — `EditorUiState` holds a Compose `TextFieldValue`. A ViewModel holding a UI type is a
  deliberate trade: inserting a marker has to move the caret, and the alternative is threading text and
  selection through as two values that must never disagree.
- **Step 8** — Import is present but disabled in the editor's bottom bar, matching the library.
- **Step 9** — `ui/theme/PrompterFonts.kt` is a seam that currently resolves every `fontId` to the device
  font. The bundled families arrive with the font step; nothing downstream changes when they do.
- **Step 9** — a script's assigned preset (`Script.presetId`) is **not yet applied** by the prompter, which
  still reads the global settings. Presets can be assigned from the library but have no effect until the
  presets step wires them through.
- **Step 14** — the line-length cap changes the *effective* text width, so that is what gets reported for
  the pace measurement. Reporting the uncapped width would make the script measure wider — and therefore
  shorter — than it actually lays out, which is the same class of bug as the font-scale one in Step 10.
- **Step 14** — the safe-area overlay is UI state, not a stored setting. It is a calibration aid, and it
  should never still be switched on the next time the app opens.
- **Step 14** — the cap only binds when the text column is wider than the requested measure, so at large
  sizes in portrait it correctly does nothing. Verified at 26sp with a 20-character cap: the column
  narrows and centres within the margins.
- **Step 13** — text and background are also choosable **independently**, not only as the five preset
  pairings. Every preset clears 7:1, so with presets alone the contrast warning could never fire and the
  readout would be decorative.
- **Step 13** — screen brightness sits in the Type tab rather than with the other scroll settings, because
  it is judged the same way as colour and contrast: by looking at the text through the glass (§6.9).
- **Step 13** — a hidden marker is not rendered at all, so its words still count toward the script's word
  count and therefore its pace. Markers are short; the drift is well under a second on a normal script.
- **Step 13** — **not verified end to end:** the brightness override applies a window attribute that a
  screenshot cannot capture, and the control sits far enough down a scrolling modal sheet that UI
  automation could not reach it reliably. The control renders and reads "System" correctly. Worth a manual
  check.
- **Step 12** — the imported custom font is **copied into app storage** rather than referenced by a
  persisted URI as the spec suggests. A URI can be revoked and the file behind it deleted, moved, or on an
  SD card that is not in the phone; losing a typeface between takes is worse than a few hundred kilobytes.
  `TypographySettings.customFontUri` therefore holds the path of the copy.
- **Step 12** — the font picker accepts any file type. Providers routinely report a `.ttf` as an octet
  stream, and filtering on font MIME types hides the file the user is looking straight at.
- **Step 12** — `FontVariation` needs `@OptIn(ExperimentalTextApi::class)`. It is the only way to drive a
  variable font's `wght` axis; without it the system picks the nearest static instance and the weight
  control appears to do nothing.
- **Step 11** — the sheet's height fraction goes on its *content*, not on `ModalBottomSheet` itself.
  Sizing the sheet container anchors it to the top of the screen with the script below it.
- **Step 11** — `ModalBottomSheet` opens its own window, which brings the system bars back. The immersive
  effect is re-keyed on the sheet's visibility so closing it restores the prompter's full-screen state.
- **Step 11** — typography edits apply to the visible text immediately and are written after a 200ms pause.
  The stored value only changes when the write lands, so the settings flow cannot emit mid-drag and fight
  the value under the reader's finger.
- **Step 11** — the weight picker offers the five named weights (300/400/500/700/900) as chips rather than a
  segmented row; five words do not fit across a narrow phone, and shrinking them would defeat showing each
  name in its own weight. 600 and 800 remain storable, just not offered by eye.
- **Step 11** — the Layout and Scroll tabs exist but say what arrives in them later.
- **Step 10** — content measurement must use the surface's own density with `fontScale` pinned to 1. It
  originally measured at the *system* font scale while the surface rendered at 1, so on a device with a
  non-default text size the script measured shorter than it drew and every read ran long — 51s instead of
  30s on a phone set to 0.8. Verified after the fix: 200 words at 400 wpm takes 30.4s, and takes the same
  30.5s at 120sp as at 72sp.
- **Step 10** — end behaviour (HOLD / LOOP / EXIT) is implemented in full, ahead of the scroll settings step
  which was only meant to expose the controls for it.
- **Step 10** — a single tap toggles the control bar. The rest of the gesture set arrives with the gestures
  step; this much is needed now or the bar cannot be recovered once it hides.
- **Step 10** — the quick-settings button in the control bar is disabled until the sheet exists.
- **Step 9** — the first and last lines settle about 4.5% of screen height *below* the nominal reading-line
  percentage, because `Trim.None` keeps half-leading above the first line of each text block. The offset is
  identical at both ends, so start and end of a script line up with each other — which is the property that
  matters. The reading-line indicator must be drawn on the same basis, or it will sit visibly above the text
  it is supposed to mark.
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
