# Progress

Tracks which build-plan steps are complete. See [`BUILD_PLAN.md`](BUILD_PLAN.md) for what each step delivers.

Tick a step only when it is merged to `main` with CI green. Record any deviation from the plan under
**Deviations** at the bottom — future work reads this file to know what actually happened.

## Phase A — Foundation

- [x] **Step 1** — Git, GitHub & open source scaffolding
- [x] **Step 2** — Build foundation & project hygiene
- [ ] **Step 3** — Domain models & pure logic
- [ ] **Step 4** — Script persistence
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
