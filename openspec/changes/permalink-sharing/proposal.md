## Why

On oCIS the permalink can become unreachable. The `Share` screen is the only entry point to it, and the `FileMenuOption.SHARE` gate in `FilterFileMenuOptionsUseCase` — designed for oC10 — hides `Share` whenever the `resharing` capability is `false` (always the case on oCIS) or the item carries no `R` permission (empty `oc:permissions` in custom spaces). Even when `Share` is reachable, the oCIS header's `permanent_link_button` is hard-coded hidden and wired to nothing, so the permalink is never shown. oC10 is unaffected and must stay exactly as it is.

## What Changes

- On oCIS accounts (identified by the `spaces` capability, `OCCapability.isSpacesAllowed()`), the `Share` menu option becomes reachable for any single selected item, independent of the `resharing`/`sharing` capabilities, the `R` permission, and the `share_via_link_feature`/`share_with_users_feature` setup flags. It stays hidden in the available-offline shortcut and for multiselection.
- On oC10 the `Share` gate is left unchanged (no observable difference).
- On oCIS, the `permanent_link_button` in the Share header (`members_activity.xml`, driven by `ShareActivity.setupFileHeader()`) is made visible and wired to `copyOrSendPrivateLink(file)`, shown only when the item has a private link and the `filesPrivateLinks` capability is not `FALSE` — mirroring oC10's `showOrHidePrivateLink()` and the `SpaceMembersActivity` reference.
- The capability/feature-flag gating of the "share with users" and "share by link" sections *inside* the Share screen is untouched on both server types.

## Capabilities

### New Capabilities
- `files-sharing/permalink`: Reachability of the `Share` screen on oCIS and the display/behavior of the permalink (private link) button in the oCIS Share header, including the show/hide rule and the copy/send action.

### Modified Capabilities
<!-- None: no existing capability spec covers Share-menu reachability or the permalink button. -->

## Impact

- **owncloudApp** (presentation):
  - `usecases/files/FilterFileMenuOptionsUseCase.kt` — split the `SHARE` gate by server type; the oCIS branch reduces to `!onlyAvailableOfflineFiles && isSingleSelection(files)`, the oC10 branch is unchanged. All existing inputs (`capabilityRepository`, `resharingAllowed`, `hasResharePermission`, the `shareViaLinkAllowed`/`shareWithUsersAllowed` `Params`) are retained because the oC10 branch still needs them — no caller or Koin changes.
  - `presentation/sharing/ShareActivity.kt` — `setupFileHeader()` shows and wires `permanentLinkButton`.
  - No unit tests are added: the change touches only the presentation layer (owncloudApp), where unit tests are not established per project convention. The oCIS reachability and permalink behavior are covered by the manual QA cases in `tasks.md`.
- **owncloudDomain / owncloudData / owncloudComLibrary**: no changes. `OCFile.privateLink` is already populated from the `oc:privatelink` PROPFIND property and `OCCapability.isSpacesAllowed()` already exists.
- **Changelog**: new `Bugfix` entry under `changelog/unreleased/`.
- No new dependencies.