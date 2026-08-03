# Teleprompter App — Build Specification

**Working title:** Prompter
**Target:** Android phone/tablet, Android Studio, Kotlin + Jetpack Compose
**Primary hardware:** Ulanzi RT02 teleprompter + its bundled Bluetooth remote
**Spec date:** 2026-08-03

---

## 0. Goals & non-goals

### What this app is
A teleprompter that is *pleasant to read from*. The reference app (`com.outsourcing.autoviewer`) works but treats typography and layout as afterthoughts: one font, one size slider, no margin control, no line-height control, and a scroll speed expressed as a meaningless integer. Those are exactly the things that determine whether a read is smooth or a stumble.

Three pillars:

1. **Typography that serves reading at distance** — size, leading, tracking, weight, measure (line length), and contrast are all first-class, all live-adjustable, all previewed on the real text.
2. **Layout/margin control for beam-splitter glass** — the RT02's glass crops and mirrors the phone's display. Margins, safe areas, and a reading-line indicator must be user-controlled, not guessed.
3. **A remote that does what you want** — full button remapping, learned from whatever the RT02 remote actually sends.

### Non-goals for v1
- No cloud, no accounts, no sync, no analytics, no ads.
- No rich text (bold/italic runs). Plain text + lightweight cue markers only.
- No video recording or camera integration. (The RT02 is a glass rig; your camera app handles capture.)
- No PDF/image prompting in v1. Text only. (Reference app had these; they're rarely used and drag in a 5 MB native PDF stack.)
- No word-level highlighting / speech-following. Possible v2.

### Privacy posture (carry this over deliberately)
The reference app's single best property was that it **declared no `INTERNET` permission**, making exfiltration kernel-impossible. Preserve that.

> **Requirement P1:** `AndroidManifest.xml` declares **no** `INTERNET` permission, and no permission that implies network. No dependency may require one. Add a CI check that greps the merged manifest for `android.permission.INTERNET` and fails the build if present.

Target permission set for v1: **none**. File import uses the Storage Access Framework (no permission needed). The HID remote needs no Bluetooth permission because pairing happens at the OS level.

---

## 1. Platform & tech stack

| Item | Choice | Notes |
|---|---|---|
| Language | Kotlin 2.x | |
| UI | Jetpack Compose (Material 3) | |
| `minSdk` | 26 (Android 8.0) | Covers ~98% of devices; gives us modern `InputDevice` APIs. |
| `targetSdk` / `compileSdk` | 36 | |
| Persistence — scripts | Room | |
| Persistence — settings | DataStore (Preferences) | |
| Async | Coroutines + Flow | |
| DI | None (manual `AppContainer`) | App is small; a DI framework is overhead here. |
| Navigation | Navigation Compose | |
| Min screen | 320dp wide | |

**Orientation:** the app supports portrait and landscape. The prompter screen defaults to landscape but is user-lockable (see §7.6).

---

## 2. Module & package layout

Single Gradle module (`:app`). Packages:

```
com.paul.prompter
├── data
│   ├── db          Room: PrompterDatabase, ScriptDao, ScriptEntity
│   ├── prefs       DataStore: SettingsRepository, PresetRepository
│   └── io          ImportExport (SAF, charset detection)
├── domain
│   ├── model       Script, Marker, TypographySettings, LayoutSettings,
│   │               ScrollSettings, KeyBinding, PromptAction, Preset
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

---

## 3. Data model

### 3.1 Room — scripts

```kotlin
@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,                 // plain text, newlines preserved
    val createdAt: Long,              // epoch millis
    val updatedAt: Long,
    val wordCount: Int,               // denormalised, recomputed on save
    val lastPositionPx: Float = 0f,   // resume point
    val presetId: Long? = null,       // per-script style override; null = global
    val sortIndex: Int = 0
)
```

```kotlin
@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY sortIndex ASC, updatedAt DESC")
    fun observeAll(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun byId(id: Long): ScriptEntity?

    @Query("SELECT * FROM scripts WHERE title LIKE '%'||:q||'%' OR body LIKE '%'||:q||'%'")
    fun search(q: String): Flow<List<ScriptEntity>>

    @Upsert suspend fun upsert(script: ScriptEntity): Long
    @Delete suspend fun delete(script: ScriptEntity)
    @Query("UPDATE scripts SET lastPositionPx = :px WHERE id = :id")
    suspend fun savePosition(id: Long, px: Float)
}
```

> **Note on the reference app's bug:** it built imported text with `sb.append(line)` and no `\n`, collapsing every script into one paragraph. Confirmed live during analysis. Do not repeat this — preserve newlines exactly.

### 3.2 Cue markers

Markers are *derived from the body text*, not stored separately. Any line matching `^\s*(#{1,3})\s+(.*)$` (Markdown-style heading) is a marker; the text after the hashes is its label. Any line that is exactly `---` is a hard section break.

```kotlin
data class Marker(val charOffset: Int, val label: String, val level: Int)
```

Markers drive: the jump-to-next/previous remote actions, the section list in the prompter, and the progress scrubber's tick marks. Markers are **rendered as normal text by default**, with a setting to visually de-emphasise or hide them (§6.9).

### 3.3 Presets

A named bundle of typography + layout + scroll settings.

```kotlin
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val typographyJson: String,
    val layoutJson: String,
    val scrollJson: String,
    val isBuiltIn: Boolean = false
)
```

Ship three built-in presets:
- **Studio** — white on black, Lexend 72sp, leading 1.5, margins 12%, reading line 40%.
- **Bright room** — pure white on pure black, Atkinson Hyperlegible 88sp, weight 700, tracking +0.02em.
- **Tight glass** — for small beam-splitters: 56sp, leading 1.35, margins 22%, reading line 50%.

---

## 4. Settings model

All three settings blocks live in DataStore as the *global default*, and can be overridden per-script via `presetId`.

### 4.1 `TypographySettings`

```kotlin
data class TypographySettings(
    val fontId: String = "lexend",        // see §6.1
    val customFontUri: String? = null,    // SAF uri if fontId == "custom"
    val sizeSp: Float = 72f,              // 16f..200f
    val weight: Int = 500,                // 300..900, step 100
    val lineHeightMul: Float = 1.5f,      // 1.0f..2.5f
    val letterSpacingEm: Float = 0.0f,    // -0.05f..0.25f
    val paragraphSpacingEm: Float = 0.6f, // 0f..3f
    val textAlign: PromptAlign = LEFT,    // LEFT, CENTER
    val caseTransform: CaseMode = NONE,   // NONE, UPPER
    val textColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0xFF000000,
    val hyphenation: Boolean = false
)
```

### 4.2 `LayoutSettings`

```kotlin
data class LayoutSettings(
    val marginLeftPct: Float = 10f,       // 0f..40f, % of screen width
    val marginRightPct: Float = 10f,
    val marginTopPct: Float = 8f,         // 0f..40f, % of screen height
    val marginBottomPct: Float = 8f,
    val linkLeftRight: Boolean = true,    // symmetric margins toggle
    val maxMeasureCh: Int = 0,            // 0 = off; else cap line length in chars
    val readingLinePct: Float = 40f,      // 0f..90f from top
    val readingLineStyle: LineStyle = ARROWS,  // OFF, LINE, ARROWS, BAND
    val readingLineColor: Long = 0x66FF3B30,
    val edgeFadePct: Float = 8f,          // 0f..25f, fade at top & bottom
    val mirrorHorizontal: Boolean = false,
    val mirrorVertical: Boolean = false,
    val orientationLock: OrientLock = FOLLOW_SENSOR
)
```

### 4.3 `ScrollSettings`

```kotlin
data class ScrollSettings(
    val speedWpm: Int = 140,              // 40..400
    val speedMode: SpeedMode = WPM,       // WPM or PIXELS
    val speedPxPerSec: Float = 60f,       // used when speedMode == PIXELS
    val speedStepWpm: Int = 10,           // remote increment
    val rampMillis: Int = 350,            // ease-in on play
    val countdownSeconds: Int = 3,        // 0 = off
    val keepScreenOn: Boolean = true,
    val brightnessOverride: Float = -1f,  // -1 = system, else 0f..1f
    val endBehaviour: EndBehaviour = HOLD  // HOLD, LOOP, EXIT
)
```

---

## 5. Screens & flows

### 5.1 Library (start destination)

- Vertical list of scripts. Each row: title, first ~80 chars of body, word count, estimated duration at current WPM (e.g. "412 words · 2:56").
- Top app bar: app name, search icon, overflow (Settings, Import).
- FAB: **New script**.
- Row overflow: Rename, Duplicate, Delete, Assign preset, Export .txt.
- **Delete uses a snackbar with Undo** (5s) rather than a confirm dialog — faster and more forgiving.
- Drag handle for manual reordering (writes `sortIndex`).
- Empty state: illustration + "New script" + "Import .txt" buttons.
- Tapping a row → **Prompter** (not the editor). Reading is the common case. Long-press or the row's edit icon → Editor.

### 5.2 Editor

- Full-height `BasicTextField` in a monospace-optional editing font (editing legibility ≠ prompting legibility; keep the editor at a comfortable ~18sp regardless of prompter settings).
- Title field at top.
- Bottom bar: word count, est. duration, **Import .txt**, **Preview** (opens prompter with unsaved content), Save.
- Autosave on pause (500ms debounce) and on lifecycle stop. No explicit save required — but keep a Save affordance for reassurance.
- Insert-marker button inserts `## ` at line start.

### 5.3 Prompter (the core screen)

Full-screen, immersive (`WindowInsetsControllerCompat` → hide system bars, `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`), `FLAG_KEEP_SCREEN_ON` when `keepScreenOn`.

Layers, bottom to top:
1. Background colour fill.
2. Scrolling text surface (mirrored per settings).
3. Edge fade gradients (top + bottom).
4. Reading-line indicator.
5. Control bar + progress scrubber (auto-hiding).
6. Countdown overlay (when starting).
7. Quick-settings bottom sheet (on demand).

**Control bar** auto-hides 3s after last touch, reappears on any tap. Contains: back, play/pause, restart, speed −/+ with live WPM readout, font −/+, mirror toggle, settings (opens sheet). All targets ≥ 48dp.

**Progress scrubber**: thin bar along the bottom edge, always visible (even when controls hidden) at 30% opacity. Shows marker ticks. Draggable to seek.

**Quick-settings sheet**: a `ModalBottomSheet` at ~55% height so the text stays visible behind it and every change previews live on the real script. Tabs: **Type**, **Layout**, **Scroll**. This is the primary settings surface — the full Settings screen is only for global defaults and remote mapping.

### 5.4 Settings

Global defaults (same controls as the quick sheet), plus:
- **Remote & buttons** → Remote mapping screen (§8).
- **Presets** → manage/create/rename/delete.
- **Import/Export all** (JSON backup via SAF).
- **About** — include a line stating the app makes no network connections.

---

## 6. Typography system (detail)

This is the differentiator. Every control below is live, previewed on the actual script, and reachable from the prompter without leaving it.

### 6.1 Fonts

Bundle four open-licence families as `res/font` resources, chosen for legibility at distance:

| id | Family | Licence | Why |
|---|---|---|---|
| `lexend` | Lexend | OFL | Designed to reduce visual stress; excellent default. |
| `atkinson` | Atkinson Hyperlegible | OFL | Maximally distinct letterforms; best for low light / poor eyesight. |
| `inter` | Inter | OFL | Neutral, tall x-height, very even colour. |
| `newsreader` | Newsreader | OFL | A serif option; some readers track lines better with serifs. |
| `system` | Device default | — | |
| `custom` | User-imported | — | `.ttf`/`.otf` via SAF; persist URI + take persistable read permission. |

Prefer variable fonts where available so `weight` is continuous.

**Font picker UI:** a list where **each row renders its own name in its own face** at ~28sp. No abstract names without specimens.

### 6.2 Size

- Range **16–200sp**, slider + `−`/`+` steppers (step 2sp), plus a numeric readout you can tap to type an exact value.
- Pinch-to-zoom on the prompter surface adjusts size directly.
- **Critical:** the prompter text must *not* be multiplied by the system font scale. A user who sets 72sp expects 72sp. Wrap the prompter surface:

```kotlin
val density = LocalDensity.current
CompositionLocalProvider(
    LocalDensity provides Density(density.density, fontScale = 1f)
) {
    PrompterSurface(...)
}
```
The rest of the app's chrome *does* respect system font scale.

### 6.3 Line height (leading)

Multiplier **1.0–2.5**, step 0.05, default **1.5**. This is the single most impactful teleprompter setting after size and is missing from most apps.

```kotlin
lineHeight = (sizeSp * lineHeightMul).sp,
lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)
```
`Trim.None` matters: it keeps the first and last line's leading intact so spacing stays even as text scrolls past the reading line.

### 6.4 Tracking (letter spacing)

**−0.05em to +0.25em**, step 0.005, default 0. Slight positive tracking measurably helps at distance and in mirrored text.

### 6.5 Weight

300–900 in steps of 100. Render as a segmented row showing "Light / Regular / Medium / Bold / Black" *in that weight*. Default 500 — 400 tends to thin out on a beam splitter, 700 blooms.

### 6.6 Paragraph spacing

Extra space between paragraphs, **0–3.0em** of font size, default 0.6em. Implemented as `Modifier.padding(bottom = …)` on each paragraph item, not as blank lines in the text.

### 6.7 Measure (line length)

Optional cap on characters per line (`maxMeasureCh`, 0 = off, else 20–90). Long lines are the classic teleprompter failure: the eye loses the return sweep. When set, constrain the text column width to `maxMeasureCh × averageCharWidth`, centred within the margins.

### 6.8 Alignment & case

- Align: **Left** (default) or **Centre**. Do **not** offer justified — it creates rivers and uneven word spacing, which is actively harmful when scrolling.
- Case: **None** (default) or **UPPERCASE**. Include a one-line hint in the UI: *"All caps is traditional but slower to read — try bold instead."* Give people the option, tell them the tradeoff.

### 6.9 Colour & contrast

- Text and background colour pickers with presets: white-on-black (default), black-on-white, amber-on-black, yellow-on-black, grey-on-black (reduced glare).
- Live **contrast ratio readout** (WCAG formula) next to the picker; warn below 7:1.
- Optional screen brightness override (`window.attributes.screenBrightness`).

### 6.10 Marker styling

Setting: markers rendered **Normal / Dimmed (50% opacity) / Accent colour / Hidden**. Hidden still keeps them as jump targets.

---

## 7. Layout & margins system (detail)

### 7.1 Margins

Four independent values as **percentages of screen dimension**, not dp — because the constraint is the beam-splitter's crop, which is proportional.

- Left / Right: 0–40% of width each. A **link toggle** (default on) keeps them symmetric; unlink for off-centre glass.
- Top / Bottom: 0–40% of height each.
- Each with slider + numeric readout + stepper.

### 7.2 Safe-area preview

A toggle in the Layout tab draws a dashed rectangle showing the text area with the current margins, over the live text, plus corner labels showing the resulting dp values. Turn it on, hold the phone in the rig, adjust until the box matches the visible glass. This is the fastest possible calibration loop and it's what makes margins genuinely usable rather than trial-and-error.

### 7.3 Reading-line indicator

The fixed point on screen where the "current" line sits.

- Position: **0–90% from top**, default 40%.
- Styles: **Off / Thin line / Arrows (◀ ▶ at both edges) / Band (subtle translucent bar)**.
- Colour + opacity configurable; default translucent red.
- Arrows sit *outside* the text margins so they never overlap words.

### 7.4 Edge fade

Linear alpha gradient at top and bottom, height 0–25% of screen (default 8%). Softens text entering and leaving the frame, which reduces the "popping" that pulls the eye.

```kotlin
Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = size.height * edgeFadePct / 100f
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black, (h / size.height) to Color.Transparent,
                1f - (h / size.height) to Color.Transparent, 1f to Color.Black
            ),
            blendMode = BlendMode.DstOut
        )
    }
```

### 7.5 Mirroring

```kotlin
Modifier.graphicsLayer {
    scaleX = if (mirrorHorizontal) -1f else 1f
    scaleY = if (mirrorVertical) -1f else 1f
}
```

Two consequences to handle explicitly:
- **Gestures invert.** Apply the same sign flip to drag deltas consumed from the mirrored surface, or (cleaner) put the gesture detector on a non-mirrored overlay above the mirrored content. Do the latter.
- **Vertical mirror reverses apparent scroll direction.** When `mirrorVertical` is on, the scroll engine's direction must flip so text still reads top-to-bottom through the glass.

Expose both toggles plus a combined "Beam splitter" quick preset in the control bar.

### 7.6 Orientation

`FOLLOW_SENSOR` (default) / `LOCK_LANDSCAPE` / `LOCK_PORTRAIT`. Rigs are usually landscape; locking prevents a rotation mid-take. Layout must fully recompose on rotation without losing scroll position — store position as a *fraction of total content height*, not pixels, so it survives re-layout.

---

## 8. Scroll engine (detail)

### 8.1 Speed in words per minute

Expressing speed as "1–100" is the reference app's worst UX sin: it means nothing, and it changes meaning whenever font size changes. Use **WPM**, and derive pixels/second from the *actual laid-out content*:

```
pxPerSecond = (wpm / 60f) * (totalContentHeightPx / wordCount)
```

This is self-calibrating: raise the font size and the content gets taller, so px/s rises automatically and the *reading pace stays identical*. That is the correct behaviour and it falls out for free.

Get `totalContentHeightPx` by measuring once with `TextMeasurer` whenever style, width, or body changes:

```kotlin
val measurer = rememberTextMeasurer()
val contentHeight by remember(body, style, columnWidthPx) {
    derivedStateOf {
        measurer.measure(
            text = AnnotatedString(body),
            style = style,
            constraints = Constraints(maxWidth = columnWidthPx)
        ).size.height
    }
}
```
For very long scripts do this in a `LaunchedEffect` on `Dispatchers.Default` and cache by `(bodyHash, styleHash, width)`.

Offer a **Pixels/sec** mode for users who prefer absolute control (`speedMode`), but default to WPM. Show the *other* unit as a subtitle so the mapping is learnable.

Also show **estimated total duration** and **time remaining** live.

### 8.2 The frame loop

Smoothness is the whole product. Drive scrolling off the frame clock and carry sub-pixel remainder — this is the difference between silky and stuttery, because `LazyListState` quantises its offset to whole pixels.

```kotlin
LaunchedEffect(isPlaying, pxPerSecond, direction) {
    if (!isPlaying) return@LaunchedEffect
    var lastFrame = withFrameNanos { it }
    var carry = 0f
    val rampStart = lastFrame

    while (isActive) {
        val now = withFrameNanos { it }
        val dtSec = (now - lastFrame) / 1_000_000_000f
        lastFrame = now

        // ease-in so the start isn't a jerk
        val rampT = ((now - rampStart) / 1_000_000f / rampMillis).coerceIn(0f, 1f)
        val eased = rampT * rampT * (3f - 2f * rampT)   // smoothstep

        val delta = pxPerSecond * eased * dtSec * direction + carry
        val whole = delta.toInt()
        carry = delta - whole

        if (whole != 0) {
            val consumed = listState.scrollBy(whole.toFloat())
            if (consumed == 0f && whole > 0) {
                onReachedEnd()          // HOLD / LOOP / EXIT
                break
            }
        }
    }
}
```

Guard `dtSec` against pathological frames: `dtSec.coerceAtMost(0.05f)`.

### 8.3 Rendering long scripts

Use `LazyColumn` with **one item per paragraph**, not a single giant `Text`. Compose degrades badly on very large single text nodes (measurement cost, and a hard glyph limit). Paragraph items keep measurement incremental.

```kotlin
val paragraphs = remember(body) { body.split("\n") }   // keep empty lines as spacers
LazyColumn(state = listState, userScrollEnabled = true) {
    item { Spacer(Modifier.height(readingLineOffsetDp)) }   // lead-in so line 1 starts at the reading line
    itemsIndexed(paragraphs) { i, para -> ParagraphText(para, style, spacing) }
    item { Spacer(Modifier.height(trailingSpaceDp)) }       // lead-out so the last line can reach the reading line
}
```

The lead-in and lead-out spacers are essential and commonly forgotten: without them the first line starts at the top edge and the last line can never reach the reading marker.

### 8.4 Transport controls

- **Play/pause** — toggles the loop; pausing preserves `carry`.
- **Restart** — animate to offset 0.
- **Nudge** — ±1 line (`sizeSp × lineHeightMul` px), for small corrections.
- **Jump marker** — animated scroll to next/previous `Marker`.
- **Manual scrub** — dragging the surface pauses auto-scroll and shows a "paused" chip; a Resume button reappears. Do not silently resume.
- **Countdown** — 3-2-1 overlay before play (configurable 0–10s), large centred numerals.
- **Blackout** — instantly fills the screen black and pauses, one tap to restore. Genuinely useful when the talent needs the glass dark between takes; map it to a remote button.

---

## 9. Remote control subsystem (detail)

### 9.1 How the RT02 remote almost certainly works

Static and dynamic analysis of the bundled app showed **no Bluetooth permissions of any kind** and no BLE/GATT code. The only way it can be receiving remote input is as an **OS-level Bluetooth HID keyboard**: you pair it in Android's Bluetooth settings, and it delivers ordinary `KeyEvent`s to whatever app has focus.

That's good news — it means:
- **No permissions needed.**
- No pairing UI to build; we just consume key events.
- Any HID remote works, not just the Ulanzi one.

**Caveat, stated plainly:** I could not confirm the RT02's specific keycodes, because that needs the physical remote. The spec therefore does not hardcode them — it *learns* them (§9.4), which is what you asked for anyway and is strictly more robust.

Typical HID remotes send some subset of: `KEYCODE_VOLUME_UP` / `VOLUME_DOWN`, `KEYCODE_PAGE_UP` / `PAGE_DOWN`, `KEYCODE_DPAD_UP` / `DOWN` / `LEFT` / `RIGHT`, `KEYCODE_DPAD_CENTER`, `KEYCODE_ENTER`, `KEYCODE_SPACE`, `KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_HEADSETHOOK`. Camera-shutter modes usually map to `VOLUME_UP`.

### 9.2 Actions

```kotlin
enum class PromptAction {
    PLAY_PAUSE, PLAY, PAUSE,
    SPEED_UP, SPEED_DOWN,
    FONT_UP, FONT_DOWN,
    NUDGE_UP, NUDGE_DOWN,
    NEXT_MARKER, PREV_MARKER,
    RESTART, JUMP_END,
    TOGGLE_MIRROR, BLACKOUT,
    NEXT_SCRIPT, PREV_SCRIPT,
    EXIT, NONE
}
```

**Default mapping** (applies to whatever a generic remote sends):

| Key | Action |
|---|---|
| `VOLUME_UP`, `PAGE_UP`, `DPAD_UP` | `SPEED_UP` |
| `VOLUME_DOWN`, `PAGE_DOWN`, `DPAD_DOWN` | `SPEED_DOWN` |
| `DPAD_CENTER`, `ENTER`, `SPACE`, `MEDIA_PLAY_PAUSE`, `HEADSETHOOK` | `PLAY_PAUSE` |
| `DPAD_RIGHT` | `NEXT_MARKER` |
| `DPAD_LEFT` | `PREV_MARKER` |
| `BACK` | `EXIT` (never remappable) |

### 9.3 Binding model & routing

Bindings are stored **per input device** so a phone's own volume rocker and the remote can behave differently.

```kotlin
data class KeyBinding(
    val deviceDescriptor: String?,  // InputDevice.getDescriptor(); null = any device
    val keyCode: Int,
    val longPress: Boolean = false,
    val action: PromptAction
)
```

Routing lives in the prompter Activity:

```kotlin
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)

    val handled = remoteKeyRouter.handle(event)
    return if (handled) true else super.dispatchKeyEvent(event)
}
```

```kotlin
class RemoteKeyRouter(
    private val store: KeyBindingStore,
    private val onAction: (PromptAction) -> Unit
) {
    fun handle(event: KeyEvent): Boolean {
        // Ignore the soft keyboard; only accept real key devices.
        val src = event.source
        val isKeyboardish = src and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD ||
                            src and InputDevice.SOURCE_DPAD     == InputDevice.SOURCE_DPAD ||
                            src and InputDevice.SOURCE_GAMEPAD  == InputDevice.SOURCE_GAMEPAD

        val descriptor = event.device?.descriptor
        val binding = store.resolve(descriptor, event.keyCode, longPress = event.isLongPress)
            ?: return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                // repeatCount > 0 means held: allow repeat only for continuous actions
                if (event.repeatCount == 0 || binding.action.isRepeatable) {
                    onAction(binding.action)
                }
                return true
            }
            KeyEvent.ACTION_UP -> return true   // swallow so volume UI never appears
        }
        return false
    }
}

private val PromptAction.isRepeatable: Boolean
    get() = this in setOf(SPEED_UP, SPEED_DOWN, FONT_UP, FONT_DOWN, NUDGE_UP, NUDGE_DOWN)
```

**Returning `true` on `ACTION_UP` is important** — otherwise a remapped `VOLUME_UP` still pops the system volume slider over your script.

### 9.4 The "learn button" flow

Remote mapping screen: a list of every `PromptAction` with its currently bound key(s). Tapping one opens a dialog:

> **Press a button on your remote**
> *Waiting…* &nbsp;&nbsp; [Cancel]

The dialog's host view takes focus and captures the next `KeyEvent`:

```kotlin
@Composable
fun KeyCaptureDialog(onCaptured: (KeyEvent) -> Unit, onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { ke ->
                val native = ke.nativeKeyEvent
                if (native.action == android.view.KeyEvent.ACTION_DOWN &&
                    native.keyCode != android.view.KeyEvent.KEYCODE_BACK) {
                    onCaptured(native); true
                } else false
            }
    ) { /* dialog content */ }
}
```

On capture, show what was detected before committing:

> Detected: **Volume Up** (keycode 24) from *Ulanzi Remote*
> [Bind to Speed Up] [Try again]

Naming the device (`event.device?.name`) confirms the press came from the remote rather than the phone's own buttons — a small touch that removes a whole class of confusion.

Also support **long-press bindings**: after capture, a "Long press" checkbox lets one physical button carry two actions.

### 9.5 Key sniffer (developer/diagnostic screen)

A hidden screen (Settings → About → tap version 5×) that logs every incoming `KeyEvent`: keycode, name via `KeyEvent.keyCodeToString()`, scancode, device name, descriptor, source flags, repeat count.

Use this on day one with the real RT02 remote to discover exactly what it sends, then fold sensible defaults into §9.2. Outside the app, `adb shell getevent -lt` does the same job.

### 9.6 Behaviour outside the prompter

Remote keys are active on the Prompter screen only. On Library/Editor they pass through normally (so volume works as volume). Exception: if a remote play/pause arrives on Library, start prompting the selected script — a nice touch, low cost.

---

## 10. Gestures (prompter surface)

| Gesture | Action |
|---|---|
| Single tap | Toggle control bar visibility |
| Double tap | Play/pause |
| Vertical drag | Manual scrub (pauses auto-scroll) |
| Pinch | Font size |
| Two-finger vertical drag | Speed adjust (live WPM readout) |
| Long press | Blackout (hold), release restores |
| Swipe from top edge | System bars (default Android behaviour) |

Every gesture has a button equivalent. Gestures are accelerators, never the only route.

---

## 11. Import / export

### 11.1 Import

`ACTION_OPEN_DOCUMENT` (not `GET_CONTENT` — it gives persistable permissions), MIME `text/plain` plus `text/markdown`, `*/*` fallback.

**Charset detection** — the reference app's approach was actually sound; reimplement it properly:
1. Read the first 3 bytes. `FF FE` → UTF-16LE, `FE FF` → UTF-16BE, `EF BB BF` → UTF-8 (strip BOM).
2. No BOM → attempt strict UTF-8 decode (`CharsetDecoder` with `REPORT` on malformed). Success → UTF-8.
3. Failure → fall back to the system default, then GBK. Show a "Text looks garbled? Change encoding" chip that lets the user re-decode with a picker.

**Preserve newlines.** Read with `bufferedReader().readText()`, not a line loop that drops separators.

Title defaults to the filename minus extension; editable before saving.

### 11.2 Export

- Single script → `.txt` via `ACTION_CREATE_DOCUMENT`.
- Full backup → JSON (scripts + presets + key bindings) via `ACTION_CREATE_DOCUMENT`; matching restore.

---

## 12. Accessibility

- All interactive elements have `contentDescription`; icon-only buttons must be labelled.
- Touch targets ≥ 48×48dp.
- App chrome respects system font scale; prompter text deliberately does not (§6.2) — document this in Settings with a one-line explanation.
- Contrast readout on colour pickers (§6.9).
- Full functionality reachable without gestures.
- Respect `Settings.Global.ANIMATOR_DURATION_SCALE` for UI transitions; the prompter scroll itself is content, not animation, so it is exempt.
- Atkinson Hyperlegible bundled specifically as a low-vision option.

---

## 13. Performance requirements

- **Sustained 60fps (or display refresh) during scroll.** This is the acceptance bar; verify with Macrobenchmark `FrameTimingMetric`, P99 frame time under budget.
- Text measurement off the main thread, cached by `(bodyHash, styleHash, widthPx)`.
- No allocation inside the frame loop.
- Enable R8 for release.
- Target: script of 10,000 words scrolls without jank on a mid-range device.

---

## 14. Build configuration

`gradle/libs.versions.toml` essentials:

```toml
[versions]
kotlin = "2.1.0"
agp = "8.7.0"
composeBom = "2025.01.00"
room = "2.6.1"
datastore = "1.1.1"
navigation = "2.8.5"
lifecycle = "2.8.7"

[libraries]
compose-bom        = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-material3  = { module = "androidx.compose.material3:material3" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
activity-compose   = { module = "androidx.activity:activity-compose", version = "1.9.3" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
lifecycle-vm       = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
room-runtime       = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx           = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler      = { module = "androidx.room:room-compiler", version.ref = "room" }
datastore-prefs    = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

Manifest skeleton:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Intentionally NO permissions. Especially not INTERNET. -->
    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:theme="@style/Theme.Prompter">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden|locale|layoutDirection"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Note `allowBackup="false"` — the reference app had it `true`, sweeping script content into cloud backups. Off by default here; we provide explicit export instead.

Single-activity architecture: one `MainActivity` hosting Compose navigation, so `dispatchKeyEvent` has a single home.

---

## 15. Build order (milestones)

**M1 — Skeleton (reading works)**
Room + DataStore, Library list, Editor, Prompter with fixed styling and a working frame-loop scroll at a fixed WPM. Nothing configurable yet. *Goal: read a script end to end.*

**M2 — Typography**
Font picker with specimens, size, leading, tracking, weight, colour. Quick-settings sheet with live preview. Preset save/load.

**M3 — Layout**
Margins with safe-area preview, reading line, edge fade, mirroring, orientation lock.

**M4 — Remote**
Key sniffer first (discover what the RT02 sends), then router, defaults, learn-button flow, per-device profiles. *Test with the real remote before finalising defaults.*

**M5 — Polish**
Countdown, blackout, markers + jump, progress scrubber, import/export, resume position, empty states, accessibility pass, Macrobenchmark.

---

## 16. Acceptance criteria

- [ ] Merged manifest contains zero `uses-permission` entries; CI enforces no `INTERNET`.
- [ ] A 10,000-word script scrolls at a sustained 60fps with no visible stutter.
- [ ] Changing font size does **not** change reading pace (WPM holds).
- [ ] Imported `.txt` preserves every newline and paragraph break.
- [ ] Imported GBK and UTF-16 files render correctly.
- [ ] Every remote button can be rebound to any action, and the binding survives app restart.
- [ ] A remapped volume key never shows the system volume UI.
- [ ] Two different remotes can hold different mappings simultaneously.
- [ ] Rotating the device preserves reading position (as a content fraction).
- [ ] Mirrored mode: gestures move text in the direction the user physically drags.
- [ ] Text is legible at 2m in a bright room using the "Bright room" preset.
- [ ] All controls reachable without gestures; TalkBack can drive the whole app.

---

## 17. Open questions

1. **RT02 remote keycodes** — needs the physical remote. Run the key sniffer (§9.5) or `adb shell getevent -lt` and record: how many buttons, what each sends, whether it's HID keyboard or BLE GATT. If it turns out to be **GATT rather than HID**, §9 changes substantially: we'd need `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN` permissions and a GATT client, and the "no permissions" property is lost. Confirm this early — it's the one finding that could move the design.
2. **Does the remote auto-repeat when held?** Determines whether hold-to-accelerate works for free.
3. **Beam-splitter orientation** — confirm whether the RT02 needs horizontal mirror, vertical, or both, and set the default accordingly.
4. **Glass aspect/crop** — measuring the actual visible area would let us ship a correct default margin preset rather than 10%.
