# Prompter

An Android teleprompter that is *pleasant to read from*.

Most teleprompter apps treat typography and layout as afterthoughts: one font, one size slider, no margin
control, no line-height control, and a scroll speed expressed as a meaningless integer from 1 to 100. Those
are exactly the things that decide whether a read is smooth or a stumble.

> **Status: early development.** The build is in progress and the app is not yet usable. See
> [`docs/BUILD_PLAN.md`](docs/BUILD_PLAN.md) for the staged plan and current position.

## Three pillars

**Typography that serves reading at distance.** Size, leading, tracking, weight, measure, and contrast are
all first-class, all live-adjustable, and all previewed on the real text while you read it.

**Layout control for beam-splitter glass.** A teleprompter rig crops and mirrors your phone's display.
Margins, safe areas, and the reading-line indicator are user-controlled rather than guessed — including a
safe-area preview you can calibrate against the actual glass in seconds.

**A remote that does what you want.** Full button remapping, learned from whatever your remote actually
sends, stored per device so two remotes can hold different mappings at once.

## Speed is measured in words per minute

Scroll speed is expressed in WPM and derived from the laid-out content:

```
pixelsPerSecond = (wpm / 60) * (totalContentHeightPx / wordCount)
```

This is self-calibrating. Raise the font size, the content gets taller, pixels-per-second rises to match, and
**your reading pace stays identical**. A number on the screen means something.

## No network. At all.

Prompter declares **no permissions whatsoever** — no `INTERNET`, and nothing that implies it. Your scripts
cannot leave the device, because the app has no mechanism to send them anywhere. This is not a policy
promise; it is enforced by the kernel.

It is also enforced by CI. Every pull request runs a check that inspects the merged manifest and fails the
build if a single `uses-permission` entry appears. The guarantee cannot silently rot.

Cloud backup is off (`allowBackup="false"`), so script content is never swept into a Google account backup.
Explicit export is provided instead.

## Building

Requires JDK 21 and the Android SDK (compileSdk 36).

```bash
git clone https://github.com/paulchibamba/prompter.git
cd prompter
./gradlew :app:assembleDebug
```

Open in Android Studio, or install with `./gradlew :app:installDebug`.

Run the checks:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug verifyNoInternetPermission
```

## Documentation

| File | What it covers |
|---|---|
| [`docs/SPEC.md`](docs/SPEC.md) | The full specification — every setting, screen, and acceptance criterion |
| [`docs/BUILD_PLAN.md`](docs/BUILD_PLAN.md) | The 25-step staged build plan |
| `docs/ARCHITECTURE.md` | Layers, packages, dependency rule, code style *(arrives in Step 2)* |
| [`docs/PROGRESS.md`](docs/PROGRESS.md) | Which steps are done, and any deviations from the plan |

## Licence

Source code is licensed under the [Apache License 2.0](LICENSE).

Bundled typefaces remain under their own SIL Open Font License, with each family's `OFL.txt` vendored
alongside it in `third_party/fonts/`:

- **Lexend** — designed to reduce visual stress
- **Atkinson Hyperlegible** — maximally distinct letterforms, by the Braille Institute
- **Inter** — neutral, tall x-height, very even colour
- **Newsreader** — a serif option, since some readers track lines better with serifs
