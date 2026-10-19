## Context

See proposal.md — Why. Two independent pieces must change, both scoped to oCIS:

1. **`Share` reachability** lives in `FilterFileMenuOptionsUseCase` (owncloudApp), a use case shared by every entry point that builds the file context menu (`MainFileListViewModel`, `FileDetailsViewModel`, and the four preview ViewModels). Its current `SHARE` gate — `!onlyAvailableOfflineFiles && shareViaLinkOrWithUsersAllowed && resharingAllowed && hasResharePermission` — was designed for oC10. Because it is shared, any relaxation must be guarded so oC10 keeps the exact same gate.
2. **The permalink button** in the oCIS Share header exists in `members_activity.xml` as `permanentLinkButton`, but `ShareActivity.setupFileHeader()` hard-codes it hidden (`ShareActivity.kt:172`) and sets no listener. oC10 uses a different screen (`OcsShareFileFragment`) where the private link already works via `showOrHidePrivateLink()`.

Relevant existing facts the design relies on:
- `OCCapability.isSpacesAllowed()` (`spaces?.enabled == true`) is the established oCIS/oC10 discriminator across the app.
- `ShareActivity.onCreate()` fetches the stored `OCCapability` once via `capabilityViewModel.getStoredCapabilities()`, branches `setupOcisLayout()` vs `setupLegacyLayout()` on `isSpacesAllowed()`, and passes the capabilities down to `setupFileHeader()`, which runs only on the oCIS branch.
- `ShareActivity` already overrides `copyOrSendPrivateLink(file)` → `fileOperationsHelper.copyOrSendPrivateLink(file)`.
- oC10's rule (`OcsShareFileFragment.showOrHidePrivateLink()`): hide when `file.privateLink.isNullOrEmpty()` or `filesPrivateLinks == CapabilityBooleanType.FALSE`.
- `FilterFileMenuOptionsUseCase` already fetches `capability = capabilityRepository.getStoredCapabilities(params.accountName)`.

## Goals / Non-Goals

**Goals:**
- Make `Share` unconditionally reachable for a single, non-available-offline selection on oCIS, without changing the oC10 gate.
- Make the oCIS permalink button visible and functional under the same show/hide rule oC10 already uses.

**Non-Goals:**
- No change to oC10 (`FilterFileMenuOptionsUseCase` oC10 branch, `OcsShareFileFragment`, `share_file_layout.xml`).
- No change to `FilterFileMenuOptionsUseCase.Params`, its callers, or its Koin factory.
- No change to the section-level gating inside the Share screen.
- No data/domain/network layer changes.

## Decisions

### D1: Branch the `SHARE` gate by server type inside the use case
Reuse the already-fetched `capability`: `val isOcis = capability?.isSpacesAllowed() == true`. Then:

```kotlin
val shareReachable = if (isOcis) {
    !onlyAvailableOfflineFiles && isSingleSelection(files)
} else {
    !onlyAvailableOfflineFiles && shareViaLinkOrWithUsersAllowed &&
        resharingAllowed && hasResharePermission
}
if (shareReachable) optionsToShow.add(FileMenuOption.SHARE)
```

- **Why in the use case** rather than at the call sites: the gate and all its inputs already live here; the six callers pass identical `Params` and would otherwise each need the oCIS branch duplicated.
- **Why keep the oC10 branch verbatim**: the change must be behavior-preserving for oC10. Consequently `resharingAllowed`, `hasResharePermission`, `anyFileSharedWithMe`, `shareViaLinkOrWithUsersAllowed`, the `capability` lookup, the `capabilityRepository` dependency, and the `shareViaLinkAllowed`/`shareWithUsersAllowed` `Params` fields are all retained (still read by the oC10 branch), so detekt's zero-tolerance "unused" rules stay satisfied and no caller/Koin edits are needed.
- **Alternative rejected**: removing the oC10-only inputs and reducing the gate to `!onlyAvailableOfflineFiles && isSingleSelection(files)` for everyone. Rejected because it changes oC10 behavior (an item "shared with me" with resharing disabled would newly show `Share`), which is explicitly out of scope.

### D2: Implement the oCIS permalink button in `ShareActivity.setupFileHeader()`
Replace the hard-coded `binding.permanentLinkButton.isVisible = false` with the oC10 rule inside a single `binding.permanentLinkButton.apply { … }` block, wiring the click to the existing override and a long press to the info toast:

```kotlin
val privateLinkDisabled = capabilities?.filesPrivateLinks == CapabilityBooleanType.FALSE
binding.permanentLinkButton.apply {
    if (file.privateLink.isNullOrEmpty() || privateLinkDisabled) {
        isVisible = false
    } else {
        isVisible = true
        setOnClickListener { copyOrSendPrivateLink(file) }
        setOnLongClickListener {
            Toast.makeText(this@ShareActivity, R.string.private_link_info, Toast.LENGTH_LONG).show()
            true
        }
    }
}
```

- **Why here**: `setupFileHeader()` already runs only on the oCIS layout branch, so no extra oCIS guard is required; the button and the `copyOrSendPrivateLink` plumbing already exist.
- **Why mirror oC10's rule** instead of inventing one: keeps permalink behavior consistent across both screens — hidden when no link or `filesPrivateLinks == FALSE`, click copies/sends the private link, and long press shows the `R.string.private_link_info` toast, exactly as `OcsShareFileFragment.showOrHidePrivateLink()` does.
- **Capability access**: a thin read-only `getStoredCapabilities()` getter (backed by `GetStoredCapabilitiesUseCase`) was added to `CapabilityViewModel`, replacing the previous `isOcisServer()` boolean accessor. `onCreate()` reads the stored `OCCapability` once through it, branches on `isSpacesAllowed()`, and passes the capabilities into `setupFileHeader(binding, capabilities)`; `filesPrivateLinks` is read from that parameter rather than re-querying Room or the view model inside the method.
- **Reference**: `SpaceMembersActivity.kt:74` wires the same button via `setOnClickListener`.

## Risks / Trade-offs

- [oC10 regression from the shared use case] → Guard the relaxation behind `isOcis` so the oC10 branch stays byte-for-byte unchanged; validated by the manual oC10 regression QA cases in `tasks.md` (item shared-with-me, resharing false, no `R` → `Share` absent).
- [`capability` is null when stored capabilities are missing] → `capability?.isSpacesAllowed() == true` evaluates to `false`, so the account falls back to the oC10 branch — the pre-change behavior, which is the safe default.
- [Detekt flags a now-unused symbol] → None expected, because the oC10 branch keeps reading every retained symbol; verify with `./gradlew detekt` during apply.

## Open Questions

- ~~Which existing `CapabilityViewModel` accessor returns the stored `OCCapability` (or specifically `filesPrivateLinks`) for the account, or whether a small read-only getter must be added.~~ **Resolved:** no existing accessor returned the full `OCCapability`, so a read-only `getStoredCapabilities()` getter was added to `CapabilityViewModel`, backed by `GetStoredCapabilitiesUseCase`; `onCreate()` calls it once and passes the result into `setupFileHeader()`. The former `isOcisServer()` boolean accessor was removed, as `getStoredCapabilities()?.isSpacesAllowed()` now covers both the layout branch and the permalink rule.