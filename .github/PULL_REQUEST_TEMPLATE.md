## What this changes

<!-- One or two sentences. If this is a build-plan step, name it: "Step 07 — Library screen". -->

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
