# Progress

Tracks which build-plan steps are complete. See [`BUILD_PLAN.md`](BUILD_PLAN.md) for what each step delivers.

Tick a step only when it is merged to `main` with CI green. Record any deviation from the plan under
**Deviations** at the bottom — future work reads this file to know what actually happened.

## Phase A — Foundation

- [x] **Step 1** — Git, GitHub & open source scaffolding
- [x] **Step 2** — Build foundation & project hygiene
- [x] **Step 3** — Domain models & pure logic
- [x] **Step 4** — Script persistence
- [ ] **Step 5** — Settings & preset persistence
- [ ] **Step 6** — App shell: theme, navigation, DI wiring

## Phase B — Reading works end to end

- [ ] **Step 7** — Library screen
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
