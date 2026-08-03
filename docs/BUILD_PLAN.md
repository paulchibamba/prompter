# Build plan

Prompter is built in 25 small, sequential steps. Each step is self-contained: it lands as one pull request,
leaves `main` green, and does not depend on work scheduled for a later step.

[`docs/PROGRESS.md`](PROGRESS.md) records which steps are done and any deviation from this plan.
[`docs/SPEC.md`](SPEC.md) is the authority on behaviour — where this plan and the spec disagree, the spec wins.

## Conventions

Every step branches from `main`, ends with `./gradlew :app:assembleDebug` passing (plus
`:app:testDebugUnitTest` where the step added tests), updates `docs/PROGRESS.md`, and merges via PR with
CI green.

**Branch names describe the change, not its position in this plan.** Use `app-shell`, `library-screen`,
`scroll-engine`, `remote-key-router` — never a step number. The numbering here is scaffolding for building
the app in a sensible order; it means nothing to someone reading the history later, and it ages badly if
steps get split, merged, or reordered.

Branch protection on `main` is deliberately **off** until Step 25. Branch-per-step is a convention followed
by discipline while the shape of the project is still moving; it becomes mandatory once the project is stable
and open to outside contribution.

---

## Phase A — Foundation

| # | Step | Delivers |
|---|---|---|
| 1 | Git, GitHub & open source scaffolding | Licence, README, CI, public repo |
| 2 | Build foundation & project hygiene | minSdk 26, Room/DataStore/Navigation dependencies, no-permission check as a Gradle task, architecture docs |
| 3 | Domain models & pure logic | Settings models, marker parsing, word count, WPM maths — all unit tested |
| 4 | Script persistence | Room database, repository, use-cases, manual DI container |
| 5 | Settings & preset persistence | DataStore settings, preset table, three built-in presets |
| 6 | App shell | Material 3 theme, navigation graph, ViewModel wiring |

## Phase B — Reading works end to end

| # | Step | Delivers |
|---|---|---|
| 7 | Library screen | Script list, search, duration estimates, undo-delete, reorder |
| 8 | Editor screen | Title and body editing, autosave, insert marker |
| 9 | Prompter surface | Immersive full-screen paragraph rendering with correct leading and lead-in/lead-out |
| 10 | **Scroll engine & transport** | Frame-clock scrolling with sub-pixel accuracy, ease-in, control bar |

**Milestone:** after Step 10 you can read a real script end to end at a steady pace. This is the point where
the app becomes useful.

## Phase C — Typography

| # | Step | Delivers |
|---|---|---|
| 11 | Quick-settings sheet & type controls | Live-previewing bottom sheet; size, leading, tracking, weight, alignment, case |
| 12 | Font system | Four bundled OFL families with continuous variable weight, specimen picker, custom font import |
| 13 | Colour, contrast & marker styling | Colour presets, live WCAG contrast readout, brightness override |

**Milestone:** raise the font size mid-read and the reading pace does not change.

## Phase D — Layout for the glass

| # | Step | Delivers |
|---|---|---|
| 14 | Margins, measure & safe-area preview | Percentage margins, line-length cap, calibrate-against-the-glass overlay |
| 15 | Reading line & edge fade | Position and style of the reading indicator, softened top and bottom edges |
| 16 | Mirroring & orientation | Horizontal/vertical mirror with correct gesture and scroll direction, orientation lock |
| 17 | Scroll settings | WPM ↔ pixels/sec, countdown, ramp, end behaviour, blackout |
| 18 | Presets management | Save, rename, apply; per-script overrides |
| 19 | Prompter gestures | Tap, double-tap, drag-scrub, pinch, two-finger speed, long-press blackout |

**Milestone:** with the phone in the rig, margins match the glass and mirrored text reads correctly.

## Phase E — The remote

| # | Step | Delivers |
|---|---|---|
| 20 | Key sniffer | Diagnostic screen logging every key event, device name and descriptor |
| 21 | Remote routing & defaults | Action enum, key router, sensible defaults, no stray system volume UI |
| 22 | Learn-button flow & per-device profiles | Press-a-button binding capture, long-press bindings, per-device mappings |

Step 20 comes before the router on purpose. It establishes what the remote actually sends — and confirms it
is a standard HID keyboard rather than a BLE GATT device. If it is the latter, Steps 21 and 22 change
substantially and the no-permissions guarantee would be at risk, so it is worth knowing first.

**Milestone:** every button rebindable to any action, bindings survive restart, two remotes hold different
mappings at once.

## Phase F — Polish

| # | Step | Delivers |
|---|---|---|
| 23 | Markers & progress scrubber | Jump to next/previous section, seekable progress bar with marker ticks |
| 24 | Import / export | SAF import with charset detection, `.txt` export, full JSON backup and restore |
| 25 | Resume, accessibility & performance | Resume position, TalkBack pass, sustained 60fps on a 10,000-word script, v1.0.0 release |

**Milestone:** the full acceptance checklist in [`docs/SPEC.md`](SPEC.md) §16.
