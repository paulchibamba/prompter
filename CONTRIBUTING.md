# Contributing

Thanks for looking. Prompter is in early development, so the fastest way to help right now is to try things
and report what breaks.

## Before you start

Read [`docs/SPEC.md`](docs/SPEC.md) for what the app is meant to do, and
[`docs/BUILD_PLAN.md`](docs/BUILD_PLAN.md) for what is being built when. The rules below are binding — a PR
that violates the dependency rule will be asked to change.

## The dependency rule

Clean architecture, three layers:

- `domain/` is **pure Kotlin**. It imports nothing from `data/`, `ui/`, or `android.*`. Ever.
- `data/` depends on `domain/` and implements its repository interfaces.
- `ui/` depends on `domain/` only. It never touches a Room entity or a DataStore key.

## Code style

- **Names describe what the code does.** `observeScriptsOrderedByRecency()`, not `getAll()`.
- **Many small functions.** Six named five-line functions beat one thirty-line function.
- **Composables are small and single-purpose**, named for what they render.
- Comments explain *why*, not *what*. If a comment explains what the code does, rename the code instead.

## Non-negotiables

**No new permissions.** Prompter declares none, and `verifyNoInternetPermission` runs in CI to keep it that
way. A PR that adds a permission needs to argue for it in the description before anything else.

**Preserve newlines when reading text.** Use `bufferedReader().readText()`, never a line loop that drops
separators. This is a real bug in a real teleprompter app and it collapses every script into one paragraph.

**Never let the system font scale touch prompter text.** A user who sets 72sp expects 72sp. App chrome does
respect font scale; the prompter surface deliberately does not.

## Pull requests

- One logical change per PR, branched from `main`.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest` must pass locally, and CI must be green.
- Add unit tests for anything in `domain/` — it is pure Kotlin, so there is no excuse not to.
- Describe what you changed and how you verified it on a device.

## Reporting bugs

Include your device, Android version, and — if it is a scrolling or rendering issue — the script length,
font size, and WPM you were using. For remote-control issues, the output of the in-app key sniffer
(Settings → About → tap the version five times) is the single most useful thing you can attach.
