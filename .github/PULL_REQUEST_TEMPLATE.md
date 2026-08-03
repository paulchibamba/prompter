## What this changes

<!-- One or two sentences describing the change itself. Title it after what it does —
     "Library screen", "Scroll engine" — not after a build-plan step number. -->

## How it was verified

<!-- Device and Android version, and what you actually exercised. "Builds" is not verification. -->

- [ ] `./gradlew :app:assembleDebug` passes
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] Installed and exercised on a device

## Checklist

- [ ] No new permissions (`verifyNoInternetPermission` still passes)
- [ ] `domain/` still imports nothing from `data/`, `ui/`, or `android.*`
- [ ] New `domain/` logic has unit tests
- [ ] `docs/PROGRESS.md` updated, including any deviation from the plan
